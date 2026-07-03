# FEATURE-06: JavaFX Menu And Battle View

## Purpose

Build the first usable JavaFX interface around the game and battle services.

## Scope

- Add `MainMenuView`.
- Add `BattleView`.
- Add `BattleRenderer`, `HudRenderer`, `DialogueBoxRenderer`, and `SpriteLoader`.
- Render floor background, party sprites, enemy sprites, HP bars, floor indicator, attack menu, and dialogue box.
- Handle mouse or keyboard attack selection.

## Acceptance Criteria

- Main menu has a Start Game action.
- Battle screen is rendered on Canvas.
- Player can select attacks through the UI.
- Dialogue and battle log messages appear at the bottom.

## Depends On

- `FEATURE-04_BATTLE_ENGINE_MVP.md`
- `FEATURE-05_NPC_FINITE_STATE_MACHINES.md`
