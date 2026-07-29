# Project Documentation
> Generated: 2026-07-29T19:22:15+02:00 | Mode: DELTA

## Tech Stack

- Runtime: Java 21
- Game platform: Minecraft 1.21.1
- Mod framework: NeoForge 21.1.235
- Build: Gradle 9.2.1 wrapper with NeoGradle UserDev 7.1.38
- Mapping layer: Parchment 2024.11.17 for Minecraft 1.21.1
- Testing: JUnit Jupiter 5.13.4 and Mockito 5.23.0
- Client UI: Minecraft GUI APIs and NeoForge client events
- Persistence: NeoForge data attachments serialized with Mojang Codecs
- Networking: NeoForge `CustomPacketPayload` and `StreamCodec`
- Data-driven content: NeoForge custom datapack registries

## Dependencies

### Core

- `net.neoforged:neoforge:21.1.235`

### Build

- `net.neoforged.gradle.userdev:7.1.38`
- `org.gradle.toolchains.foojay-resolver-convention:1.0.0`

### Testing

- `org.junit:junit-bom:5.13.4`
- `org.junit.jupiter:junit-jupiter`
- `org.mockito:mockito-core:5.23.0`
- `org.junit.platform:junit-platform-launcher`

## Architecture Pattern

The project uses a package-layered mod architecture:

- `OrvMod` is the composition root and registers mod-bus and game-bus
  listeners.
- `data` owns persistent attachment models and registration.
- `economy` owns the server-authoritative coin application service.
- `command` exposes validated Brigadier entry points.
- `network` owns packet contracts, codecs, registration, and dispatch.
- `client` owns keybindings, HUD/screen rendering, and client snapshots.
- `guild` owns in-memory guild domain rules and snapshot creation.
- `sponsor` owns immutable sponsor state and its server application service.
- `stigma` owns datapack definitions, effect bindings, execution policy, and
  NeoForge production adapters.

State mutation is intended to pass through a server-side service before a
clientbound synchronization packet is emitted.

## Folder Structure

```text
src/main/java/com/weeeedddd/orv/
├── OrvMod.java               # mod entry point and event wiring
├── client/                   # physical-client input, UI, and snapshots
├── command/                  # Brigadier command registration
├── data/                     # Codec-backed NeoForge attachments
├── economy/                  # server-side coin transactions
├── guild/                    # guild domain and in-memory service
├── network/                  # CustomPacketPayload contracts and handlers
├── sponsor/                  # persistent sponsor state and service
└── stigma/                   # datapack registry and execution engine

src/main/resources/
├── META-INF/neoforge.mods.toml
├── assets/orv/lang/          # English and German translations
└── data/orv/orv/stigma/      # built-in stigma JSON definitions

src/test/java/com/weeeedddd/orv/
├── client/system/            # concurrent cache and handler tests
├── command/                  # command-tree and permission tests
├── data/                     # transaction and NBT round-trip tests
├── network/                  # real-buffer payload codec tests
├── sponsor/                  # immutable state and persistence tests
└── stigma/                   # codec, registry resource, and engine tests

tasks/                        # implementation plan and phase checklist
```

Generated directories such as `build/`, `.gradle/`, `run/`, and `logs/` are
not source architecture.

## Code Style Conventions

- Java types use PascalCase; methods and fields use camelCase.
- Constants use `UPPER_SNAKE_CASE`.
- Utility/service classes are `final` with private constructors.
- Immutable transport/state shapes use records.
- Public methods use explicit parameter and return types; `var` is not used.
- Long method calls are formatted one argument per line.
- Validation occurs at record/class construction and service boundaries.
- Package names are rooted at `com.weeeedddd.orv`.

## Modularity Practices

- Registry ownership stays in a dedicated registration class.
- Mutable coin state is hidden behind `ICoinData` and `CoinService`.
- Commands delegate to services instead of directly writing attachments.
- Network wire formats are separated from guild domain snapshots.
- Client-only UI state is kept below `client`.
- Stigma execution policy is generic and unit-testable; `ServerStigmaEngine`
  adapts it to Minecraft runtime types.
- Data-driven definitions reference code-side effects by identifiers.

## Data Architecture

- `orv:coin_data` is an `AttachmentType<ICoinData>` backed by `CoinData`.
- `orv:player_data` stores strength and energy; legacy data without energy
  decodes with a zero default.
- Both attachment types use Mojang Codecs and `copyOnDeath()`.
- `orv:player_sponsor` persists constellation, active stigma IDs, probability,
  and absolute cooldown ticks with a Mojang Codec and `copyOnDeath()`.
- `orv:stigma` is a custom datapack registry populated from JSON at world load
  and reload.
- Client HUD values are immutable `SystemDataSnapshot` records stored in a
  `ConcurrentHashMap`; the cache is never authoritative for gameplay.
- Guild state currently uses synchronized in-memory maps and is not durable.
- Client guild state is exposed as an immutable `GuildSnapshot`.

## Cross-Cutting Concerns

- Server authority: mutable gameplay APIs accept `ServerPlayer`.
- Authorization: administrative commands require permission level 2; guild
  mutations validate role authority server-side.
- Validation: negative balances, overflow, packet list sizes, string lengths,
  cooldowns, effect IDs, and role transitions are checked.
- Localization: user-facing command feedback uses translation keys.
- Error handling: invalid commands return failures; malformed packet sizes
  throw before allocating unbounded lists.
- Secrets: no credentials or environment secrets are present in source.

## Service Communication

- Player and guild updates use NeoForge play-channel custom payloads.
- `orv:system_sync` carries UUID, coins, energy, and a bounded constellation
  name from server to client.
- Packet handlers enqueue state changes through `IPayloadContext`.
- Login, respawn, and dimension-change events trigger a complete system sync.
- Successful coin and energy mutations trigger targeted system snapshots.
- Sponsor selection and successful stigma execution update the same system
  snapshot with the authoritative constellation and energy.
- `ServerStigmaEngine` resolves definitions from `RegistryAccess`, executes
  registered server effects, and commits attachment state only on success.
- Client logout clears cached system snapshots.
- Guild request/action payloads are server-authorized and return snapshots.

## Test Coverage

- Overall line coverage: not measured; JaCoCo is not configured.
- Current automated tests: 51 passing tests across twelve test classes.
- Patterns: pure unit tests, Codec/NBT round trips, Brigadier tree inspection,
  permission predicates, real-buffer payload round trips, handler thread
  handoff, concurrent cache updates, datapack Codec validation, sponsor NBT
  persistence, and transactional engine behavior.
- Key untested areas: live client/server packet integration, guild actions,
  HUD rendering, reconnect, death/respawn, and dimension changes.

## Entry Points

- Mod entry: `src/main/java/com/weeeedddd/orv/OrvMod.java`
- Stigma API: `src/main/java/com/weeeedddd/orv/stigma/ServerStigmaEngine.java`
- Build: `build.gradle`, `settings.gradle`, `gradle.properties`
- Metadata: `src/main/resources/META-INF/neoforge.mods.toml`
- Build command: `gradlew clean build`
- Client run: `gradlew runClient`
- GameTest server: `gradlew runGameTestServer`

## Last Scanned

2026-07-29T19:22:15+02:00
