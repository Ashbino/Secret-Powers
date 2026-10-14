package com.thegildedking.secretpowers.power;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.util.Messages;
import org.bukkit.entity.Player;

import java.util.UUID;

public abstract class Power {

    protected final SecretPowersPlugin plugin;
    private final PowerType type;

    protected Power(SecretPowersPlugin plugin, PowerType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public PowerType type() {
        return type;
    }

    /** Try to use the power. Return true only if it really happened (starts the cooldown). */
    public abstract boolean activate(Player player);

    /** Remove temporary effects/tasks for one player (death, quit, reroll). */
    public void cleanup(UUID id) {}

    /** Remove everything (event stop, shutdown). */
    public void cleanupAll() {}

    public boolean isPassive() {
        return type.passive();
    }

    public boolean enabled() {
        return bool("enabled", true);
    }

    public int cooldownSeconds() {
        return Math.max(0, integer("cooldown", 30));
    }

    protected double dbl(String key, double def) {
        return plugin.getConfig().getDouble("powers." + type.configKey() + "." + key, def);
    }

    protected int integer(String key, int def) {
        return plugin.getConfig().getInt("powers." + type.configKey() + "." + key, def);
    }

    protected boolean bool(String key, boolean def) {
        return plugin.getConfig().getBoolean("powers." + type.configKey() + "." + key, def);
    }

    protected static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    protected static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Private failure message. Does not consume the cooldown. */
    protected boolean fail(Player player, String message) {
        Messages.actionBar(player, "<gray>" + message);
        return false;
    }
}
