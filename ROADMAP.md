# Dungeon Crawler Carl - Structured Roadmap

This roadmap translates `AGENTS.md` into a step-by-step implementation plan for a small JavaFX turn-based dungeon battler. The priority is to satisfy the A08 assignment requirements with clear OOP, visible finite state machines, DAO-based SQLite loading, and a playable six-floor flow.

## Product Target

Build a private educational JavaFX game where the player controls Carl, Donut, and later Mongo through six linear boss encounters. The game uses a side-view battle screen, turn-based menu combat, a bottom dialogue box, SQLite-loaded runtime data, and simple Canvas rendering.

## Non-Negotiable Requirements

- Java 25+, Maven, JavaFX.
- JavaFX Canvas with `GraphicsContext` for battle rendering.
- SQLite through JDBC.
- DAO pattern with SQL isolated in DAO/JDBC classes.
- Clear 3-layer separation: model, service, DAO, controller/view.
- At least two NPC finite state machines: `BossEnemy` and `TrashEnemy`.
- Six playable floors in sequence.
- Party data, attacks, enemies, floors, and dialogue loaded from SQLite.
- Highscore or run result save operation through DAO.
- Final PDF documentation with screenshots, architecture notes, FSM evidence, database schema, and reflection.

## Implementation Phases

### 0. Project Setup And Build Baseline

Create a reliable Maven/JavaFX baseline before adding game logic.

Deliverables:

- Maven compiler settings for Java 25 or the installed project JDK.
- JavaFX dependencies and run plugin.
- SQLite JDBC dependency.
- Clean package root under `at.fhooe.ald`.
- `Main` launcher and `App` JavaFX application class.
- Resource folders for database files and assets.

Feature brief:

- `FEATURES/FEATURE-00_PROJECT_SETUP.md`

Done when:

- `mvn test` compiles.
- `mvn javafx:run` or the agreed run command starts an empty JavaFX window.

### 1. Core Domain Model

Build the model layer first so services, DAOs, and rendering can share stable data structures.

Deliverables:

- Entity hierarchy: `Entity -> Battler -> PlayerCharacter / Enemy -> TrashEnemy / BossEnemy`.
- Combat data classes: `Attack`, `Battle`, `BattleTurn`, `BattleResult`, `Floor`, `DialogueLine`, `HighScore`.
- Enums: `AttackEffect`, `TargetType`, `BossEnemyState`, `TrashEnemyState`.
- Interfaces where useful: `Targetable`, `TurnActor`, `StateMachine`.

Feature brief:

- `FEATURES/FEATURE-01_CORE_DOMAIN_MODEL.md`

Done when:

- Model classes compile.
- Mutable collections are not exposed directly.
- Models contain no SQL and no JavaFX rendering code.

### 2. Database Schema, Seed Data, And DAO Layer

Add SQLite-backed game data loading with clear DAO boundaries.

Deliverables:

- `schema.sql`, `seed.sql`, and optional generated `game.db`.
- Tables: `characters`, `attacks`, `character_attacks`, `enemies`, `enemy_attacks`, `floors`, `floor_enemies`, `dialogue_lines`, `high_scores`.
- DAO interfaces and JDBC implementations.
- `Database` connection helper.
- `DatabaseInitializer` that copies the bundled database to a writable user directory.

Feature brief:

- `FEATURES/FEATURE-02_DATABASE_AND_DAO.md`

Done when:

- Party, attacks, enemies, floors, and dialogue can be loaded from DAOs.
- At least one save operation exists, preferably `HighScoreDao.save(...)`.
- SQL only appears in DAO/JDBC/database classes.

### 3. Game Flow And Floor Progression

Create the service/controller flow that moves from menu to floor intro to battle to victory/game-over.

Deliverables:

- `GameService` for run state and floor progression.
- `EncounterService` for creating each floor battle from DAO data.
- `DialogueService` for intro lines and battle log messages.
- `GameController` coordinating main menu, battle, victory, and game-over screens.

