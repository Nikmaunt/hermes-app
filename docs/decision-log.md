# Hermes — Decision Log

Key engineering decisions with rationale. Newest milestone last. This is the
"why", meant to survive the authors forgetting; the "what" is in the code.

---

## M0 — Foundation

### D1. Module structure: core-modules + feature-packages
`:core:model` and `:core:brain` are pure `kotlin("jvm")` (Android-free →
logic is unit-testable without Robolectric/a device). `:core:data` and
`:core:ui` are Android libraries. Features live as packages under `:app`,
depending only on `:core:*`, so any package can be promoted to a real
`:feature:*` module later without rewrites. Rejected full per-feature
modularization (boilerplate cost > benefit at this size) and a pure monolith
(loses the pure-JVM test boundary).

### D2. Toolchain pinned to what is proven on the build machine
Gradle 8.14.3, AGP 8.13.0 (both already cached), Kotlin 2.2.0, KSP
2.2.0-2.0.2, compileSdk/targetSdk 36, minSdk 31, JDK 21. Every catalog
coordinate was verified resolvable before pinning.

### D3. minSdk 31
BiometricPrompt maturity, `EncryptedSharedPreferences`, exact alarms
(`setExactAndAllowWhileIdle` / `setAlarmClock`), and the modern
`dataExtractionRules` (API 31+) — which lets us drop legacy backup config.

### D4. `build-logic` convention plugins compile with `-Xskip-metadata-version-check` — KNOWN COMPROMISE
`kotlin-dsl` compiles build-logic with Gradle's embedded Kotlin (2.0.x), but
the convention plugins reference AGP/Kotlin/Hilt artifacts built with Kotlin
2.2.0. The flag silences the metadata **binary-version** check. This is a
suppressed forward-compat warning, NOT a root fix; it is safe here because we
only call stable Gradle APIs of those plugins.
**Exit path (cleanest first):** (1) when Gradle's embedded Kotlin reaches ≥2.2
(a Gradle upgrade), delete the flag; (2) or apply standalone
`org.jetbrains.kotlin.jvm` 2.2.0 to build-logic instead of the embedded
compiler. Note: a *downgrade* of Kotlin in build-logic does NOT fix it — the
embedded compiler still has to *read* 2.2 metadata from the plugin jars.

### D5. `Project.libs` catalog accessor in build-logic MUST be `internal`
A public `Project.libs` extension leaks onto the build-script classpath of
every module that applies a convention plugin and shadows Gradle's generated
type-safe `LibrariesForLibs` accessor (making `libs.<lib>` unresolved in
module build files). Keeping it `internal` confines it to build-logic.

### D6. Time is a single storage format: INTEGER epoch-millis, UTC — everywhere
No ISO strings in the DB. Instants (createdAt, occurredAt, learnedAt, dueAt,
generatedAt) are true UTC instants. Calendar dates (renewsOn, cancelBy,
tickDate, forDate) are the millis of that date at **00:00 UTC**. One
arithmetic domain so the M4 alarm/deadline math never trips over a mix.
Rejected epoch-day for pure dates (would be a second unit).

### D7. All calendar date-only writes go through one `CalendarDates.toDateMillis`
The "date-only as UTC-midnight millis" convention only holds if a single
normalization function is the sole writer path. Otherwise it drifts between
writers and `UNIQUE(habitId, tickDate)` silently admits two ticks per day.

### D8. Only `followups.dueAt` is alarm-bearing; a dated fact lives ONLY in followups
`memory_facts` has no date/deadline columns (`learnedAt` is metadata, not a
domain date). A note yielding both a durable fact and a deadline produces two
rows — a dateless memory_fact + a followup — linked by one `sourceNoteId`,
never a date copied into both. One source of truth for dated facts = no
resync bug. Document `renewsOn/cancelBy` are display + 30-day-window only;
anything needing a reminder becomes a followup.

### D9. `memory_facts` is append-only with supersede; the invariant is held by DAO types
A row is one revision. Live = `supersededById IS NULL AND forgottenAt IS NULL`,
exposed as the `memory_facts_live` view — "current memory" is one query, not a
chain reconstruction. Consolidation (M4) depends only on
`MemoryConsolidationDao`, whose sole writes are: insert a new revision, and a
marker-only UPDATE of the supersede pointer (never the fact content). There is
no `@Delete` and no `@Update(MemoryFactEntity)` in any DAO — the append-only
guarantee is structural. User "forget" is a non-destructive `forgottenAt`
tombstone (content kept for undo/audit). A detekt rule backstops "no
destructive verb on the fact entity". Structural fix for the proven prod bug
"background compaction loses facts".

