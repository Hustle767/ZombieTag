package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

/**
 * Blocks ALL commands for active round participants
 * except the ones explicitly allowed in config under commands.whitelist.
 */
public class CommandBlockListener implements Listener {

    private final MainClass plugin;
    private final GameState gameState;
    private final PlayerRegistry registry;
    private final Settings settings;

    public CommandBlockListener(MainClass plugin,
                                GameState gameState,
                                PlayerRegistry registry,
                                Settings settings) {
        this.plugin = plugin;
        this.gameState = gameState;
        this.registry = registry;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Only care while the game is actually running
        if (!gameState.isRunning()) return;

        PlayerState ps = registry.get(player.getUniqueId());
        if (ps == null || !ps.isIngame()) return; // player not in the round -> allow anything

        // Command text like "/spawn arg1 arg2"
        String full = event.getMessage();
        if (full == null || full.isEmpty()) return;

        // Normalize:
        // - Lowercase
        // - Trim spaces
        // - Grab the root ("/spawn" from "/spawn home bed")
        String lower = full.toLowerCase().trim();
        if (!lower.startsWith("/")) {
            // not actually a slash command? bail
            return;
        }

        // Extract just the first token
        int spaceIndex = lower.indexOf(' ');
        String baseCmd = (spaceIndex == -1) ? lower : lower.substring(0, spaceIndex); // "/spawn"

        // Check whitelist from settings
        List<String> allowed = settings.commandWhitelist;
        if (allowed != null) {
            for (String allow : allowed) {
                if (allow == null || allow.isEmpty()) continue;
                String a = allow.toLowerCase().trim();

                // We accept either exact ("/spawn") or no-slash form ("spawn") in config
                if (!a.startsWith("/")) {
                    a = "/" + a;
                }

                if (baseCmd.equals(a)) {
                    // allowed, let it through
                    return;
                }
            }
        }

        // Not allowed -> block
        event.setCancelled(true);

        // Feedback using the same message as other restrictions
        plugin.getLang().actionBar(player, "restrictions.blocked");
    }
}
