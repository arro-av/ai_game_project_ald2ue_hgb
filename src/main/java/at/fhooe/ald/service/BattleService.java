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
import java.util.stream.IntStream;

public class BattleService {
    private static final int PARTY_DAMAGE_BUFF_PERCENT = 15;
    private static final int BURN_DAMAGE = 30;
    private static final int BLEED_DAMAGE = 25;
    private static final int INFECTION_DAMAGE = 35;
    private static final int MAX_SCATTERERS = 4;
    private static final int MAX_NYMPHS = 6;

    private final DamageCalculator damageCalculator;
    private final TargetSelector targetSelector;
    private final Map<Battler, Map<Integer, Integer>> cooldowns;
    private final Map<Battle, Integer> partyShieldCharges;
    private final Map<Battle, Integer> partyDamageBuffCharges;
    private final Map<Battle, Integer> partyDamageDebuffCharges;
    private final Map<Battle, Integer> nymphDamageBoostCharges;
    private final Map<Battle, Integer> gruulCountdowns;
    private final Map<Battle, Map<Battler, StatusEffectState>> statusEffects;

    public BattleService(DamageCalculator damageCalculator, TargetSelector targetSelector) {
        this.damageCalculator = damageCalculator;
        this.targetSelector = targetSelector;
        this.cooldowns = new HashMap<>();
        this.partyShieldCharges = new HashMap<>();
        this.partyDamageBuffCharges = new HashMap<>();
        this.partyDamageDebuffCharges = new HashMap<>();
        this.nymphDamageBoostCharges = new HashMap<>();
        this.gruulCountdowns = new HashMap<>();
        this.statusEffects = new HashMap<>();
    }

    public BattleResult usePlayerAttack(Battle battle, PlayerCharacter actor, Attack attack) {
        return usePlayerAttack(battle, actor, attack, null);
    }

    public BattleResult usePlayerAttack(Battle battle, PlayerCharacter actor, Attack attack, Targetable selectedTarget) {
        requireInProgress(battle);
        requireAlive(actor);
        requireKnownAttack(actor, attack);
        requireReady(actor, attack);
        requireLegalTarget(battle, attack, true, selectedTarget);
        applyStartOfTurnEffects(battle, actor);
        if (battle.isFinished() || !actor.isAlive()) {
            battle.advanceTurn();
            return battle.getResult();
        }

        applyAttack(battle, actor, attack, true, selectedTarget);
        tickCooldowns(battle);
        startCooldown(actor, attack);
        updateBattleResult(battle);
        battle.advanceTurn();
        return battle.getResult();
    }

