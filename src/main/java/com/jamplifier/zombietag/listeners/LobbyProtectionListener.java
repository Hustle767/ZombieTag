// src/main/java/com/jamplifier/zombietag/listeners/LobbyProtectionListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.core.GamePhase;
import com.jamplifier.zombietag.core.GameState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;
import java.util.UUID;

public class LobbyProtectionListener implements Listener {

    private final GameState state;
    private final boolean blockPvp;
    private final boolean blockAllDamage;
    private final boolean blockHunger;

    /** Enable everything by default (PvP + all damage + hunger). */
    public LobbyProtectionListener(GameState state) {
        this(state, true, true, true);
    }

    /** Fine-grained constructor if you want to toggle features. */
    public LobbyProtectionListener(GameState state, boolean blockPvp, boolean blockAllDamage, boolean blockHunger) {
        this.state = Objects.requireNonNull(state, "state");
        this.blockPvp = blockPvp;
        this.blockAllDamage = blockAllDamage;
        this.blockHunger = blockHunger;
    }

    // ---------- Helpers ----------

    /** Is the server currently in a lobby-like phase? */
    private boolean isLobbyPhase() {
        GamePhase phase = state.getPhase();
        return phase == GamePhase.LOBBY || phase == GamePhase.COUNTDOWN;
    }

    /** Is this specific player considered in the lobby list? */
    private boolean isInLobbyList(Player p) {
        if (p == null) return false;
        UUID id = p.getUniqueId();
        // state.getLobbyPlayers() contains Player objects
        return state.getLobbyPlayers().stream().anyMatch(lp -> lp.getUniqueId().equals(id));
    }

    /** Should this player be protected right now (phase OR explicitly in lobby list)? */
    private boolean isLobbyProtected(Player p) {
        // Only protect if we're in a lobby-like phase *and* the player is in the lobby list
        return isLobbyPhase() && isInLobbyList(p);
    }

    // ---------- PvP-only blocking (melee + projectiles) ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!blockAllDamage) return;
        if (!(e.getEntity() instanceof Player p)) return;

        // Lobby protection only for queued players
        if (isLobbyProtected(p)) {
            e.setCancelled(true);
            return;
        }

        // Optional: protect all players during grace while RUNNING
        if (state.getPhase() == GamePhase.RUNNING &&
            System.currentTimeMillis() < state.getGraceEndsAtMs()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!blockPvp) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        Player damager = null;
        if (e.getDamager() instanceof Player dp) damager = dp;
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player dp) damager = dp;
        if (damager == null) return;

        // Block PvP in lobby only for queued players
        if (isLobbyProtected(victim) || isLobbyProtected(damager)) {
            e.setCancelled(true);
            return;
        }

        // Optional: block PvP during grace while RUNNING
        if (state.getPhase() == GamePhase.RUNNING &&
            System.currentTimeMillis() < state.getGraceEndsAtMs()) {
            e.setCancelled(true);
        }
    }


    // ---------- Hunger drain blocking ----------

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!blockHunger) return;
        if (!(e.getEntity() instanceof Player p)) return;

        if (isLobbyProtected(p)) {
            e.setCancelled(true);
        }
    }
}
