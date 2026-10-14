package com.thegildedking.secretpowers.phase;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.util.Messages;
import com.thegildedking.secretpowers.util.YamlStore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

public final class PhaseManager {

    private final SecretPowersPlugin plugin;
    private final File file;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1.0f,
            BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    private Phase phase = Phase.IDLE;
    private int remaining;
    private int total = 1;
    private BukkitTask task;
    private int saveCounter;

    public PhaseManager(SecretPowersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
    }

    // ------------------------------------------------------------ persistence

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        try {
            phase = Phase.valueOf(y.getString("phase", "IDLE"));
        } catch (IllegalArgumentException ex) {
            phase = Phase.IDLE;
        }
        remaining = Math.max(0, y.getInt("remaining", 0));
        total = Math.max(1, y.getInt("total", 1));

        if (isCountingDown()) {
            plugin.getLogger().info("Resuming " + phase.display() + " with " + remaining + "s left.");
            startTask();
            updateBar();
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("phase", phase.name());
        y.set("remaining", remaining);
        y.set("total", total);
        YamlStore.save(y, file, plugin.getLogger());
    }

    public void shutdown() {
        stopTask();
        save();
        hideBarFromAll();
    }

    // ------------------------------------------------------------ queries

    public Phase getPhase() {
        return phase;
    }

    public int getRemaining() {
        return remaining;
    }

    public boolean isCountingDown() {
        return phase == Phase.PREPARATION || phase == Phase.FINAL_COUNTDOWN;
    }

    public boolean powersUsable() {
        if (phase == Phase.ACTIVE) return true;
        return phase == Phase.PREPARATION
                && plugin.getConfig().getBoolean("preparation.allow-power-testing", false);
    }

    public boolean isPvpAllowed() {
        if (isCountingDown()) {
            return plugin.getConfig().getBoolean("preparation.pvp-enabled", false);
        }
        return true;
    }

    public String describeStatus() {
        if (isCountingDown()) {
            return phase.display() + " - " + Messages.formatTime(remaining) + " remaining";
        }
        return phase.display();
    }

    // ------------------------------------------------------------ transitions

    /** Returns an error message, or null on success. */
    public String start() {
        if (phase != Phase.IDLE) {
            return "An event is already running (" + phase.display() + "). Use /sp admin stop first.";
        }
        beginPreparation();
        return null;
    }

    public void stop() {
        stopTask();
        phase = Phase.IDLE;
        remaining = 0;
        hideBarFromAll();
        plugin.powers().cleanupAll();
        save();
        if (announce()) Messages.broadcast("<yellow>The event has been stopped by an administrator.");
    }

    /** Returns an error message, or null on success. */
    public String setPhase(Phase target) {
        if (target == null) return "Unknown phase. Use idle, preparation, final or active.";
        switch (target) {
            case IDLE -> {
                if (phase == Phase.IDLE) return "Already idle.";
                stop();
            }
            case PREPARATION -> {
                stopTask();
                beginPreparation();
            }
            case FINAL_COUNTDOWN -> enterFinal();
            case ACTIVE -> activate();
        }
        return null;
    }

    private void beginPreparation() {
        int minutes = Math.max(1, plugin.getConfig().getInt("event.preparation-minutes", 20));
        total = minutes * 60;
        remaining = total;
        phase = Phase.PREPARATION;

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.powers().ensureAssigned(p, false);
        }
        showBarToAll();
        updateBar();
        startTask();
        save();

        if (announce()) {
            Messages.broadcast("<green>Preparation has begun!</green> <gray>The SMP starts in <white>"
                    + minutes + " minute(s)</white>. Every player has received a secret power.");
            Messages.titleAll("<gold>Preparation Phase", "<gray>Check your power with /sp power");
        }
    }

    private void enterFinal() {
        int fs = finalSeconds();
        phase = Phase.FINAL_COUNTDOWN;
        remaining = fs;
        total = fs;

        // Validate that everyone has a saved power, and remind them privately.
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.powers().ensureAssigned(p, true);
        }
        showBarToAll();
        updateBar();
        startTask();
        save();

        if (announce()) {
            Messages.broadcast("<red><bold>The SMP is about to begin!</bold></red> <gray>Get ready.");
            Messages.titleAll("<red>Get ready!", "<gray>The SMP begins in " + fs + " seconds");
        }
    }

    private void activate() {
        stopTask();
        phase = Phase.ACTIVE;
        remaining = 0;
        hideBarFromAll();

        plugin.powers().resetAllCooldowns();
        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.powers().ensureAssigned(p, false);
        }
        save();

        if (announce()) {
            Messages.broadcast("<green><bold>The SMP has officially begun!</bold></green> <gray>PvP and powers are now enabled.");
            Messages.titleAll("<green>The SMP has begun!", "<gray>Good luck. Trust no one.");
        }
    }

    // ------------------------------------------------------------ countdown

    private void tick() {
        if (!isCountingDown()) {
            stopTask();
            return;
        }
        remaining--;

        if (phase == Phase.PREPARATION) {
            if (remaining <= finalSeconds()) {
                enterFinal();
                return;
            }
            if (remaining % 60 == 0) {
                int minutes = remaining / 60;
                List<Integer> marks = plugin.getConfig().getIntegerList("event.announce-at-minutes");
                if (marks.contains(minutes) && announce()) {
                    Messages.broadcast("<yellow>" + minutes + " minute(s)</yellow> <gray>until the SMP begins.");
                }
            }
        } else {
            if (remaining <= 0) {
                activate();
                return;
            }
            List<Integer> marks = plugin.getConfig().getIntegerList("event.final-announce-at-seconds");
            if (marks.contains(remaining) && announce()) {
                Messages.broadcast("<red>The SMP begins in " + remaining + "...");
                Messages.titleAll("<gold>" + remaining, "<gray>The SMP is about to begin");
            }
        }

        updateBar();
        if (++saveCounter % 10 == 0) save();
    }

    private void startTask() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private int finalSeconds() {
        return Math.max(1, plugin.getConfig().getInt("event.final-countdown-seconds", 10));
    }

    private boolean announce() {
        return plugin.getConfig().getBoolean("announcements.broadcast-phase-changes", true);
    }

    // ------------------------------------------------------------ boss bar

    private void updateBar() {
        if (phase == Phase.PREPARATION) {
            bar.color(BossBar.Color.YELLOW);
            bar.name(Messages.parse("<gold>Preparation</gold> <gray>- SMP begins in</gray> <white>"
                    + Messages.formatTime(remaining)));
        } else if (phase == Phase.FINAL_COUNTDOWN) {
            bar.color(BossBar.Color.RED);
            bar.name(Messages.parse("<red><bold>FINAL COUNTDOWN</bold></red> <gray>-</gray> <white>"
                    + Messages.formatTime(remaining)));
        }
        bar.progress(Math.max(0f, Math.min(1f, remaining / (float) Math.max(1, total))));
    }

    private void showBarToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bar);
    }

    private void hideBarFromAll() {
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bar);
    }

    public void onJoin(Player p) {
        if (isCountingDown()) p.showBossBar(bar);
    }
}
