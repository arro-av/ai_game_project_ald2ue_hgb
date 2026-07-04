package at.fhooe.ald.service;

import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.model.TargetType;
import java.util.Comparator;
import java.util.List;

public class TargetSelector {

    public List<? extends Targetable> selectTargets(Battle battle, TargetType targetType, boolean actorIsPlayer) {
        return switch (targetType) {
            case SINGLE_ENEMY -> List.of(selectFirstEnemyTarget(battle, actorIsPlayer));
            case ALL_ENEMIES -> selectEnemySide(battle, actorIsPlayer);
            case SELF -> List.of();
            case SINGLE_ALLY -> List.of(selectFirstAllyTarget(battle, actorIsPlayer));
            case ALL_ALLIES -> selectAllySide(battle, actorIsPlayer);
            case LOWEST_HP_ALLY -> List.of(selectLowestHpTarget(selectEnemySide(battle, actorIsPlayer)));
            case RANDOM_ENEMY -> List.of(selectFirstEnemyTarget(battle, actorIsPlayer));
        };
    }

    public Enemy selectFirstAliveEnemy(Battle battle) {
        return battle.getAliveEnemies().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No alive enemies available"));
    }

    public PlayerCharacter selectFirstAlivePartyMember(Battle battle) {
        return battle.getAlivePartyMembers().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No alive party members available"));
    }

    private Targetable selectFirstEnemyTarget(Battle battle, boolean actorIsPlayer) {
        return actorIsPlayer ? selectFirstAliveEnemy(battle) : selectFirstAlivePartyMember(battle);
    }

    private Targetable selectFirstAllyTarget(Battle battle, boolean actorIsPlayer) {
        return actorIsPlayer ? selectFirstAlivePartyMember(battle) : selectFirstAliveEnemy(battle);
    }

    private List<? extends Targetable> selectEnemySide(Battle battle, boolean actorIsPlayer) {
        return actorIsPlayer ? battle.getAliveEnemies() : battle.getAlivePartyMembers();
    }

    private List<? extends Targetable> selectAllySide(Battle battle, boolean actorIsPlayer) {
        return actorIsPlayer ? battle.getAlivePartyMembers() : battle.getAliveEnemies();
    }

    private Targetable selectLowestHpTarget(List<? extends Targetable> targets) {
        return targets.stream()
                .min(Comparator.comparingInt(Targetable::getCurrentHp))
                .orElseThrow(() -> new IllegalStateException("No targets available"));
    }
}
