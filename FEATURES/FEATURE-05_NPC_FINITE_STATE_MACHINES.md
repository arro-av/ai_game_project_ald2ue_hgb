# FEATURE-05: NPC Finite State Machines

## Purpose

Satisfy the assignment FSM requirement with visible boss and trash enemy state changes.

## Scope

- Add `BossEnemyStateMachine`.
- Add `TrashEnemyStateMachine`.
- Update states after damage and before enemy action choice.
- Unlock attacks by state.
- Log readable transition messages.

## Acceptance Criteria

- Boss states progress from intro to phase one, phase two, final phase, and dead.
- Trash states progress through normal, aggressive, desperate, and dead.
- State changes affect behavior.
- Transition messages can be shown in the dialogue box or console.

## Depends On

- `FEATURE-04_BATTLE_ENGINE_MVP.md`
