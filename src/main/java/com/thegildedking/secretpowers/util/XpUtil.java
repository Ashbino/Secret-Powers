package com.thegildedking.secretpowers.util;

import org.bukkit.entity.Player;

public final class XpUtil {

    private XpUtil() {}

    public static int totalAtLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) Math.round(2.5 * level * level - 40.5 * level + 360);
        return (int) Math.round(4.5 * level * level - 162.5 * level + 2220);
    }

    public static int xpToNext(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    public static int total(Player p) {
        int level = p.getLevel();
        return totalAtLevel(level) + Math.round(p.getExp() * xpToNext(level));
    }

    public static void setTotal(Player p, int xp) {
        p.setLevel(0);
        p.setExp(0f);
        if (xp > 0) p.giveExp(xp);
    }
}
