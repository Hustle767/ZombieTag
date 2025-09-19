package com.jamplifier.zombietag.commands;

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

    public CommandsRouter(
            com.jamplifier.zombietag.MainClass plugin,
            LobbyService lobby,
            GameService game,
            ConfigStats stats,
            Settings settings,
            Spawns spawns,
            PlayerRegistry registry
    ) {
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
            case "join":   return playerCmds.join(p);
            case "leave":  return playerCmds.leave(p);
            case "top":    return playerCmds.top(p);
            case "stats":  return playerCmds.stats(p, args);
            // admin
            case "reload":   return adminCmds.reload(p);
            case "setspawn": return adminCmds.setspawn(p, args);
            case "teleport": return adminCmds.teleport(p, args);
            default:
                p.sendMessage("§eInvalid command. Use /zombietag help.");
                return true;
        }
    }
}
