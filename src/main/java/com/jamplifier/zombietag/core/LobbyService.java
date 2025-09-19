// core/LobbyService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class LobbyService {
    private final MainClass plugin;
    private final Settings settings;
    private final Spawns spawns;
    private final GameState state;

    public LobbyService(MainClass plugin, Settings settings, Spawns spawns, GameState state) {
        this.plugin = plugin; this.settings = settings; this.spawns = spawns; this.state = state;
    }

    public boolean inLobby(Player p) { return state.getLobbyPlayers().contains(p); }

    public void joinLobby(Player p) {
        var lobby = state.getLobbyPlayers();

        // full?
        if (lobby.size() >= settings.maxPlayers) {
            p.sendMessage("§cThe lobby is full! Please wait for the next round.");
            return;
        }

        // already in lobby?
        if (lobby.contains(p)) {
            p.sendMessage("§eYou’re already in the lobby. (" + lobby.size() + "/" + settings.maxPlayers + ")");
            return;
        }

        // add + announce
        lobby.add(p);
        lobby.forEach(lp -> lp.sendMessage("§a" + p.getName() + " joined! (" + lobby.size() + "/" + settings.maxPlayers + ")"));

        // decide what to do next
        int needed = Math.max(0, settings.playerNeeded - lobby.size());

        if (needed <= 0) {
            // threshold met
            if (state.getPhase() == GamePhase.LOBBY && state.getLobbyCountdownTask() == null) {
                startLobbyCountdown();  // this should set phase=COUNTDOWN and set the task
            } else if (state.isCountdown()) {
                p.sendMessage("§eCountdown is already running…");
            }
            return;
        }

        // still waiting (don’t spam if countdown already started)
        if (!state.isCountdown()) {
            String plural = (needed == 1 ? "" : "s");
            lobby.forEach(lp -> lp.sendMessage("§eWaiting for " + needed + " more player" + plural + "..."));
        }
    }


    public void leaveLobby(Player p) {
        if (state.getLobbyPlayers().remove(p)) {
            p.sendMessage("§cYou have left the lobby!");
            int cur = state.getLobbyPlayers().size();
            state.getLobbyPlayers().forEach(lp -> lp.sendMessage("§7There are now " + cur + " out of " + settings.maxPlayers + " players in the lobby."));
        } else p.sendMessage("§cYou are not in the lobby!");
    }

    private void startLobbyCountdown() {
        state.setPhase(GamePhase.COUNTDOWN);

        BukkitRunnable task = new BukkitRunnable() {
            int seconds = settings.lobbyCountdownSeconds; // e.g. 10

            @Override public void run() {
                var lobby = state.getLobbyPlayers();

                // cancel if threshold lost
                if (lobby.size() < settings.playerNeeded) {
                    lobby.forEach(p -> p.sendMessage("§cNot enough players! Countdown canceled."));
                    state.setPhase(GamePhase.LOBBY);
                    this.cancel();
                    state.setLobbyCountdownTask(null);
                    return;
                }

                if (seconds == 0) {
                    this.cancel();
                    state.setLobbyCountdownTask(null);
                    // hand off to game
                    plugin.getGameService().startGameFromLobby();
                    return;
                }

                if (seconds <= 5 || seconds % 5 == 0) {
                    lobby.forEach(p -> p.sendMessage("§eStarting in " + seconds + " seconds..."));
                }
                seconds--;
            }
        };

        state.setLobbyCountdownTask(task);
        task.runTaskTimer(plugin, 0L, 20L);
    }
}