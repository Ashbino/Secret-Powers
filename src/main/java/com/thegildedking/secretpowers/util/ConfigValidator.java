package com.thegildedking.secretpowers.util;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigValidator {

    private static final Object[][] MIN_RULES = {
            {"event.preparation-minutes", 1.0},
            {"event.final-countdown-seconds", 1.0},
            {"activation.feedback-throttle-seconds", 0.0},
            {"teams.max-size", 2.0},
            {"teams.invite-expire-seconds", 5.0},
            {"powers.phantom.cooldown", 0.0}, {"powers.phantom.max-range", 1.0},
            {"powers.juggernaut.cooldown", 0.0}, {"powers.juggernaut.duration", 1.0},
            {"powers.stormcaller.cooldown", 0.0}, {"powers.stormcaller.range", 3.0},
            {"powers.ghost.cooldown", 0.0}, {"powers.ghost.duration", 1.0},
            {"powers.lifebinder.cooldown", 0.0}, {"powers.lifebinder.heal-amount", 0.5},
            {"powers.lifebinder.range", 1.0},
            {"powers.skybound.cooldown", 0.0}, {"powers.skybound.upward-velocity", 0.1},
            {"powers.skybound.max-velocity", 0.1},
            {"powers.soulthief.cooldown", 0.0}, {"powers.soulthief.max-levels-stolen", 1.0},
            {"powers.frostbite.cooldown", 0.0}, {"powers.frostbite.duration", 1.0},
            {"powers.frostbite.range", 1.0},
            {"powers.inferno.cooldown", 0.0}, {"powers.inferno.duration", 1.0},
            {"powers.inferno.aura-radius", 1.0},
            {"powers.forcewave.cooldown", 0.0}, {"powers.forcewave.range", 1.0},
            {"powers.forcewave.strength", 0.1}, {"powers.forcewave.max-velocity", 0.1},
    };

    private ConfigValidator() {}

    public static void validate(SecretPowersPlugin plugin) {
        FileConfiguration c = plugin.getConfig();

        for (Object[] rule : MIN_RULES) {
            fixMin(plugin, (String) rule[0], (Double) rule[1]);
        }

        int slot = c.getInt("activation.preferred-slot", 8);
        if (slot < 0 || slot > 8) {
            plugin.getLogger().warning("activation.preferred-slot must be 0-8. Using 8.");
            c.set("activation.preferred-slot", 8);
        }

        String mat = c.getString("activation.item-material", "NETHER_STAR");
        Material m = Material.matchMaterial(mat == null ? "" : mat);
        if (m == null || !m.isItem()) {
            plugin.getLogger().warning("activation.item-material '" + mat + "' is invalid. Using NETHER_STAR.");
            c.set("activation.item-material", "NETHER_STAR");
        }

        int prepSeconds = c.getInt("event.preparation-minutes", 20) * 60;
        if (c.getInt("event.final-countdown-seconds", 10) >= prepSeconds) {
            int fixed = Math.max(1, Math.min(10, prepSeconds - 1));
            plugin.getLogger().warning("event.final-countdown-seconds must be shorter than the preparation phase. Using " + fixed + ".");
            c.set("event.final-countdown-seconds", fixed);
        }
    }

    private static void fixMin(SecretPowersPlugin plugin, String path, double min) {
        FileConfiguration c = plugin.getConfig();
        if (!c.isSet(path)) return;
        boolean number = c.isInt(path) || c.isDouble(path) || c.isLong(path);
        if (!number || c.getDouble(path) < min) {
            Object def = c.getDefaults() == null ? null : c.getDefaults().get(path);
            plugin.getLogger().warning("Invalid value for '" + path + "' (must be a number >= " + min
                    + "). Using default: " + def);
            c.set(path, def);
        }
    }
}
