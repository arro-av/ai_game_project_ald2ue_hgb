package at.fhooe.ald.service;

import at.fhooe.ald.model.Attack;
import at.fhooe.ald.model.AttackEffect;
import at.fhooe.ald.model.Battle;
import at.fhooe.ald.model.BattleResult;
import at.fhooe.ald.model.BattleTurn;
import at.fhooe.ald.model.Battler;
import at.fhooe.ald.model.BossEnemy;
import at.fhooe.ald.model.Enemy;
import at.fhooe.ald.model.PlayerCharacter;
import at.fhooe.ald.model.TargetType;
import at.fhooe.ald.model.Targetable;
import at.fhooe.ald.model.TrashEnemy;
import at.fhooe.ald.model.fsm.BossEnemyStateMachine;
import at.fhooe.ald.model.fsm.TrashEnemyStateMachine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BattleService {
    private final DamageCalculator damageCalculator;
    private final TargetSelector targetSelector;
    private final Map<Battler, Map<Integer, Integer>> cooldowns;

    public BattleService(DamageCalculator damageCalculator, TargetSelector targetSelector) {
        this.damageCalculator = damageCalculator;
        this.targetSelector = targetSelector;
        this.cooldowns = new HashMap<>();
    }

    public BattleResult usePlayerAttack(Battle battle, PlayerCharacter actor, Attack attack) {
        requireInProgress(battle);
        requireAlive(actor);
        requireKnownAttack(actor, attack);
        requireReady(actor, attack);

        applyAttack(battle, actor, attack, true);
        tickCooldowns(battle);
        startCooldown(actor, attack);
        updateBattleResult(battle);
        battle.advanceTurn();
        return battle.getResult();
    }

    public BattleResult performEnemyTurn(Battle battle, Enemy actor) {
        requireInProgress(battle);
        requireAlive(actor);
        updateEnemyState(battle, actor);
        Attack attack = chooseFirstReadyAttack(actor);
        applyAttack(battle, actor, attack, false);
        tickCooldowns(battle);
        startCooldown(actor, attack);
        updateBattleResult(battle);
        battle.advanceTurn();
        return battle.getResult();
    }

    public List<Battler> getTurnOrder(Battle battle) {
        return battle.getAliveActorsBySpeed();
    }

    public int getRemainingCooldown(Battler actor, Attack attack) {
        return cooldowns.getOrDefault(actor, Map.of()).getOrDefault(attack.getId(), 0);
    }

    public boolean isReady(Battler actor, Attack attack) {
        return getRemainingCooldown(actor, attack) == 0;
    }

    public void tickCooldowns(Battle battle) {
        battle.getAliveActorsBySpeed().forEach(this::tickCooldowns);
    }

    public BattleResult updateBattleResult(Battle battle) {
        if (battle.getAlivePartyMembers().isEmpty()) {
            battle.markDefeat();
        } else if (battle.getAliveEnemies().isEmpty()) {
            battle.markVictory();
        }
        return battle.getResult();
    }

    private void applyAttack(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer) {
        if (attack.getEffect() == AttackEffect.HEAL || attack.getTargetType() == TargetType.SELF) {
            applyHealing(battle, actor, attack, actorIsPlayer);
            return;
        }

        List<? extends Targetable> targets = targetSelector.selectTargets(battle, attack.getTargetType(), actorIsPlayer);
        int totalAmount = 0;
        List<Enemy> damagedEnemies = new java.util.ArrayList<>();
        for (Targetable target : targets) {
            int damage = damageCalculator.rollDamage(attack);
            target.receiveDamage(damage);
            totalAmount += damage;
            if (target instanceof Enemy enemy) {
                damagedEnemies.add(enemy);
            }
        }

        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                targetLabel(targets),
                totalAmount,
                actor.getName() + " used " + attack.getName() + " for " + totalAmount + " damage."
        ));
        damagedEnemies.forEach(enemy -> updateEnemyState(battle, enemy));
    }

    private void applyHealing(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer) {
        List<? extends Targetable> targets = attack.getTargetType() == TargetType.SELF
                ? List.of(actor)
                : targetSelector.selectTargets(battle, attack.getTargetType(), actorIsPlayer);
        int totalAmount = 0;
        for (Targetable target : targets) {
            int healing = damageCalculator.rollDamage(attack);
            if (target instanceof Battler battler) {
                battler.heal(healing);
            }
            totalAmount += healing;
        }

        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                targetLabel(targets),
                totalAmount,
                actor.getName() + " used " + attack.getName() + " for " + totalAmount + " healing."
        ));
    }

    private Attack chooseFirstReadyAttack(Battler actor) {
        return actor.getAttacks().stream()
                .filter(attack -> isUnlockedForActor(actor, attack))
                .filter(attack -> isReady(actor, attack))
                .findFirst()
                .orElseGet(() -> actor.getAttacks().stream()
                        .filter(attack -> isUnlockedForActor(actor, attack))
                        .findFirst()
                        .orElseGet(() -> actor.getAttacks().getFirst()));
    }

    private boolean isUnlockedForActor(Battler actor, Attack attack) {
        if (attack.getUnlockState() == null || attack.getUnlockState().isBlank()) {
            return true;
        }
        if (actor instanceof BossEnemy bossEnemy) {
            return bossEnemy.getState().name().equals(attack.getUnlockState());
        }
        if (actor instanceof TrashEnemy trashEnemy) {
            return trashEnemy.getState().name().equals(attack.getUnlockState());
        }
        return true;
    }

    private void updateEnemyState(Battle battle, Enemy enemy) {
        Optional<String> transition = Optional.empty();
        if (enemy instanceof BossEnemy bossEnemy) {
            transition = new BossEnemyStateMachine(bossEnemy).update();
        } else if (enemy instanceof TrashEnemy trashEnemy) {
            transition = new TrashEnemyStateMachine(trashEnemy).update();
        }
        transition.ifPresent(message -> battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                enemy.getName(),
                "FSM",
                enemy.getName(),
                0,
                message
        )));
    }

    private void startCooldown(Battler actor, Attack attack) {
        if (attack.getCooldown() <= 0) {
            return;
        }
        cooldowns.computeIfAbsent(actor, ignored -> new HashMap<>()).put(attack.getId(), attack.getCooldown());
    }

    private void tickCooldowns(Battler actor) {
        Map<Integer, Integer> actorCooldowns = cooldowns.get(actor);
        if (actorCooldowns == null) {
            return;
        }
        actorCooldowns.replaceAll((attackId, remaining) -> Math.max(0, remaining - 1));
    }

    private String targetLabel(List<? extends Targetable> targets) {
        if (targets.isEmpty()) {
            return "";
        }
        if (targets.size() == 1 && targets.getFirst() instanceof Battler battler) {
            return battler.getName();
        }
        return targets.size() + " targets";
    }

    private void requireInProgress(Battle battle) {
        if (battle.isFinished()) {
            throw new IllegalStateException("Battle is already finished");
        }
    }

    private void requireAlive(Battler actor) {
        if (!actor.isAlive()) {
            throw new IllegalStateException(actor.getName() + " cannot act because they are defeated");
        }
    }

    private void requireKnownAttack(Battler actor, Attack attack) {
        if (actor.getAttacks().stream().noneMatch(knownAttack -> knownAttack.getId() == attack.getId())) {
            throw new IllegalArgumentException(actor.getName() + " does not know " + attack.getName());
        }
    }

    private void requireReady(Battler actor, Attack attack) {
        if (!isReady(actor, attack)) {
            throw new IllegalStateException(attack.getName() + " is on cooldown");
        }
    }
}
