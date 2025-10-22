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

    // --- NEW: end timestamps for countdown + game ---
    private long lobbyCountdownEndsAtMs = 0L;
    private long gameEndsAtMs = 0L;

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

    // --- NEW: setters when timers start/cancel ---
    public void setLobbyCountdownSeconds(int seconds) {
        this.lobbyCountdownEndsAtMs = seconds > 0
                ? System.currentTimeMillis() + seconds * 1000L
                : 0L;
    }
    public void clearLobbyCountdownTime() { this.lobbyCountdownEndsAtMs = 0L; }

    public void setGameSecondsLeft(int seconds) {
        this.gameEndsAtMs = seconds > 0
                ? System.currentTimeMillis() + seconds * 1000L
                : 0L;
    }
    public void clearGameTime() { this.gameEndsAtMs = 0L; }

    // --- NEW: getters used by PAPI ---
    public int getRemainingLobbySeconds() {
        if (lobbyCountdownEndsAtMs <= 0) return 0;
        long rem = (lobbyCountdownEndsAtMs - System.currentTimeMillis()) / 1000L;
        return (int)Math.max(0, rem);
    }
    public int getRemainingGameSeconds() {
        if (gameEndsAtMs <= 0) return 0;
        long rem = (gameEndsAtMs - System.currentTimeMillis()) / 1000L;
        return (int)Math.max(0, rem);
    }

    public void clearAll() {
        gamePlayers.clear();
        lobbyPlayers.clear();
        initialZombie = null;
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        if (gameTimerTask != null) gameTimerTask.cancel();
        if (stayStillTask != null) stayStillTask.cancel();
        lobbyCountdownTask = gameTimerTask = stayStillTask = null;

        // --- NEW: reset timeouts on hard clear ---
        lobbyCountdownEndsAtMs = 0L;
        gameEndsAtMs = 0L;

        phase = GamePhase.LOBBY;
    }
}