    public BattleResult performEnemyTurn(Battle battle, Enemy actor) {
        requireInProgress(battle);
        requireAlive(actor);
        applyStartOfTurnEffects(battle, actor);
        if (battle.isFinished() || !actor.isAlive()) {
            battle.advanceTurn();
            return battle.getResult();
        }
        updateEnemyState(battle, actor);
        applyEnemyStartOfTurnSpecials(battle, actor);
        Optional<Attack> selectedAttack = chooseFirstReadyAttack(actor);
        if (selectedAttack.isEmpty()) {
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    "Wait",
                    actor.getName(),
                    0,
                    actor.getName() + " waits for an opening."
            ));
            tickCooldowns(battle);
            updateBattleResult(battle);
            battle.advanceTurn();
            return battle.getResult();
        }
        Attack attack = selectedAttack.get();
        applyAttack(battle, actor, attack, false, null);
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

    public List<? extends Targetable> getLegalTargets(Battle battle, Attack attack, boolean actorIsPlayer) {
        return targetSelector.getLegalTargets(battle, attack.getTargetType(), actorIsPlayer);
    }

    public boolean requiresTargetSelection(Battle battle, Attack attack, boolean actorIsPlayer) {
        return getLegalTargets(battle, attack, actorIsPlayer).size() > 1;
    }

    public List<Battler> getTurnPreview(Battle battle, int count) {
        List<Battler> turnOrder = getTurnOrder(battle);
        if (turnOrder.isEmpty() || count <= 0) {
            return List.of();
        }
        return IntStream.range(0, count)
                .mapToObj(index -> turnOrder.get(index % turnOrder.size()))
                .toList();
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

    private void applyAttack(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer,
                             Targetable selectedTarget) {
        if (actorIsPlayer && attack.getEffect() == AttackEffect.SHIELD) {
            applyPartyShield(battle, actor, attack);
            return;
        }
        if (actorIsPlayer && attack.getEffect() == AttackEffect.BUFF_ATTACK) {
            applyPartyDamageBuff(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && attack.getEffect() == AttackEffect.SPAWN) {
            applySpawnSpecial(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && attack.getEffect() == AttackEffect.DEVOUR) {
            applyDevourSpecial(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && actor.getName().equals("Scatterer") && attack.getName().equals("Offering")) {
            applyOfferingSpecial(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && actor.getName().equals("Gore-Gore") && attack.getName().equals("Summon Gruul")) {
            applySummonGruulSpecial(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && actor.getName().equals("Heather") && attack.getName().equals("Hibernate")) {
            applyHibernateSpecial(battle, actor, attack);
            return;
        }
        if (!actorIsPlayer && actor.getName().equals("Circe") && attack.getName().equals("Gelee Royale")) {
            applyGeleeRoyaleSpecial(battle, actor, attack);
            return;
        }
        if (attack.getEffect() == AttackEffect.HEAL && attack.getTargetType() != TargetType.SINGLE_ENEMY) {
            applyHealing(battle, actor, attack, actorIsPlayer);
            return;
        }

        DamageOutcome outcome = applyDamage(battle, actor, attack, actorIsPlayer, selectedTarget);
        applyPostDamageEffect(battle, actor, attack, outcome, actorIsPlayer);
    }

    private DamageOutcome applyDamage(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer,
                                      Targetable selectedTarget) {
        List<? extends Targetable> targets = targetSelector.selectTargets(battle, attack.getTargetType(),
                actorIsPlayer, selectedTarget);
        String targetLabel = targetLabel(targets);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                targetLabel,
                0,
                actor.getName() + " uses " + attack.getName() + " on " + targetLabel + "."
        ));
        int totalAmount = 0;
        List<Enemy> damagedEnemies = new java.util.ArrayList<>();
        boolean partyShielded = !actorIsPlayer && targets.stream().anyMatch(PlayerCharacter.class::isInstance)
                && hasPartyShield(battle);
        for (Targetable target : targets) {
            int damage = partyShielded ? 0 : damageCalculator.rollDamage(attack);
            damage = applyDamageModifiers(battle, actor, attack, target, damage, actorIsPlayer);
            target.receiveDamage(damage);
            totalAmount += damage;
            if (target instanceof Enemy enemy) {
                damagedEnemies.add(enemy);
            }
        }
        if (partyShielded) {
            consumePartyShield(battle);
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    "Shield",
                    "Party",
                    0,
                    "Protective Shell blocks the incoming damage."
            ));
        }
        if (actorIsPlayer && totalAmount > 0) {
            consumePartyDamageBuff(battle);
            consumePartyDamageDebuff(battle);
        }
        if (!actorIsPlayer && actor.getName().equals("Mantis Nymph") && totalAmount > 0) {
            consumeNymphDamageBoost(battle);
        }

        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                targetLabel,
                totalAmount,
                actor.getName() + " used " + attack.getName() + " for " + totalAmount + " damage."
        ));
        damagedEnemies.forEach(enemy -> updateEnemyState(battle, enemy));
        return new DamageOutcome(totalAmount, damagedEnemies, targets);
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

    private void applyPartyShield(Battle battle, Battler actor, Attack attack) {
        partyShieldCharges.put(battle, 1);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                actor.getName() + " used " + attack.getName() + ". The party is protected from the next enemy attack."
        ));
    }

    private void applyPartyDamageBuff(Battle battle, Battler actor, Attack attack) {
        partyDamageBuffCharges.put(battle, 1);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                actor.getName() + " used " + attack.getName() + ". Party damage is increased by "
                        + PARTY_DAMAGE_BUFF_PERCENT + "% for the next attack."
        ));
    }

    private void applyPostDamageEffect(Battle battle, Battler actor, Attack attack, DamageOutcome outcome,
                                       boolean actorIsPlayer) {
        switch (attack.getEffect()) {
            case HEAL -> healActorAfterDamage(battle, actor, attack);
            case BURN -> applyStatusToTargets(battle, actor, attack, outcome.targets(), "Burn", BURN_DAMAGE, 1);
            case BLEED -> applyStatusToTargets(battle, actor, attack, outcome.targets(), "Bleed", BLEED_DAMAGE, 2);
            case INFECTION -> applyStatusToTargets(battle, actor, attack, outcome.targets(), "Infection",
                    INFECTION_DAMAGE, 2);
            case DEBUFF_DEFENSE -> applyPartyDamageDebuff(battle, actor, attack);
            default -> {
            }
        }
        if (actorIsPlayer) {
            applyReflectIfNeeded(battle, actor, attack, outcome);
        }
        if (actorIsPlayer && attack.getName().equals("Explosive Toss") && outcome.totalAmount() > 0) {
            applyExplosiveTossBacklash(battle, actor, attack, outcome.totalAmount());
        }
    }

    private void healActorAfterDamage(Battle battle, Battler actor, Attack attack) {
        int healing = Math.max(1, actor.getMaxHp() / 10);
        actor.heal(healing);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                actor.getName(),
                healing,
                actor.getName() + " recovers " + healing + " HP."
        ));
    }

    private void applyStatusToTargets(Battle battle, Battler actor, Attack attack,
                                      List<? extends Targetable> targets, String effectName,
                                      int damage, int turns) {
        Map<Battler, StatusEffectState> battleEffects = statusEffects.computeIfAbsent(battle, ignored -> new HashMap<>());
        for (Targetable target : targets) {
            if (target instanceof Battler battler && battler.isAlive()) {
                battleEffects.put(battler, new StatusEffectState(effectName, damage, turns));
                battle.addTurn(new BattleTurn(
                        battle.getTurnNumber(),
                        actor.getName(),
                        attack.getName(),
                        battler.getName(),
                        0,
                        battler.getName() + " is affected by " + effectName + "."
                ));
            }
        }
    }

    private void applyExplosiveTossBacklash(Battle battle, Battler actor, Attack attack, int totalDamage) {
        int backlash = Math.max(1, totalDamage * 10 / 100);
        for (PlayerCharacter character : battle.getAlivePartyMembers()) {
            character.receiveDamage(backlash);
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                backlash,
                "Explosive Toss backlash hits each party member for " + backlash + " damage."
        ));
    }

    private void applyEnemyStartOfTurnSpecials(Battle battle, Enemy actor) {
        if (actor instanceof BossEnemy bossEnemy && actor.getName().equals("Circe")
                && !bossEnemy.getState().name().equals("FINAL_PHASE")) {
            spawnEnemyByName(battle, "Mantis Nymph", MAX_NYMPHS)
                    .ifPresent(spawned -> battle.addTurn(new BattleTurn(
                            battle.getTurnNumber(),
                            actor.getName(),
                            "Brood Mother",
                            spawned.getName(),
                            0,
                            "Brood Mother creates a Mantis Nymph."
                    )));
        }
        if (actor instanceof BossEnemy bossEnemy && actor.getName().equals("Hoarder")
                && bossEnemy.getState().name().equals("PHASE_TWO")) {
            spawnEnemyByName(battle, "Scatterer", MAX_SCATTERERS)
                    .ifPresent(spawned -> battle.addTurn(new BattleTurn(
                            battle.getTurnNumber(),
                            actor.getName(),
                            "Garbage Spawn",
                            spawned.getName(),
                            0,
                            "Garbage Spawn passively creates a Scatterer."
                    )));
        }
    }

    private void applySpawnSpecial(Battle battle, Battler actor, Attack attack) {
        String spawnName = actor.getName().equals("Circe") ? "Mantis Nymph" : "Scatterer";
        int maxAlive = actor.getName().equals("Circe") ? MAX_NYMPHS : MAX_SCATTERERS;
        Optional<Enemy> spawned = spawnEnemyByName(battle, spawnName, maxAlive);
        String message = spawned
                .map(enemy -> actor.getName() + " used " + attack.getName() + ". " + enemy.getName() + " joins the fight.")
                .orElse(actor.getName() + " used " + attack.getName() + ", but no more " + spawnName + " can fit.");
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                spawnName,
                0,
                message
        ));
    }

    private void applyDevourSpecial(Battle battle, Battler actor, Attack attack) {
        List<Enemy> scatterers = battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Scatterer"))
                .toList();
        int healing = scatterers.stream().mapToInt(Enemy::getCurrentHp).sum();
        scatterers.forEach(scatterer -> scatterer.receiveDamage(scatterer.getCurrentHp()));
        actor.heal(healing);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Scatterers",
                healing,
                actor.getName() + " devours " + scatterers.size() + " Scatterers and heals " + healing + " HP."
        ));
        scatterers.forEach(scatterer -> updateEnemyState(battle, scatterer));
    }

    private void applyOfferingSpecial(Battle battle, Battler actor, Attack attack) {
        Optional<Enemy> boss = battle.getAliveEnemies().stream()
                .filter(BossEnemy.class::isInstance)
                .findFirst();
        int healing = actor.getCurrentHp();
        boss.ifPresent(enemy -> enemy.heal(healing));
        actor.receiveDamage(actor.getCurrentHp());
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                boss.map(Enemy::getName).orElse("Boss"),
                healing,
                actor.getName() + " offers itself and heals the boss for " + healing + " HP."
        ));
        if (actor instanceof TrashEnemy trashEnemy) {
            updateEnemyState(battle, trashEnemy);
        }
    }

    private void applySummonGruulSpecial(Battle battle, Battler actor, Attack attack) {
        int remaining = gruulCountdowns.getOrDefault(battle, 2) - 1;
        gruulCountdowns.put(battle, remaining);
        if (remaining <= 0) {
            battle.getAlivePartyMembers().forEach(character -> character.receiveDamage(character.getMaxHp()));
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    attack.getName(),
                    "Party",
                    0,
                    "Summon Gruul completes. The party is destroyed."
            ));
            updateBattleResult(battle);
            return;
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                actor.getName() + " begins Summon Gruul. " + remaining + " turn remains."
        ));
    }

    private void applyHibernateSpecial(Battle battle, Battler actor, Attack attack) {
        int healing = Math.max(1, actor.getMaxHp() * 30 / 100);
        actor.heal(healing);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                actor.getName(),
                healing,
                actor.getName() + " hibernates and heals " + healing + " HP."
        ));
    }

    private void applyGeleeRoyaleSpecial(Battle battle, Battler actor, Attack attack) {
        int boostedNymphs = (int) battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals("Mantis Nymph"))
                .count();
        nymphDamageBoostCharges.put(battle, boostedNymphs);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Mantis Nymphs",
                0,
                actor.getName() + " feeds the nymphs. Their next " + boostedNymphs + " attacks are stronger."
        ));
    }

    private void applyPartyDamageDebuff(Battle battle, Battler actor, Attack attack) {
        partyDamageDebuffCharges.put(battle, 1);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                attack.getName() + " weakens the party's next attack."
        ));
    }

    private void applyReflectIfNeeded(Battle battle, Battler actor, Attack attack, DamageOutcome outcome) {
        for (Targetable target : outcome.targets()) {
            if (target instanceof BossEnemy bossEnemy
                    && bossEnemy.getName().equals("Denise")
                    && bossEnemy.getState().name().equals("FINAL_PHASE")
                    && outcome.totalAmount() > 0) {
                int reflected = Math.max(1, outcome.totalAmount() * 20 / 100);
                actor.receiveDamage(reflected);
                battle.addTurn(new BattleTurn(
                        battle.getTurnNumber(),
                        bossEnemy.getName(),
                        "Reflect",
                        actor.getName(),
                        reflected,
                        "Reflect sends " + reflected + " damage back to " + actor.getName() + "."
                ));
                updateBattleResult(battle);
                return;
            }
        }
    }

    private void applyStartOfTurnEffects(Battle battle, Battler actor) {
        Map<Battler, StatusEffectState> battleEffects = statusEffects.get(battle);
        if (battleEffects == null) {
            return;
        }
        StatusEffectState effect = battleEffects.get(actor);
        if (effect == null) {
            return;
        }
        actor.receiveDamage(effect.damage());
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                effect.name(),
                actor.getName(),
                effect.damage(),
                actor.getName() + " takes " + effect.damage() + " " + effect.name() + " damage."
        ));
        if (effect.remainingTurns() <= 1 || !actor.isAlive()) {
            battleEffects.remove(actor);
        } else {
            battleEffects.put(actor, effect.tick());
        }
        updateBattleResult(battle);
        if (actor instanceof Enemy enemy) {
            updateEnemyState(battle, enemy);
        }
    }

    private Optional<Attack> chooseFirstReadyAttack(Battler actor) {
        return actor.getAttacks().stream()
                .filter(attack -> isUnlockedForActor(actor, attack))
                .filter(attack -> isReady(actor, attack))
                .findFirst();
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

    private void requireLegalTarget(Battle battle, Attack attack, boolean actorIsPlayer, Targetable selectedTarget) {
        List<? extends Targetable> legalTargets = getLegalTargets(battle, attack, actorIsPlayer);
        if (legalTargets.isEmpty() || selectedTarget == null) {
            return;
        }
        if (!legalTargets.contains(selectedTarget)) {
            throw new IllegalArgumentException("Selected target is not valid for " + attack.getName());
        }
    }

    private boolean hasPartyShield(Battle battle) {
        return partyShieldCharges.getOrDefault(battle, 0) > 0;
    }

    private void consumePartyShield(Battle battle) {
        partyShieldCharges.computeIfPresent(battle, (ignored, charges) -> Math.max(0, charges - 1));
    }

    private int applyDamageModifiers(Battle battle, Battler actor, Attack attack, Targetable target, int damage,
                                     boolean actorIsPlayer) {
        int modifiedDamage = damage;
        if (actorIsPlayer && partyDamageBuffCharges.getOrDefault(battle, 0) > 0) {
            modifiedDamage += modifiedDamage * PARTY_DAMAGE_BUFF_PERCENT / 100;
        }
        if (actorIsPlayer && partyDamageDebuffCharges.getOrDefault(battle, 0) > 0) {
            modifiedDamage /= 2;
        }
        if (actorIsPlayer && target instanceof BossEnemy bossEnemy && bossEnemy.getName().equals("Denise")
                && actor.getName().equals("Donut")) {
            modifiedDamage /= 2;
        }
        if (actorIsPlayer && target instanceof BossEnemy bossEnemy && bossEnemy.getName().equals("Hoarder")) {
            long scatterersAlive = battle.getAliveEnemies().stream()
                    .filter(enemy -> enemy.getName().equals("Scatterer"))
                    .count();
            modifiedDamage = (int) Math.round(modifiedDamage * Math.max(0.10, 1.0 - scatterersAlive * 0.15));
        }
        if (!actorIsPlayer && actor.getName().equals("Gore-Gore") && target instanceof Battler battler
                && hasStatus(battle, battler, "Bleed")) {
            modifiedDamage += modifiedDamage * 20 / 100;
        }
        if (!actorIsPlayer && actor.getName().equals("Heather") && attack.getName().equals("Roller Skate Charge")) {
            int missingHpPercent = 100 - actor.getCurrentHp() * 100 / actor.getMaxHp();
            modifiedDamage += modifiedDamage * missingHpPercent / 100;
        }
        if (!actorIsPlayer && actor.getName().equals("Mantis Nymph") && attack.getName().equals("Swarm Bite")) {
            long otherNymphs = battle.getAliveEnemies().stream()
                    .filter(enemy -> enemy.getName().equals("Mantis Nymph"))
                    .count() - 1;
            modifiedDamage += modifiedDamage * Math.max(0, (int) otherNymphs) * 20 / 100;
        }
        if (!actorIsPlayer && actor.getName().equals("Mantis Nymph")
                && nymphDamageBoostCharges.getOrDefault(battle, 0) > 0) {
            modifiedDamage += modifiedDamage * 25 / 100;
        }
        return Math.max(0, modifiedDamage);
    }

    private void consumePartyDamageBuff(Battle battle) {
        partyDamageBuffCharges.computeIfPresent(battle, (ignored, charges) -> Math.max(0, charges - 1));
    }

    private void consumePartyDamageDebuff(Battle battle) {
        partyDamageDebuffCharges.computeIfPresent(battle, (ignored, charges) -> Math.max(0, charges - 1));
    }

    private void consumeNymphDamageBoost(Battle battle) {
        nymphDamageBoostCharges.computeIfPresent(battle, (ignored, charges) -> Math.max(0, charges - 1));
    }

    private boolean hasStatus(Battle battle, Battler battler, String statusName) {
        return Optional.ofNullable(statusEffects.get(battle))
                .map(effects -> effects.get(battler))
                .filter(effect -> effect.name().equals(statusName))
                .isPresent();
    }

    private Optional<Enemy> spawnEnemyByName(Battle battle, String name, int maxAlive) {
        long aliveCount = battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals(name))
                .count();
        if (aliveCount >= maxAlive) {
            return Optional.empty();
        }
        Optional<Enemy> prototype = battle.getEnemies().stream()
                .filter(enemy -> enemy.getName().equals(name))
                .findFirst();
        if (prototype.isEmpty()) {
            return Optional.empty();
        }
        Enemy spawned = copyEnemy(prototype.get());
        battle.addEnemy(spawned);
        return Optional.of(spawned);
    }

    private Enemy copyEnemy(Enemy enemy) {
        if (enemy instanceof BossEnemy) {
            return new BossEnemy(
                    enemy.getId(),
                    enemy.getName(),
                    enemy.getMaxHp(),
                    enemy.getMaxHp(),
                    enemy.getSpeed(),
                    enemy.getSpritePath(),
                    enemy.getPassiveName(),
                    enemy.getPassiveDescription(),
                    enemy.getAttacks()
            );
        }
        return new TrashEnemy(
                enemy.getId(),
                enemy.getName(),
                enemy.getMaxHp(),
                enemy.getMaxHp(),
                enemy.getSpeed(),
                enemy.getSpritePath(),
                enemy.getPassiveName(),
                enemy.getPassiveDescription(),
                enemy.getAttacks()
        );
    }

    private record DamageOutcome(int totalAmount, List<Enemy> damagedEnemies, List<? extends Targetable> targets) {
    }

    private record StatusEffectState(String name, int damage, int remainingTurns) {
        private StatusEffectState tick() {
            return new StatusEffectState(name, damage, remainingTurns - 1);
        }
    }
}
