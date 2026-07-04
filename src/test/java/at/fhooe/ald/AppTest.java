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
import at.fhooe.ald.model.BattleResult;
import at.fhooe.ald.model.BossEnemy;
import at.fhooe.ald.model.HighScore;
import at.fhooe.ald.model.TrashEnemy;
import at.fhooe.ald.model.fsm.BossEnemyState;
import at.fhooe.ald.model.fsm.TrashEnemyState;
import at.fhooe.ald.service.BattleService;
import at.fhooe.ald.service.DialogueService;
import at.fhooe.ald.service.DamageCalculator;
import at.fhooe.ald.service.EncounterService;
import at.fhooe.ald.service.GameService;
import at.fhooe.ald.service.GameStatus;
import at.fhooe.ald.service.TargetSelector;
import java.nio.file.Files;
import java.time.LocalDateTime;
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
        assertFalse(testDaos.dialogueDao.findByFloorNumber(1).isEmpty());

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
    void gameFlowEndsWithGameOverWhenPartyIsDefeated() throws Exception {
        var gameService = createGameService();
        var battle = gameService.startNewGame();

        battle.getParty().forEach(character -> character.receiveDamage(character.getMaxHp()));
        var status = gameService.updateStatusFromCurrentBattle();

        assertEquals(GameStatus.GAME_OVER, status);
        assertTrue(battle.isFinished());
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

        battleService.usePlayerAttack(battle, donut, fireball);

        assertEquals(2, battleService.getRemainingCooldown(donut, fireball));
        assertThrows(IllegalStateException.class, () -> battleService.usePlayerAttack(battle, donut, fireball));

        battleService.tickCooldowns(battle);
        battleService.tickCooldowns(battle);

        assertTrue(battleService.isReady(donut, fireball));
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
    void bossFsmTransitionsAreLoggedAndUnlockPhaseAttacks() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var hoarder = (BossEnemy) battle.getEnemies().stream()
                .filter(enemy -> enemy instanceof BossEnemy)
                .findFirst()
                .orElseThrow();

        battleService.performEnemyTurn(battle, hoarder);

        assertEquals(BossEnemyState.PHASE_ONE, hoarder.getState());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("INTRO -> PHASE_ONE")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Garbage Spawn")));

        hoarder.receiveDamage(700);
        battleService.performEnemyTurn(battle, hoarder);

        assertEquals(BossEnemyState.PHASE_TWO, hoarder.getState());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("PHASE_ONE -> PHASE_TWO")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Pile Collapse")));
    }

    @Test
    void trashFsmTransitionsAreLoggedAndUnlockStateAttacks() throws Exception {
        var battle = createGameService().startNewGame();
        var battleService = createBattleService();
        var scatterer = (TrashEnemy) battle.getEnemies().stream()
                .filter(enemy -> enemy instanceof TrashEnemy)
                .findFirst()
                .orElseThrow();

        scatterer.receiveDamage(50);
        battleService.performEnemyTurn(battle, scatterer);

        assertEquals(TrashEnemyState.AGGRESSIVE, scatterer.getState());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("NORMAL -> AGGRESSIVE")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Claw Jab")));

        scatterer.receiveDamage(30);
        battleService.performEnemyTurn(battle, scatterer);

        assertEquals(TrashEnemyState.DESPERATE, scatterer.getState());
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getMessage().contains("AGGRESSIVE -> DESPERATE")));
        assertTrue(battle.getTurnLog().stream().anyMatch(turn -> turn.getActionName().equals("Offering")));
    }

    private GameService createGameService() throws Exception {
        var testDaos = createTestDaos();
        var dialogueService = new DialogueService(testDaos.dialogueDao);
        var encounterService = new EncounterService(testDaos.floorDao, testDaos.characterDao, dialogueService);
        return new GameService(encounterService, testDaos.floorDao);
    }

    private BattleService createBattleService() {
        return new BattleService(new DamageCalculator(new Random(1)), new TargetSelector());
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
