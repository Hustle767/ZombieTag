// core/GameState.java
package com.jamplifier.zombietag.core;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameState {
    private GamePhase phase = GamePhase.LOBBY;

    // Players for the current round
    private final List<Player> gamePlayers = new ArrayList<>();
    private final List<Player> lobbyPlayers = new ArrayList<>();

    // Initial zombie for the round
    private UUID initialZombie;

    // Independent timers
    private BukkitRunnable lobbyCountdownTask;
    private BukkitRunnable gameTimerTask;
    private BukkitRunnable stayStillTask;

    public GamePhase getPhase() { return phase; }
    public void setPhase(GamePhase phase) { this.phase = phase; }

    public List<Player> getLobbyPlayers() { return lobbyPlayers; }
    public List<Player> getGamePlayers() { return gamePlayers; }

    public UUID getInitialZombie() { return initialZombie; }
    public void setInitialZombie(UUID id) { this.initialZombie = id; }

    public BukkitRunnable getLobbyCountdownTask() { return lobbyCountdownTask; }
    public void setLobbyCountdownTask(BukkitRunnable t) { this.lobbyCountdownTask = t; }

    public BukkitRunnable getGameTimerTask() { return gameTimerTask; }
    public void setGameTimerTask(BukkitRunnable t) { this.gameTimerTask = t; }

    public BukkitRunnable getStayStillTask() { return stayStillTask; }
    public void setStayStillTask(BukkitRunnable t) { this.stayStillTask = t; }

    public boolean isRunning() { return phase == GamePhase.RUNNING; }
    public boolean isCountdown() { return phase == GamePhase.COUNTDOWN; }
    
    private long graceEndsAtMs;
    public long getGraceEndsAtMs(){ return graceEndsAtMs; }
    public void setGraceEndsAtMs(long t){ graceEndsAtMs = t; }


    public void clearAll() {
        gamePlayers.clear();
        lobbyPlayers.clear();
        initialZombie = null;
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        if (gameTimerTask != null) gameTimerTask.cancel();
        if (stayStillTask != null) stayStillTask.cancel();
        lobbyCountdownTask = gameTimerTask = stayStillTask = null;
        phase = GamePhase.LOBBY;
    }
}
