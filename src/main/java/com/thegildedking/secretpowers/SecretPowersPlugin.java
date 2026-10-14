package com.thegildedking.secretpowers;

import com.thegildedking.secretpowers.command.SpCommand;
import com.thegildedking.secretpowers.listener.ActivationListener;
import com.thegildedking.secretpowers.listener.CombatListener;
import com.thegildedking.secretpowers.listener.ItemProtectionListener;
import com.thegildedking.secretpowers.listener.PlayerListener;
import com.thegildedking.secretpowers.phase.PhaseManager;
import com.thegildedking.secretpowers.power.ActivationItem;
import com.thegildedking.secretpowers.power.PowerManager;
import com.thegildedking.secretpowers.team.TeamManager;
import com.thegildedking.secretpowers.util.ConfigValidator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SecretPowersPlugin extends JavaPlugin {

    private ActivationItem activationItem;
    private TeamManager teamManager;
    private PowerManager powerManager;
    private PhaseManager phaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigValidator.validate(this);

        activationItem = new ActivationItem(this);
        teamManager = new TeamManager(this);
        powerManager = new PowerManager(this);
        phaseManager = new PhaseManager(this);

        teamManager.load();
        powerManager.load();
        phaseManager.load();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ActivationListener(this), this);
        pm.registerEvents(new ItemProtectionListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);

        SpCommand command = new SpCommand(this);
        PluginCommand pc = getCommand("sp");
        if (pc == null) {
            getLogger().severe("Command 'sp' missing from plugin.yml!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pc.setExecutor(command);
        pc.setTabCompleter(command);

        // Save changed data every 30 seconds.
        getServer().getScheduler().runTaskTimer(this, this::saveAll, 600L, 600L);
        getLogger().info("SecretPowers enabled.");
    }

    @Override
    public void onDisable() {
        if (phaseManager != null) phaseManager.shutdown();
        if (powerManager != null) powerManager.shutdown();
        if (teamManager != null) teamManager.save();
    }

    public void saveAll() {
        powerManager.saveIfDirty();
        teamManager.saveIfDirty();
    }

    public void reloadAll() {
        reloadConfig();
        ConfigValidator.validate(this);
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info("[debug] " + message);
        }
    }

    public ActivationItem activationItem() { return activationItem; }
    public TeamManager teams() { return teamManager; }
    public PowerManager powers() { return powerManager; }
    public PhaseManager phases() { return phaseManager; }
}
