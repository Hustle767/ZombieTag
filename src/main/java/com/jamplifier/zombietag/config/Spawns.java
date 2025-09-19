// config/Spawns.java
package com.jamplifier.zombietag.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class Spawns {

    private FileConfiguration cfg;  // keep cfg so we can reload later
    private Location lobby;
    private Location game;

    public Spawns(FileConfiguration cfg) {
        this.cfg = cfg;
        this.lobby = load("LobbySpawn");
        this.game  = load("GameSpawn");
    }

    /** Re-read spawn locations from the given config (or reuse the previous one if null). */
    public void reload(FileConfiguration newCfg) {
        if (newCfg != null) this.cfg = newCfg;
        this.lobby = load("LobbySpawn");
        this.game  = load("GameSpawn");
    }

    private Location load(String base) {
        if (cfg == null) return null;

        String worldName = cfg.getString(base + ".world");
        if (worldName == null || worldName.isEmpty()) return null;

        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;

        double x = cfg.getDouble(base + ".X");
        double y = cfg.getDouble(base + ".Y");
        double z = cfg.getDouble(base + ".Z");
        float yaw = (float) cfg.getDouble(base + ".Yaw", 0.0);
        float pitch = (float) cfg.getDouble(base + ".Pitch", 0.0);

        return new Location(w, x, y, z, yaw, pitch);
    }

    public Location lobby() { return lobby == null ? null : lobby.clone(); }
    public Location game()  { return game  == null ? null : game.clone(); }
}
