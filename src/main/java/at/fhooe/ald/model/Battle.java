package at.fhooe.ald.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Battle {
    private final int floorNumber;
    private final String floorName;
    private final List<PlayerCharacter> party;
    private final List<Enemy> enemies;
    private final List<DialogueLine> introDialogue;
    private final List<BattleTurn> turnLog;
    private BattleResult result;
    private int turnNumber;

    public Battle(int floorNumber, String floorName, List<PlayerCharacter> party,
                  List<Enemy> enemies, List<DialogueLine> introDialogue) {
        this.floorNumber = floorNumber;
        this.floorName = Objects.requireNonNullElse(floorName, "");
        this.party = new ArrayList<>(party == null ? List.of() : party);
        this.enemies = new ArrayList<>(enemies == null ? List.of() : enemies);
        this.introDialogue = new ArrayList<>(introDialogue == null ? List.of() : introDialogue);
        this.turnLog = new ArrayList<>();
        this.result = BattleResult.IN_PROGRESS;
        this.turnNumber = 1;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public String getFloorName() {
        return floorName;
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
        enemies.add(Objects.requireNonNull(enemy));
    }

    public void addTurn(BattleTurn turn) {
        turnLog.add(Objects.requireNonNull(turn));
    }

    public void advanceTurn() {
        turnNumber++;
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
        List<Battler> actors = new ArrayList<>();
        actors.addAll(getAlivePartyMembers());
        actors.addAll(getAliveEnemies());
        actors.sort((first, second) -> Integer.compare(second.getSpeed(), first.getSpeed()));
        return List.copyOf(actors);
    }
}
