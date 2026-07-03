## Project Overview

This project is a private educational JavaFX game for the A08 "Dungeons & Java" assignment.

The game is a small 2D side-view turn-based dungeon battler. The player controls a fixed party of three characters: Carl, Donut, and Mongo. The party fights through six dungeon floors. Each floor contains one combat encounter with an enemy NPC boss which might include trash mobs too. Combat is turn-based and menu-driven.

The visual style is pixel art, shown from the side like a simplified fighting game or JRPG battle screen. The bottom part of the screen contains a dialogue/conversation box used for intro dialogue, attack feedback, state changes, and win/lose messages.

## Assignment Priorities

The project must satisfy the following core requirements:

1. Playable JavaFX game
2. Clear OOP architecture
3. At least two NPC types using finite state machines
4. DAO-based database integration
5. Short PDF documentation with screenshots, architecture notes, FSM tables/diagrams, database schema, and reflection

## Core Game Scope

### Included

* Main menu with "Start Game"
* Six-floor linear dungeon progression
* Turn-based battle screen
* Three playable party members
* Three fixed attacks per party member
* Enemy NPCs with three fixed attacks
* Boss NPCs with phase-based behaviour
* Dialogue box at the bottom of the screen
* Intro dialogue before each fight
* Battle log messages during combat
* Victory and game-over states
* Runtime data loading from SQLite through DAO classes

### Not Included

Do not implement these unless explicitly requested later:

* Multiplayer
* REST leaderboard
* Real-time movement
* Free dungeon exploration
* Procedural map generation
* Merchant/shop system
* Inventory system
* Equipment system
* Skill trees
* Random loot
* Save/load game state
* Complex animations
* Physics or collision systems
* Pathfinding

Keep the game small, stable, and easy to explain.

## Technology Stack

* Java 25+
* Maven
* JavaFX
* JavaFX Canvas with GraphicsContext for rendering
* SQLite
* JDBC
* DAO pattern
* 3-layer architecture
* Optional: JUnit for simple service tests

Do not use external game engines.

Do not use Lombok.

Do not use reflection-heavy frameworks.

Do not add dependencies unless they are clearly necessary.

## Runtime Flow

The intended game flow is:

```text
Application Start
→ Main Menu
→ Load database data
→ Create party
→ Floor 1 intro dialogue
→ Battle
→ Floor cleared
→ Floor 2 intro dialogue
→ Battle
→ ...
→ Floor 6 final battle
→ Victory screen
→ Return to main menu
```

If all player characters are defeated:

```text
Battle
→ Game Over
→ Return to main menu
```

## Gameplay Rules

### Party

The player has exactly three party members:

* Carl
* Donut
* Mongo → joins on floor 3

Party characters do not use finite state machines. They are controlled by the player through a simple turn-based attack menu.

Each party member has:

* name
* max HP
* current HP
* speed or initiative value
* three fixed attacks
* sprite path
* optional portrait path

The party members and their attacks are loaded from the database.
### Party Attacks and Passives

| Character     | Role                        | Passive                                                                                                                                                                            | Attack 1                                                                                 | Attack 2                                                                                                                                          | Attack 3                                                                                    |
|---------------| --------------------------- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------| ------------------------------------------------------------------------------------------- |
| Carl - 600HP  | Durable front-line fighter  | **Guardian** — when Carl drops below 30% HP, incoming damage against him is reduced by 50%. He also tanks the hit, if a team member would be downed by an attack. (Once per Fight) | **Roundhouse Kick** — single-target physical attack. — **DMG:** 40-80 (**CD:** 0)        | **Explosive Toss** — stronger multi-target attack with high damage. Hurts the party for 0–30% of the damage dealt. — **DMG:** 120-200 (**CD:** 0) | **Protective Shell** — defensive move that nullifies party damage for one turn. (**CD:** 3) |
| Donut - 200HP | Fast magic/support attacker | **Royal Charm** — Every second turn one random enemy gets chosen. Donut does not take damage from this enemy for one turn.                                                         | **Magic Missile** — ranged spell attack against one enemy. — **DMG:** 60-100 (**CD:** 0) | **Fireball** — strong multi-target spell that leaves enemies burning for one turn. — **DMG:** 250-320 (**CD:** 2)                                 | **Healing Song** — heals each party member for 20–30% of their max HP. (**CD:** 2)          |
| Mongo - 300HP | Aggressive melee attacker   | **Loyal Beast** — Mongo deals 30% to enemies who directly attacked Donut                                                                                                           | **Raptor Bite** — single-target bite attack. — **DMG:** 60-80 (**CD:** 0)                | **Gut Ripper** — damages one enemy and heals Mongo for 5–15% of his max HP. — **DMG:** 80-100 (**CD:** 1)                                         | **Raptor Roar** — increases party damage by 15% for one turn. (**CD:** 3)                   |

