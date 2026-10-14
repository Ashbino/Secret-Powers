package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public final class StormcallerPower extends Power {

    public StormcallerPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.STORMCALLER);
    }

    @Override
    public boolean activate(Player player) {
        double range = clamp(dbl("range", 15.0), 3.0, 64.0);
        RayTraceResult hit = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);
        if (hit == null) return fail(player, "No target in range.");

        World world = player.getWorld();
        Location target = hit.getHitPosition().toLocation(world);

        if (!world.getWorldBorder().isInside(target)) return fail(player, "Target is outside the world border.");
        if (target.distanceSquared(player.getLocation()) < 9.0) return fail(player, "Target is too close.");

        boolean damage = bool("deal-damage", true);
        boolean fire = bool("set-fire", false);

        if (damage) {
            world.strikeLightning(target);
            if (!fire) {
                // Remove any fire created by the strike on the next tick.
                Bukkit.getScheduler().runTask(plugin, () -> extinguish(target));
            }
        } else {
            world.strikeLightningEffect(target);
            if (fire) {
                Block above = target.getBlock();
                if (above.getType().isAir() && above.getRelative(0, -1, 0).getType().isSolid()) {
                    above.setType(Material.FIRE);
                }
            }
        }
        return true;
    }

    private void extinguish(Location center) {
        World world = center.getWorld();
        if (world == null) return;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() == Material.FIRE) b.setType(Material.AIR);
                }
            }
        }
    }
}