Feature brief:

- `FEATURES/FEATURE-03_GAME_FLOW.md`

Done when:

- Starting a game creates the party and floor 1 encounter.
- Clearing a floor advances to the next floor.
- Losing all party members triggers game over.
- Clearing floor 6 triggers victory.

### 4. Battle Engine MVP

Implement a simple, testable turn-based combat loop before special effects and detailed encounter gimmicks.

Deliverables:

- `BattleService` for turn order, actor selection, basic attacks, win/loss checks.
- `DamageCalculator` for base damage rolls and modifiers.
- `TargetSelector` for legal target choices.
- Cooldown tracking for attacks.
- Battle log events for UI and documentation evidence.

Feature brief:

- `FEATURES/FEATURE-04_BATTLE_ENGINE_MVP.md`

Done when:

- Player characters can choose one of three actions.
- Enemies can attack.
- HP changes correctly.
- Victory/loss is detected.
- Core battle behavior can be tested without JavaFX.

### 5. NPC Finite State Machines

Add the assignment-critical FSM behavior and make transitions visible.

Deliverables:

- `BossEnemyStateMachine`.
- `TrashEnemyStateMachine`.
- State transition logs shown in dialogue box and optionally console.
- Boss phase thresholds: intro, >60%, <=60%, <=30%, dead.
- Trash thresholds: normal, aggressive, desperate, dead.

Feature brief:

- `FEATURES/FEATURE-05_NPC_FINITE_STATE_MACHINES.md`

Done when:

- Boss and trash enemies change states during battle.
- State changes affect available attacks.
- Transition messages are readable enough to screenshot for the PDF.

### 6. JavaFX Main Menu And Battle Canvas

Build the first playable UI shell around the existing services.

Deliverables:

- `MainMenuView` with Start Game.
- `BattleView` containing Canvas and input handling.
- `BattleRenderer`, `HudRenderer`, `DialogueBoxRenderer`, `SpriteLoader`.
- Basic attack menu with mouse or keyboard selection.
- Floor indicator, HP bars, sprites, and bottom dialogue box.

Feature brief:

- `FEATURES/FEATURE-06_JAVAFX_MENU_AND_BATTLE_VIEW.md`

Done when:

- The player can start the game from the main menu.
- The battle screen renders party, enemies, HP, floor, menu, and dialogue.
- Attack selection drives `BattleService`.

### 7. Player Attacks, Passives, Effects, And Cooldowns

Implement the party identity and simple combat effects.

Deliverables:

- Carl, Donut, and Mongo loaded from DB with exactly three attacks each.
- Mongo joins on floor 3.
- Simple effect support for healing, attack buffs, damage reduction, burn/bleed/infection-style damage over time where needed.
- Party passives implemented only as far as needed for readable gameplay.

Feature brief:

- `FEATURES/FEATURE-07_PARTY_ACTIONS_AND_EFFECTS.md`

Done when:

- Each party member has three usable actions.
- Cooldowns prevent repeated special spam.
- Effects are visible through HP changes and battle log text.
- Mongo appears from floor 3 onward.

### 8. Six Encounters And Boss Specials

Add the six encounter data sets and boss/trash-specific behavior incrementally.

Deliverables:

- Floor 1: Hoarder + Scatterers.
- Floor 2: Ralph.
- Floor 3: Heather.
- Floor 4: Gore-Gore.
- Floor 5: Denise.
- Floor 6: Circe + Mantis Nymphs.
- Encounter-specific behavior kept in service logic, not in DAO or views.

Feature brief:

- `FEATURES/FEATURE-08_SIX_ENCOUNTERS.md`

Done when:

- All six floors can be played in order.
- Each boss has phase-specific behavior.
- Trash enemies use their FSM where present.
- Difficulty is playable enough for screenshots and demonstration.

### 9. Victory, Game Over, And Highscore Save

Close the gameplay loop with end screens and persistence evidence.

