package com.jamplifier.commands;

import com.jamplifier.MainClass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Admin implements CommandExecutor {

    private MainClass plugin;

    public Admin(MainClass plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("zombietag.admin")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        // Check if the command has the correct number of arguments
        if (args.length != 2) {
            player.sendMessage("§eUsage: /zombietag setspawn <lobby|game>");
            return true;
        }

        // Validate arguments
        if (!args[0].equalsIgnoreCase("setspawn") || 
            (!args[1].equalsIgnoreCase("lobby") && !args[1].equalsIgnoreCase("game"))) {
            player.sendMessage("§eUsage: /zombietag setspawn <lobby|game>");
            return true;
        }

        Location loc = player.getLocation();
        if (args[1].equalsIgnoreCase("lobby")) {
            plugin.getConfig().set("LobbySpawn.world", loc.getWorld().getName());
            plugin.getConfig().set("LobbySpawn.X", loc.getX());
            plugin.getConfig().set("LobbySpawn.Y", loc.getY());
            plugin.getConfig().set("LobbySpawn.Z", loc.getZ());
            plugin.saveConfig();
            player.sendMessage("§aLobby spawn set!");
        } else if (args[1].equalsIgnoreCase("game")) {
            plugin.getConfig().set("GameSpawn.world", loc.getWorld().getName());
            plugin.getConfig().set("GameSpawn.X", loc.getX());
            plugin.getConfig().set("GameSpawn.Y", loc.getY());
            plugin.getConfig().set("GameSpawn.Z", loc.getZ());
            plugin.saveConfig();
            player.sendMessage("§aGame spawn set!");
        }

        return true;
    }

    
    
    
}
