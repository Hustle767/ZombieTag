// config/Settings.java
package com.jamplifier.zombietag.config;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings {

    // meta
    public int  configVersion;

    // lobby
    public int  playerNeeded;
    public int  maxPlayers;
    public int  lobbyCountdownSeconds;
    public boolean autoRejoin;
    public boolean queueDuringGame;

    // game
    public int  gameLengthSeconds;
    public int  graceSeconds;
    public boolean announceGameLength;

    // rewards (survivors)
    public boolean rewardEnabled;
    public String  rewardCommand;

    // items
    public String headItemType; // e.g., "ZOMBIE_HEAD"

    // messages
    public String msgZombieWin;
    public String msgSurvivorWin;

    // effects
    public int blindnessSeconds;
    public int nightVisionSeconds;

    // stay-still
    public boolean stayStillEnabled;
    public int  stayStillSeconds;
    public String stayStillMessage;

    public Settings(FileConfiguration cfg) {
        reload(cfg);
    }

    /** Re-read all settings from the provided config (v2 layout). */
    public void reload(FileConfiguration cfg) {
        // meta
        this.configVersion         = cfg.getInt("version", 2);

        // lobby
        this.playerNeeded          = cfg.getInt("lobby.player_needed", 2);
        this.maxPlayers            = cfg.getInt("lobby.max_players", 20);
        this.lobbyCountdownSeconds = cfg.getInt("lobby.countdown_seconds", 10);
        this.autoRejoin            = cfg.getBoolean("lobby.auto_rejoin", true);
        this.queueDuringGame = cfg.getBoolean("lobby.queue_during_game", true);

        // game
        this.gameLengthSeconds     = cfg.getInt("game.length_seconds", 300);
        this.graceSeconds          = cfg.getInt("game.grace_seconds", 10);
        this.announceGameLength    = cfg.getBoolean("game.announce_length", true);

        // rewards
        this.rewardEnabled         = cfg.getBoolean("rewards.survivors.enabled", true);
        this.rewardCommand         = cfg.getString("rewards.survivors.command", "give {player} golden_apple 2");

        // items
        this.headItemType          = cfg.getString("items.zombie_helmet", "ZOMBIE_HEAD");

        // messages
        this.msgZombieWin          = cfg.getString("messages.zombie_win", "§cZombies win! All players have been infected.");
        this.msgSurvivorWin        = cfg.getString("messages.survivor_win", "§aSurvivors win! You escaped the zombie apocalypse!");

        // effects
        this.blindnessSeconds      = cfg.getInt("effects.blindness_seconds", 10);
        this.nightVisionSeconds    = cfg.getInt("effects.night_vision_seconds", 10);

        // stay still
        this.stayStillEnabled      = cfg.getBoolean("stay_still.enabled", true);
        this.stayStillSeconds      = cfg.getInt("stay_still.time_limit_seconds", 30);
        this.stayStillMessage      = cfg.getString("stay_still.message",
                "§cYou stayed still for too long and turned into a zombie!");
    }
}
