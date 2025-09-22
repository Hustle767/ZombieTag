package com.jamplifier.zombietag.Util;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Super-lightweight language/messages loader.
 * - File: <plugins>/ZombieTag/lang.yml
 * - Color codes: use '&' (e.g., &aHello)
 * - Placeholders: {name} replaced from map or varargs
 */
public class Lang {
    private final MainClass plugin;
    private final File file;
    private YamlConfiguration yml;
    private String prefix = "";

    public Lang(MainClass plugin) {
        this.plugin = plugin;
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        // Lang file path: plugins/ZombieTag/lang.yml
        this.file = new File(plugin.getDataFolder(), "lang.yml");

        // If a default is bundled in the jar at /lang.yml, copy it once
        if (!file.exists()) {
            try {
                plugin.saveResource("lang.yml", false);
            } catch (Throwable ignore) {
                // Not bundled — create an empty file
                try { file.createNewFile(); } catch (IOException ignored) {}
            }
        }
        reload();
    }

    /** Reload lang.yml from disk. */
    public void reload() {
        this.yml = YamlConfiguration.loadConfiguration(file);
        this.prefix = color(yml.getString("prefix", ""));
    }

    /** Get colored, formatted message by key (returns the key itself if missing). */
    public String tr(String key, Map<String, ?> placeholders) {
        String raw = yml.getString(key);
        if (raw == null) raw = key;
        if (placeholders != null) {
            for (Map.Entry<String, ?> e : placeholders.entrySet()) {
                raw = raw.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return color(prefix + raw);
    }

    /** Varargs convenience: tr("lobby.joined", "player", name, "count", 3) */
    public String tr(String key, Object... kv) {
        return tr(key, m(kv));
    }

    /** Send to one player. */
    public void send(Player p, String key, Object... kv) {
        p.sendMessage(tr(key, kv));
    }

    /** Send to many players. */
    public void send(Iterable<? extends Player> players, String key, Object... kv) {
        String msg = tr(key, kv);
        for (Player p : players) p.sendMessage(msg);
    }

    /** Action bar helper. */
    public void actionBar(Player p, String key, Object... kv) {
        p.sendActionBar(tr(key, kv));
    }

    /** Check if a key exists in lang.yml. */
    public boolean has(String key) {
        return yml.isSet(key);
    }

    /** Set (or seed) a value in-memory and save to disk. Useful for first-run defaults. */
    public void setAndSave(String key, String value) {
        yml.set(key, value);
        saveSilently();
    }

    /** Save lang.yml (called by setAndSave). */
    private void saveSilently() {
        try { yml.save(file); } catch (IOException e) {
            plugin.getLogger().warning("[Lang] Could not save lang.yml: " + e.getMessage());
        }
    }

    /** Small helper to build placeholder maps. */
    public static Map<String, Object> m(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
