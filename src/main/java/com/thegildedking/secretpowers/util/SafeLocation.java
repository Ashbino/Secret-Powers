package com.thegildedking.secretpowers.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.EnumSet;
import java.util.Set;

public final class SafeLocation {

    private static final Set<Material> DANGEROUS = EnumSet.of(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK,
            Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.POWDER_SNOW, Material.WITHER_ROSE,
            Material.POINTED_DRIPSTONE);

    private SafeLocation() {}

    /** Snaps a location to the centre of its block (feet position). */
    public static Location snap(Location loc) {
        return new Location(loc.getWorld(),
                Math.floor(loc.getX()) + 0.5, Math.floor(loc.getY()), Math.floor(loc.getZ()) + 0.5,
                loc.getYaw(), loc.getPitch());
    }

    public static boolean isSafe(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;
        if (!w.getWorldBorder().isInside(loc)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        if (!w.isChunkLoaded(x >> 4, z >> 4)) return false;
        if (y <= w.getMinHeight() || y >= w.getMaxHeight() - 2) return false;

        Block feet = w.getBlockAt(x, y, z);
        Block head = feet.getRelative(BlockFace.UP);
        if (!isFree(feet) || !isFree(head)) return false;

        for (int i = 1; i <= 5; i++) {
            Block below = feet.getRelative(BlockFace.DOWN, i);
            if (DANGEROUS.contains(below.getType())) return false;
            if (!below.isPassable() || below.isLiquid()) return true;
        }
        return false; // too long a fall
    }

    private static boolean isFree(Block b) {
        return b.isPassable() && !DANGEROUS.contains(b.getType());
    }
}
