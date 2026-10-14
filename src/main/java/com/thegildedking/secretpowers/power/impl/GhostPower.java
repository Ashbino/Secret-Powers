package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostPower extends Power {

    private record State(BukkitTask end, BukkitTask hide) {}

    private static final EquipmentSlot[] HELD = {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND};
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Map<UUID, State> active = new HashMap<>();

    public GhostPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.GHOST);
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) return fail(player, "Already active.");

        int ticks = clamp(integer("duration", 5), 1, 60) * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ticks, 0, false, false, true));

        BukkitTask hide = null;
        if (bool("hide-held-items", true) || bool("hide-armor", true)) {
            hide = Bukkit.getScheduler().runTaskTimer(plugin, () -> hideGear(player), 0L, 10L);
        }
        BukkitTask end = Bukkit.getScheduler().runTaskLater(plugin, () -> end(id), ticks);
        active.put(id, new State(end, hide));
        return true;
    }

    /** Called when the ghost attacks something. */
    public void breakInvisibility(Player player) {
        UUID id = player.getUniqueId();
        if (!active.containsKey(id)) return;
        end(id);
        Messages.actionBar(player, "<gray>Your cover is broken.");
    }

    private void end(UUID id) {
        State state = active.remove(id);
        if (state == null) return;
        state.end().cancel();
        if (state.hide() != null) state.hide().cancel();

        Player player = Bukkit.getPlayer(id);
        if (player != null && player.isOnline()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            restoreGear(player);
        }
    }

    private void hideGear(Player ghost) {
        if (!ghost.isOnline()) return;
        ItemStack air = new ItemStack(Material.AIR);
        for (Player viewer : ghost.getWorld().getPlayers()) {
            if (viewer.equals(ghost)) continue;
            if (bool("hide-held-items", true)) {
                for (EquipmentSlot slot : HELD) viewer.sendEquipmentChange(ghost, slot, air);
            }
            if (bool("hide-armor", true)) {
                for (EquipmentSlot slot : ARMOR) viewer.sendEquipmentChange(ghost, slot, air);
            }
        }
    }

    private void restoreGear(Player ghost) {
        EquipmentSlot[][] groups = {HELD, ARMOR};
        for (Player viewer : ghost.getWorld().getPlayers()) {
            if (viewer.equals(ghost)) continue;
            for (EquipmentSlot[] group : groups) {
                for (EquipmentSlot slot : group) {
                    ItemStack real = ghost.getInventory().getItem(slot);
                    viewer.sendEquipmentChange(ghost, slot, real == null ? new ItemStack(Material.AIR) : real);
                }
            }
        }
    }

    @Override
    public void cleanup(UUID id) {
        end(id);
    }

    @Override
    public void cleanupAll() {
        for (UUID id : new ArrayList<>(active.keySet())) end(id);
    }
}
