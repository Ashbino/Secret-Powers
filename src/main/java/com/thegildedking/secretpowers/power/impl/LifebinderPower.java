package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class LifebinderPower extends Power {

    public LifebinderPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.LIFEBINDER);
    }

    @Override
    public boolean activate(Player player) {
        double range = clamp(dbl("range", 6.0), 1.0, 32.0);
        double amount = clamp(dbl("heal-amount", 4.0), 0.5, 40.0);
        boolean needSight = bool("require-line-of-sight", true);

        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (!plugin.teams().areTeammates(player.getUniqueId(), other.getUniqueId())) continue;
            double dist = other.getLocation().distanceSquared(player.getLocation());
            if (dist > range * range || dist >= bestDist) continue;
            if (!needsHealing(other)) continue;
            if (needSight && !player.hasLineOfSight(other)) continue;
            best = other;
            bestDist = dist;
        }

        Player target = best;
        if (target == null) {
            if (bool("heal-self-if-no-teammate", true) && needsHealing(player)) {
                target = player;
            } else {
                return fail(player, "Nobody nearby needs healing.");
            }
        }

        heal(target, amount);
        target.getWorld().spawnParticle(Particle.HEART, target.getEyeLocation().add(0, 0.3, 0), 5, 0.3, 0.3, 0.3, 0);
        if (!target.equals(player)) {
            Messages.actionBar(target, "<green>A teammate healed you.");
        }
        return true;
    }

    private boolean needsHealing(Player p) {
        return p.getHealth() < maxHealth(p) - 0.01;
    }

    private double maxHealth(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        return attr == null ? 20.0 : attr.getValue();
    }

    private void heal(Player p, double amount) {
        p.setHealth(Math.min(maxHealth(p), p.getHealth() + amount));
    }
}
