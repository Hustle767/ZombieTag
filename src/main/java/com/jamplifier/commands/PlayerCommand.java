package com.jamplifier.commands;

import com.jamplifier.MainClass;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommand implements CommandExecutor {

    private MainClass plugin;

    public PlayerCommand(MainClass plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("zombietag.admin.teleport")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length != 2 || !args[0].equalsIgnoreCase("teleport") ||
                (!args[1].equalsIgnoreCase("lobby") && !args[1].equalsIgnoreCase("game"))) {
            player.sendMessage("§eUsage: /zombietag teleport <lobby|game>");
            return true;
        }

        Location targetLocation;

        if (args[1].equalsIgnoreCase("lobby")) {
            targetLocation = new Location(
                    plugin.getServer().getWorld(plugin.getConfig().getString("LobbySpawn.world")),
                    plugin.getConfig().getDouble("LobbySpawn.X"),
                    plugin.getConfig().getDouble("LobbySpawn.Y"),
                    plugin.getConfig().getDouble("LobbySpawn.Z")
            );
        } else { // args[1].equalsIgnoreCase("game")
            targetLocation = new Location(
                    plugin.getServer().getWorld(plugin.getConfig().getString("GameSpawn.world")),
                    plugin.getConfig().getDouble("GameSpawn.X"),
                    plugin.getConfig().getDouble("GameSpawn.Y"),
                    plugin.getConfig().getDouble("GameSpawn.Z")
            );
        }

        if (targetLocation.getWorld() == null) {
            player.sendMessage("§cThe world for this location is invalid!");
            return true;
        }

        player.teleport(targetLocation);
        player.sendMessage("§aTeleported to " + args[1] + " spawn!");
        return true;
    }
}
