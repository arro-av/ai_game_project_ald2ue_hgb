DELETE FROM high_scores;
DELETE FROM dialogue_lines;
DELETE FROM floor_enemies;
DELETE FROM floors;
DELETE FROM enemy_attacks;
DELETE FROM enemies;
DELETE FROM character_attacks;
DELETE FROM attacks;
DELETE FROM characters;

INSERT INTO characters (id, name, max_hp, speed, sprite_path, portrait_path, join_floor) VALUES
(1, 'Carl', 600, 30, '/assets/sprites/party/carl_idle.png', '/assets/sprites/party/carl_portrait.png', 1),
(2, 'Donut', 200, 50, '/assets/sprites/party/donut_idle.png', '/assets/sprites/party/donut_portrait.png', 1),
(3, 'Mongo', 300, 60, '/assets/sprites/party/mongo_idle.png', '/assets/sprites/party/mongo_portrait.png', 3);

INSERT INTO attacks (id, name, description, min_damage, max_damage, target_type, effect, cooldown) VALUES
(1, 'Roundhouse Kick', 'Single-target physical attack.', 60, 120, 'SINGLE_ENEMY', 'NONE', 0),
(2, 'Explosive Toss', 'Strong multi-target attack that can potentially hurt the party.', 200, 260, 'ALL_ENEMIES', 'NONE', 1),
(3, 'Protective Shell', 'Nullifies party damage for one turn.', 0, 0, 'ALL_ALLIES', 'SHIELD', 4),
(4, 'Magic Missile', 'Ranged spell attack against one enemy.', 100, 140, 'SINGLE_ENEMY', 'NONE', 0),
(5, 'Fireball', 'Strong multi-target spell that leaves enemies burning.', 250, 320, 'ALL_ENEMIES', 'BURN', 2),
(6, 'Healing Song', 'Heals each party member and cleanses the direct target.', 120, 160, 'ALL_ALLIES', 'HEAL', 2),
(7, 'Raptor Bite', 'Single-target bite attack.', 90, 120, 'SINGLE_ENEMY', 'BLEED', 0),
(8, 'Gut Ripper', 'Damages one enemy and heals Mongo.', 60, 90, 'SINGLE_ENEMY', 'HEAL', 1),
(9, 'Raptor Roar', 'Increases party damage for one turn.', 0, 0, 'ALL_ALLIES', 'BUFF_ATTACK', 3),
(101, 'Garbage Spawn', 'Spawns one Scatterer.', 0, 0, 'SELF', 'SPAWN', 0),
(102, 'Pile Collapse', 'Heavy multi-target junk attack.', 70, 100, 'ALL_ENEMIES', 'NONE', 0),
(103, 'Devour', 'Ingests all Scatterers alive for their remaining HP.', 0, 0, 'SELF', 'DEVOUR', 0),
(104, 'Bug Bite', 'Weak single-target attack.', 20, 30, 'SINGLE_ENEMY', 'NONE', 0),
(105, 'Claw Jab', 'Mediocre single-target attack.', 30, 40, 'SINGLE_ENEMY', 'NONE', 0),
(106, 'Offering', 'Offers itself to the boss.', 0, 0, 'SINGLE_ALLY', 'HEAL', 0),
(201, 'Rodent Bite', 'Basic single-target boss attack that applies Pestilence.', 60, 90, 'SINGLE_ENEMY', 'INFECTION', 0),
(202, 'Rake', 'Double physical attack that applies Pestilence.', 100, 130, 'SINGLE_ENEMY', 'INFECTION', 0),
(301, 'Bear Maul', 'Strong single-target physical attack.', 0, 110, 'ALL_ENEMIES', 'BLEED', 0),
(302, 'Roller Skate Charge', 'High-damage attack with stun chance.', 120, 160, 'SINGLE_ENEMY', 'STUN', 1),
(303, 'Hibernate', 'Reduces damage and heals for two turns.', 0, 0, 'SELF', 'HEAL', 3),
(401, 'Slash', 'Aggressive attack that can leave bleeding.', 100, 120, 'SINGLE_ENEMY', 'BLEED', 0),
(402, 'Meat Hook', 'Heavy attack against the lowest HP party member.', 160, 200, 'LOWEST_HP_ALLY', 'BLEED', 1),
(403, 'Summon Gruul', 'Two-turn final phase countdown.', 0, 0, 'ALL_ENEMIES', 'NONE', 0),
(501, 'Goose Bite', 'Fast single-target physical attack.', 60, 90, 'SINGLE_ENEMY', 'NONE', 0),
(502, 'Mother''s Honk', 'AoE attack that lowers party damage.', 30, 50, 'ALL_ENEMIES', 'DEBUFF_DEFENSE', 1),
(601, 'Twin Babies', 'Creates an extra Mantis Nymph.', 0, 0, 'SELF', 'SPAWN', 0),
(602, 'Gelee Royale', 'Boosts nymph damage for their next attack.', 0, 0, 'ALL_ALLIES', 'BUFF_ATTACK', 1),
(603, 'Decapitate', 'Extremely high damage targeted attack.', 180, 250, 'SINGLE_ENEMY', 'NONE', 0),
(604, 'Bug Slash', 'Weak single-target attack.', 20, 40, 'SINGLE_ENEMY', 'NONE', 0),
(605, 'Swarm Bite', 'Stronger for each other nymph alive.', 20, 30, 'SINGLE_ENEMY', 'BUFF_ATTACK', 0),
(606, 'Wild Slash', 'Randomly distributed desperate damage.', 100, 180, 'RANDOM_ENEMY', 'NONE', 0);

