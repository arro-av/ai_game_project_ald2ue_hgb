package at.fhooe.ald.service;

import at.fhooe.ald.dao.FloorDao;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GameService {
    private final EncounterService encounterService;
    private final FloorDao floorDao;
    private GameStatus status;
    private int currentFloorNumber;
    private int finalFloorNumber;
    private int turnsTaken;
    private boolean currentBattleTurnsRecorded;
    private Battle currentBattle;

    public GameService(EncounterService encounterService, FloorDao floorDao) {
        this.encounterService = encounterService;
        this.floorDao = floorDao;
        this.status = GameStatus.NOT_STARTED;
    }

    public Battle startNewGame() throws SQLException {
        finalFloorNumber = loadFinalFloorNumber();
        currentFloorNumber = 1;
        turnsTaken = 0;
        currentBattleTurnsRecorded = false;
        currentBattle = encounterService.createBattle(currentFloorNumber);
        status = GameStatus.IN_BATTLE;
        return currentBattle;
    }

    public Battle clearCurrentFloor() throws SQLException {
        requireBattleInProgress();
        currentBattle.markVictory();
        recordCurrentBattleTurns();
        if (currentFloorNumber >= finalFloorNumber) {
            status = GameStatus.VICTORY;
            return currentBattle;
        }
        currentFloorNumber++;
        currentBattle = encounterService.createBattle(currentFloorNumber);
        currentBattleTurnsRecorded = false;
        status = GameStatus.IN_BATTLE;
        return currentBattle;
    }

    public void markPartyDefeated() {
        requireBattleStarted();
        currentBattle.markDefeat();
        recordCurrentBattleTurns();
        status = GameStatus.GAME_OVER;
    }

    public GameStatus updateStatusFromCurrentBattle() throws SQLException {
        requireBattleStarted();
        if (currentBattle.getAlivePartyMembers().isEmpty()) {
            markPartyDefeated();
        } else if (currentBattle.getAliveEnemies().isEmpty()) {
            clearCurrentFloor();
        }
        return status;
    }

    public GameStatus getStatus() {
        return status;
    }

    public int getCurrentFloorNumber() {
        return currentFloorNumber;
    }

    public int getFloorsCleared() {
        if (status == GameStatus.VICTORY) {
            return finalFloorNumber;
        }
        return Math.max(0, currentFloorNumber - 1);
    }

    public int getTurnsTaken() {
        if (currentBattle == null || currentBattleTurnsRecorded) {
            return turnsTaken;
        }
        return turnsTaken + currentBattle.getTurnNumber();
    }

    public Optional<Battle> getCurrentBattle() {
        return Optional.ofNullable(currentBattle);
    }

    public List<PlayerCharacter> getActiveParty() {
        requireBattleStarted();
        return currentBattle.getParty();
    }

    public List<Enemy> getCurrentEnemies() {
        requireBattleStarted();
        return currentBattle.getEnemies();
    }

    private int loadFinalFloorNumber() throws SQLException {
        return floorDao.findAll().stream()
                .map(floor -> floor.getFloorNumber())
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalStateException("No floors configured"));
    }

    private void recordCurrentBattleTurns() {
        if (!currentBattleTurnsRecorded && currentBattle != null) {
            turnsTaken += currentBattle.getTurnNumber();
            currentBattleTurnsRecorded = true;
        }
    }

    private void requireBattleStarted() {
        if (currentBattle == null) {
            throw new IllegalStateException("No game has been started");
        }
    }

    private void requireBattleInProgress() {
        requireBattleStarted();
        if (status != GameStatus.IN_BATTLE) {
            throw new IllegalStateException("Game is not in battle state");
        }
    }
}
