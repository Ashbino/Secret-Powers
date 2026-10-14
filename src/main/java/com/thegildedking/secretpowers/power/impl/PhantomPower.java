package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.util.SafeLocation;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class PhantomPower extends Power {

    public PhantomPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.PHANTOM);
    }

    @Override
    public boolean activate(Player player) {
        double max = clamp(dbl("max-range", 8.0), 1.0, 64.0);
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        double reach = max;
        RayTraceResult hit = world.rayTraceBlocks(eye, dir, max, FluidCollisionMode.NEVER, true);
        if (hit != null) {
            reach = Math.max(0, hit.getHitPosition().distance(eye.toVector()) - 0.6);
        }

        Location start = player.getLocation();
        for (double d = reach; d >= 1.0; d -= 0.5) {
            Location candidate = SafeLocation.snap(start.clone().add(dir.clone().multiply(d)));
            if (!SafeLocation.isSafe(candidate)) continue;

            world.spawnParticle(Particle.PORTAL, start.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            if (!player.teleport(candidate, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                return fail(player, "You cannot teleport right now.");
            }
            player.setFallDistance(0f);
            world.spawnParticle(Particle.PORTAL, candidate.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
            return true;
        }
        return fail(player, "No safe place to teleport to.");
    }
}
