# FEATURE-06B: Tactical Battle UI Flow

## Purpose

Prepare the battle screen for a readable turn-by-turn JRPG-style flow instead of resolving actions instantly.

## Scope

- Add a small UI input state machine for battle interaction:
  - `SELECT_ATTACK`
  - `SHOW_ATTACK_DESCRIPTION`
  - `SELECT_TARGET`
  - `RESOLVE_ACTION`
  - `WAIT_FOR_CONFIRM`
  - `ENEMY_TURN`
- Show the selected attack description in the dialogue box before target selection.
- Let the player choose a valid target after selecting an attack.
- Keep target validation in service/controller logic, not inside rendering code.
- Split action resolution into readable battle events, for example:
  - actor announces the selected attack
  - animation or sound placeholder can play
  - damage/healing result is shown
  - status/effect changes are shown
- Require Enter or click confirmation between battle event messages.
- Add a turn preview column showing the next five actors by portrait.
- Use each battler's speed/haste value to determine turn order preview.
- Keep the battle UI layout compatible with the planned final HUD:
  - bottom left: player team portraits, names, and HP
  - bottom middle: dialogue and attack descriptions
  - bottom right: enemy portraits, names, and HP bars
  - top right: current floor number and floor title
  - left side: next five turns

## Acceptance Criteria

- Clicking an attack does not immediately resolve the whole turn.
- The dialogue box can show the selected attack description.
- The player can select a target for single-target attacks.
- Battle messages can be advanced step by step.
- The renderer has stable regions for party HUD, enemy HUD, dialogue, floor info, and turn preview.
- The turn preview is derived from battle/service state and not hardcoded in the view.

## Depends On

- `FEATURE-04_BATTLE_ENGINE_MVP.md`
- `FEATURE-06_JAVAFX_MENU_AND_BATTLE_VIEW.md`
