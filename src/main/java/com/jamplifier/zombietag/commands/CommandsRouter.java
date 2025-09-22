package com.jamplifier.zombietag.commands;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class CommandsRouter implements CommandExecutor {
    private final PlayerCommands playerCmds;
    private final AdminCommands adminCmds;
    private final MainClass plugin;               // ⬅️ add this

    public CommandsRouter(
            MainClass plugin,                     // ⬅️ keep this type
            LobbyService lobby,
            GameService game,
            ConfigStats stats,
            Settings settings,
            Spawns spawns,
            PlayerRegistry registry
    ) {
        this.plugin = plugin;                     // ⬅️ store plugin
        this.playerCmds = new PlayerCommands(plugin, lobby, game, stats, settings, spawns, registry);
        this.adminCmds  = new AdminCommands(plugin, lobby, game, stats, settings, spawns, registry);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return playerCmds.help(p);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            // player
            case "join": {
                // 🔒 Block joining while a game is active or ending
                GamePhase phase = plugin.getGameState().getPhase();
                if ((phase == GamePhase.RUNNING || phase == GamePhase.ENDING) && !plugin.getSettings().queueDuringGame) {
                    p.sendMessage("§cA game is currently running. Please wait for the next round.");
                    return true;
                }
                // also prevent if they're already in the current game
                if (plugin.getGameState().getGamePlayers().contains(p)) {
                    p.sendMessage("§eYou’re already in an active round!");
                    return true;
                }
                return playerCmds.join(p);
            }
            case "leave":  return playerCmds.leave(p);
            case "top":    return playerCmds.top(p);
            case "stats":  return playerCmds.stats(p, args);
            // admin
            case "reload":   return adminCmds.reload(p);
            case "setspawn": return adminCmds.setspawn(p, args);
            case "teleport": return adminCmds.teleport(p, args);
            case "info":     return adminCmds.info(p, args);

            default:
                p.sendMessage("§eInvalid command. Use /zombietag help.");
                return true;
        }
    }
}
