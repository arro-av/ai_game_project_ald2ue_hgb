# FEATURE-07: Party Actions And Effects

## Purpose

Make Carl, Donut, and Mongo feel distinct while keeping combat effects simple.

## Scope

- Implement three fixed attacks per party member.
- Implement cooldowns.
- Implement simple effects used by the party: healing, damage prevention, attack buff, burn, and self-damage where needed.
- Implement simple passives if they can be kept readable.
- Ensure Mongo joins on floor 3.

## Acceptance Criteria

- Carl, Donut, and Mongo each have exactly three usable actions.
- Attack effects visibly change battle state.
- Cooldowns are shown or communicated clearly.
- Effects create battle log messages.

## Depends On

- `FEATURE-04_BATTLE_ENGINE_MVP.md`
- `FEATURE-06_JAVAFX_MENU_AND_BATTLE_VIEW.md`
- `FEATURE-06B_TACTICAL_BATTLE_UI_FLOW.md`
