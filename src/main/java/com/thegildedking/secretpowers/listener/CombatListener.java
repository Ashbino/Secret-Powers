package com.thegildedking.secretpowers.listener;

import com.thegildedking.secretpowers.SecretPowersPlugin;
import com.thegildedking.secretpowers.phase.Phase;
import com.thegildedking.secretpowers.power.PowerType;
import com.thegildedking.secretpowers.power.impl.GhostPower;
import com.thegildedking.secretpowers.power.impl.SoulThiefPower;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class CombatListener implements Listener {

    private final SecretPowersPlugin plugin;

    public CombatListener(SecretPowersPlugin plugin) {
        this.plugin = plugin;
    }

    /** No PvP during preparation / final countdown (configurable). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (!plugin.phases().isPvpAllowed()) {
            event.setCancelled(true);
        }
    }

    /** Ghost breaks on any attack; Soul Thief triggers on successful melee hits. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) return;

        GhostPower ghost = (GhostPower) plugin.powers().get(PowerType.GHOST);
        ghost.breakInvisibility(attacker);

        if (event.isCancelled()) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (plugin.phases().getPhase() != Phase.ACTIVE) return;
        if (plugin.powers().getAssigned(attacker.getUniqueId()) != PowerType.SOUL_THIEF) return;

        SoulThiefPower soulThief = (SoulThiefPower) plugin.powers().get(PowerType.SOUL_THIEF);
        soulThief.onHit(attacker, victim);
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
