package com.thegildedking.secretpowers.power;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.phase.Phase;
import com.thegildedking.secretpowers.power.impl.ForcewavePower;
import com.thegildedking.secretpowers.power.impl.FrostbitePower;
import com.thegildedking.secretpowers.power.impl.GhostPower;
import com.thegildedking.secretpowers.power.impl.InfernoPower;
import com.thegildedking.secretpowers.power.impl.JuggernautPower;
import com.thegildedking.secretpowers.power.impl.LifebinderPower;
import com.thegildedking.secretpowers.power.impl.PhantomPower;
import com.thegildedking.secretpowers.power.impl.SkyboundPower;
import com.thegildedking.secretpowers.power.impl.SoulThiefPower;
import com.thegildedking.secretpowers.power.impl.StormcallerPower;
import com.thegildedking.secretpowers.util.Messages;
import com.thegildedking.secretpowers.util.YamlStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PowerManager {

    private final SecretPowersPlugin plugin;
    private final File file;
    private final Map<PowerType, Power> powers = new EnumMap<>(PowerType.class);
    private final Map<UUID, PowerType> assignments = new HashMap<>();
    private final Map<UUID, Long> cooldownEnd = new HashMap<>();
    private final Map<UUID, Long> lastFeedback = new HashMap<>();
    private boolean dirty;

    public PowerManager(SecretPowersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");

        register(new PhantomPower(plugin));
        register(new JuggernautPower(plugin));
        register(new StormcallerPower(plugin));
        register(new GhostPower(plugin));
        register(new LifebinderPower(plugin));
        register(new SkyboundPower(plugin));
        register(new SoulThiefPower(plugin));
        register(new FrostbitePower(plugin));
        register(new InfernoPower(plugin));
        register(new ForcewavePower(plugin));
    }

    private void register(Power power) {
        powers.put(power.type(), power);
    }

    public Power get(PowerType type) {
        return powers.get(type);
    }

    // ------------------------------------------------------------ persistence

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection ps = y.getConfigurationSection("powers");
        if (ps != null) {
            for (String key : ps.getKeys(false)) {
                try {
                    assignments.put(UUID.fromString(key), PowerType.valueOf(ps.getString(key, "")));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignoring invalid entry in players.yml: " + key);
                }
            }
        }

        long now = System.currentTimeMillis();
        ConfigurationSection cs = y.getConfigurationSection("cooldowns");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                try {
                    long end = cs.getLong(key);
                    if (end > now) cooldownEnd.put(UUID.fromString(key), end);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignoring invalid cooldown entry in players.yml: " + key);
                }
            }
        }
        plugin.getLogger().info("Loaded " + assignments.size() + " saved player power(s).");
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        assignments.forEach((id, type) -> y.set("powers." + id, type.name()));
        long now = System.currentTimeMillis();
        cooldownEnd.forEach((id, end) -> {
            if (end > now) y.set("cooldowns." + id, end);
        });
        YamlStore.save(y, file, plugin.getLogger());
        dirty = false;
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    public void shutdown() {
        cleanupAll();
        save();
    }

    // ------------------------------------------------------------ assignment

    public PowerType getAssigned(UUID id) {
        return assignments.get(id);
    }

    public boolean hasPower(UUID id) {
        return assignments.containsKey(id);
    }

    /**
     * Gives the player a power if they have none (never rerolls an existing one),
     * makes sure they hold their Power Core while an event is running,
     * and privately tells them about the power when it is new (or when reveal is forced).
     */
    public void ensureAssigned(Player player, boolean revealExisting) {
        UUID id = player.getUniqueId();
        boolean fresh = false;

        if (!assignments.containsKey(id)) {
            PowerType chosen = choose(id, null);
            if (chosen == null) {
                plugin.getLogger().warning("No powers are enabled in config.yml; cannot assign a power.");
                return;
            }
            assignments.put(id, chosen);
            dirty = true;
            fresh = true;
            plugin.debug("Assigned a power to a player.");
        }

        if (plugin.phases().getPhase() != Phase.IDLE) {
            plugin.activationItem().ensure(player);
        }
        if (fresh || revealExisting) reveal(player);
        saveIfDirty();
    }

    /** Admin: force a specific power. */
    public void set(UUID id, PowerType type) {
        cleanupPlayer(id);
        assignments.put(id, type);
        cooldownEnd.remove(id);
        dirty = true;
        saveIfDirty();
    }

    /** Admin: new random power for one player. */
    public boolean reroll(Player player) {
        UUID id = player.getUniqueId();
        cleanupPlayer(id);
        PowerType old = assignments.remove(id);
        cooldownEnd.remove(id);
        PowerType chosen = choose(id, old);
        if (chosen == null) {
            dirty = true;
            return false;
        }
        assignments.put(id, chosen);
        dirty = true;
        ensureAssigned(player, true);
        return true;
    }

    /** Admin: wipe everyone's power and re-assign online players. */
    public void rerollAll() {
        cleanupAll();
        assignments.clear();
        cooldownEnd.clear();
        dirty = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ensureAssigned(p, true);
        }
        saveIfDirty();
    }

    private PowerType choose(UUID forPlayer, PowerType avoid) {
        List<PowerType> pool = new ArrayList<>();
        for (Power p : powers.values()) {
            if (p.enabled()) pool.add(p.type());
        }
        if (pool.isEmpty()) return null;

        if (!plugin.getConfig().getBoolean("assignment.allow-duplicates", false)) {
            Set<PowerType> used = new HashSet<>();
            for (Map.Entry<UUID, PowerType> e : assignments.entrySet()) {
                if (!e.getKey().equals(forPlayer)) used.add(e.getValue());
            }
            List<PowerType> free = new ArrayList<>();
            for (PowerType t : pool) {
                if (!used.contains(t)) free.add(t);
            }
            if (!free.isEmpty()) pool = free; // otherwise: more players than powers, allow repeats
        }
        if (avoid != null && pool.size() > 1) pool.remove(avoid);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    // ------------------------------------------------------------ private info

    public void reveal(Player player) {
        PowerType type = assignments.get(player.getUniqueId());
        if (type == null) return;
        Power power = powers.get(type);

        Messages.raw(player, "<gold><bold>--- Your Secret Power ---");
        Messages.raw(player, "<yellow>Power: <white>" + type.displayName());
        Messages.raw(player, "<yellow>What it does: <gray>" + type.description());
        Messages.raw(player, "<yellow>How to use: <gray>" + type.activationText());
        Messages.raw(player, type.passive()
                ? "<yellow>Cooldown: <gray>" + power.cooldownSeconds() + "s between steals"
                : "<yellow>Cooldown: <gray>" + power.cooldownSeconds() + "s");
        if (!plugin.phases().powersUsable()) {
            Messages.raw(player, "<yellow>Status: <gray>Your power unlocks when the SMP begins.");
        }
        Messages.raw(player, "<red>Keep it secret. Only your teammates may know.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.3f);
    }

    // ------------------------------------------------------------ activation

    public void tryActivate(Player player) {
        UUID id = player.getUniqueId();

        if (!plugin.phases().powersUsable()) {
            feedback(player, "<gray>Your power is dormant for now.");
            return;
        }
        if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) return;

        PowerType type = assignments.get(id);
        if (type == null) {
            feedback(player, "<gray>You do not have a power.");
            return;
        }
        Power power = powers.get(type);
        if (!power.enabled()) {
            feedback(player, "<gray>Your power has been disabled by an administrator.");
            return;
        }
        if (power.isPassive()) {
            feedback(player, "<gray>Your power is passive. Nothing to activate.");
            return;
        }
        long left = remainingCooldown(id);
        if (left > 0) {
            feedback(player, "<gray>Power ready in <white>" + left + "s");
            return;
        }

        boolean used;
        try {
            used = power.activate(player);
        } catch (Exception ex) {
            plugin.getLogger().severe("A power failed to activate: " + ex);
            ex.printStackTrace();
            used = false;
        }
        if (used) startCooldown(id, power.cooldownSeconds());
    }

    // ------------------------------------------------------------ cooldowns

    public long remainingCooldown(UUID id) {
        Long end = cooldownEnd.get(id);
        if (end == null) return 0;
        long ms = end - System.currentTimeMillis();
        if (ms <= 0) {
            cooldownEnd.remove(id);
            return 0;
        }
        return (long) Math.ceil(ms / 1000.0);
    }

    public void startCooldown(UUID id, int seconds) {
        if (seconds <= 0) return;
        cooldownEnd.put(id, System.currentTimeMillis() + seconds * 1000L);
        dirty = true;
    }

    public void resetAllCooldowns() {
        cooldownEnd.clear();
        dirty = true;
    }

    private void feedback(Player player, String mini) {
        long now = System.currentTimeMillis();
        long gap = plugin.getConfig().getInt("activation.feedback-throttle-seconds", 2) * 1000L;
        Long last = lastFeedback.get(player.getUniqueId());
        if (last != null && now - last < gap) return;
        lastFeedback.put(player.getUniqueId(), now);

        if ("CHAT".equalsIgnoreCase(plugin.getConfig().getString("activation.cooldown-feedback", "ACTIONBAR"))) {
            Messages.raw(player, mini);
        } else {
            Messages.actionBar(player, mini);
        }
    }

    // ------------------------------------------------------------ cleanup

    public void cleanupPlayer(UUID id) {
        for (Power p : powers.values()) p.cleanup(id);
        lastFeedback.remove(id);
    }

    public void cleanupAll() {
        for (Power p : powers.values()) p.cleanupAll();
        lastFeedback.clear();
    }
}
