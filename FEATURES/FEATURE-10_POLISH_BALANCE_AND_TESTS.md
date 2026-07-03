# FEATURE-10: Polish, Balance, And Tests

## Purpose

Stabilize the implementation before screenshots and documentation.

## Scope

- Add focused tests for battle service behavior.
- Add focused tests for FSM transitions.
- Add focused tests for floor progression.
- Balance HP, damage, and cooldowns enough for a full playthrough.
- Validate resource paths.
- Fix obvious UI overlap or unreadable text.

## Acceptance Criteria

- Maven compiles and tests pass.
- A full game can be played from main menu to victory.
- Losing is possible and behaves correctly.
- No missing asset paths break the app.

## Depends On

- `FEATURE-09_END_STATES_AND_HIGHSCORE.md`