Attack effects should stay simple. Damage, healing, buffs, and debuffs are enough. The final implementation may rename attacks or adjust their exact behaviour, but every party member should keep exactly three fixed actions for a clean and readable battle system.


### Attacks

Each attack has:

* name
* description
* base damage
* target type
* optional effect
* optional cooldown

Keep attacks simple.

Recommended target types:

```text
SINGLE_ENEMY
ALL_ENEMIES
SELF
ALLY
```

Recommended effects:

```text
NONE
STUN
HEAL
BUFF_ATTACK
DEBUFF_DEFENSE
```

Only implement effects that are actually needed by the current game.

### Enemy Encounters, Attacks, Passives, and Phase Unlocks

The game contains six linear boss encounters. Each encounter is defined by a boss enemy and, where needed, additional TrashEnemy units.

Boss enemies use the global `BossEnemyState` FSM:

Boss attacks unlock by phase. Trash enemy attacks unlock by trash state. Cooldown reductions should stay simple and only happen where explicitly listed.

#### Encounter 1 — Hoarder + Scatterers

| Entity            | Type       | Passive / Special                                                 | Attack                                                                             | Unlock State | Cooldown / Phase Behaviour |
|-------------------| ---------- |-------------------------------------------------------------------|------------------------------------------------------------------------------------| ------------ |----------------------------|
| Hoarder - 1400HP  | BossEnemy  | **Greedy Bulk** — takes 15% less damage for each Scatterer alive. | **Garbage Spawn** — Spawns one Scatterer.                                          | PHASE_ONE    | **CD:** 0                  |
| Hoarder           | BossEnemy  | **Greedy Bulk** & **Garbage Spawn**                               | **Pile Collapse** — heavy multi-target junk attack. — **DMG:** 70-100              | PHASE_TWO    | **CD:** 0                  |
| Hoarder           | BossEnemy  | **Greedy Bulk** & **Garbage Spawn**                               | **Devour** — ingest all scatterers alive for their remaining HP                    | FINAL_PHASE  | **CD:** 0                  |
| Scatterer - 100HP | TrashEnemy | **NONE**                                                          | **Bug Bite** — weak single-target attack. — **DMG:** 20-30                         | NORMAL       | **CD:** 0                  |
| Scatterer         | TrashEnemy | **NONE**                                                          | **Claw Jab** — mediocre single-target attack. — **DMG:** 30-40                     | AGGRESSIVE   | **CD:** 0                  |
| Scatterer         | TrashEnemy | **NONE**                                                          | **Offering** — Offers himself to the Hoarder. Boss heals remaining HP of Scatterer | DESPERATE    | **CD:** 0                  |

Phase behaviour:

| State       | Behaviour                                                                                                               |
| ----------- |-------------------------------------------------------------------------------------------------------------------------|
| PHASE_ONE   | Hoarder only uses `Garbage Spawn`.                                                                                      |
| PHASE_TWO   | Hoarder  unlocks `Pile Collapse`. She spams it. `Garbage Spawn` gets passiveley triggered each turn. (max 4 scatterers) |
| FINAL_PHASE | Hoarder unlocks spamming `Devour` and uses it if there is at least one Scatterer.                                       |

#### Encounter 2 — Ralph

