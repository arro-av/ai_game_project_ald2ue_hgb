package at.fhooe.ald;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.fhooe.ald.dao.jdbc.DatabaseInitializer;
import at.fhooe.ald.dao.jdbc.JdbcAttackDao;
import at.fhooe.ald.dao.jdbc.JdbcCharacterDao;
import at.fhooe.ald.dao.jdbc.JdbcDialogueDao;
import at.fhooe.ald.dao.jdbc.JdbcEnemyDao;
import at.fhooe.ald.dao.jdbc.JdbcFloorDao;
import at.fhooe.ald.dao.jdbc.JdbcHighScoreDao;
import at.fhooe.ald.controller.GameController;
import at.fhooe.ald.model.BattleResult;
import at.fhooe.ald.model.Battler;
import at.fhooe.ald.model.BossEnemy;
import at.fhooe.ald.model.HighScore;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.TargetType;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.model.TrashEnemy;
import at.fhooe.ald.model.fsm.BossEnemyState;
import at.fhooe.ald.model.fsm.TrashEnemyState;
import at.fhooe.ald.service.BattleService;
import at.fhooe.ald.service.DialogueService;
import at.fhooe.ald.service.DamageCalculator;
import at.fhooe.ald.service.EncounterService;
import at.fhooe.ald.service.GameService;
import at.fhooe.ald.service.GameStatus;
import at.fhooe.ald.service.HighScoreService;
import at.fhooe.ald.service.TargetSelector;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void projectBaselineIsConfigured() {
        assertEquals("at.fhooe.ald", App.class.getPackageName());
    }

    @Test
    void sqliteDaosLoadSeedDataAndSaveHighScore() throws Exception {
        var testDaos = createTestDaos();

        assertEquals(2, testDaos.characterDao.findAvailableForFloor(1).size());
        assertEquals(3, testDaos.characterDao.findAvailableForFloor(3).size());
        assertEquals(6, testDaos.floorDao.findAll().size());
        assertEquals(3, testDaos.floorDao.findByNumber(1).orElseThrow().getEnemies().size());
        for (int floorNumber = 1; floorNumber <= 6; floorNumber++) {
            assertFalse(testDaos.dialogueDao.findByFloorNumber(floorNumber).isEmpty());
        }

        testDaos.highScoreDao.save(new HighScore(0, "Test", 6, true, 42, LocalDateTime.now()));

        assertTrue(testDaos.highScoreDao.findAll().stream().anyMatch(HighScore::isVictory));
    }

    @Test
    void gameFlowStartsOnFloorOneAndAddsMongoOnFloorThree() throws Exception {
        var gameService = createGameService();

        var firstBattle = gameService.startNewGame();

        assertEquals(GameStatus.IN_BATTLE, gameService.getStatus());
        assertEquals(1, firstBattle.getFloorNumber());
        assertEquals(2, firstBattle.getParty().size());
        assertTrue(firstBattle.getParty().stream().noneMatch(character -> character.getName().equals("Mongo")));

        gameService.clearCurrentFloor();
        var thirdBattle = gameService.clearCurrentFloor();

        assertEquals(3, thirdBattle.getFloorNumber());
        assertEquals(3, thirdBattle.getParty().size());
        assertTrue(thirdBattle.getParty().stream().anyMatch(character -> character.getName().equals("Mongo")));
    }

    @Test
    void gameFlowEndsWithVictoryAfterFloorSix() throws Exception {
        var gameService = createGameService();
        gameService.startNewGame();

        for (int i = 0; i < 6; i++) {
            gameService.clearCurrentFloor();
        }

        assertEquals(GameStatus.VICTORY, gameService.getStatus());
        assertEquals(6, gameService.getCurrentFloorNumber());
    }

    @Test
    void gameControllerSavesRunResultOnce() throws Exception {
        var testDaos = createTestDaos();
        var dialogueService = new DialogueService(testDaos.dialogueDao);
        var encounterService = new EncounterService(testDaos.floorDao, testDaos.characterDao, dialogueService);
        var gameService = new GameService(encounterService, testDaos.floorDao);
        var highScoreService = new HighScoreService(testDaos.highScoreDao);
        var controller = new GameController(gameService, highScoreService);

        controller.startGame();
        for (int i = 0; i < 6; i++) {
            controller.advanceAfterVictory();
        }
        controller.saveRunResultIfNeeded();
        controller.saveRunResultIfNeeded();

        var savedRuns = testDaos.highScoreDao.findAll();
        assertEquals(1, savedRuns.size());
        assertTrue(savedRuns.getFirst().isVictory());
        assertEquals(6, savedRuns.getFirst().getFloorsCleared());
        assertTrue(savedRuns.getFirst().getTurnsTaken() > 0);
    }

    @Test
    void gameFlowEndsWithGameOverWhenPartyIsDefeated() throws Exception {
        var gameService = createGameService();
        var battle = gameService.startNewGame();

        battle.getParty().forEach(character -> character.receiveDamage(character.getMaxHp()));
        var status = gameService.updateStatusFromCurrentBattle();

        assertEquals(GameStatus.GAME_OVER, status);
        assertTrue(battle.isFinished());
    }

    @Test
    void turnPreviewStartsAtCurrentActorAndFollowsSpeedOrder() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var initialOrder = battle.getAliveActorsBySpeed().stream().map(Battler::getName).toList();

        assertEquals(initialOrder,
                battleService.getTurnPreview(battle, 5).stream().map(Battler::getName).toList());

        var currentActor = (at.fhooe.ald.model.PlayerCharacter) battle.getCurrentActor().orElseThrow();
        battleService.usePlayerAttack(battle, currentActor, currentActor.getAttacks().getFirst(),
                battle.getAliveEnemies().getFirst());

        assertEquals(initialOrder.get(1), battle.getCurrentActor().orElseThrow().getName());
        assertEquals(List.of(initialOrder.get(1), initialOrder.get(2), initialOrder.get(3),
                        initialOrder.get(4), initialOrder.get(0)),
                battleService.getTurnPreview(battle, 5).stream().map(Battler::getName).toList());
    }

    @Test
    void battleServiceAppliesPlayerDamageAndLogsTurn() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var carl = battle.getParty().getFirst();
        var hoarder = battle.getAliveEnemies().getFirst();
        var attack = carl.getAttacks().getFirst();
        int hpBefore = hoarder.getCurrentHp();

        battleService.usePlayerAttack(battle, carl, attack);

        assertTrue(hoarder.getCurrentHp() < hpBefore);
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals(attack.getName())));
        assertEquals(2, battle.getTurnNumber());
    }

    @Test
    void battleServiceAppliesPlayerDamageToSelectedTarget() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var carl = battle.getParty().getFirst();
        var hoarder = battle.getAliveEnemies().getFirst();
        var scatterer = battle.getAliveEnemies().get(1);
        var attack = carl.getAttacks().getFirst();
        int hoarderHpBefore = hoarder.getCurrentHp();
        int scattererHpBefore = scatterer.getCurrentHp();

        battleService.usePlayerAttack(battle, carl, attack, scatterer);

        assertEquals(hoarderHpBefore, hoarder.getCurrentHp());
        assertTrue(scatterer.getCurrentHp() < scattererHpBefore);
    }

    @Test
    void battleServiceProvidesFiveActorTurnPreview() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();

        var preview = battleService.getTurnPreview(battle, 5);

        assertEquals(5, preview.size());
        assertEquals("Donut", preview.getFirst().getName());
        assertEquals("Hoarder", preview.getLast().getName());
    }

    @Test
    void battleServiceLetsEnemiesAttackParty() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = battle.getAliveEnemies().getFirst();
        var carl = battle.getParty().getFirst();
        int hpBefore = carl.getCurrentHp();

        battleService.performEnemyTurn(battle, hoarder);

        assertTrue(carl.getCurrentHp() <= hpBefore);
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActorName().equals(hoarder.getName())));
    }

    @Test
    void battleServiceBlocksAttacksOnCooldownAndTicksThemDown() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var donut = battle.getParty().get(1);
        var fireball = donut.getAttacks().get(1);
        var magicMissile = donut.getAttacks().getFirst();
        var hoarder = battle.getAliveEnemies().getFirst();

        battleService.usePlayerAttack(battle, donut, fireball);

        assertEquals(2, battleService.getRemainingCooldown(donut, fireball));
        assertThrows(IllegalStateException.class, () -> battleService.usePlayerAttack(battle, donut, fireball));

        battleService.usePlayerAttack(battle, donut, magicMissile, hoarder);

        assertEquals(1, battleService.getRemainingCooldown(donut, fireball));
        assertThrows(IllegalStateException.class, () -> battleService.usePlayerAttack(battle, donut, fireball));

        battleService.tickCooldowns(battle);

        assertTrue(battleService.isReady(donut, fireball));
    }

    @Test
    void healingSongRestoresPartyHp() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var carl = battle.getParty().getFirst();
        var donut = battle.getParty().get(1);
        var healingSong = donut.getAttacks().get(2);
        carl.receiveDamage(120);
        donut.receiveDamage(80);
        int carlHpBefore = carl.getCurrentHp();
        int donutHpBefore = donut.getCurrentHp();

        battleService.usePlayerAttack(battle, donut, healingSong);

        assertTrue(carl.getCurrentHp() > carlHpBefore);
        assertTrue(donut.getCurrentHp() > donutHpBefore);
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("healing")));
    }

    @Test
    void healingSongCleansesTargetAndAppliesHealingOverTime() throws Exception {
        var battle = createBattleAtFloor(2);
        var battleService = createBattleServiceTargetingFirstPartyMember();
        var carl = battle.getParty().getFirst();
        var donut = battle.getParty().get(1);
        var ralph = battle.getAliveEnemies().getFirst();
        var healingSong = donut.getAttacks().get(2);

        battleService.performEnemyTurn(battle, ralph);
        PlayerCharacter infectedTarget = battle.getAlivePartyMembers().stream()
                .filter(character -> battleService.hasStatusEffect(battle, character, "Infection"))
                .findFirst()
                .orElseThrow();
        infectedTarget.receiveDamage(Math.max(1, infectedTarget.getCurrentHp() - 1));
        int hpBeforeHealingSong = infectedTarget.getCurrentHp();

        battleService.usePlayerAttack(battle, donut, healingSong, infectedTarget);

        assertFalse(battleService.hasStatusEffect(battle, infectedTarget, "Infection"));
        assertTrue(battleService.hasStatusEffect(battle, infectedTarget, "Healing Song"));
        assertTrue(infectedTarget.getCurrentHp() > hpBeforeHealingSong);

        int hpBeforeHot = infectedTarget.getCurrentHp();
        battleService.usePlayerAttack(battle, infectedTarget, infectedTarget.getAttacks().getFirst(), ralph);

        assertTrue(infectedTarget.getCurrentHp() > hpBeforeHot);
    }

    @Test
    void protectiveShellBlocksNextEnemyAttack() throws Exception {
        var gameService = createGameService();
        gameService.startNewGame();
        var battle = gameService.clearCurrentFloor();
        var battleService = createBattleService();
        var carl = battle.getParty().getFirst();
        var ralph = battle.getAliveEnemies().getFirst();
        var protectiveShell = carl.getAttacks().get(2);
        int carlHpBefore = carl.getCurrentHp();

        battleService.usePlayerAttack(battle, carl, protectiveShell);
        battleService.performEnemyTurn(battle, ralph);

        assertEquals(carlHpBefore, carl.getCurrentHp());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("blocks")));
    }

    @Test
    void royalCharmExcludesDonutFromCharmedEnemyTargets() throws Exception {
        var battle = createBattleAtFloor(2);
        var battleService = createBattleService();
        var carl = battle.getParty().getFirst();
        var donut = battle.getParty().get(1);
        var ralph = battle.getAliveEnemies().getFirst();
        var magicMissile = donut.getAttacks().getFirst();
        carl.receiveDamage(carl.getMaxHp());

        battleService.usePlayerAttack(battle, donut, magicMissile, ralph);
        int donutHpBefore = donut.getCurrentHp();
        battleService.tickCooldowns(battle);
        battleService.usePlayerAttack(battle, donut, magicMissile, ralph);

        assertTrue(battle.getTurnLog().stream()
                .anyMatch(turn -> turn.getMessage().contains("Royal Charm protects Donut from Ralph")));
        assertTrue(battleService.hasRoyalCharm(battle, ralph));

        battleService.performEnemyTurn(battle, ralph);

        assertEquals(donutHpBefore, donut.getCurrentHp());
        assertTrue(battle.getTurnLog().stream()
                .anyMatch(turn -> turn.getMessage().contains("cannot find a valid target")));
    }

    @Test
    void lethalInfectionKillsTargetOnNextTurnWhenNotDispelled() throws Exception {
        var battle = createBattleAtFloor(2);
        var battleService = createBattleServiceTargetingFirstPartyMember();
        var carl = battle.getParty().getFirst();
        var donut = battle.getParty().get(1);
        var ralph = battle.getAliveEnemies().getFirst();
        donut.receiveDamage(donut.getMaxHp());

        battleService.performEnemyTurn(battle, ralph);
        ralph.receiveDamage(900);
        battleService.performEnemyTurn(battle, ralph);

        PlayerCharacter infectedTarget = battle.getAlivePartyMembers().stream()
                .filter(character -> battleService.hasStatusEffect(battle, character, "Lethal Infection"))
                .findFirst()
                .orElseThrow();
        battleService.usePlayerAttack(battle, carl, carl.getAttacks().getFirst(), ralph);

        assertTrue(infectedTarget.isAlive());
        battleService.performEnemyTurn(battle, ralph);

        assertFalse(infectedTarget.isAlive());
    }

    @Test
    void fireballBurnsEnemiesOnTheirTurn() throws Exception {
        var gameService = createGameService();
        gameService.startNewGame();
        var battle = gameService.clearCurrentFloor();
        var battleService = createBattleService();
        var donut = battle.getParty().get(1);
        var ralph = battle.getAliveEnemies().getFirst();
        var fireball = donut.getAttacks().get(1);

        battleService.usePlayerAttack(battle, donut, fireball);
        int hpAfterFireball = ralph.getCurrentHp();
        battleService.performEnemyTurn(battle, ralph);
        int hpAfterFirstBurnTick = ralph.getCurrentHp();

        int firstBurnTick = hpAfterFireball - hpAfterFirstBurnTick;
        assertTrue(firstBurnTick >= 50);
        assertTrue(firstBurnTick <= 96);
        assertTrue(battleService.hasStatusEffect(battle, ralph, "Burn"));
        battleService.performEnemyTurn(battle, ralph);
        int secondBurnTick = hpAfterFirstBurnTick - ralph.getCurrentHp();

        assertTrue(secondBurnTick >= 50);
        assertTrue(secondBurnTick <= 96);
        assertFalse(battleService.hasStatusEffect(battle, ralph, "Burn"));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("Burn damage")));
    }

    @Test
    void gutRipperDamagesEnemyAndHealsMongo() throws Exception {
        var gameService = createGameService();
        gameService.startNewGame();
        gameService.clearCurrentFloor();
        var battle = gameService.clearCurrentFloor();
        var battleService = createBattleService();
        var mongo = battle.getParty().stream()
                .filter(character -> character.getName().equals("Mongo"))
                .findFirst()
                .orElseThrow();
        var heather = battle.getAliveEnemies().getFirst();
        var gutRipper = mongo.getAttacks().get(1);
        mongo.receiveDamage(100);
        int mongoHpBefore = mongo.getCurrentHp();
        int heatherHpBefore = heather.getCurrentHp();

        battleService.usePlayerAttack(battle, mongo, gutRipper, heather);

        assertTrue(heather.getCurrentHp() < heatherHpBefore);
        assertTrue(mongo.getCurrentHp() > mongoHpBefore);
    }

    @Test
    void raptorRoarBuffsNextPartyAttack() throws Exception {
        var buffedBattle = createFloorThreeBattle();
        var normalBattle = createFloorThreeBattle();
        var buffedService = createBattleService();
        var normalService = createBattleService();
        var buffedMongo = buffedBattle.getParty().stream()
                .filter(character -> character.getName().equals("Mongo"))
                .findFirst()
                .orElseThrow();
        var buffedCarl = buffedBattle.getParty().getFirst();
        var normalCarl = normalBattle.getParty().getFirst();
        var buffedHeather = buffedBattle.getAliveEnemies().getFirst();
        var normalHeather = normalBattle.getAliveEnemies().getFirst();

        buffedService.usePlayerAttack(buffedBattle, buffedMongo, buffedMongo.getAttacks().get(2));
        buffedService.usePlayerAttack(buffedBattle, buffedCarl, buffedCarl.getAttacks().getFirst(), buffedHeather);
        normalService.usePlayerAttack(normalBattle, normalCarl, normalCarl.getAttacks().getFirst(), normalHeather);

        int buffedDamage = buffedHeather.getMaxHp() - buffedHeather.getCurrentHp();
        int normalDamage = normalHeather.getMaxHp() - normalHeather.getCurrentHp();
        assertTrue(buffedDamage > normalDamage);
    }

    @Test
    void hoarderSpawnsScattererAndDevoursAddsInFinalPhase() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = (BossEnemy) battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Hoarder"))
                .findFirst()
                .orElseThrow();

        battleService.performEnemyTurn(battle, hoarder);

        long scatterersAfterSpawn = battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .count();
        assertTrue(scatterersAfterSpawn >= 3);
        assertTrue(scatterersAfterSpawn <= 4);

        hoarder.receiveDamage(1100);
        battleService.performEnemyTurn(battle, hoarder);

        assertEquals(0, battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .count());
        assertTrue(hoarder.getCurrentHp() > 300);
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("devours")));
    }

    @Test
    void spawnedEnemiesDoNotGiveCurrentActorAnotherImmediateTurn() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = (BossEnemy) battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Hoarder"))
                .findFirst()
                .orElseThrow();
        while (battle.getCurrentActor().orElseThrow() != hoarder) {
            battle.advanceTurn();
        }
        var orderBeforeSpawn = battle.getAliveActorsBySpeed();
        String expectedNextActor = orderBeforeSpawn.get((orderBeforeSpawn.indexOf(hoarder) + 1)
                % orderBeforeSpawn.size()).getName();

        battleService.performEnemyTurn(battle, hoarder);

        long scatterersAfterSpawn = battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .count();
        assertTrue(scatterersAfterSpawn >= 3);
        assertTrue(scatterersAfterSpawn <= 4);
        assertEquals(expectedNextActor, battle.getCurrentActor().orElseThrow().getName());
    }

    @Test
    void hoarderFinalPhaseSpawnsPassivelyBeforeDevour() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = (BossEnemy) battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Hoarder"))
                .findFirst()
                .orElseThrow();
        battleService.performEnemyTurn(battle, hoarder);
        battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .forEach(enemy -> enemy.receiveDamage(enemy.getCurrentHp()));
        hoarder.receiveDamage(1000);

        battleService.performEnemyTurn(battle, hoarder);

        assertTrue(battle.getTurnLog().stream()
                .anyMatch(turn -> turn.getMessage().contains("Garbage Spawn passively creates a Scatterer")));
        assertTrue(battle.getTurnLog().stream()
                .anyMatch(turn -> turn.getMessage().contains("devours 1 Scatterers")));
        assertEquals(0, battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .count());
    }

    @Test
    void circeBroodMotherAndTwinBabiesSpawnNymphs() throws Exception {
        var battle = createBattleAtFloor(6);
        var battleService = createBattleService();
        var circe = (BossEnemy) battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Circe"))
                .findFirst()
                .orElseThrow();

        battleService.performEnemyTurn(battle, circe);

        assertEquals(4, battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Mantis Nymph"))
                .count());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("Brood Mother")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Twin Babies")));

        battleService.performEnemyTurn(battle, circe);

        assertEquals(4, battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Mantis Nymph"))
                .count());
    }

    @Test
    void goreGoreSummonGruulDefeatsPartyAfterCountdown() throws Exception {
        var battle = createBattleAtFloor(4);
        var battleService = createBattleService();
        var goreGore = (BossEnemy) battle.getAliveEnemies().getFirst();

        battleService.performEnemyTurn(battle, goreGore);
        assertEquals(BattleResult.IN_PROGRESS, battle.getResult());
        goreGore.receiveDamage(1900);

        battleService.performEnemyTurn(battle, goreGore);
        assertEquals(BattleResult.IN_PROGRESS, battle.getResult());

        battleService.performEnemyTurn(battle, goreGore);
        assertEquals(BattleResult.IN_PROGRESS, battle.getResult());

        battleService.performEnemyTurn(battle, goreGore);

        assertEquals(BattleResult.DEFEAT, battle.getResult());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("Summon Gruul completes")));
    }

    @Test
    void deniseReflectsDamageInFinalPhase() throws Exception {
        var battle = createBattleAtFloor(5);
        var battleService = createBattleService();
        var donut = battle.getParty().get(1);
        var denise = (BossEnemy) battle.getAliveEnemies().getFirst();
        battleService.performEnemyTurn(battle, denise);
        denise.receiveDamage(1300);
        int donutHpBefore = donut.getCurrentHp();

        battleService.usePlayerAttack(battle, donut, donut.getAttacks().getFirst(), denise);

        assertTrue(donut.getCurrentHp() < donutHpBefore);
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Reflect")));
    }

    @Test
    void battleServiceDetectsVictoryAndDefeat() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();

        battle.getEnemies().forEach(enemy -> enemy.receiveDamage(enemy.getMaxHp()));

        assertEquals(BattleResult.VICTORY, battleService.updateBattleResult(battle));

        var secondBattle = createGameService().startNewGame();
        secondBattle.getParty().forEach(character -> character.receiveDamage(character.getMaxHp()));

        assertEquals(BattleResult.DEFEAT, battleService.updateBattleResult(secondBattle));
    }

    @Test
    void battleServiceOrdersActorsBySpeed() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();

        var turnOrder = battleService.getTurnOrder(battle);

        assertEquals("Donut", turnOrder.getFirst().getName());
        assertTrue(turnOrder.getFirst().getSpeed() >= turnOrder.getLast().getSpeed());
    }

    @Test
    void bossFsmTransitionsUnlockPhaseAttacksWithoutLogSpam() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = (BossEnemy) battle.getEnemies().stream()
                .filter(enemy -> enemy instanceof BossEnemy)
                .findFirst()
                .orElseThrow();

        battleService.performEnemyTurn(battle, hoarder);

        assertEquals(BossEnemyState.PHASE_ONE, hoarder.getState());
        assertFalse(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("FSM")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Garbage Spawn")));

        hoarder.receiveDamage(700);
        battleService.performEnemyTurn(battle, hoarder);

        assertEquals(BossEnemyState.PHASE_TWO, hoarder.getState());
        assertFalse(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("PHASE_ONE -> PHASE_TWO")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Pile Collapse")));
    }

    @Test
    void trashFsmTransitionsUnlockStateAttacksWithoutLogSpam() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var scatterer = (TrashEnemy) battle.getEnemies().stream()
                .filter(enemy -> enemy instanceof TrashEnemy)
                .findFirst()
                .orElseThrow();

        scatterer.receiveDamage(50);
        battleService.performEnemyTurn(battle, scatterer);

        assertEquals(TrashEnemyState.AGGRESSIVE, scatterer.getState());
        assertFalse(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("FSM")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Bug Bite")));

        scatterer.receiveDamage(30);
        battleService.performEnemyTurn(battle, scatterer);

        assertEquals(TrashEnemyState.DEAD, scatterer.getState());
        assertFalse(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("AGGRESSIVE -> DESPERATE")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Offering")));
    }

    private GameService createGameService() throws Exception {
        var testDaos = createTestDaos();
        var dialogueService = new DialogueService(testDaos.dialogueDao);
        var encounterService = new EncounterService(testDaos.floorDao, testDaos.characterDao, dialogueService);
        return new GameService(encounterService, testDaos.floorDao);
    }

    private at.fhooe.ald.model.Battle createFloorThreeBattle() throws Exception {
        return createBattleAtFloor(3);
    }

    private at.fhooe.ald.model.Battle createBattleAtFloor(int floorNumber) throws Exception {
        var gameService = createGameService();
        var battle = gameService.startNewGame();
        for (int currentFloor = 1; currentFloor < floorNumber; currentFloor++) {
            battle = gameService.clearCurrentFloor();
        }
        return battle;
    }

    private BattleService createBattleService() {
        return new BattleService(new DamageCalculator(new Random(1)), new TargetSelector());
    }

    private BattleService createBattleServiceTargetingFirstPartyMember() {
        return new BattleService(new DamageCalculator(new Random(1)), new TargetSelector() {
            @Override
            public List<? extends Targetable> selectTargets(at.fhooe.ald.model.Battle battle, TargetType targetType,
                                                            boolean actorIsPlayer, Targetable selectedTarget) {
                if (!actorIsPlayer
                        && (targetType == TargetType.SINGLE_ENEMY || targetType == TargetType.RANDOM_ENEMY)) {
                    return List.of(battle.getAlivePartyMembers().getFirst());
                }
                return super.selectTargets(battle, targetType, actorIsPlayer, selectedTarget);
            }
        });
    }

    private TestDaos createTestDaos() throws Exception {
        var databasePath = Files.createTempDirectory("dungeon-crawler-carl-test").resolve("game.db");
        var database = new DatabaseInitializer(databasePath).initialize();
        var attackDao = new JdbcAttackDao(database);
        var characterDao = new JdbcCharacterDao(database, attackDao);
        var enemyDao = new JdbcEnemyDao(database, attackDao);
        var floorDao = new JdbcFloorDao(database, enemyDao);
        var dialogueDao = new JdbcDialogueDao(database);
        var highScoreDao = new JdbcHighScoreDao(database);
        return new TestDaos(characterDao, floorDao, dialogueDao, highScoreDao);
    }

    private record TestDaos(
            JdbcCharacterDao characterDao,
            JdbcFloorDao floorDao,
            JdbcDialogueDao dialogueDao,
            JdbcHighScoreDao highScoreDao
    ) {
    }
}