Deliverables:

- `VictoryView`.
- `GameOverView`.
- Return-to-menu flow.
- Highscore/run summary model and save through `HighScoreDao`.
- Optional highscore display on main menu.

Feature brief:

- `FEATURES/FEATURE-09_END_STATES_AND_HIGHSCORE.md`

Done when:

- Winning and losing both lead to clear screens.
- The game can return to main menu.
- A run result can be saved to SQLite.

### 10. Polish, Balancing, And Test Pass

Stabilize the game before documentation.

Deliverables:

- Compile and run checks.
- Focused service tests for damage, FSM transitions, and floor progression.
- Simple balancing pass for HP/damage/cooldowns.
- Placeholder asset cleanup and path validation.

Feature brief:

- `FEATURES/FEATURE-10_POLISH_BALANCE_AND_TESTS.md`

Done when:

- Maven compiles and tests pass.
- A full playthrough is possible.
- No obvious UI overlap or broken asset paths remain.

### 11. Assignment Documentation Package

Prepare the final PDF and submission evidence.

Deliverables:

- Main menu screenshot.
- Battle screenshot.
- FSM screenshots or transcript.
- Class hierarchy overview.
- Architecture explanation.
- Database schema section.
- How-to-run section.
- Manual student reflection about AI use.
- ZIP root contains `AGENTS.md` and at least one `FEATURE.md`.

Feature brief:

- `FEATURES/FEATURE-11_DOCUMENTATION_PACKAGE.md`

Done when:

- PDF covers every assignment bullet.
- Screenshots match the final running app.
- Reflection is written manually by the student.

## Assets Needed

The game can start with placeholders, but final screenshots will look much better if these assets are provided consistently.

### Required Gameplay Assets

- Party idle sprites:
  - `carl_idle.png`
  - `donut_idle.png`
  - `mongo_idle.png`
- Enemy/boss idle sprites:
  - `hoarder_idle.png`
  - `scatterer_idle.png`
  - `ralph_idle.png`
  - `heather_idle.png`
  - `gore_gore_idle.png`
  - `denise_idle.png`
  - `circe_idle.png`
  - `mantis_nymph_idle.png`
- Six floor backgrounds:
  - `floor_01_background.png`
  - `floor_02_background.png`
  - `floor_03_background.png`
  - `floor_04_background.png`
  - `floor_05_background.png`
  - `floor_06_background.png`

### Optional But Useful Assets

- Hurt sprites for party and bosses.
- Attack pose sprites for party members.
- Small effect sprites for fire, heal, poison/infection, bleed, stun, shield.
- Portraits for dialogue:
  - `carl_portrait.png`
  - `donut_portrait.png`
  - `mongo_portrait.png`
  - boss portraits if desired.
- UI panel images:
  - `dialogue_panel.png`
  - `button_default.png`
  - `button_hover.png`
  - `button_selected.png`

### Asset Guidelines

- Use PNG.
- Use lowercase filenames with underscores.
- Keep a consistent pixel-art style.
- Prefer transparent backgrounds for sprites.
- Recommended sprite sizes:
  - Party and trash enemies: 96x96 or 128x128.
  - Bosses: 160x160 to 256x256.
  - Backgrounds: 1280x720 or 960x540.
- Store paths relative to resources, for example `/assets/sprites/party/carl_idle.png`.

### Fallback Plan

If final art is not ready, implement colored placeholder sprites or simple generated pixel-art placeholders first. The database should still use the final resource-relative paths so replacing assets later does not require code changes.

## Suggested Implementation Order

1. Setup Maven/JavaFX/SQLite.
2. Build models and enums.
3. Build schema, seed data, and DAOs.
4. Build game flow services.
5. Build battle MVP.
6. Add FSMs.
7. Add JavaFX menu and battle screen.
8. Add player effects and cooldowns.
9. Add all six encounters.
10. Add end states and highscore save.
11. Test, balance, and polish.
12. Produce final documentation.
