package com.jamplifier.zombietag.stats;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigStats {
    private final MainClass plugin;

    public ConfigStats(MainClass plugin) { this.plugin = plugin; }

    private String base(UUID id, String key) { return "Players." + id + "." + key; }
    private FileConfiguration cfg() { return plugin.getConfig(); }

    // Int stats
    public int getInt(UUID id, String key, int def) { return cfg().getInt(base(id, key), def); }
    public void setInt(UUID id, String key, int value) { cfg().set(base(id, key), value); plugin.saveConfig(); }
    public void addInt(UUID id, String key, int amount) { setInt(id, key, getInt(id, key, 0) + amount); }

    // For your existing calls like getPlayerStat(..., "tags", "0")
    public String getPlayerStat(UUID id, String key, String def) { return String.valueOf(getInt(id, key, Integer.parseInt(def))); }

    // Helmet persistence (store only material name for simplicity)
    public void saveHelmet(UUID id, ItemStack helmet) {
        cfg().set(base(id, "originalHelmet"), helmet == null ? "none" : helmet.getType().name());
        plugin.saveConfig();
    }
    public String loadHelmetType(UUID id) { return cfg().getString(base(id, "originalHelmet"), "none"); }

    // Leaderboards: returns entries sorted desc by stat value
    public List<Map.Entry<UUID, Integer>> getLeaderboard(String key) {
        ConfigurationSection players = cfg().getConfigurationSection("Players");
        if (players == null) return Collections.emptyList();

        List<Map.Entry<UUID, Integer>> rows = new ArrayList<>();
        for (String uuidStr : players.getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuidStr);
                int val = players.getInt(uuidStr + "." + key, 0);
                rows.add(Map.entry(id, val));
            } catch (IllegalArgumentException ignore) { }
        }
        return rows.stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }
}