| Entity         | Type      | Passive / Special                                                                                                          | Attack                                                              | Unlock State | Cooldown / Phase Behaviour |
|----------------| --------- |----------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------| ------------ |----------------------------|
| Ralph - 1200HP | BossEnemy | **Pestilence** — infects direct target he attacks with a damage over time effect, which ticks for 30-45 **DMG** each turn. | **Rodent Bite** — basic single-target boss attack. — **DMG:** 40-60 | PHASE_ONE    | **CD:** 0                  |
| Ralph          | BossEnemy | **Pestilence**                                                                                                             | **Rake** — stronger physical attack. — **DMG:** 100-130             | PHASE_TWO    | **CD:** 0                  |
| Ralph          | BossEnemy | **Pestilence** & **Lethal Infection** — Party members infected with Pestilence die after their next turn.                  | **NONE**                                                            | FINAL_PHASE  | **CD:** 0                  |

Phase behaviour:

| State       | Behaviour                           |
| ----------- |-------------------------------------|
| PHASE_ONE   | Ralph only uses `Rodent Bite`.      |
| PHASE_TWO   | Ralph  unlocks `Rake`. He spams it. |
| FINAL_PHASE | Ralph keeps spamming `Rake`         |


#### Encounter 3 — Heather

| Entity           | Type      | Passive / Special                                                                                            | Attack                                                                                                  | Unlock State | Cooldown / Phase Behaviour |
|------------------| --------- |--------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------| ------------ |----------------------------|
| Heather - 2400HP | BossEnemy | **Acceleration** — Each percent of missing HP makes her faster & increases Roller Skate Charge Damage by 1%. | **Bear Maul** — strong single-target physical attack. — **DMG:** 90-130                                 | PHASE_ONE    | **CD:** 0                  |
| Heather          | BossEnemy | **Acceleration**                                                                                             | **Roller Skate Charge** — high-damage attack with a 50% chance to stun for one turn. — **DMG:** 120-180 | PHASE_TWO    | **CD:** 1                  |
| Heather          | BossEnemy | **Acceleration**                                                                                             | **Hibernate** — Takes 80% less damage for two turns. Heals by 30% each turn but can not attack.         | FINAL_PHASE  | **CD:** 3                  |

Phase behaviour:

| State       | Behaviour                                                           |
| ----------- |---------------------------------------------------------------------|
| PHASE_ONE   | Heather only uses `Bear Maul`.                                      |
| PHASE_TWO   | Heather unlocks `Roller Skate Charge`. She uses it whenever she can |
| FINAL_PHASE | Heathere starts `Hibernate`. She does not attack for 2 turns.       |

#### Encounter 4 — Gore-Gore

| Entity             | Type      | Passive / Special                                             | Attack                                                                                                                              | Unlock State | Cooldown / Phase Behaviour |
|--------------------| --------- |---------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------------------------|
| Gore-Gore - 2600HP | BossEnemy | **Blood Frenzy** — deals 20% more damage on bleeding targets. | **Slash** — aggressive single-target attack. 50% chance to leave the target bleeding.  — **DMG:** 100-120                           | PHASE_ONE    | **CD:** 0                  |
| Gore-Gore          | BossEnemy | **Blood Frenzy**                                              | **Meat Hook** — heavy attack against the party member with the lowest HP. Leaves target bleeding & Ignores Charm — **DMG:** 160-200 | PHASE_TWO    | **CD:** 1                  |
| Gore-Gore          | BossEnemy | **Blood Frenzy**                                              | **Summon Gruul** — Summoning the war god Gruul takes two turns. If it completes all party members die.                              | FINAL_PHASE  | **CD:** 0                  |

Phase behaviour:

| State       | Behaviour                                                          |
| ----------- |--------------------------------------------------------------------|
| PHASE_ONE   | Gore-Gore only uses `Slash`.                                       |
| PHASE_TWO   | Gore-Gore unlocks `Meat Hook`. He uses it whenever she can         |
| FINAL_PHASE | Gore-Gore starts `Summon Gruul`. Killing him fast is the only way. |

#### Encounter 5 — Denise

| Entity          | Type      | Passive / Special                                                                                                                                     | Attack                                                                                                                                                         | Unlock State | Cooldown / Phase Behaviour |
|-----------------| --------- |-------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------------------------|
| Denise - 1800HP | BossEnemy | **Nothing-Touched Mother** — reduces incoming magic damage by 50%. Reduction increases by 0.5% with each percent of less HP                           | **Goose Bite** — fast single-target physical attack. — **DMG:** 60-90                                                                                          | PHASE_ONE    | **CD:** 0                  |
| Denise          | BossEnemy | **Nothing-Touched Mother**                                                                                                                            | **Mother's Honk** — loud AoE attack that deals low damage and lowers party damage by 50% for one turn. Permanently nerfs party damage by 10%. — **DMG:** 30-50 | PHASE_TWO    | **CD:** 1                  |
| Denise          | BossEnemy | **Nothing-Touched Mother** & **Reflect** — 15-30% of damage taken gets reflected back on the attacker, lowering the amount of damage taken by denise. | **Mother's Honk**                                                                                                                                              | FINAL_PHASE  | **CD:** 0                  |

