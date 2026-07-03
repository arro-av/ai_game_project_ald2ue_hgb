# FEATURE-03: Game Flow

## Purpose

Implement the high-level progression from main menu to six floors, victory, or game over.

## Scope

- Add `GameService`.
- Add `EncounterService`.
- Add `DialogueService`.
- Add `GameController`.
- Create party at game start.
- Add Mongo to the active party starting on floor 3.
- Advance floors after victory.

## Acceptance Criteria

- Starting a game creates floor 1.
- Clearing a floor advances to the next floor.
- Floor 6 victory ends the run.
- All party members defeated ends the run.

## Depends On

- `FEATURE-02_DATABASE_AND_DAO.md`
