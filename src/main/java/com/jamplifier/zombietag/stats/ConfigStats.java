package com.jamplifier.zombietag.stats;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConfigStats {
    private final MainClass plugin;
    private final File statsDir;

    // Per-player file + yml cache
    private final Map<UUID, FileConfiguration> cache = new HashMap<>();
    private final Map<UUID, File> fileMap = new HashMap<>();

    // ---- Keys commonly used for leaderboards ----
    public static final String KEY_TAGS = "tags";
    public static final String KEY_SURVIVALS = "survivals";

    // ---- Leaderboard cache (per key) ----
    private static final class LbCache {
        final long expiresAt;
        final List<Map.Entry<UUID, Integer>> rows; // sorted desc by value
        final Map<UUID, Integer> valueMap;         // quick lookup for rank calc
        LbCache(long expiresAt,
                List<Map.Entry<UUID, Integer>> rows,
                Map<UUID, Integer> valueMap) {
            this.expiresAt = expiresAt;
            this.rows = rows;
            this.valueMap = valueMap;
        }
    }
    private final Map<String, LbCache> leaderboardCache = new ConcurrentHashMap<>();
    private long leaderboardTtlMs = 10_000L; // default 10s

    public ConfigStats(MainClass plugin) {
        this.plugin = plugin;
        // Ensure folders exist
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        this.statsDir = new File(plugin.getDataFolder(), "stats");
        if (!statsDir.exists()) statsDir.mkdirs();
    }

    // Optionally allow changing cache TTL from code/config later
    public void setLeaderboardTtlMs(long ttlMs) { this.leaderboardTtlMs = Math.max(1000L, ttlMs); }

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

    private void updateLastKnownName(UUID id) {
        // store a last known name for leaderboards (works offline)
        String name = null;
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        if (op != null) name = op.getName();
        if (name != null && !name.isEmpty()) {
            FileConfiguration y = load(id);
            if (!name.equals(y.getString("lastName"))) {
                y.set("lastName", name);
                save(id);
            }
        }
    }

    private String getLastKnownName(UUID id) {
        String name = load(id).getString("lastName", null);
        if (name != null && !name.isEmpty()) return name;
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op != null ? op.getName() : id.toString();
    }

    private void invalidateLeaderboard(String key) {
        leaderboardCache.remove(key);
    }

    // -------- API (existing) --------

    // Int stats
    public int getInt(UUID id, String key, int def) {
        return load(id).getInt(key, def);
    }

    public void setInt(UUID id, String key, int value) {
        FileConfiguration y = load(id);
        y.set(key, value);
        updateLastKnownName(id); // keep name fresh
        save(id);
        invalidateLeaderboard(key); // values changed => bust cache
    }

    public void addInt(UUID id, String key, int amount) {
        setInt(id, key, getInt(id, key, 0) + amount);
    }

    // For existing calls like getPlayerStat(..., "tags", "0")
    public String getPlayerStat(UUID id, String key, String def) {
        return String.valueOf(getInt(id, key, Integer.parseInt(def)));
    }

    // -------- Helmet persistence (new: store full ItemStack with legacy support) --------

    /**
     * Save the player's original helmet.
     * - Stores the full ItemStack (with NBT) at "helmets.item".
     * - Also stores a legacy material string at "originalHelmet" for backward compatibility.
     */
    public void saveHelmet(UUID id, ItemStack helmet) {
        FileConfiguration y = load(id);
        if (helmet == null) {
            y.set("helmets.item", null);
            y.set("originalHelmet", "none");
        } else {
            // store a clone to avoid external mutation
            y.set("helmets.item", helmet.clone());
            y.set("originalHelmet", helmet.getType().name());
        }
        save(id);
    }

    /**
     * Load the exact saved helmet item (clone) or null if none stored.
     */
    public ItemStack loadHelmet(UUID id) {
        ItemStack it = load(id).getItemStack("helmets.item");
        return it == null ? null : it.clone();
    }

    /**
     * Legacy helper retained for compatibility.
     * Returns a material name. If a full item is present, derives the type from it.
     * Otherwise, falls back to the old "originalHelmet" string field.
     */
    public String loadHelmetType(UUID id) {
        FileConfiguration y = load(id);
        if (y.contains("helmets.item")) {
            ItemStack it = y.getItemStack("helmets.item");
            return (it == null) ? "none" : it.getType().name();
        }
        return y.getString("originalHelmet", "none");
    }

    /**
     * Clear any stored helmet values (both new and legacy).
     */
    public void clearHelmet(UUID id) {
        FileConfiguration y = load(id);
        y.set("helmets.item", null);
        y.set("originalHelmet", null);
        save(id);
    }

    /**
     * Leaderboard scan: returns a list of (UUID, value) sorted desc.
     * Kept for backward compatibility. Now uses an in-memory cache.
     */
    public List<Map.Entry<UUID, Integer>> getLeaderboard(String key) {
        long now = System.currentTimeMillis();
        LbCache cached = leaderboardCache.get(key);
        if (cached != null && cached.expiresAt > now) {
            return cached.rows; // cached sorted rows
        }

        File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            leaderboardCache.put(key, new LbCache(now + leaderboardTtlMs, Collections.emptyList(), Collections.emptyMap()));
            return Collections.emptyList();
        }

        List<Map.Entry<UUID, Integer>> rows = new ArrayList<>();
        Map<UUID, Integer> values = new HashMap<>();

        for (File f : files) {
            String uuidStr = f.getName().substring(0, f.getName().length() - 4); // trim .yml
            try {
                UUID id = UUID.fromString(uuidStr);
                FileConfiguration yml = YamlConfiguration.loadConfiguration(f);
                int val = yml.getInt(key, 0);
                rows.add(Map.entry(id, val));
                values.put(id, val);
            } catch (IllegalArgumentException ignore) {
                // skip non-uuid files
            }
        }

        rows = rows.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());

        leaderboardCache.put(key, new LbCache(now + leaderboardTtlMs, rows, values));
        return rows;
    }

    // -------- New convenience API for placeholders --------

    public int getTags(UUID id) {
        return getInt(id, KEY_TAGS, 0);
    }

    public int getSurvivals(UUID id) {
        return getInt(id, KEY_SURVIVALS, 0);
    }

    /**
     * 1-based rank for a key, or -1 if not found.
     */
    public int getRank(UUID id, String key) {
        // Try cache first
        LbCache lc = leaderboardCache.get(key);
        List<Map.Entry<UUID, Integer>> rows = (lc != null && lc.expiresAt > System.currentTimeMillis())
                ? lc.rows
                : getLeaderboard(key); // refreshes cache if needed

        int idx = 0;
        for (Map.Entry<UUID, Integer> e : rows) {
            if (e.getKey().equals(id)) return idx + 1; // 1-based
            idx++;
        }
        return -1;
    }

    public int getTagsRank(UUID id) {
        return getRank(id, KEY_TAGS);
    }

    public int getSurvivalsRank(UUID id) {
        return getRank(id, KEY_SURVIVALS);
    }

    // Entry shape for top lists with names
    public static record Entry(String name, UUID id, int value) {}

    public List<Entry> getTopTaggers(int limit) {
        return toNamedTop(getLeaderboard(KEY_TAGS), limit);
    }

    public List<Entry> getTopSurvivals(int limit) {
        return toNamedTop(getLeaderboard(KEY_SURVIVALS), limit);
    }

    private List<Entry> toNamedTop(List<Map.Entry<UUID, Integer>> raw, int limit) {
        if (raw.isEmpty()) return Collections.emptyList();
        int take = Math.max(1, Math.min(limit, raw.size()));
        List<Entry> out = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            Map.Entry<UUID, Integer> row = raw.get(i);
            out.add(new Entry(getLastKnownName(row.getKey()), row.getKey(), row.getValue()));
        }
        return out;
    }
}
