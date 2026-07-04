PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS characters (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    max_hp INTEGER NOT NULL,
    speed INTEGER NOT NULL,
    sprite_path TEXT NOT NULL,
    portrait_path TEXT,
    join_floor INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS attacks (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    min_damage INTEGER NOT NULL DEFAULT 0,
    max_damage INTEGER NOT NULL DEFAULT 0,
    target_type TEXT NOT NULL,
    effect TEXT NOT NULL DEFAULT 'NONE',
    cooldown INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS character_attacks (
    character_id INTEGER NOT NULL,
    attack_id INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    PRIMARY KEY (character_id, attack_id),
    FOREIGN KEY (character_id) REFERENCES characters(id),
    FOREIGN KEY (attack_id) REFERENCES attacks(id)
);

CREATE TABLE IF NOT EXISTS enemies (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    enemy_type TEXT NOT NULL,
    max_hp INTEGER NOT NULL,
    speed INTEGER NOT NULL,
    sprite_path TEXT NOT NULL,
    passive_name TEXT,
    passive_description TEXT
);

CREATE TABLE IF NOT EXISTS enemy_attacks (
    enemy_id INTEGER NOT NULL,
    attack_id INTEGER NOT NULL,
    slot INTEGER NOT NULL,
    unlock_state TEXT NOT NULL,
    PRIMARY KEY (enemy_id, slot),
    FOREIGN KEY (enemy_id) REFERENCES enemies(id),
    FOREIGN KEY (attack_id) REFERENCES attacks(id)
);

CREATE TABLE IF NOT EXISTS floors (
    id INTEGER PRIMARY KEY,
    floor_number INTEGER NOT NULL UNIQUE,
    name TEXT NOT NULL,
    background_path TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS floor_enemies (
    floor_id INTEGER NOT NULL,
    enemy_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (floor_id, enemy_id),
    FOREIGN KEY (floor_id) REFERENCES floors(id),
    FOREIGN KEY (enemy_id) REFERENCES enemies(id)
);

CREATE TABLE IF NOT EXISTS dialogue_lines (
    id INTEGER PRIMARY KEY,
    floor_number INTEGER NOT NULL,
    speaker TEXT,
    text TEXT NOT NULL,
    display_order INTEGER NOT NULL,
    FOREIGN KEY (floor_number) REFERENCES floors(floor_number)
);

CREATE TABLE IF NOT EXISTS high_scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_name TEXT NOT NULL,
    floors_cleared INTEGER NOT NULL,
    victory INTEGER NOT NULL,
    turns_taken INTEGER NOT NULL,
    created_at TEXT NOT NULL
);
