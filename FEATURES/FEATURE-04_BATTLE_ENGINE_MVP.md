# FEATURE-04: Battle Engine MVP

## Purpose

Create the first playable combat loop without full special-case encounter behavior.

## Scope

- Add `BattleService`.
- Add `DamageCalculator`.
- Add `TargetSelector`.
- Implement turn order from speed or initiative.
- Implement basic damage, healing, cooldowns, and death checks.
- Emit battle log messages.

## Acceptance Criteria

- Player characters can use attacks.
- Enemies can choose and use attacks.
- HP changes correctly.
- Cooldowns tick down.
- Victory and loss are detected.
- Core logic can run without JavaFX.

## Depends On

- `FEATURE-03_GAME_FLOW.md`
