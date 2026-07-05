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
import at.fhooe.ald.model.fsm.BossEnemyState;
import at.fhooe.ald.model.fsm.TrashEnemyState;
import at.fhooe.ald.model.fsm.BossEnemyStateMachine;
import at.fhooe.ald.model.fsm.TrashEnemyStateMachine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class BattleService {
    private static final int PARTY_DAMAGE_BUFF_PERCENT = 15;
    private static final int STATUS_EFFECT_TICKS = 2;
    private static final int STATUS_TICK_MIN_PERCENT = 20;
    private static final int STATUS_TICK_MAX_PERCENT = 30;
    private static final int HEALING_OVER_TIME_TICKS = 2;
    private static final int MAX_SCATTERERS = 4;
    private static final int MAX_NYMPHS = 4;

    private final DamageCalculator damageCalculator;
    private final TargetSelector targetSelector;
    private final Map<Battler, Map<Integer, Integer>> cooldowns;
    private final Map<Battle, Battler> partyShieldSources;
    private final Map<Battle, Integer> partyDamageBuffCharges;
    private final Map<Battle, Integer> partyDamageDebuffCharges;
    private final Map<Battle, Integer> nymphDamageBoostCharges;
    private final Map<Battle, Integer> gruulCountdowns;
    private final Map<Battle, Integer> donutTurnCounts;
    private final Map<Battle, Enemy> donutCharmTargets;
    private final Map<Battle, Map<Battler, List<StatusEffectState>>> statusEffects;

    public BattleService(DamageCalculator damageCalculator, TargetSelector targetSelector) {
        this.damageCalculator = damageCalculator;
        this.targetSelector = targetSelector;
        this.cooldowns = new HashMap<>();
        this.partyShieldSources = new HashMap<>();
        this.partyDamageBuffCharges = new HashMap<>();
        this.partyDamageDebuffCharges = new HashMap<>();
        this.nymphDamageBoostCharges = new HashMap<>();
        this.gruulCountdowns = new HashMap<>();
        this.donutTurnCounts = new HashMap<>();
        this.donutCharmTargets = new HashMap<>();
        this.statusEffects = new HashMap<>();
    }

    public BattleResult usePlayerAttack(Battle battle, PlayerCharacter actor, Attack attack) {
        return usePlayerAttack(battle, actor, attack, null);
    }

    public BattleResult usePlayerAttack(Battle battle, PlayerCharacter actor, Attack attack, Targetable selectedTarget) {
        requireInProgress(battle);
        requireAlive(actor);
        requireKnownAttack(actor, attack);
        if (hasStatus(battle, actor, "Stun")) {
            boolean turnSkipped = applyStartOfTurnEffects(battle, actor);
            if (battle.isFinished() || !actor.isAlive() || turnSkipped) {
                if (turnSkipped) {
                    tickCooldowns(actor);
                }
                battle.advanceTurn();
                return battle.getResult();
            }
        }
        requireReady(actor, attack);
        requireLegalTarget(battle, attack, true, selectedTarget);
        boolean turnSkipped = applyStartOfTurnEffects(battle, actor);
        if (battle.isFinished() || !actor.isAlive()) {
            battle.advanceTurn();
            return battle.getResult();
        }
        if (turnSkipped) {
            tickCooldowns(actor);
            battle.advanceTurn();
            return battle.getResult();
        }

        applyAttack(battle, actor, attack, true, selectedTarget);
        tickCooldowns(actor);
        startCooldown(actor, attack);
        updateBattleResult(battle);
        battle.advanceTurn();
        return battle.getResult();
    }

    public BattleResult performEnemyTurn(Battle battle, Enemy actor) {
        requireInProgress(battle);
        requireAlive(actor);
        applyNonStatusStartOfTurnEffects(battle, actor);
        updateEnemyState(battle, actor);
        applyRalphLethalInfectionKills(battle, actor);
        if (battle.isFinished()) {
            battle.advanceTurn();
            return battle.getResult();
        }
        applyEnemyStartOfTurnSpecials(battle, actor);
        applyStatusTicks(battle, actor);
        if (battle.isFinished() || !actor.isAlive()) {
            battle.advanceTurn();
            return battle.getResult();
        }
        if (consumeStunIfPresent(battle, actor)) {
            expireRoyalCharmAfterEnemyTurn(battle, actor);
            tickCooldowns(actor);
            updateBattleResult(battle);
            battle.advanceTurn();
            return battle.getResult();
        }
        if (advanceGruulRitualIfNeeded(battle, actor)) {
            battle.advanceTurn();
            return battle.getResult();
        }
        Optional<Attack> selectedAttack = chooseFirstReadyAttack(battle, actor);
        if (selectedAttack.isEmpty()) {
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    "Wait",
                    actor.getName(),
                    0,
                    actor.getName() + " waits for an opening."
            ));
            expireRoyalCharmAfterEnemyTurn(battle, actor);
            tickCooldowns(actor);
            updateBattleResult(battle);
            battle.advanceTurn();
            return battle.getResult();
        }
        Attack attack = selectedAttack.get();
        applyAttack(battle, actor, attack, false, null);
        expireRoyalCharmAfterEnemyTurn(battle, actor);
        tickCooldowns(actor);
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

    public boolean hasPartyProtection(Battle battle) {
        return hasPartyShield(battle);
    }

    public boolean hasPartyDamageBoost(Battle battle) {
        return partyDamageBuffCharges.getOrDefault(battle, 0) > 0;
    }

    public boolean hasNymphDamageBoost(Battle battle) {
        return nymphDamageBoostCharges.getOrDefault(battle, 0) > 0;
    }

    public boolean hasStatusEffect(Battle battle, Battler battler, String statusName) {
        return hasStatus(battle, battler, statusName);
    }

    public boolean hasRoyalCharm(Battle battle, Battler battler) {
        return battler instanceof Enemy && donutCharmTargets.get(battle) == battler;
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
        Battler currentActor = battle.getCurrentActor().orElse(turnOrder.getFirst());
        int currentIndex = Math.max(0, turnOrder.indexOf(currentActor));
        return IntStream.range(0, count)
                .mapToObj(index -> turnOrder.get((currentIndex + index) % turnOrder.size()))
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
            applyHealing(battle, actor, attack, actorIsPlayer, selectedTarget);
            return;
        }

        DamageOutcome outcome = attack.getName().equals("Rake")
                ? applyDoubleDamage(battle, actor, attack, actorIsPlayer, selectedTarget)
                : applyDamage(battle, actor, attack, actorIsPlayer, selectedTarget);
        applyPostDamageEffect(battle, actor, attack, outcome, actorIsPlayer);
    }

    private DamageOutcome applyDamage(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer,
                                      Targetable selectedTarget) {
        List<? extends Targetable> targets = selectTargetsForAttack(battle, actor, attack, actorIsPlayer,
                selectedTarget);
        String targetLabel = targetLabel(targets);
        if (targets.isEmpty()) {
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    attack.getName(),
                    "",
                    0,
                    actor.getName() + " cannot find a valid target."
            ));
            return new DamageOutcome(0, List.of(), targets, Map.of());
        }
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
        Map<Targetable, Integer> damageByTarget = new IdentityHashMap<>();
        boolean partyShielded = !actorIsPlayer && targets.stream().anyMatch(PlayerCharacter.class::isInstance)
                && hasPartyShield(battle);
        for (Targetable target : targets) {
            int damage = partyShielded ? 0 : damageCalculator.rollDamage(attack);
            damage = applyDamageModifiers(battle, actor, attack, target, damage, actorIsPlayer);
            target.receiveDamage(damage);
            totalAmount += damage;
            damageByTarget.put(target, damage);
            addDamageAnimationTurn(battle, actor, attack, target, damage);
            if (target instanceof Enemy enemy) {
                damagedEnemies.add(enemy);
            }
        }
        if (partyShielded) {
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
                actor.getName() + " used " + attack.getName() + System.lineSeparator()
                        + totalAmount + " damage."
        ));
        damagedEnemies.forEach(enemy -> updateEnemyState(battle, enemy));
        return new DamageOutcome(totalAmount, damagedEnemies, targets, damageByTarget);
    }

    private DamageOutcome applyDoubleDamage(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer,
                                            Targetable selectedTarget) {
        Targetable fixedTarget = selectedTarget;
        if (fixedTarget == null && isSingleTargetType(attack.getTargetType())) {
            List<? extends Targetable> selectedTargets = selectTargetsForAttack(battle, actor, attack, actorIsPlayer,
                    null);
            fixedTarget = selectedTargets.isEmpty() ? null : selectedTargets.getFirst();
        }
        DamageOutcome firstHit = applyDamage(battle, actor, attack, actorIsPlayer, fixedTarget);
        if (battle.isFinished()) {
            return firstHit;
        }
        DamageOutcome secondHit = applyDamage(battle, actor, attack, actorIsPlayer, fixedTarget);
        Map<Targetable, Integer> combinedDamage = new IdentityHashMap<>();
        firstHit.damageByTarget().forEach(combinedDamage::put);
        secondHit.damageByTarget().forEach((target, damage) ->
                combinedDamage.merge(target, damage, Integer::sum));
        List<Enemy> damagedEnemies = new ArrayList<>(firstHit.damagedEnemies());
        for (Enemy enemy : secondHit.damagedEnemies()) {
            if (!damagedEnemies.contains(enemy)) {
                damagedEnemies.add(enemy);
            }
        }
        return new DamageOutcome(firstHit.totalAmount() + secondHit.totalAmount(),
                damagedEnemies, firstHit.targets(), combinedDamage);
    }

    private List<? extends Targetable> selectTargetsForAttack(Battle battle, Battler actor, Attack attack,
                                                              boolean actorIsPlayer, Targetable selectedTarget) {
        if (!actorIsPlayer && hasRoyalCharm(battle, actor) && targetsParty(attack.getTargetType())) {
            List<Targetable> legalTargets = new ArrayList<>(battle.getAlivePartyMembers().stream()
                    .filter(character -> !character.getName().equals("Donut"))
                    .toList());
            if (legalTargets.isEmpty()) {
                return List.of();
            }
            return switch (attack.getTargetType()) {
                case ALL_ENEMIES -> legalTargets;
                case LOWEST_HP_ALLY -> List.of(legalTargets.stream()
                        .min((first, second) -> Integer.compare(first.getCurrentHp(), second.getCurrentHp()))
                        .orElseThrow());
                case SINGLE_ENEMY, RANDOM_ENEMY -> List.of(legalTargets.get(
                        ThreadLocalRandom.current().nextInt(legalTargets.size())));
                default -> targetSelector.selectTargets(battle, attack.getTargetType(), actorIsPlayer,
                        selectedTarget);
            };
        }
        return targetSelector.selectTargets(battle, attack.getTargetType(), actorIsPlayer, selectedTarget);
    }

    private boolean targetsParty(TargetType targetType) {
        return targetType == TargetType.SINGLE_ENEMY
                || targetType == TargetType.RANDOM_ENEMY
                || targetType == TargetType.ALL_ENEMIES
                || targetType == TargetType.LOWEST_HP_ALLY;
    }

    private boolean isSingleTargetType(TargetType targetType) {
        return targetType == TargetType.SINGLE_ENEMY || targetType == TargetType.RANDOM_ENEMY;
    }

    private void addDamageAnimationTurn(Battle battle, Battler actor, Attack attack, Targetable target, int damage) {
        if (damage <= 0 || !(target instanceof Battler battler)) {
            return;
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                battler.getName(),
                damage,
                ""
        ));
    }

    private void applyHealing(Battle battle, Battler actor, Attack attack, boolean actorIsPlayer,
                              Targetable selectedTarget) {
        List<? extends Targetable> targets = attack.getTargetType() == TargetType.SELF
                ? List.of(actor)
                : targetSelector.selectTargets(battle, attack.getTargetType(), actorIsPlayer);
        int totalAmount = 0;
        Map<Targetable, Integer> healingByTarget = new IdentityHashMap<>();
        for (Targetable target : targets) {
            int healing = damageCalculator.rollDamage(attack);
            if (target instanceof Battler battler) {
                battler.heal(healing);
            }
            healingByTarget.put(target, healing);
            totalAmount += healing;
        }
        if (actorIsPlayer && attack.getName().equals("Healing Song")) {
            Targetable hotTarget = selectedTarget == null ? actor : selectedTarget;
            applyHealingSongTargetBonus(battle, actor, attack, hotTarget, healingByTarget.getOrDefault(hotTarget, 0));
        }

        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                targetLabel(targets),
                totalAmount,
                actor.getName() + " used " + attack.getName() + System.lineSeparator()
                        + totalAmount + " healing."
        ));
    }

    private void applyPartyShield(Battle battle, Battler actor, Attack attack) {
        partyShieldSources.put(battle, actor);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                actor.getName() + " used " + attack.getName()
                        + ". The party is protected until " + actor.getName() + "'s next turn."
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
            case HEAL -> healActorAfterDamage(battle, actor, attack, outcome);
            case BURN -> applyStatusToTargets(battle, actor, attack, outcome, "Burn");
            case BLEED -> applyStatusToTargets(battle, actor, attack, outcome, "Bleed");
            case INFECTION -> applyInfectionEffect(battle, actor, attack, outcome);
            case STUN -> applyStunToTargets(battle, actor, attack, outcome);
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

    private void healActorAfterDamage(Battle battle, Battler actor, Attack attack, DamageOutcome outcome) {
        int healing = attack.getName().equals("Gut Ripper")
                ? outcome.totalAmount()
                : Math.max(1, actor.getMaxHp() / 10);
        if (healing <= 0) {
            return;
        }
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
                                      DamageOutcome outcome, String effectName) {
        Map<Battler, List<StatusEffectState>> battleEffects =
                statusEffects.computeIfAbsent(battle, ignored -> new HashMap<>());
        for (Targetable target : outcome.targets()) {
            int triggeringDamage = outcome.damageFor(target);
            if (target instanceof Battler battler && battler.isAlive()) {
                List<StatusEffectState> effects = battleEffects.computeIfAbsent(battler, ignored -> new ArrayList<>());
                effects.removeIf(effect -> effect.name().equals(effectName));
                if (triggeringDamage <= 0) {
                    continue;
                }
                effects.add(StatusEffectState.damage(effectName, triggeringDamage, STATUS_EFFECT_TICKS));
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

    private void applyStunToTargets(Battle battle, Battler actor, Attack attack, DamageOutcome outcome) {
        Map<Battler, List<StatusEffectState>> battleEffects =
                statusEffects.computeIfAbsent(battle, ignored -> new HashMap<>());
        for (Targetable target : outcome.targets()) {
            if (!(target instanceof Battler battler) || !battler.isAlive() || outcome.damageFor(target) <= 0) {
                continue;
            }
            if (attack.getName().equals("Roller Skate Charge") && ThreadLocalRandom.current().nextBoolean()) {
                continue;
            }
            List<StatusEffectState> effects = battleEffects.computeIfAbsent(battler, ignored -> new ArrayList<>());
            effects.removeIf(effect -> effect.name().equals("Stun"));
            effects.add(StatusEffectState.stun());
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    attack.getName(),
                    battler.getName(),
                    0,
                    battler.getName() + " is stunned and will miss the next turn."
            ));
        }
    }

    private void applyLethalInfectionToTargets(Battle battle, Battler actor, Attack attack, DamageOutcome outcome) {
        Map<Battler, List<StatusEffectState>> battleEffects =
                statusEffects.computeIfAbsent(battle, ignored -> new HashMap<>());
        for (Targetable target : outcome.targets()) {
            if (target instanceof Battler battler && battler.isAlive() && outcome.damageFor(target) > 0) {
                List<StatusEffectState> effects = battleEffects.computeIfAbsent(battler, ignored -> new ArrayList<>());
                effects.removeIf(effect -> effect.name().equals("Lethal Infection"));
                effects.add(StatusEffectState.lethalInfection());
                battle.addTurn(new BattleTurn(
                        battle.getTurnNumber(),
                        actor.getName(),
                        attack.getName(),
                        battler.getName(),
                        0,
                        battler.getName() + " is affected by Lethal Infection."
                ));
            }
        }
    }

    private void applyInfectionEffect(Battle battle, Battler actor, Attack attack, DamageOutcome outcome) {
        if (actor instanceof BossEnemy bossEnemy
                && bossEnemy.getName().equals("Ralph")
                && bossEnemy.getState() == BossEnemyState.FINAL_PHASE) {
            applyLethalInfectionToTargets(battle, actor, attack, outcome);
            return;
        }
        applyStatusToTargets(battle, actor, attack, outcome, "Infection");
    }

    private void applyRalphLethalInfectionKills(Battle battle, Enemy actor) {
        if (!(actor instanceof BossEnemy bossEnemy)
                || !bossEnemy.getName().equals("Ralph")
                || bossEnemy.getState() != BossEnemyState.FINAL_PHASE) {
            return;
        }
        Map<Battler, List<StatusEffectState>> battleEffects = statusEffects.get(battle);
        if (battleEffects == null) {
            return;
        }
        for (var entry : List.copyOf(battleEffects.entrySet())) {
            Battler battler = entry.getKey();
            List<StatusEffectState> effects = entry.getValue();
            boolean lethal = effects.stream().anyMatch(effect -> effect.name().equals("Lethal Infection"));
            if (!lethal || !battler.isAlive()) {
                continue;
            }
            int damage = battler.getCurrentHp();
            battler.receiveDamage(damage);
            effects.removeIf(effect -> effect.name().equals("Lethal Infection"));
            if (effects.isEmpty()) {
                battleEffects.remove(battler);
            }
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    "Lethal Infection",
                    battler.getName(),
                    damage,
                    battler.getName() + " is killed by Lethal Infection."
            ));
        }
        updateBattleResult(battle);
    }

    private void applyHealingSongTargetBonus(Battle battle, Battler actor, Attack attack, Targetable target,
                                             int triggeringHealing) {
        if (!(target instanceof Battler battler) || triggeringHealing <= 0) {
            return;
        }
        dispelNegativeEffects(battle, battler);
        Map<Battler, List<StatusEffectState>> battleEffects =
                statusEffects.computeIfAbsent(battle, ignored -> new HashMap<>());
        List<StatusEffectState> effects = battleEffects.computeIfAbsent(battler, ignored -> new ArrayList<>());
        effects.removeIf(effect -> effect.name().equals("Healing Song"));
        effects.add(StatusEffectState.healing("Healing Song", triggeringHealing, HEALING_OVER_TIME_TICKS));
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                battler.getName(),
                0,
                battler.getName() + " is cleansed and receives Healing Song over time."
        ));
    }

    private void dispelNegativeEffects(Battle battle, Battler battler) {
        Map<Battler, List<StatusEffectState>> battleEffects = statusEffects.get(battle);
        if (battleEffects == null) {
            return;
        }
        List<StatusEffectState> effects = battleEffects.get(battler);
        if (effects == null) {
            return;
        }
        effects.removeIf(StatusEffectState::negative);
        if (effects.isEmpty()) {
            battleEffects.remove(battler);
        }
    }

    private void applyExplosiveTossBacklash(Battle battle, Battler actor, Attack attack, int totalDamage) {
        int backlashPercent = ThreadLocalRandom.current().nextInt(0, 11);
        int backlash = totalDamage * backlashPercent / 100;
        for (PlayerCharacter character : battle.getAlivePartyMembers()) {
            character.receiveDamage(backlash);
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                backlash,
                "Explosive Toss backlash hits each party member for " + backlash + " damage ("
                        + backlashPercent + "%)."
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
                && (bossEnemy.getState().name().equals("PHASE_TWO")
                || bossEnemy.getState().name().equals("FINAL_PHASE"))) {
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
        int spawnAttempts = actor.getName().equals("Hoarder") && attack.getName().equals("Garbage Spawn")
                ? ThreadLocalRandom.current().nextInt(1, 3)
                : 1;
        List<Enemy> spawnedEnemies = new ArrayList<>();
        for (int i = 0; i < spawnAttempts; i++) {
            spawnEnemyByName(battle, spawnName, maxAlive).ifPresent(spawnedEnemies::add);
        }
        String message = spawnedEnemies.isEmpty()
                ? actor.getName() + " used " + attack.getName() + ", but no more " + spawnName + " can fit."
                : actor.getName() + " used " + attack.getName() + ". " + spawnedEnemies.size()
                        + " " + spawnName + (spawnedEnemies.size() == 1 ? " joins" : "s join") + " the fight.";
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
        gruulCountdowns.put(battle, 2);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                "Party",
                0,
                actor.getName() + " begins Summon Gruul. The party has one Gore-Gore turn to stop it."
        ));
    }

    private boolean advanceGruulRitualIfNeeded(Battle battle, Battler actor) {
        if (!actor.getName().equals("Gore-Gore")) {
            return false;
        }
        int remaining = gruulCountdowns.getOrDefault(battle, 0);
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        if (remaining <= 0) {
            gruulCountdowns.remove(battle);
            battle.getAlivePartyMembers().forEach(character -> character.receiveDamage(character.getMaxHp()));
            battle.addTurn(new BattleTurn(
                    battle.getTurnNumber(),
                    actor.getName(),
                    "Summon Gruul",
                    "Party",
                    0,
                    "Summon Gruul completes. The party is destroyed."
            ));
            updateBattleResult(battle);
            return true;
        }
        gruulCountdowns.put(battle, remaining);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                "Summon Gruul",
                "Party",
                0,
                "The Gruul ritual continues. It will complete on Gore-Gore's next turn."
        ));
        return false;
    }

    private void applyHibernateSpecial(Battle battle, Battler actor, Attack attack) {
        int healPercent = ThreadLocalRandom.current().nextInt(20, 31);
        int healing = Math.max(1, actor.getMaxHp() * healPercent / 100);
        actor.heal(healing);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                attack.getName(),
                actor.getName(),
                healing,
                actor.getName() + " hibernates and heals " + healing + " HP (" + healPercent + "%)."
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

    private boolean applyStartOfTurnEffects(Battle battle, Battler actor) {
        applyNonStatusStartOfTurnEffects(battle, actor);
        applyStatusTicks(battle, actor);
        if (battle.isFinished() || !actor.isAlive()) {
            return false;
        }
        if (consumeStunIfPresent(battle, actor)) {
            return true;
        }
        applyPlayerStartOfTurnPassives(battle, actor);
        return false;
    }

    private void applyNonStatusStartOfTurnEffects(Battle battle, Battler actor) {
        expirePartyShieldCreatedBy(battle, actor);
    }

    private void applyStatusTicks(Battle battle, Battler actor) {
        Map<Battler, List<StatusEffectState>> battleEffects = statusEffects.get(battle);
        List<StatusEffectState> actorEffects = battleEffects == null ? null : battleEffects.get(actor);
        if (actorEffects != null && !actorEffects.isEmpty()) {
            for (StatusEffectState effect : List.copyOf(actorEffects)) {
                if (effect.lethal() || effect.name().equals("Stun")) {
                    continue;
                }
                int tickAmount = rollStatusTickAmount(effect.baseAmount());
                if (effect.lethal()) {
                    tickAmount = actor.getCurrentHp();
                    actor.receiveDamage(tickAmount);
                } else if (effect.healing()) {
                    actor.heal(tickAmount);
                } else {
                    actor.receiveDamage(tickAmount);
                }
                battle.addTurn(new BattleTurn(
                        battle.getTurnNumber(),
                        actor.getName(),
                        effect.name(),
                        actor.getName(),
                        tickAmount,
                        statusTickMessage(actor, effect, tickAmount)
                ));
                actorEffects.remove(effect);
                if (effect.remainingTicks() > 1 && actor.isAlive()) {
                    actorEffects.add(effect.tick());
                }
                if (!actor.isAlive()) {
                    break;
                }
            }
            if (actorEffects.isEmpty() || !actor.isAlive()) {
                battleEffects.remove(actor);
            }
            updateBattleResult(battle);
            if (actor instanceof Enemy enemy) {
                updateEnemyState(battle, enemy);
            }
        }
    }

    private boolean consumeStunIfPresent(Battle battle, Battler actor) {
        Map<Battler, List<StatusEffectState>> battleEffects = statusEffects.get(battle);
        List<StatusEffectState> actorEffects = battleEffects == null ? null : battleEffects.get(actor);
        if (actorEffects == null || actorEffects.stream().noneMatch(effect -> effect.name().equals("Stun"))) {
            return false;
        }
        actorEffects.removeIf(effect -> effect.name().equals("Stun"));
        if (actorEffects.isEmpty()) {
            battleEffects.remove(actor);
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                "Stun",
                actor.getName(),
                0,
                actor.getName() + " is stunned and loses the turn."
        ));
        return true;
    }

    private String statusTickMessage(Battler actor, StatusEffectState effect, int amount) {
        if (effect.lethal()) {
            return actor.getName() + " is killed by Lethal Infection.";
        }
        if (effect.healing()) {
            return actor.getName() + " recovers " + amount + " HP from " + effect.name() + ".";
        }
        return actor.getName() + " takes " + amount + " " + effect.name() + " damage.";
    }

    private int rollStatusTickAmount(int baseAmount) {
        if (baseAmount <= 0) {
            return 0;
        }
        int percent = ThreadLocalRandom.current().nextInt(STATUS_TICK_MIN_PERCENT, STATUS_TICK_MAX_PERCENT + 1);
        return Math.max(1, baseAmount * percent / 100);
    }

    private void applyPlayerStartOfTurnPassives(Battle battle, Battler actor) {
        if (actor instanceof PlayerCharacter && actor.getName().equals("Donut")) {
            applyRoyalCharm(battle, actor);
        }
    }

    private void applyRoyalCharm(Battle battle, Battler actor) {
        int donutTurn = donutTurnCounts.merge(battle, 1, Integer::sum);
        if (donutTurn % 2 != 0) {
            return;
        }
        List<Enemy> aliveEnemies = battle.getAliveEnemies();
        if (aliveEnemies.isEmpty()) {
            return;
        }
        Enemy target = aliveEnemies.get(ThreadLocalRandom.current().nextInt(aliveEnemies.size()));
        donutCharmTargets.put(battle, target);
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                actor.getName(),
                "Royal Charm",
                target.getName(),
                0,
                "Royal Charm protects Donut from " + target.getName() + " for one turn."
        ));
    }

    private Optional<Attack> chooseFirstReadyAttack(Battle battle, Battler actor) {
        Optional<Attack> goreGoreRitualAttack = chooseGoreGoreRitualAttack(battle, actor);
        if (goreGoreRitualAttack.isPresent()) {
            return goreGoreRitualAttack;
        }
        Optional<Attack> heatherFinalPhaseAttack = chooseHeatherFinalPhaseAttack(actor);
        if (heatherFinalPhaseAttack.isPresent()) {
            return heatherFinalPhaseAttack;
        }
        return actor.getAttacks().stream()
                .filter(attack -> isUnlockedForActor(actor, attack))
                .filter(attack -> isUsableForCurrentBattleState(battle, actor, attack))
                .filter(attack -> isReady(actor, attack))
                .findFirst();
    }

    private boolean isUsableForCurrentBattleState(Battle battle, Battler actor, Attack attack) {
        if (actor.getName().equals("Hoarder") && attack.getName().equals("Devour")) {
            return countAliveEnemiesByName(battle, "Scatterer") > 0;
        }
        if (actor.getName().equals("Gore-Gore") && attack.getName().equals("Summon Gruul")) {
            return gruulCountdowns.getOrDefault(battle, 0) <= 0;
        }
        return true;
    }

    private Optional<Attack> chooseGoreGoreRitualAttack(Battle battle, Battler actor) {
        if (!actor.getName().equals("Gore-Gore") || gruulCountdowns.getOrDefault(battle, 0) <= 0) {
            return Optional.empty();
        }
        return findReadyAttackByName(actor, "Meat Hook")
                .or(() -> findReadyAttackByName(actor, "Slash"));
    }

    private Optional<Attack> chooseHeatherFinalPhaseAttack(Battler actor) {
        if (!(actor instanceof BossEnemy bossEnemy)
                || !bossEnemy.getName().equals("Heather")
                || bossEnemy.getState() != BossEnemyState.FINAL_PHASE) {
            return Optional.empty();
        }
        return findReadyAttackByName(actor, "Hibernate")
                .or(() -> findReadyAttackByName(actor, "Roller Skate Charge"))
                .or(() -> findReadyAttackByName(actor, "Bear Maul"));
    }

    private Optional<Attack> findReadyAttackByName(Battler actor, String attackName) {
        return actor.getAttacks().stream()
                .filter(attack -> attack.getName().equals(attackName))
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
        if (enemy instanceof BossEnemy bossEnemy) {
            new BossEnemyStateMachine(bossEnemy).update();
        } else if (enemy instanceof TrashEnemy trashEnemy) {
            new TrashEnemyStateMachine(trashEnemy).update();
        }
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
        Battler source = partyShieldSources.get(battle);
        return source != null && source.isAlive();
    }

    private void expirePartyShieldCreatedBy(Battle battle, Battler actor) {
        if (partyShieldSources.get(battle) == actor) {
            partyShieldSources.remove(battle);
        }
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
        if (actorIsPlayer && actor.getName().equals("Carl") && actor.getCurrentHp() * 100 < actor.getMaxHp() * 30) {
            modifiedDamage += modifiedDamage * 20 / 100;
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
        if (!actorIsPlayer && actor instanceof TrashEnemy trashEnemy
                && trashEnemy.getName().equals("Scatterer")
                && trashEnemy.getState() == TrashEnemyState.AGGRESSIVE
                && attack.getName().equals("Bug Bite")) {
            modifiedDamage += modifiedDamage / 2;
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
        return applyRoyalCharmDamageBlock(battle, actor, target, Math.max(0, modifiedDamage), actorIsPlayer);
    }

    private int applyRoyalCharmDamageBlock(Battle battle, Battler actor, Targetable target, int damage,
                                           boolean actorIsPlayer) {
        if (actorIsPlayer || damage <= 0 || !(target instanceof PlayerCharacter playerCharacter)) {
            return damage;
        }
        Enemy charmedEnemy = donutCharmTargets.get(battle);
        if (charmedEnemy != actor || !playerCharacter.getName().equals("Donut")) {
            return damage;
        }
        battle.addTurn(new BattleTurn(
                battle.getTurnNumber(),
                "Donut",
                "Royal Charm",
                actor.getName(),
                0,
                "Royal Charm blocks " + actor.getName() + "'s damage to Donut."
        ));
        return 0;
    }

    private void expireRoyalCharmAfterEnemyTurn(Battle battle, Enemy actor) {
        Enemy charmedEnemy = donutCharmTargets.get(battle);
        if (charmedEnemy == actor) {
            donutCharmTargets.remove(battle);
        }
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
                .stream()
                .flatMap(List::stream)
                .anyMatch(effect -> effect.name().equals(statusName));
    }

    private Optional<Enemy> spawnEnemyByName(Battle battle, String name, int maxAlive) {
        long aliveCount = countAliveEnemiesByName(battle, name);
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

    private long countAliveEnemiesByName(Battle battle, String name) {
        return battle.getAliveEnemies().stream()
                .filter(enemy -> enemy.getName().equals(name))
                .count();
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

    private record DamageOutcome(int totalAmount, List<Enemy> damagedEnemies, List<? extends Targetable> targets,
                                 Map<Targetable, Integer> damageByTarget) {
        private int damageFor(Targetable target) {
            return damageByTarget.getOrDefault(target, 0);
        }
    }

    private record StatusEffectState(String name, int baseAmount, int remainingTicks,
                                     boolean healing, boolean lethal, boolean negative) {
        private static StatusEffectState damage(String name, int baseDamage, int remainingTicks) {
            return new StatusEffectState(name, baseDamage, remainingTicks, false, false, true);
        }

        private static StatusEffectState healing(String name, int baseHealing, int remainingTicks) {
            return new StatusEffectState(name, baseHealing, remainingTicks, true, false, false);
        }

        private static StatusEffectState lethalInfection() {
            return new StatusEffectState("Lethal Infection", 0, 1, false, true, true);
        }

        private static StatusEffectState stun() {
            return new StatusEffectState("Stun", 0, 1, false, false, true);
        }

        private StatusEffectState tick() {
            return new StatusEffectState(name, baseAmount, remainingTicks - 1, healing, lethal, negative);
        }
    }
}
