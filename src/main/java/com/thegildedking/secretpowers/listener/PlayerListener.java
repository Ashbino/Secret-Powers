package com.thegildedking.secretpowers.listener;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.phase.Phase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public final class PlayerListener implements Listener {

    private final SecretPowersPlugin plugin;

    public PlayerListener(SecretPowersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.phases().onJoin(player);

        // Run next tick so the player is fully in the world.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (plugin.phases().getPhase() == Phase.IDLE) return;

            UUID id = player.getUniqueId();
            // Existing powers are never rerolled; only players with NO power get one.
            if (!plugin.powers().hasPower(id)
                    && !plugin.getConfig().getBoolean("assignment.assign-late-joiners", true)) {
                return;
            }
            plugin.powers().ensureAssigned(player, false);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.powers().cleanupPlayer(event.getPlayer().getUniqueId());
        plugin.saveAll();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // The core never drops; it is handed back on respawn.
        event.getDrops().removeIf(plugin.activationItem()::isCore);
        plugin.powers().cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (plugin.phases().getPhase() == Phase.IDLE) return;
            if (plugin.powers().hasPower(player.getUniqueId())) {
                plugin.activationItem().ensure(player);
            }
        });
    }
}
