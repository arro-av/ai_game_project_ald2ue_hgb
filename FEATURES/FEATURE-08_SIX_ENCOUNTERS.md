# FEATURE-08: Six Encounters

## Purpose

Implement all six floors with boss phase behavior and trash enemy behavior where applicable.

## Scope

- Floor 1: Hoarder and Scatterers.
- Floor 2: Ralph.
- Floor 3: Heather.
- Floor 4: Gore-Gore.
- Floor 5: Denise.
- Floor 6: Circe and Mantis Nymphs.
- Add encounter-specific service logic without putting rules in DAO or view classes.
- Seed all encounter data in SQLite.

## Acceptance Criteria

- All six floors are playable in sequence.
- Each boss changes behavior by phase.
- Trash enemies use their FSM.
- Encounter mechanics are visible in battle logs.

## Depends On

- `FEATURE-05_NPC_FINITE_STATE_MACHINES.md`
- `FEATURE-07_PARTY_ACTIONS_AND_EFFECTS.md`
