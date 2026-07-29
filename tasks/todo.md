# ORV-System-Mod Task List

## Phase 1 — Complete

- [x] Linear project: `ORV-System-Mod`
- [x] Linear epic: `ORV-20 Core ORV Systems`
- [x] Linear child: `ORV-21 Server-side Coin Economy & Attachments`
- [x] Linear child: `ORV-22 Custom Networking & Client Sync`
- [x] Linear child: `ORV-23 Constellation & Stigma Registry Framework`
- [x] GitHub issue #1: Server-side Coin Economy & Attachments
- [x] GitHub issue #2: Custom Networking & Client Sync
- [x] GitHub issue #3: Constellation & Stigma Registry Framework
- [x] Cross-links and suggested feature branches added

## Phase 2 — Complete

- [x] Write failing transaction tests for `ICoinData`
- [x] Implement the server-authoritative coin-domain API
- [x] Write persistence round-trip tests
- [x] Migrate to the dedicated `coin_data` attachment
- [x] Write permission and command-tree tests
- [x] Implement `/orv coins add|remove|set`
- [x] Run `gradlew clean build`

## Phase 3 — Complete

- [x] Specify and test `SyncSystemDataPayload`
- [x] Register `orv:system_sync`
- [x] Implement the thread-safe client cache
- [x] Add join and mutation sync triggers
- [x] Retire the legacy coin/player payload safely
- [x] Persist energy with backward-compatible player-data decoding
- [x] Render coins, energy, and constellation from immutable HUD snapshots

## Phase 4 — Complete

- [x] Specify and test `StigmaDefinition`
- [x] Implement the `orv:stigma` datapack registry
- [x] Register extensible EffectLogic identifiers
- [x] Specify and persist `IPlayerSponsor`
- [x] Persist active stigmas, probability, and cooldowns
- [x] Implement and test `StigmaEngine`
- [x] Connect engine commits to Energy and system sync
- [x] Package a validated `orv:recovery` example

## Definition of Done

- [x] Phase 2 tests cover success, boundary, and failure paths
- [x] Phase 2 preserves server authority
- [x] Coin persistence and synchronization behavior is verified
- [x] `gradlew clean build` succeeds on Java 21
- [x] Phase 2 tracking issues and documentation are current
- [x] Phase 3 packet codecs use bounded input and real-buffer tests
- [x] Phase 3 handler thread handoff and concurrent cache updates are tested
- [x] Phase 3 preserves server authority and clears stale logout state
- [x] Phase 4 data definitions and sponsor NBT round-trip are tested
- [x] Phase 4 covers ownership, mana, cooldown, overflow, and rollback paths
- [x] Phase 4 remains server-authoritative and data-driven
