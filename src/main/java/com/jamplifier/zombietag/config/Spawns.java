package com.jamplifier.zombietag.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

public class Spawns {

    private FileConfiguration cfg;
    private Location lobby;
    private Location game;
    private Location exit; // NEW

    public Spawns(FileConfiguration cfg) {
        this.cfg = cfg;
        this.lobby = load("spawns.lobby");
        this.game  = load("spawns.game");
        this.exit  = load("spawns.exit"); // NEW
    }

    public void reload(FileConfiguration newCfg) {
        if (newCfg != null) this.cfg = newCfg;
        this.lobby = load("spawns.lobby");
        this.game  = load("spawns.game");
        this.exit  = load("spawns.exit"); // NEW
    }

    private Location load(String base) {
        if (cfg == null) return null;

        String worldName = cfg.getString(base + ".world");
        if (worldName == null || worldName.isEmpty()) return null;

        World w = Bukkit.getWorld(worldName);
        if (w == null) return null;

        double x = cfg.getDouble(base + ".x");
        double y = cfg.getDouble(base + ".y");
        double z = cfg.getDouble(base + ".z");
        float yaw = (float) cfg.getDouble(base + ".yaw", 0.0);
        float pitch = (float) cfg.getDouble(base + ".pitch", 0.0);

        return new Location(w, x, y, z, yaw, pitch);
    }

    public Location lobby() { return lobby == null ? null : lobby.clone(); }
    public Location game()  { return game  == null ? null : game.clone(); }
    public Location exit()  { return exit  == null ? null : exit.clone(); } // NEW
}
