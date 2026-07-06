# Dungeon Crawler Carl - JavaFX Assignment Game

This is a private university assignment project for the University of Applied Sciences Upper Austria, Campus Hagenberg. It was created for the A08 "Dungeons & Java" assignment.

This project is not affiliated with, endorsed by, or connected to Dungeon Crawler Carl, its author, publisher, or rights holders. It is a private educational fan project only.

## Project

The game is a small 2D turn-based dungeon battler built with JavaFX. The player controls a fixed party across six linear combat encounters. Each floor contains an intro dialogue, a battle against a boss encounter, and simple victory or game-over handling.

Core features:

- JavaFX main menu and battle screen
- Canvas-based 2D rendering with sprites, backgrounds, HUD, and dialogue box
- Three playable party members with fixed attacks
- Six floor boss encounters with additional trash enemies where needed
- Boss and trash enemy finite state machines
- Runtime game data loaded from SQLite through DAO classes
- High score persistence

## Tech Stack

- Java 25+
- Maven
- JavaFX
- SQLite with JDBC
- DAO-based data access
- Layered model, service, DAO, controller, and view structure

## Run

Use the Maven wrapper from the project root:

```powershell
.\mvnw.cmd javafx:run
```

On Unix-like systems:

```bash
./mvnw javafx:run
```

## Notes

The project is intentionally small in scope. It focuses on demonstrating object-oriented design, finite state machines, JavaFX rendering, and database-backed game data for the assignment.