Phase behaviour:

| State       | Behaviour                                                     |
| ----------- |---------------------------------------------------------------|
| PHASE_ONE   | Denise only uses `Goose Bite`.                                |
| PHASE_TWO   | Denise unlocks `Mother's Honk`. She uses it whenever she can  |
| FINAL_PHASE | Denise spams `Mother's Honk` each turn.                       |


#### Encounter 6 — Circe

| Entity               | Type       | Passive / Special                                                                                     | Attack                                                                                                                                            | Unlock State | Cooldown / Phase Behaviour |
|----------------------| ---------- |-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------| ------------ |----------------------------|
| Circe - 3000HP       | BossEnemy  | **Brood Mother** — gives birth to one Mantis Nymph at the start of each of her turns. (starts with 2) | **Twin Babies** — she gives birth to an extra Nymph.                                                                                              | PHASE_ONE    | **CD:** 0                  |
| Circe                | BossEnemy  | **Brood Mother**                                                                                      | **Gelee Royale** — she feeds the nymphs giving them 10-25% Damage Boost for their next attack                                                     | PHASE_TWO    | **CD:** 1                  |
| Circe                | BossEnemy  | **Mantis Queen Form** — Circe leaves her passive Brood Mother Form.                                   | **Decapitate** — extremely high damage targeted attack. **DMG:** 180-250                                                                          | FINAL_PHASE  | **CD:** 0                  |
| Mantis Nymph - 120HP | TrashEnemy | **Fresh Hatchling** — low HP add spawned by Circe.                                                    | **Bug Slash** — weak single-target attack. **DMG:** 20-40                                                                                         | NORMAL       | **CD:** 0                  |
| Mantis Nymph         | TrashEnemy | **Fresh Hatchling**                                                                                   | **Swarm Bite** — 20% stronger for each other nymph alive. **DMG:** 20-30                                                                          | AGGRESSIVE   | **CD:** 0                  |
| Mantis Nymph         | TrashEnemy | **Fresh Hatchling**                                                                                   | **Wild Slash** — Slashes like a maniac all over the place. Total Damage is randomly distributed between all involved characters. **DMG:** 100-180 | DESPERATE    | **CD:** 0                  |

Phase behaviour:

| State       | Behaviour                                                   |
| ----------- |-------------------------------------------------------------|
| PHASE_ONE   | Circe only uses `Twin Babies`.                              |
| PHASE_TWO   | Diwata unlocks `Gelee Royale`. She uses it whenever she can |
| FINAL_PHASE | Diwata turns into Circe. She spams `Decapitate` each turn.  |

## Finite State Machines

The assignment requires at least two NPC types with distinct finite state machines. This project uses:

1. TrashEnemy FSM
2. BossEnemy FSM

State changes must be visible either in the dialogue box, console log, or PDF screenshots.

### TrashEnemy FSM

Use this enum:

```java
public enum TrashEnemyState {
    NORMAL,
    AGGRESSIVE,
    DESPRATE,
    DEAD
}
```

State behaviour:

| State      | Behaviour                        | Transition Trigger |
|------------|----------------------------------| ------------------ |
| NORMAL     | Uses basic attacks               | Enemy HP <= 60%    |
| AGGRESSIVE | Uses stronger attacks more often | Enemy HP <= 25%    |
| DESPERATE  | Uses last resort abilities       | Enemy HP <= 0      |
| DEAD       | Cannot act                       | Final state        |

The FSM update method must be called during the battle update cycle, especially after damage is applied and before the enemy chooses an action.

### BossEnemy FSM

Use this enum:

```java
public enum BossEnemyState {
    INTRO,
    PHASE_ONE,
    PHASE_TWO,
    FINAL_PHASE,
    DEAD
}
```

