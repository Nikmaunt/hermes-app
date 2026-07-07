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

---

## M0 — `:core:brain` (extraction & chat contract)

### D18. `:core:brain` is a thin transport + provider-agnostic services; validation is written ONCE
`BrainProvider` is only the transport — given messages it returns raw text + token
usage, and knows nothing about prompts, validation, or retries. The chat and
extraction use-cases (`ChatService`, `ExtractionService`) sit ABOVE it and are the
sole home of prompt-building, decode, validation and retry. Fake/OpenRouter/on-device
each implement only `complete()`. Two consequences: (1) a `FakeProvider`'s raw output
flows through the SAME `ExtractionValidator` as a real provider, so demo fixtures are
the genuine product of the pipeline, not a bypass; (2) `:core:brain` depends only on
`:core:model`, NEVER `:core:data` — it knows the semantic model, and mapping
`Extracted*` → Room entities (stamping id/createdAt/sourceNoteId/dedupKey) is the M2
data layer's job. Rejected a single fat `BrainProvider.chat()+extract()` interface: it
would duplicate validation per provider and couple providers to policy. Pure
`kotlin("jvm")` (does not apply AGP) is the structural check that the contract stays
Android-free (§5); the on-device ML Kit GenAI provider arrives in M1 behind this
contract with its Android code in `:app`.

### D19. The extraction boundary is STRICT — the deliberate opposite of D12
LLM output is untrusted. `ExtractionValidator` HARD-rejects an unknown *filtering*
enum (`MemoryCategory`, `DocumentKind`), an unparseable/out-of-window date, and money
with an amount but no currency. This is deliberately the opposite of the storage-read
degrade policy (D12): there the DB is the only source, so an unknown value degrades to
keep the row; here the input is untrusted, so we reject rather than silently mis-file.
*Soft* optional enums with a natural default (`Criticality`→NORMAL, `ProjectStatus`→
ACTIVE, `HabitCadence`→DAILY, `BillingPeriod`→MONTHLY) degrade to the default WITH a
warning — degraded, but never silently. Granularity is a hybrid: a structural/JSON
failure triggers one hardened retry; an element-level hard violation drops only that
item. Nothing is lost silently — a dropped item is preserved as a `RejectedItem` (its
exact raw fragment + the violations) and a degrade/default as a `warning`; both ride in
`ExtractionResult` for the M2 review UI.

### D20. The model never emits ids/timestamps/`sourceNoteId`/`dedupKey`; dates are ISO, resolved against injected context
`Extracted*` items carry only semantic fields. Row `id`, `createdAt`, `sourceNoteId`
and `dedupKey` are stamped at persist (M2), so the model cannot forge provenance or
break idempotency. The model emits dates as ISO local strings (`{date:"YYYY-MM-DD"}` or
`{dateTime:"YYYY-MM-DDTHH:MM"}`) and resolves relative dates against `BrainContext`
(now, zone) — no epoch-millis from the model (LLMs are bad at them), no NL date parsing
in Kotlin. Timed → a true instant in the zone; date-only → CalendarDates UTC-midnight
millis (D6).

### D21. A dateless followup is rejected at the extraction boundary
`followups.dueAt` is required and NEVER defaulted. A "followup" the model returns
without a resolvable date is a hard violation (`MISSING_DUE_DATE`) → rejected for
review, never stored dateless and never redirected into a dated `memory_fact`. This
enforces D8 at the untrusted boundary; `dueIsDateOnly` carries the date-only/timed
distinction. (Contrast D23: `occurredAt`/`decidedAt` DO default to now — those columns
are not alarm-bearing.)

### D22. Extraction/chat context is injected → deterministic, unit-testable
`BrainContext(nowMillis, zoneId, language)` is supplied by the caller; nothing in
`:core:brain` reads the wall clock or the device zone. `language` is the preferred
OUTPUT language (summary/brief/chat) — the input note's language is the model's to
detect, never language-detected in Kotlin. `FakeProvider` is deterministic (a handler
over request + call-index, no clock/random), serving both unit tests and Demo mode.

### D23. `occurredAt`/`decidedAt` default to now; numeric/date bounds guard a runaway model
Absent `occurredAt` (transactions) / `decidedAt` (decisions) default to `context.now`
WITH a warning — an optional field must not cost a valid row; a present-but-unparseable
value is a hard violation (garbage is a strong signal, absence is not). Bumpers on
untrusted output: list sizes are capped per type (warning on overflow, never silent),
amounts beyond a sanity bound and dates outside now ± 100y are rejected.

### D24. `source` is a system field, not an extracted one
The `*_source` enums (`TransactionSource`: manual/notification/gmail/import, …) record
HOW a row entered the app — which the model cannot know. So `ExtractedTransaction` has
no `source`; the persistence layer stamps it, exactly like `id`/`createdAt`. Dropping
it from the contract stops the model inventing provenance.

### D25. Over-budget input is an explicit outcome, never a silent truncation
A note longer than the provider input budget returns
`ExtractionOutcome.TooLarge(noteChars, limitChars)` WITHOUT calling the model. We never
truncate to fit — half of a shared PDF would be lost without a trace. Chunking is
deferred (M1+); the honest outcome exists in the contract from day one.

### D26. Chat context is shaped for M5 retrieval without coupling brain to `:core:data`
`ChatContext` carries `brain` + `retrievedFacts` + `recentFollowups` as plain
read-models. M5 fills them from the data layer and passes them in; `ChatService.reply(
history, context)` keeps its signature and `:core:brain` never gains a `:core:data`
dependency (D18). Idempotency of a re-run extraction is likewise NOT brain's job — it is
the persistence layer's, via `transactions.dedupKey` and the followups
`(sourceNoteId, dueAt)` repo dedup check (D15); brain only runs extraction at
temperature 0 to *tend* toward reproducibility.
