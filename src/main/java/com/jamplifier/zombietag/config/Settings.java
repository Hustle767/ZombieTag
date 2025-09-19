// config/Settings.java
package com.jamplifier.zombietag.config;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
    public final int playerNeeded;
    public final int maxPlayers;
    public final int graceSeconds;
    public final int gameLengthSeconds;
    public final boolean survivorRewardEnabled;
    public final String survivorRewardCommand;
    public final boolean stayStillEnabled;
    public final int stayStillSeconds;
    public final String stayStillMessage;
    public final String headItemType;
    public final boolean announceGameLength;
    public final int lobbyCountdownSeconds;
    public final boolean autoRejoin;
    

    public Settings(FileConfiguration cfg) {
        playerNeeded = cfg.getInt("PlayerNeeded", 2);
        maxPlayers = cfg.getInt("MaxPlayers", 20);
        graceSeconds = cfg.getInt("GracePeriod", 10);
        gameLengthSeconds = cfg.getInt("GameLength", 300);
        announceGameLength = cfg.getBoolean("AnnounceGameLength", true);
        survivorRewardEnabled = cfg.getBoolean("SurvivorReward.Enabled", true);
        survivorRewardCommand = cfg.getString("SurvivorReward.Command", "");
        stayStillEnabled = cfg.getBoolean("StayStillTimer.Enabled", true);
        stayStillSeconds = cfg.getInt("StayStillTimer.TimeLimit", 30);
        stayStillMessage = cfg.getString("StayStillTimer.Message",
                "§cYou stayed still for too long and turned into a zombie!");
        headItemType = cfg.getString("HeadItem.Type", "ZOMBIE_HEAD");
     // NEW
        lobbyCountdownSeconds = cfg.getInt("Lobby.CountdownSeconds", 10);
        autoRejoin            = cfg.getBoolean("auto-rejoin", true);
    }
}
