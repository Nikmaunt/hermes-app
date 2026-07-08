# Hermes

A **local-first personal assistant** for Android. You jot a quick note — a doctor's
appointment, a payment, a fact about someone, a decision, a habit, a subscription — and
Hermes runs it through an LLM extraction pipeline and files what it finds into the right
place: followups you'll be reminded about, durable memory, money, documents, people,
projects. Everything lives on the device.

Status: **M0 (foundation) complete** — the module skeleton, the Room schema, the extraction
contract, the design system, and a navigable shell with a working **Demo mode**. The "why"
behind every engineering decision is in [docs/decision-log.md](docs/decision-log.md) (D1–D38);
this README is the map, not a restatement of it.

## Architecture

Five Gradle modules. `:core:model` and `:core:brain` are **pure `kotlin("jvm")`** — they
apply no Android plugin, so their logic is unit-testable without a device or Robolectric, and
the boundary is enforced structurally (an accidental Android import fails to compile).

```
                                   :app
              single activity · onboarding · navigation · Demo seeder
                 │              │               │              │
                 ▼              ▼               ▼              │
             :core:ui      :core:brain      :core:data         │
             Compose        extraction         Room            │
             M3 theme,      & chat, the      schema v1,        │
             primitives     untrusted-       DAOs, FTS         │
             (no model)     output boundary    │              │
                                 │              ▼              │
                                 └────────► :core:model ◄──────┘
                                        pure JVM · Android-free
                                        Money, enums, dates, ids
```

- **`:core:model`** — value types and rules shared everywhere: `Money` (per-currency, no
  cross-currency total), enums stored as stable wire strings, `CalendarDates` (the single
  date-only normalization point), `DedupKeys` (idempotency).
- **`:core:brain`** — the provider-agnostic extraction & chat services and the **untrusted-
  output boundary** (`ExtractionValidator`). `BrainProvider` is only transport; a `FakeProvider`
  flows through the exact same validation as a real one. Depends only on `:core:model`.
- **`:core:data`** — Room schema v1 (13 tables + a live view + two standalone FTS4 mirrors),
  append-only memory with supersede, soft-delete throughout. Depends on `:core:model`.
- **`:core:ui`** — Material 3 theme, design tokens (with a WCAG contrast test) and shared
  primitives (`HermesScreen`, empty-state, skeletons, `SensitiveContent`). Compose-only.
- **`:app`** — the composition root: single activity, onboarding state machine, navigation,
  Hilt wiring, the Demo seeder, and the read screens.

## Privacy model

Hermes is built to keep your data on your phone.

- **No backend, no telemetry, no analytics, no crash reporting.** There is no Hermes server.
- **Network is allow-listed by milestone, not open:** only your own LLM endpoint (M1) and
  Gmail (M8). Everything else is local.
- **Secrets never touch general storage.** The API key and tokens live only in the Android
  Keystore / `EncryptedSharedPreferences` (M1) — never in DataStore, logs, exceptions, or any
  export. Non-secret settings (theme, chosen provider, endpoint host, output language) are in
  a plain DataStore.
- **Nothing leaves via backup or transfer.** `allowBackup=false` plus `dataExtractionRules`
  excluding both cloud backup and device-to-device transfer, from the first `:app` commit.
- **TLS only.** `usesCleartextTraffic=false` and a network security config pinned to system
  trust anchors (no user CAs) — cleartext stays forbidden through M0 and M1.

See [D32](docs/decision-log.md) for the full security posture and its exit conditions.

## Demo mode

Choosing **Demo** during onboarding seeds a database that is the genuine product of the
extraction pipeline, not a hand-filled table: a handful of realistic notes (EN + RU) are run
through the real `FakeProvider → ExtractionService → ExtractionValidator`, and the rows they
produce populate Today, Inbox, Memory, Money, Documents, Habits, People and Projects. One
note yields both a dated followup **and** a dateless, sensitive health fact from the same
text — the core memory/deadline split, visible in the running app. Demo is deterministic and
re-entry never double-seeds. (See [D36–D38](docs/decision-log.md).)

## Build & run

Requires JDK 21; the Gradle wrapper pins the rest of the toolchain.

```bash
# Full verification: build every module + detekt + ktlint + all unit tests
./gradlew check

# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device / emulator (minSdk 31)
adb install app/build/outputs/apk/debug/app-debug.apk
```

Room DAO/schema tests run on the JVM under Robolectric (native SQLite, for the real FTS4
tokenizer); the pure-JVM modules test without any Android runtime.