State behaviour:

| State       | Behaviour                         | Transition Trigger      |
| ----------- | --------------------------------- |-------------------------|
| INTRO       | Shows boss intro dialogue         | Intro dialogue finished |
| PHASE_ONE   | Uses standard boss attack pattern | Boss HP > 60%           |
| PHASE_TWO   | Unlocks stronger attack pattern   | Boss HP <= 60%          |
| FINAL_PHASE | Uses strongest attack pattern     | Boss HP <= 30%          |
| DEAD        | Cannot act                        | Boss HP <= 0%             |

Boss state changes should print readable messages, for example:

```text
BossEnemy: PHASE_ONE → PHASE_TWO because HP dropped below 60%.
```

These messages can be used in the PDF as FSM demonstration evidence.

## Architecture Rules

Use a clean package structure. Keep model, service, DAO, controller, and view responsibilities separate.

### Required Layer Separation

* No SQL in model classes.
* No SQL in controller classes.
* No SQL in view classes.
* No JavaFX rendering code in DAO classes.
* No battle logic in DAO implementations.
* No database access directly from JavaFX views.
* Model classes should mainly represent game state.
* Service classes contain game rules.
* DAO classes only load and save data.
* Controllers connect UI input to services.
* Views render the current state.

## Recommended Package Structure

Use this structure:

```text
src/
  main/
    java/
      at/fhooe/ald/
        App.java
        Main.java

        config/
          GameConfig.java
          DatabaseConfig.java

        controller/
          MainMenuController.java
          BattleController.java
          GameController.java

        model/
          Entity.java
          Battler.java
          PlayerCharacter.java
          Enemy.java
          TrashEnemy.java
          BossEnemy.java
          Attack.java
          AttackEffect.java
          TargetType.java
          Battle.java
          BattleResult.java
          BattleTurn.java
          Floor.java
          DialogueLine.java
          HighScore.java

        model/fsm/
          StateMachine.java
          TrashEnemyState.java
          BossEnemyState.java
          TrashEnemyStateMachine.java
          BossEnemyStateMachine.java

        service/
          GameService.java
          BattleService.java
          EncounterService.java
          DialogueService.java
          DamageCalculator.java
          TargetSelector.java
          HighScoreService.java

        dao/
          CharacterDao.java
          AttackDao.java
          EnemyDao.java
          FloorDao.java
          DialogueDao.java
          HighScoreDao.java

        dao/jdbc/
          JdbcCharacterDao.java
          JdbcAttackDao.java
          JdbcEnemyDao.java
          JdbcFloorDao.java
          JdbcDialogueDao.java
          JdbcHighScoreDao.java
          Database.java
          DatabaseInitializer.java

        view/
          MainMenuView.java
          BattleView.java
          VictoryView.java
          GameOverView.java

        view/render/
          BattleRenderer.java
          SpriteLoader.java
          HudRenderer.java
          DialogueBoxRenderer.java

        util/
          ResourcePaths.java
          RandomProvider.java
          TextFormatter.java

    resources/
      db/
        schema.sql
        seed.sql
        game.db

      assets/
        sprites/
          party/
          enemies/
          bosses/
          effects/

        backgrounds/
          floors/

        ui/
          buttons/
          panels/
          icons/
```

## OOP Design

Use meaningful inheritance.

Recommended class hierarchy:

```text
Entity
└── Battler
    ├── PlayerCharacter
    └── Enemy
        ├── TrashEnemy
        └── BossEnemy
```

Rules:

* All fields must be private.
* Use getters where needed.
* Use methods for state changes instead of public fields.
* Use `@Override` where inheritance methods are implemented.
* Do not expose mutable internal lists directly.
* Prefer small methods with clear responsibilities.

Recommended interfaces:

```java
public interface TurnActor {
    boolean canAct();
    BattleAction chooseAction(Battle battle);
}
```

```java
public interface Targetable {
    boolean isAlive();
    int getCurrentHp();
    void receiveDamage(int amount);
}
```

```java
public interface StateMachine<S extends Enum<S>> {
    S getCurrentState();
    void update(Battle battle);
}
```

Rendering should preferably be handled by view/render classes instead of putting JavaFX code directly into model entities.

## JavaFX Rendering Rules

Use JavaFX Canvas and GraphicsContext for the main battle screen.

