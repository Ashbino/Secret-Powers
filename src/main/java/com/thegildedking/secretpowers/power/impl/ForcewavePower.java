package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ForcewavePower extends Power {

    public ForcewavePower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.FORCEWAVE);
    }

    @Override
    public boolean activate(Player player) {
        double range = clamp(dbl("range", 5.0), 1.0, 32.0);
        double strength = clamp(dbl("strength", 1.2), 0.1, 5.0);
        double maxVelocity = clamp(dbl("max-velocity", 2.0), 0.1, 4.0);
        boolean hitTeammates = bool("affect-teammates", false);

        Location center = player.getLocation();
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player) || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (!hitTeammates && plugin.teams().areTeammates(player.getUniqueId(), other.getUniqueId())) continue;

            Vector push = other.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() > range * range) continue;

            push.setY(0);
            if (push.lengthSquared() < 1.0E-4) {
                push = center.getDirection().setY(0);
                if (push.lengthSquared() < 1.0E-4) push = new Vector(1, 0, 0);
            }
            push.normalize().multiply(strength);
            push.setY(0.35);
            if (push.length() > maxVelocity) {
                push.normalize().multiply(maxVelocity);
            }
            other.setVelocity(push);
        }

        center.getWorld().spawnParticle(Particle.CLOUD, center.clone().add(0, 0.3, 0), 30, range / 3, 0.1, range / 3, 0.05);
        return true;
    }
}
