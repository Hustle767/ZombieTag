package com.jamplifier.zombietag.Util;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.core.GamePhase;
import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.stats.ConfigStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Placeholders extends PlaceholderExpansion {

    private final MainClass plugin;
    private final ConfigStats stats;
    private final GameState state;

    private static final class CacheItem {
        final long expiresAt;
        final List<ConfigStats.Entry> data;
        CacheItem(long expiresAt, List<ConfigStats.Entry> data) {
            this.expiresAt = expiresAt;
            this.data = data;
        }
    }
    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();
    private final long ttlMillis = 10_000; // 10s cache for leaderboards

    public Placeholders(MainClass plugin, ConfigStats stats, GameState state) {
        this.plugin = plugin;
        this.stats = stats;
        this.state = state;
    }

    @Override public @NotNull String getIdentifier() { return "zombietag"; }
    @Override public @NotNull String getAuthor()     { return "Jamplifier"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String p = params.toLowerCase(Locale.ROOT);

        // --- Player stats ---
        if (p.equals("player_tags")) {
            if (player == null || player.getUniqueId() == null) return "0";
            return String.valueOf(stats.getTags(player.getUniqueId()));
        }
        if (p.equals("player_survivals")) {
            if (player == null || player.getUniqueId() == null) return "0";
            return String.valueOf(stats.getSurvivals(player.getUniqueId()));
        }
        if (p.equals("player_tags_rank")) {
            if (player == null || player.getUniqueId() == null) return "-";
            int r = stats.getTagsRank(player.getUniqueId());
            return r > 0 ? String.valueOf(r) : "-";
        }
        if (p.equals("player_survivals_rank")) {
            if (player == null || player.getUniqueId() == null) return "-";
            int r = stats.getSurvivalsRank(player.getUniqueId());
            return r > 0 ? String.valueOf(r) : "-";
        }

        // --- Leaderboards ---
        if (p.startsWith("top_taggers_") || p.startsWith("top_survivals_")) {
            boolean taggers = p.startsWith("top_taggers_");
            String tail = p.substring(p.indexOf('_', 4) + 1);
            String[] parts = tail.split("_");
            if (parts.length != 2) return "";
            int index;
            try { index = Integer.parseInt(parts[0]); } catch (NumberFormatException e) { return ""; }
            String field = parts[1];

            int pos = Math.max(0, index - 1);
            List<ConfigStats.Entry> list = getCached(taggers ? "taggers" : "survivals", 50);
            if (pos >= list.size()) return "";

            ConfigStats.Entry e = list.get(pos);
            if (field.equals("name"))  return e.name();
            if (field.equals("value")) return String.valueOf(e.value());
            return "";
        }

        // --- Live game info ---
        if (p.equals("phase")) {
            GamePhase ph = state.getPhase();
            return ph == null ? "UNKNOWN" : ph.name();
        }
        if (p.equals("lobby_count")) {
            return String.valueOf(state.getLobbyPlayers().size());
        }
        if (p.equals("game_count")) {
            return String.valueOf(state.getGamePlayers().size());
        }
        if (p.equals("countdown_seconds")) {
            return String.valueOf(Math.max(0, state.getRemainingLobbySeconds()));
        }
        if (p.equals("time_left")) {
            return String.valueOf(Math.max(0, state.getRemainingGameSeconds()));
        }

        return null;
    }

    private List<ConfigStats.Entry> getCached(String kind, int limit) {
        long now = System.currentTimeMillis();
        CacheItem ci = cache.get(kind);
        if (ci != null && ci.expiresAt > now) return ci.data;

        List<ConfigStats.Entry> fresh =
            "taggers".equals(kind) ? stats.getTopTaggers(limit) : stats.getTopSurvivals(limit);

        cache.put(kind, new CacheItem(now + ttlMillis, fresh));
        return fresh;
    }
}
