# FEATURE-00: Project Setup

## Purpose

Create a clean JavaFX/Maven baseline that can compile, run, and support SQLite.

## Scope

- Configure Maven compiler settings.
- Add JavaFX dependencies and run plugin.
- Add SQLite JDBC dependency.
- Create `Main` launcher and JavaFX `App`.
- Create resource folders for `db` and `assets`.
- Keep the package root `at.fhooe.ald`.

## Acceptance Criteria

- `mvn test` compiles.
- The JavaFX app starts and shows a basic window.
- No game logic is implemented in this step.

## Depends On

- None.
