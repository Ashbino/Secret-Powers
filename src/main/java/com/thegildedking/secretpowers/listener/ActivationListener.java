package com.thegildedking.secretpowers.listener;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Turns a right-click with the Power Core into a power activation. */
public final class ActivationListener implements Listener {

    private final SecretPowersPlugin plugin;
    private final Map<UUID, Integer> lastTick = new HashMap<>();

    public ActivationListener(SecretPowersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null) return;

        ItemStack item = event.getItem();
        if (!plugin.activationItem().isCore(item)) return;

        Player player = event.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            // Don't fire a power when the player is just opening a chest, door, etc.
            if (clicked != null && clicked.getType().isInteractable() && !player.isSneaking()) return;
            if (event.useItemInHand() == Event.Result.DENY) return;
        }

        event.setUseItemInHand(Event.Result.DENY); // the core has no vanilla use
        activate(player, item);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (!plugin.activationItem().isCore(item)) return;

        event.setCancelled(true); // never place the core into frames, trade with it, etc.
        Entity clicked = event.getRightClicked();
        if (clicked instanceof Hanging || clicked instanceof ArmorStand) return;
        activate(player, item);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTick.remove(event.getPlayer().getUniqueId());
    }

    private void activate(Player player, ItemStack item) {
        UUID id = player.getUniqueId();
        if (!plugin.activationItem().isOwnedBy(item, id)) return;

        // One activation per tick, even if several events fire for the same click.
        int now = Bukkit.getCurrentTick();
        Integer last = lastTick.put(id, now);
        if (last != null && last == now) return;

        plugin.powers().tryActivate(player);
    }
}
