package com.jamplifier.zombietag.Util;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        this.file = new File(plugin.getDataFolder(), "lang.yml");

        if (!file.exists()) {
            try {
                plugin.saveResource("lang.yml", false);
            } catch (Throwable ignore) {
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

    /** Core translate with Map placeholders. */
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

    /** Varargs convenience: tr("lobby.joined", "player", name, "count", 3) or tr("k", map). */
    public String tr(String key, Object... kv) {
        if (kv != null && kv.length == 1 && kv[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> map = (Map<String, ?>) kv[0];
            return tr(key, map);
        }
        return tr(key, m(kv));
    }

    /** Send to one player (varargs or single-Map). */
    public void send(Player p, String key, Object... kv) {
        p.sendMessage(tr(key, kv));
    }
    public void send(Player p, String key, Map<String, ?> placeholders) {
        p.sendMessage(tr(key, placeholders));
    }

    /** Send to many players (varargs or single-Map). */
    public void send(Iterable<? extends Player> players, String key, Object... kv) {
        String msg = tr(key, kv);
        for (Player pl : players) pl.sendMessage(msg);
    }
    public void send(Iterable<? extends Player> players, String key, Map<String, ?> placeholders) {
        String msg = tr(key, placeholders);
        for (Player pl : players) pl.sendMessage(msg);
    }

    /** Action bar helper (varargs or single-Map). */
    public void actionBar(Player p, String key, Object... kv) {
        p.sendActionBar(tr(key, kv));
    }
    public void actionBar(Player p, String key, Map<String, ?> placeholders) {
        p.sendActionBar(tr(key, placeholders));
    }

    /** Check if a key exists in lang.yml. */
    public boolean has(String key) {
        return yml.isSet(key);
    }

    /** Set (or seed) a value in-memory and save to disk. */
    public void setAndSave(String key, String value) {
        yml.set(key, value);
        saveSilently();
    }

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
