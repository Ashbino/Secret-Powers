package com.thegildedking.secretpowers.listener;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.ActivationItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/** Stops players from dropping, storing or duplicating the Power Core. */
public final class ItemProtectionListener implements Listener {

    private final ActivationItem items;

    public ItemProtectionListener(SecretPowersPlugin plugin) {
        this.items = plugin.activationItem();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (items.isCore(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean coreCurrent = items.isCore(current);
        boolean coreCursor = items.isCore(cursor);
        boolean involves = coreCurrent || coreCursor;

        ClickType click = event.getClick();
        if (!involves && click == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            involves = items.isCore(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        }
        if (!involves && click == ClickType.SWAP_OFFHAND) {
            involves = items.isCore(event.getWhoClicked().getInventory().getItemInOffHand());
        }
        if (!involves) return;

        // Dropping via the inventory screen or clicking outside the window.
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP
                || event.getSlotType() == InventoryType.SlotType.OUTSIDE) {
            event.setCancelled(true);
            return;
        }
        // Hiding it inside a bundle.
        if ((coreCursor && isBundle(current)) || (coreCurrent && isBundle(cursor))) {
            event.setCancelled(true);
            return;
        }
        // Any other container (chest, ender chest, anvil, hopper...) is off limits.
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) return;
        if (items.isCore(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (items.isCore(event.getPlayerItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isBundle(ItemStack item) {
        return item != null && item.getType().name().endsWith("BUNDLE");
    }
}
