# FEATURE-02: Database And DAO

## Purpose

Load game data from SQLite through DAO interfaces and JDBC implementations.

## Scope

- Add `schema.sql` and `seed.sql`.
- Add tables for characters, attacks, enemies, floors, dialogue, and highscores.
- Add DAO interfaces for characters, attacks, enemies, floors, dialogue, and highscores.
- Add JDBC implementations.
- Add `Database` and `DatabaseInitializer`.
- Copy bundled database data to a writable user directory for runtime use.

## Acceptance Criteria

- Party data can be loaded from the database.
- Enemy and floor data can be loaded from the database.
- Dialogue lines can be loaded from the database.
- `HighScoreDao.save(...)` or equivalent persists a result.
- SQL is isolated to DAO/JDBC/database classes.

## Depends On

- `FEATURE-01_CORE_DOMAIN_MODEL.md`
