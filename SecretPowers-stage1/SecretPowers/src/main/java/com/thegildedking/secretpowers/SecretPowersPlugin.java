package com.thegildedking.secretpowers;

import org.bukkit.plugin.java.JavaPlugin;

public final class SecretPowersPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Creates plugins/SecretPowers/config.yml on first run.
        saveDefaultConfig();
        getLogger().info("SecretPowers enabled (Stage 1 skeleton).");
    }

    @Override
    public void onDisable() {
        getLogger().info("SecretPowers disabled.");
    }
}
