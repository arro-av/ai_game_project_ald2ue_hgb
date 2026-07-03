# FEATURE-01: Core Domain Model

## Purpose

Create the model classes and enums used by the battle system, DAO layer, and renderer.

## Scope

- Add `Entity`, `Battler`, `PlayerCharacter`, `Enemy`, `TrashEnemy`, and `BossEnemy`.
- Add `Attack`, `Battle`, `BattleTurn`, `BattleResult`, `Floor`, `DialogueLine`, and `HighScore`.
- Add `AttackEffect`, `TargetType`, `BossEnemyState`, and `TrashEnemyState`.
- Add small interfaces where useful: `Targetable`, `TurnActor`, `StateMachine`.

## Acceptance Criteria

- Models compile independently of JavaFX and JDBC.
- Fields are private.
- State changes happen through methods.
- Mutable lists are not exposed directly.

## Depends On

- `FEATURE-00_PROJECT_SETUP.md`
