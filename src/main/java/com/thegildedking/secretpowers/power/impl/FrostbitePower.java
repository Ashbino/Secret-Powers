package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.util.Messages;
import com.thegildedking.secretpowers.util.TargetUtil;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FrostbitePower extends Power {

    /** Victim -> time (ms) when the current slow ends. Prevents re-applying to extend/bypass it. */
    private final Map<UUID, Long> slowedUntil = new HashMap<>();

    public FrostbitePower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.FROSTBITE);
    }

    @Override
    public boolean activate(Player player) {
        double range = clamp(dbl("range", 8.0), 1.0, 32.0);
        int ticks = clamp(integer("duration", 3), 1, 30) * 20;
        int amplifier = clamp(integer("slowness-amplifier", 2), 0, 5);

        Player target = TargetUtil.lookedAtPlayer(player, range);
        if (target == null) return fail(player, "No target in range.");
        if (plugin.teams().areTeammates(player.getUniqueId(), target.getUniqueId())) {
            return fail(player, "That is a teammate.");
        }

        long now = System.currentTimeMillis();
        slowedUntil.values().removeIf(end -> end <= now);
        if (slowedUntil.containsKey(target.getUniqueId())) {
            return fail(player, "Target is already slowed.");
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amplifier, false, false, true));
        slowedUntil.put(target.getUniqueId(), now + ticks * 50L);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        Messages.actionBar(target, "<aqua>You feel yourself slowing down...");
        return true;
    }

    @Override
    public void cleanup(UUID id) {
        slowedUntil.remove(id);
    }

    @Override
    public void cleanupAll() {
        slowedUntil.clear();
    }
}
