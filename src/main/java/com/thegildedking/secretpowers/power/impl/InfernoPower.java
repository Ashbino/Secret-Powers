package com.thegildedking.secretpowers.power.impl;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.power.Power;
import com.thegildedking.secretpowers.power.PowerType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InfernoPower extends Power {

    private final Map<UUID, BukkitTask> auras = new HashMap<>();

    public InfernoPower(SecretPowersPlugin plugin) {
        super(plugin, PowerType.INFERNO);
    }

    @Override
    public boolean activate(Player player) {
        UUID id = player.getUniqueId();
        stopAura(id);

        int ticks = clamp(integer("duration", 5), 1, 60) * 20;
        double radius = clamp(dbl("aura-radius", 3.0), 1.0, 16.0);
        boolean burn = bool("burn-enemies", true);
        int burnTicks = clamp(integer("burn-seconds", 3), 1, 20) * 20;

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, ticks, 0, false, false, true));

        int[] elapsed = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player owner = Bukkit.getPlayer(id);
            if (owner == null || !owner.isOnline() || owner.isDead() || elapsed[0] >= ticks) {
                stopAura(id);
                return;
            }
            owner.getWorld().spawnParticle(Particle.FLAME, owner.getLocation().add(0, 1, 0),
                    8, radius / 2, 0.5, radius / 2, 0.01);

            if (burn) {
                for (Player other : owner.getWorld().getPlayers()) {
                    if (other.equals(owner) || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
                    if (plugin.teams().areTeammates(id, other.getUniqueId())) continue;
                    if (other.getLocation().distanceSquared(owner.getLocation()) <= radius * radius) {
                        other.setFireTicks(Math.max(other.getFireTicks(), burnTicks));
                    }
                }
            }
            elapsed[0] += 5;
        }, 0L, 5L);
        auras.put(id, task);
        return true;
    }

    private void stopAura(UUID id) {
        BukkitTask task = auras.remove(id);
        if (task != null) task.cancel();
    }

    @Override
    public void cleanup(UUID id) {
        if (auras.containsKey(id)) {
            stopAura(id);
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
    }

    @Override
    public void cleanupAll() {
        for (UUID id : new ArrayList<>(auras.keySet())) cleanup(id);
    }
}
