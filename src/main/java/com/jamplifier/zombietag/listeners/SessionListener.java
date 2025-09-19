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
        if (id.equals(state.getInitialZombie())) {
            Bukkit.broadcastMessage("§cThe initial zombie (" + p.getName() + ") has left. Ending the game.");
            game.endGame(true);
            return;
        }

        state.getGamePlayers().remove(p);
        registry.remove(id);

        if (state.getGamePlayers().size() < plugin.getSettings().playerNeeded &&
        	    state.isRunning()) {
            state.getGamePlayers().forEach(gp -> gp.sendMessage("§cNot enough players! The game will now reset."));
            game.endGame(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (state.getGamePlayers().contains(p)) {
        	PlayerState d = registry.get(p.getUniqueId());
            if (d != null) {
                helmets.restoreHelmet(p);
                d.setIngame(false); d.setZombie(false);
                state.getGamePlayers().remove(p);
                state.getLobbyPlayers().remove(p);
                registry.remove(p.getUniqueId());
                Location ls = spawns.lobby();
                if (ls != null) p.teleport(ls);
                p.sendMessage("§aYou have been returned to the lobby.");
            }
        }
    }
}