The battle screen should include:

* background image for the current floor
* player party sprites on one side
* enemy sprites on the other side
* HP bars
* attack menu
* dialogue/conversation box at the bottom
* current floor indicator
* simple victory/game-over messages

Keyboard input may be used for menu navigation, but mouse buttons are acceptable for attack selection if implemented clearly.

Recommended controls:

```text
Mouse click: choose menu option
Enter: confirm selected option
Arrow keys / WASD: move menu selection
Escape: back/cancel where useful
```

Do not overbuild the UI.

## Database Design

Use SQLite.

Game data should be loaded from the database at startup or when a floor begins.

Recommended tables:

```sql
characters
attacks
character_attacks
enemies
enemy_attacks
floors
floor_enemies
dialogue_lines
high_scores
```

Minimum DAO interfaces:

```text
CharacterDao
AttackDao
EnemyDao
DialogueDao
HighScoreDao
```

At least two DAO interfaces are required, but this project should use separate DAOs for clarity.

### DAO Rules

* Use JDBC implementations.
* Use PreparedStatement for all queries.
* Do not concatenate user input into SQL strings.
* Implement `findAll()` where useful.
* Implement at least one save operation, preferably `HighScoreDao.save(score)`.
* Keep SQL inside DAO/JDBC classes only.
* Keep game logic outside DAO classes.

### Runtime Database Handling

Recommended approach:

* Store initial `game.db`, `schema.sql`, and `seed.sql` under `src/main/resources/db`.
* On first application start, copy `game.db` to a writable user directory.
* Use the copied database for runtime access so highscores can be saved.

Example target location:

```text
user.home/.a08-dungeon-battler/game.db
```

## Asset Rules

Sprites should be loaded from `src/main/resources/assets`.

Use lowercase filenames with underscores.

Good:

```text
carl_idle.png
donut_attack.png
mongo_hurt.png
floor_01_background.png
boss_03_idle.png
dialogue_panel.png
```

Bad:

```text
Carl FINAL new.png
Sprite(1).png
boss-final-copy-copy.png
```

All sprite paths stored in the database should use resource-relative paths, for example:

```text
/assets/sprites/party/carl_idle.png
```

## Content and Naming

This is a private educational fan project. Do not add public distribution wording, store pages, marketing text, or commercial language.

Do not add copyright claims over existing protected names or characters.

Do not add public GitHub deployment instructions.

The repository is intended to remain private.

## Documentation Requirements

The final PDF should contain:

* screenshot of the running main menu
* screenshot of the battle screen
* class hierarchy overview
* short architecture note
* FSM table or diagram for TrashEnemy
* FSM table or diagram for BossEnemy
* FSM transition transcript or screenshots
* database schema
* short how-to-run note
* reflection about AI use

The reflection must be written manually by the student.

## Development Workflow for AI Assistants

When helping with this project:

1. Keep the scope small.
2. Do not introduce new systems unless requested.
3. Prefer simple working code over complex abstractions.
4. Preserve the package structure.
5. Keep battle logic in service classes.
6. Keep SQL in DAO/JDBC classes.
7. Keep JavaFX rendering in view/render classes.
8. After code changes, ensure Maven still compiles.
9. Add comments only where they clarify non-obvious logic.
10. Do not silently rename major classes or packages.
11. Do not replace the architecture without explaining why.
12. Do not add networking or multiplayer.
13. Do not add a merchant, shop, inventory, equipment, or procedural dungeon.
14. Do not hardcode data that should come from the database.

## Definition of Done

The project is considered complete when:

* Maven project compiles and runs.
* Main menu starts the game.
* Six floors can be played in sequence.
* The player can win or lose.
* Three party members are visible and usable.
* Each party member has three attacks.
* Enemies and bosses can attack.
* TrashEnemy uses a visible FSM.
* BossEnemy uses a visible FSM.
* Game data is loaded through DAOs from SQLite.
* Highscore can be saved.
* JavaFX Canvas renders the battle screen.
* HUD shows HP and floor/combat status.
* Dialogue box displays intro and battle text.
* PDF documentation contains screenshots, architecture note, FSM documentation, database schema, and reflection.
* ZIP root contains `AGENTS.md` and at least one `FEATURE.md`.
