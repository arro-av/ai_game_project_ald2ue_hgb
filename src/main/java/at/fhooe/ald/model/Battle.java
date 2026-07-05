package at.fhooe.ald.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Battle {
    private final int floorNumber;
    private final String floorName;
    private final String backgroundPath;
    private final List<PlayerCharacter> party;
    private final List<Enemy> enemies;
    private final List<DialogueLine> introDialogue;
    private final List<BattleTurn> turnLog;
    private final List<Battler> turnOrder;
    private BattleResult result;
    private int turnNumber;
    private int currentTurnIndex;

    public Battle(int floorNumber, String floorName, List<PlayerCharacter> party,
                  List<Enemy> enemies, List<DialogueLine> introDialogue) {
        this(floorNumber, floorName, "", party, enemies, introDialogue);
    }

    public Battle(int floorNumber, String floorName, String backgroundPath, List<PlayerCharacter> party,
                  List<Enemy> enemies, List<DialogueLine> introDialogue) {
        this.floorNumber = floorNumber;
        this.floorName = Objects.requireNonNullElse(floorName, "");
        this.backgroundPath = Objects.requireNonNullElse(backgroundPath, "");
        this.party = new ArrayList<>(party == null ? List.of() : party);
        this.enemies = new ArrayList<>(enemies == null ? List.of() : enemies);
        this.introDialogue = new ArrayList<>(introDialogue == null ? List.of() : introDialogue);
        this.turnLog = new ArrayList<>();
        this.turnOrder = createInitialTurnOrder();
        this.result = BattleResult.IN_PROGRESS;
        this.turnNumber = 1;
        this.currentTurnIndex = 0;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public String getBackgroundPath() {
        return backgroundPath;
    }

    public List<PlayerCharacter> getParty() {
        return List.copyOf(party);
    }

    public List<Enemy> getEnemies() {
        return List.copyOf(enemies);
    }

    public List<DialogueLine> getIntroDialogue() {
        return List.copyOf(introDialogue);
    }

    public List<BattleTurn> getTurnLog() {
        return List.copyOf(turnLog);
    }

    public BattleResult getResult() {
        return result;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public boolean isFinished() {
        return result != BattleResult.IN_PROGRESS;
    }

    public void addEnemy(Enemy enemy) {
        Enemy newEnemy = Objects.requireNonNull(enemy);
        enemies.add(newEnemy);
        addToTurnOrder(newEnemy);
    }

    public void addTurn(BattleTurn turn) {
        turnLog.add(Objects.requireNonNull(turn));
    }

    public void advanceTurn() {
        turnNumber++;
        moveToNextAliveTurnIndex();
    }

    public Optional<Battler> getCurrentActor() {
        if (!hasAliveActors()) {
            return Optional.empty();
        }
        normalizeCurrentTurnIndex();
        return Optional.of(turnOrder.get(currentTurnIndex));
    }

    public void markVictory() {
        result = BattleResult.VICTORY;
    }

    public void markDefeat() {
        result = BattleResult.DEFEAT;
    }

    public List<PlayerCharacter> getAlivePartyMembers() {
        return party.stream().filter(PlayerCharacter::isAlive).toList();
    }

    public List<Enemy> getAliveEnemies() {
        return enemies.stream().filter(Enemy::isAlive).toList();
    }

    public List<Battler> getAliveActorsBySpeed() {
        return turnOrder.stream().filter(Battler::isAlive).toList();
    }

    private List<Battler> createInitialTurnOrder() {
        List<Battler> actors = new ArrayList<>();
        actors.addAll(party);
        actors.addAll(enemies);
        actors.sort((first, second) -> Integer.compare(second.getSpeed(), first.getSpeed()));
        return actors;
    }

    private void addToTurnOrder(Battler battler) {
        int insertIndex = 0;
        while (insertIndex < turnOrder.size() && turnOrder.get(insertIndex).getSpeed() >= battler.getSpeed()) {
            insertIndex++;
        }
        turnOrder.add(insertIndex, battler);
        if (insertIndex <= currentTurnIndex) {
            currentTurnIndex++;
        }
        currentTurnIndex = Math.floorMod(currentTurnIndex, turnOrder.size());
    }

    private void moveToNextAliveTurnIndex() {
        if (!hasAliveActors()) {
            return;
        }
        currentTurnIndex = Math.floorMod(currentTurnIndex + 1, turnOrder.size());
        normalizeCurrentTurnIndex();
    }

    private void normalizeCurrentTurnIndex() {
        currentTurnIndex = Math.floorMod(currentTurnIndex, turnOrder.size());
        int checked = 0;
        while (checked < turnOrder.size() && !turnOrder.get(currentTurnIndex).isAlive()) {
            currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size();
            checked++;
        }
    }

    private boolean hasAliveActors() {
        return turnOrder.stream().anyMatch(Battler::isAlive);
    }
}
