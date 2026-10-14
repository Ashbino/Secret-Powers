package com.thegildedking.secretpowers.util;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class TargetUtil {

    private TargetUtil() {}

    /** The player the viewer is looking at, within range, with no wall in between. */
    public static Player lookedAtPlayer(Player viewer, double range) {
        World world = viewer.getWorld();
        Location eye = viewer.getEyeLocation();
        Vector dir = eye.getDirection();

        RayTraceResult entityHit = world.rayTraceEntities(eye, dir, range, 0.6, e ->
                e instanceof Player p
                        && !p.equals(viewer)
                        && !p.isDead()
                        && p.getGameMode() != GameMode.SPECTATOR);
        if (entityHit == null || !(entityHit.getHitEntity() instanceof Player target)) return null;

        RayTraceResult blockHit = world.rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        if (blockHit != null) {
            double blockDist = blockHit.getHitPosition().distance(eye.toVector());
            double entityDist = entityHit.getHitPosition().distance(eye.toVector());
            if (blockDist < entityDist) return null;
        }
        return target;
    }
}
