// listeners/WorldGuardListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class WorldGuardListener implements Listener {
    private final PlayerRegistry registry;
    private final GameState state;

    // Match MainClass: new WorldGuardListener(registry, gameState)
    public WorldGuardListener(PlayerRegistry registry, GameState state) {
        this.registry = registry;   // use the shared registry
        this.state = state;         // use state for lobby list
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        PlayerState d = registry.get(p.getUniqueId());
        if (d != null && (d.isIngame() || state.getLobbyPlayers().contains(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        PlayerState d = registry.get(p.getUniqueId());
        if (d != null && (d.isIngame() || state.getLobbyPlayers().contains(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        PlayerState d = registry.get(p.getUniqueId());
        if (d != null && (d.isIngame() || state.getLobbyPlayers().contains(p))) {
            e.setCancelled(true);
        }
    }
}
