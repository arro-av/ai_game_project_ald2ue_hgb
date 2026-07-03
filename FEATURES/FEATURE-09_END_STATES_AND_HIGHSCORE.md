# FEATURE-09: End States And Highscore

## Purpose

Complete the playable loop with victory, game-over, and persistence evidence.

## Scope

- Add `VictoryView`.
- Add `GameOverView`.
- Add return-to-menu actions.
- Save a run result through `HighScoreDao`.
- Optionally show saved highscores on the main menu.

## Acceptance Criteria

- Winning floor 6 shows victory.
- Losing the party shows game over.
- Both screens can return to the main menu.
- A highscore or run summary is saved to SQLite.

## Depends On

- `FEATURE-08_SIX_ENCOUNTERS.md`