INSERT INTO character_attacks (character_id, attack_id, slot) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3),
(2, 4, 1), (2, 5, 2), (2, 6, 3),
(3, 7, 1), (3, 8, 2), (3, 9, 3);

INSERT INTO enemies (id, name, enemy_type, max_hp, speed, sprite_path, passive_name, passive_description) VALUES
(1, 'Hoarder', 'BOSS', 1400, 20, '/assets/sprites/bosses/hoarder_idle.png', 'Greedy Bulk', 'Takes less damage for each Scatterer alive.'),
(2, 'Scatterer', 'TRASH', 100, 40, '/assets/sprites/enemies/scatterer_idle.png', 'None', ''),
(3, 'Ralph', 'BOSS', 1200, 60, '/assets/sprites/bosses/ralph_idle.png', 'Pestilence', 'Direct attacks poison the target. In final phase this becomes Lethal Infection.'),
(4, 'Heather', 'BOSS', 2400, 40, '/assets/sprites/bosses/heather_idle.png', 'Acceleration', 'Missing HP makes her faster and stronger.'),
(5, 'Gore-Gore', 'BOSS', 2600, 30, '/assets/sprites/bosses/gore_gore_idle.png', 'Blood Frenzy', 'Deals more damage to bleeding targets.'),
(6, 'Denise', 'BOSS', 1800, 40, '/assets/sprites/bosses/denise_idle.png', 'Nothing-Touched Mother', 'Reduces incoming magic damage.'),
(7, 'Circe', 'BOSS', 3000, 70, '/assets/sprites/bosses/circe_idle.png', 'Brood Mother', 'Spawns a Mantis Nymph at the start of her turns.'),
(8, 'Mantis Nymph', 'TRASH', 120, 60, '/assets/sprites/enemies/mantis_nymph_idle.png', 'Fresh Hatchling', 'Low HP add spawned by Circe.');

INSERT INTO enemy_attacks (enemy_id, attack_id, slot, unlock_state) VALUES
(1, 101, 1, 'PHASE_ONE'), (1, 102, 2, 'PHASE_TWO'), (1, 103, 3, 'FINAL_PHASE'),
(2, 104, 1, 'NORMAL'), (2, 104, 2, 'AGGRESSIVE'), (2, 106, 3, 'DESPERATE'),
(3, 201, 1, 'PHASE_ONE'), (3, 202, 2, 'PHASE_TWO'), (3, 202, 3, 'FINAL_PHASE'),
(4, 301, 1, 'PHASE_ONE'), (4, 302, 2, 'PHASE_TWO'), (4, 303, 3, 'FINAL_PHASE'),
(5, 401, 1, 'PHASE_ONE'), (5, 402, 2, 'PHASE_TWO'), (5, 403, 3, 'FINAL_PHASE'),
(6, 501, 1, 'PHASE_ONE'), (6, 502, 2, 'PHASE_TWO'), (6, 502, 3, 'FINAL_PHASE'),
(7, 601, 1, 'PHASE_ONE'), (7, 602, 2, 'PHASE_TWO'), (7, 603, 3, 'FINAL_PHASE'),
(8, 604, 1, 'NORMAL'), (8, 605, 2, 'AGGRESSIVE'), (8, 606, 3, 'DESPERATE');

INSERT INTO floors (id, floor_number, name, background_path) VALUES
(1, 1, 'Hoarder Den', '/assets/backgrounds/floors/floor_01_background.png'),
(2, 2, 'Rat Nest', '/assets/backgrounds/floors/floor_02_background.png'),
(3, 3, 'Grimaldi Circus', '/assets/backgrounds/floors/floor_03_background.png'),
(4, 4, 'Subway Tangle', '/assets/backgrounds/floors/floor_04_background.png'),
(5, 5, 'Mother''s Roost', '/assets/backgrounds/floors/floor_05_background.png'),
(6, 6, 'Mantis Hive', '/assets/backgrounds/floors/floor_06_background.png');

INSERT INTO floor_enemies (floor_id, enemy_id, quantity) VALUES
(1, 1, 1), (1, 2, 2),
(2, 3, 1),
(3, 4, 1),
(4, 5, 1),
(5, 6, 1),
(6, 7, 1), (6, 8, 2);

INSERT INTO dialogue_lines (id, floor_number, speaker, text, display_order) VALUES
(1, 1, 'System AI', 'B-B-B-BOSSBATTLE! This room smells like wet trash and bad decisions. The encounter on this floor is the one and only disgusting Hoarder. Good luck noobs!', 1),
(2, 2, 'System AI', 'B-B-B-BOSSBATTLE! Ralph is waiting in the Rat Nest, and he did not brush his teeth. If you ever wondered where the plague originated... it was this motherfucker!', 1),
(3, 3, 'System AI', 'B-B-B-BOSSBATTLE! Welcome to the Circus. If you hate clowns, do not worry, we are not THIS crazy. In fact you are facing Heather the cursed undead ballerina bear. Playing dead does not work with this savage!', 1),
(4, 4, 'System AI', 'B-B-B-BOSSBATTLE! Your train got cancelled. But you are lucky, Gore-Gore owns this stop, and every rail line ends in bleeding. Oh.. maybe you are the opposite of lucky lol.', 1),
(5, 5, 'System AI', 'B-B-B-BOSSBATTLE! You like crispy duck? Murderer! Shame on you. Denise has entered the arena, and her honk is absolutely HONKING! Prepare to get rect you son of a beautiful woman.', 1),
(6, 6, 'System AI', 'FINAL B-B-B-BOSSBATTLE!: You bastards made it this far?! Well your lucky strike is over, because Circe the brood mother is all violence and dominance. Having fun with this babe ends in decapitation!', 1);
