package com.jamplifier.zombietag.Util;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class CommandsTabCompleter implements TabCompleter {
    private final MainClass plugin;

    private static final List<String> BASE_PLAYER = Arrays.asList(
            "join", "leave", "top", "stats", "help"
    );
    private static final List<String> BASE_ADMIN = Arrays.asList(
            "reload", "setspawn", "teleport", "info"
    );
    // UPDATED: include "exit"
    private static final List<String> LOBBY_GAME_EXIT = Arrays.asList("lobby", "game", "exit");
    private static final List<String> INFO_SECTIONS = Arrays.asList(
            "all", "lobby", "game", "rewards", "items", "effects", "stay", "staystill", "spawns", "spawn"
    );

    public CommandsTabCompleter(MainClass plugin) {
        this.plugin = plugin;
    }

    private List<String> match(String arg, Collection<String> options) {
        if (arg == null || arg.isEmpty()) return new ArrayList<>(options);
        String a = arg.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(a))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean isAdmin = sender.hasPermission("zombietag.admin");

        // Arg 1: subcommand
        if (args.length == 1) {
            List<String> base = new ArrayList<>(BASE_PLAYER);
            if (isAdmin) base.addAll(BASE_ADMIN);
            return match(args[0], base);
        }

        // Arg 2: sub-args
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);

            switch (sub) {
                case "setspawn":
                    if (!isAdmin) return Collections.emptyList();
                    return match(args[1], LOBBY_GAME_EXIT); // include exit

                case "teleport":
                    if (!isAdmin) return Collections.emptyList();
                    return match(args[1], LOBBY_GAME_EXIT); // include exit

                case "info":
                    if (!isAdmin) return Collections.emptyList();
                    return match(args[1], INFO_SECTIONS);

                case "stats": {
                    // suggest online player names
                    List<String> names = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(Objects::nonNull)
                            .sorted()
                            .collect(Collectors.toList());
                    return match(args[1], names);
                }

                default:
                    return Collections.emptyList();
            }
        }

        // No deeper suggestions for current commands
        return Collections.emptyList();
    }
}