### D10. FTS is standalone and LIVE-ONLY, maintained at write choke-points
Room's `@Fts4(contentEntity=…)` requires an INTEGER rowid PK on the content
table (fights our `TEXT id`) and would index every revision including dead
ones. Instead we use a standalone FTS4 mirror populated by hand at the
choke-points: inserted on capture, deleted on supersede/forget. This keeps the
index lean (live rows only) and the back-link column (`memId`/`noteId`) is
`notIndexed` so it never appears in MATCH results.
**Tradeoff considered:** the simpler "Room external-content over the whole
table + live filter in the query" would index dead revisions (index grows with
revision chains). We chose live-only maintenance because the write
choke-points already exist and it avoids the growth entirely.

### D11. FTS4 tokenizer: `unicode61 remove_diacritics=2`; prefix search only
Case- and diacritic-insensitive (e.g. "cafe" matches "Café"). No ru/pl
stemming — FTS4 gives prefix matching, not morphology. Documented limitation.

### D12. Enums stored as TEXT; unknown value degrades to a known fallback (NOT a crash)
One central `WireEnum` + `enumFromWire` mapping (the `HermesConverters` TYPE
converters are the DB boundary). Storage stays additive: a value written by a
newer app version, or a hand-edited import, degrades to a safe known value and
never loses the row. This is DELIBERATELY the opposite of the old Hermes Lens
zod contract, where an unknown enum was an error forcing a cache fallback —
there is no second service to protect here, the DB is the only source, so we
degrade instead of failing.

### D13. Money is `amountMinor + currency`; null ≠ 0; no cross-currency total
Absence of an amount is a null `Money`, never `Money(0)`. `sumByCurrency`
skips nulls (a currency seen only as null never appears) and keeps a real zero.
There is no aggregate scalar total anywhere; per-currency totals are computed
in queries/domain — matching the sidecar parsers.

### D14. `sourceNoteId` is a logical reference, not a foreign key — integrity rests on soft-delete
The whole schema is soft-delete/append (notes → status, memory_facts →
supersede/forgotten, followups → cancelled, habits/projects → archived), so a
`sourceNoteId` never dangles because rows are never physically removed.
**Precondition to record:** soft-delete IS the integrity guarantee for
`sourceNoteId`. Any future real `DELETE FROM notes` (M7 export/wipe/import, or
a cleanup) MUST either null/clean the references or not exist — without an FK
the DB will not object. This is an M7 checklist item.

### D15. Idempotency: extraction is idempotent per note — the hook is in the schema
`id TEXT` PK stops a duplicate row, not a *second row with the same meaning* on
re-run (process killed after the LLM call; doubled notification/Gmail).
`transactions.dedupKey` (`DedupKeys.transaction(...)`, a hash that normalizes a
null merchant to empty — SQL NULLs compare unequal) is `UNIQUE`, so a replay is
a no-op. `followups` carry an index on `(sourceNoteId, dueAt)` for a repository
dedup check ("already extracted from this note for this date"), not a hard
UNIQUE (title/criticality legitimately change on re-extraction).

### D16. Tests: JUnit4 everywhere; Room tested under Robolectric NATIVE SQLite
One test stack (JUnit4 + Truth + Turbine + coroutines-test) for JVM and
androidTest. Room DAO/schema tests run on the JVM via Robolectric. Robolectric's
**legacy** sqlite4java lacks the `unicode61` tokenizer, so we set
`sqliteMode=NATIVE` (Android's real SQLite) to exercise the FTS4 schema and
diacritic folding for real. Tests run at SDK 34 (Robolectric 4.14 has no
android-all for 36 yet); the app still targets 36.

### D17. Migration policy is verifiable from v1
`exportSchema=true`, schema JSON committed under `core/data/schemas/`, DB
version is the single `HermesDatabase.VERSION` constant, upgrades go through an
explicit Migration list. A JVM test opens the real DB and round-trips; a
MigrationTestHelper scaffold will validate upgrades in M1+. v1 has nothing to
migrate from, but the harness exists so adding a column later cannot skip a
migration test.
