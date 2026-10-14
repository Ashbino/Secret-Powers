package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class JuggernautPower extends Power {

    public JuggernautPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.JUGGERNAUT);
    }

    @Override
    public boolean activate(Player player) {
        int ticks = clamp(integer("duration", 5), 1, 120) * 20;
        int resistance = clamp(integer("resistance-amplifier", 1), 0, 4);
        int slowness = clamp(integer("slowness-amplifier", 1), 0, 4);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, ticks, resistance, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, slowness, false, false, true));
        return true;
    }
}
