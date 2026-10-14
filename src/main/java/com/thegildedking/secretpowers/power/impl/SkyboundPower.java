package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class SkyboundPower extends Power {

    public SkyboundPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.SKYBOUND);
    }

    @Override
    public boolean activate(Player player) {
        double cap = clamp(dbl("max-velocity", 2.0), 0.1, 4.0);
        double up = clamp(dbl("upward-velocity", 1.4), 0.1, cap);

        Vector velocity = player.getVelocity();
        velocity.setY(up);
        player.setVelocity(velocity);
        player.setFallDistance(0f);

        int slowFall = clamp(integer("slow-fall-seconds", 4), 0, 30);
        if (slowFall > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, slowFall * 20, 0, false, false, true));
        }
        return true;
    }
}
