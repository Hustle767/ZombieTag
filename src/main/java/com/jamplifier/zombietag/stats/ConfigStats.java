package com.jamplifier.zombietag.stats;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigStats {
    private final MainClass plugin;
    private final File statsDir;
    private final Map<UUID, FileConfiguration> cache = new HashMap<>();
    private final Map<UUID, File> fileMap = new HashMap<>();

    public ConfigStats(MainClass plugin) {
        this.plugin = plugin;
        // Ensure folders exist
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.statsDir = new File(plugin.getDataFolder(), "stats");
        if (!statsDir.exists()) statsDir.mkdirs();
    }

    // -------- internals --------
    private File fileFor(UUID id) {
        return fileMap.computeIfAbsent(id, k -> new File(statsDir, k.toString() + ".yml"));
    }

    private FileConfiguration load(UUID id) {
        return cache.computeIfAbsent(id, k -> YamlConfiguration.loadConfiguration(fileFor(k)));
    }

    private void save(UUID id) {
        try {
            load(id).save(fileFor(id));
        } catch (IOException e) {
            plugin.getLogger().severe("[ZombieTag] Failed saving stats for " + id + ": " + e.getMessage());
        }
    }

    // -------- API (same signatures you already use) --------

    // Int stats
    public int getInt(UUID id, String key, int def) {
        return load(id).getInt(key, def);
    }

    public void setInt(UUID id, String key, int value) {
        load(id).set(key, value);
        save(id);
    }

    public void addInt(UUID id, String key, int amount) {
        setInt(id, key, getInt(id, key, 0) + amount);
    }

    // For existing calls like getPlayerStat(..., "tags", "0")
    public String getPlayerStat(UUID id, String key, String def) {
        return String.valueOf(getInt(id, key, Integer.parseInt(def)));
    }

    // Helmet persistence (store material name)
    public void saveHelmet(UUID id, ItemStack helmet) {
        load(id).set("originalHelmet", helmet == null ? "none" : helmet.getType().name());
        save(id);
    }

    public String loadHelmetType(UUID id) {
        return load(id).getString("originalHelmet", "none");
    }

    // Leaderboard: scan /stats/*.yml for the key
    public List<Map.Entry<UUID, Integer>> getLeaderboard(String key) {
        File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) return Collections.emptyList();

        List<Map.Entry<UUID, Integer>> rows = new ArrayList<>();
        for (File f : files) {
            String uuidStr = f.getName().substring(0, f.getName().length() - 4); // trim .yml
            try {
                UUID id = UUID.fromString(uuidStr);
                FileConfiguration yml = YamlConfiguration.loadConfiguration(f);
                rows.add(Map.entry(id, yml.getInt(key, 0)));
            } catch (IllegalArgumentException ignore) {
                // skip non-uuid files
            }
        }
        return rows.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }
}
