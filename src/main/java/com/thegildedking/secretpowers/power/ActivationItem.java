package com.thegildedking.secretpowers.power;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.util.Messages;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/** The "Power Core": the key item used to trigger powers on a vanilla client. */
public final class ActivationItem {

    private final SecretPowersPlugin plugin;
    private final NamespacedKey key;

    public ActivationItem(SecretPowersPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "power_core");
    }

    public ItemStack create(UUID owner) {
        String name = plugin.getConfig().getString("activation.item-material", "NETHER_STAR");
        Material mat = Material.matchMaterial(name == null ? "" : name);
        if (mat == null || !mat.isItem()) mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Messages.itemName(
                plugin.getConfig().getString("activation.item-name", "<gold>Power Core")));
        meta.lore(List.of(
                Messages.itemName("<gray>Right-click to use your power."),
                Messages.itemName("<dark_gray>Bound to you. Cannot be dropped.")));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, owner.toString());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public boolean isOwnedBy(ItemStack item, UUID id) {
        if (!isCore(item)) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return id.toString().equals(pdc.get(key, PersistentDataType.STRING));
    }

    /** Makes sure the player has exactly one Power Core of their own, removing copies and foreign cores. */
    public void ensure(Player player) {
        PlayerInventory inv = player.getInventory();
        boolean found = false;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (!isCore(it)) continue;
            if (!found && isOwnedBy(it, player.getUniqueId())) {
                if (it.getAmount() != 1) it.setAmount(1);
                found = true;
            } else {
                inv.setItem(i, null);
            }
        }
        if (!found) give(player);
    }

    private void give(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack core = create(player.getUniqueId());
        int slot = Math.max(0, Math.min(8, plugin.getConfig().getInt("activation.preferred-slot", 8)));
        ItemStack current = inv.getItem(slot);
        if (current == null || current.getType().isAir()) {
            inv.setItem(slot, core);
        } else if (inv.firstEmpty() != -1) {
            inv.addItem(core);
        } else {
            Messages.send(player, "<red>Your inventory is full. Make room, then run <white>/sp power</white> to receive your Power Core.");
        }
    }
}
