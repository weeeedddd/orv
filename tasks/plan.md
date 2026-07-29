# Implementation Plan: ORV-System-Mod

## Overview

The ORV-System-Mod targets NeoForge 1.21.1 on Java 21. Its core is a
server-authoritative player-state model, an explicit client synchronization
contract, and data-driven constellation/stigma definitions. Delivery follows
TDD: tests define persistence, transaction, codec, and engine behavior before
or alongside production changes.

## Current Baseline

- `orv:coin_data` is the authoritative persistent coin attachment.
- `orv:player_data` stores persistent strength and energy values.
- `SyncSystemDataPayload` publishes immutable HUD snapshots to the client.
- Sponsor data can extend the system-sync sender in Phase 4 without moving
  authority into the client cache.

## Architecture Decisions

- The server owns all mutable ORV gameplay state.
- Attachments persist player-owned state; clients only receive immutable
  snapshots.
- Packet schemas use explicit size/range validation and versioned migration.
- Stigma definitions are data-driven; effect implementations are registered
  server-side and referenced by identifiers.
- Every mutation flows through an application service so validation, sync, and
  audit behavior remain consistent.

## Dependency Graph

1. Coin attachment and transaction API
2. System-data payload and client snapshot cache
3. Sponsor attachment and stigma registry/engine

Networking depends on the authoritative data model. Sponsor and stigma systems
depend on the synchronized system-data schema for HUD-visible state.

## Phase 1: Project Management and Setup

- [x] Create Linear project `ORV-System-Mod`.
- [x] Create epic `ORV-20 Core ORV Systems`.
- [x] Create child issue `ORV-21 Server-side Coin Economy & Attachments`.
- [x] Create child issue `ORV-22 Custom Networking & Client Sync`.
- [x] Create child issue `ORV-23 Constellation & Stigma Registry Framework`.
- [x] Create and cross-link GitHub issues #1, #2, and #3.
- [x] Add suggested feature branches to both tracking systems.

### Checkpoint: Phase 1

- [x] Every child issue has testable acceptance criteria.
- [x] Dependencies are encoded in Linear.
- [x] GitHub and Linear links are bidirectional.
- [x] No Phase 2 production code has been started.

## Phase 2: Coin Economy

### Task 2.1: Define the coin-domain contract

**Acceptance criteria**

- `ICoinData` exposes `addCoins`, `removeCoins`, `getCoins`, and `hasEnough`.
- Invalid values and arithmetic overflow cannot corrupt state.
- Tests describe all transaction semantics.

**Verification**

- Focused unit tests pass.
- `gradlew clean build` succeeds.

**Dependencies:** None.

### Task 2.2: Persist coin state through a NeoForge attachment

**Acceptance criteria**

- Serialization round trips preserve supported balances.
- Reconnect and configured death-copy behavior retain state.
- Existing `PlayerData` migration has one authoritative source of truth.

**Verification**

- Codec/NBT and persistence tests pass.
- Manual reconnect/death checks are documented.

**Dependencies:** Task 2.1.

### Task 2.3: Add administrator coin commands

**Acceptance criteria**

- `/orv coins add|remove|set` requires permission level 2.
- All command mutations use the transaction service.
- Validation failures return useful translatable feedback.

**Verification**

- Brigadier command tests pass.
- Client synchronization is invoked after successful mutations.

**Dependencies:** Tasks 2.1 and 2.2.

### Checkpoint: Phase 2

- [x] `ICoinData` transaction tests passed through RED and GREEN.
- [x] `orv:coin_data` persists through a Codec-backed NBT representation.
- [x] Mutations are exposed through a server-only `CoinService`.
- [x] Admin commands require permission level 2.
- [x] Full clean build succeeds.

## Phase 3: Networking and Client Sync

- [x] Define and test `SyncSystemDataPayload`.
- [x] Register `orv:system_sync` on the clientbound play channel.
- [x] Implement an immutable, thread-safe client snapshot cache.
- [x] Trigger sync on join and authoritative state changes.
- [x] Replace the legacy coin/player payload with system and stats payloads.

### Checkpoint: Phase 3

- [x] UUID, coins, energy, and constellation name round-trip through a real
  `FriendlyByteBuf`.
- [x] The handler publishes a complete snapshot through `enqueueWork`.
- [x] Concurrent updates cannot expose a partially updated HUD state.
- [x] Legacy player NBT without energy migrates to zero energy.
- [x] The HUD reads synchronized system values from the client cache.
- [x] Full clean build succeeds.

## Phase 4: Sponsor and Stigma System

- [x] Define and validate data-driven `StigmaDefinition` resources.
- [x] Persist `IPlayerSponsor` state.
- [x] Implement a transactional, server-side `StigmaEngine`.
- [x] Cover mana, cooldown, ownership, probability, and rollback paths.

### Checkpoint: Phase 4

- [x] Register `orv:stigma` through `DataPackRegistryEvent.NewRegistry`.
- [x] Bind JSON definitions to extensible code-side EffectLogic IDs.
- [x] Persist constellation, active stigmas, probability, and cooldowns in
  `orv:player_sponsor`.
- [x] Deduct energy and commit cooldown only after a successful server effect.
- [x] Keep failed effects from changing attachment resource state.
- [x] Synchronize energy and constellation through `orv:system_sync`.
- [x] Package and validate the built-in `orv:recovery` definition.
- [x] Full clean build succeeds.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Existing attachment and payload overlap new contracts | High | Write migration tests and keep one source of truth |
| Client mutation leaks into common code | High | Keep handlers snapshot-only and test directionality |
| Datapack reload leaves invalid stigma references | Medium | Validate atomically before publishing a new registry snapshot |
| Coin overflow or partial transactions corrupt balances | High | Use checked arithmetic and transaction-level tests |

## Open Questions

- Should death always copy sponsor/stigma state, or should this be configurable?
- Which initial effect-logic implementations are needed for the first playable
  stigma?
- Should the system-sync payload be per-player only or support observed-party
  members later?
