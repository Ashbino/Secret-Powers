package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerManager;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.util.Messages;
import com.thegildedking.secretpowers.util.XpUtil;
import org.bukkit.entity.Player;

/** Passive power: triggered from CombatListener on successful melee hits. */
public final class SoulThiefPower extends Power {

    public SoulThiefPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.SOUL_THIEF);
    }

    @Override
    public boolean activate(Player player) {
        return fail(player, "Your power is passive.");
    }

    public void onHit(Player attacker, Player victim) {
        PowerManager powers = plugin.powers();
        if (!enabled()) return;
        if (powers.remainingCooldown(attacker.getUniqueId()) > 0) return;
        if (plugin.teams().areTeammates(attacker.getUniqueId(), victim.getUniqueId())) return;

        int levels = clamp(integer("max-levels-stolen", 3), 1, 30);
        int total = XpUtil.total(victim);
        if (total <= 0) return;

        int floor = XpUtil.totalAtLevel(Math.max(0, victim.getLevel() - levels));
        int steal = total - floor;
        if (steal <= 0) return;

        XpUtil.setTotal(victim, total - steal);
        attacker.giveExp(steal);
        powers.startCooldown(attacker.getUniqueId(), cooldownSeconds());
        Messages.actionBar(attacker, "<green>+" + steal + " XP");
    }
}
