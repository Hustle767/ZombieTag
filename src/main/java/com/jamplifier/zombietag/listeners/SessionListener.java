// listeners/SessionListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.jamplifier.zombietag.core.PlayerRegistry;

import java.util.ArrayList;
import java.util.UUID;

public class SessionListener implements Listener {
    private final MainClass plugin;
    private final GameState state;
    private final Spawns spawns;
    private final HelmetService helmets;
    private final GameService game;
    private final PlayerRegistry registry;
    
    public SessionListener(MainClass plugin, GameState st, Spawns sp, HelmetService h, GameService g, PlayerRegistry reg) {
        this.plugin = plugin; this.state = st; this.spawns = sp; this.helmets = h; this.game = g;
        this.registry = reg; // NEW
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        helmets.restoreHelmet(p);

        // If the initial zombie leaves, end the game
        if (id.equals(state.getInitialZombie())) {
            Bukkit.broadcastMessage("§cThe initial zombie (" + p.getName() + ") has left. Ending the game.");
            game.endGame(true);
            return;
        }

        // Always remove from lists
        state.getGamePlayers().remove(p);
        state.getLobbyPlayers().remove(p);

        // Keep the registry entry; just clear round flags to avoid stale states
        PlayerState ps = registry.getOrCreate(id);
        ps.setIngame(false);
        ps.setZombie(false);

        // If running and we dropped below threshold, end the game
        if (state.isRunning() && state.getGamePlayers().size() < plugin.getSettings().playerNeeded) {
            state.getGamePlayers().forEach(gp -> gp.sendMessage("§cNot enough players! The game will now reset."));
            game.endGame(true);
        }
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Only act if they (still) appear in the running game's list
        if (!state.getGamePlayers().contains(p)) return;

        // Restore visuals/gear
        helmets.restoreHelmet(p);

        // Sync registry flags instead of removing the entry (prevents tag desync)
        PlayerState d = registry.getOrCreate(p.getUniqueId());
        d.setIngame(false);
        d.setZombie(false);

        // Detach from game lists
        state.getGamePlayers().remove(p);
        state.getLobbyPlayers().remove(p);  // ensure they aren't accidentally queued twice

        // Teleport to lobby spawn (safe either way)
        Location ls = spawns.lobby();
        if (ls != null) p.teleport(ls);

        // Re-queue only if auto-rejoin is enabled
        if (plugin.getSettings().autoRejoin) {
            if (!state.getLobbyPlayers().contains(p)) {
                state.getLobbyPlayers().add(p);
            }
            p.sendMessage("§aYou have been returned to the lobby.");

            // If you're already back in the lobby phase, you can try to kick off a countdown.
            // Safe even if countdown can't start (guardrails in LobbyService handle it).
            try {
                plugin.getLobbyService().maybeStartCountdown();
            } catch (Throwable ignored) {}
        } else {
            // Auto-rejoin OFF: do NOT add to lobby queue
            p.sendMessage("§7Game over or in progress. Use §a/zombietag join§7 to queue again.");
        }
    }

}
