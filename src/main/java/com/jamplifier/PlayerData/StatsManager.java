package com.jamplifier.PlayerData;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public class StatsManager {
    private final Path statsFolder;

    public StatsManager(File dataFolder) {
        this.statsFolder = Paths.get(dataFolder.getPath(), "stats");
        try {
            Files.createDirectories(statsFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getPlayerStats(UUID playerUUID) {
        Path playerFile = statsFolder.resolve(playerUUID + ".yml");
        if (!Files.exists(playerFile)) {
            Properties stats = new Properties();
            stats.setProperty("tags", "0");
            stats.setProperty("survivals", "0");
            try {
                stats.store(Files.newBufferedWriter(playerFile), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return stats.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> Integer.parseInt(e.getValue().toString())
                    ));
        }
        try {
            Properties stats = new Properties();
            stats.load(Files.newBufferedReader(playerFile));
            return stats.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> Integer.parseInt(e.getValue().toString())
                    ));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public String getPlayerStat(UUID playerUUID, String stat, String defaultValue) {
        Path playerFile = statsFolder.resolve(playerUUID + ".yml");
        if (!Files.exists(playerFile)) {
            return defaultValue;
        }
        try {
            Properties stats = new Properties();
            stats.load(Files.newBufferedReader(playerFile));
            return stats.getProperty(stat, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public void updatePlayerStat(UUID playerUUID, String stat, String value) {
        Path playerFile = statsFolder.resolve(playerUUID + ".yml");
        Properties stats = new Properties();
        try {
            if (Files.exists(playerFile)) {
                stats.load(Files.newBufferedReader(playerFile));
            }
            stats.setProperty(stat, value);
            stats.store(Files.newBufferedWriter(playerFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerStat(UUID playerUUID, String stat, int amount) {
        Path playerFile = statsFolder.resolve(playerUUID + ".yml");
        Properties stats = new Properties();
        try {
            if (Files.exists(playerFile)) {
                stats.load(Files.newBufferedReader(playerFile));
            }
            int current = Integer.parseInt(stats.getProperty(stat, "0"));
            stats.setProperty(stat, String.valueOf(current + amount));
            stats.store(Files.newBufferedWriter(playerFile), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Map.Entry<UUID, Integer>> getLeaderboard(String stat) {
        List<Map.Entry<UUID, Integer>> leaderboard = new ArrayList<>();
        try {
            Files.list(statsFolder).forEach(file -> {
                try {
                    Properties stats = new Properties();
                    stats.load(Files.newBufferedReader(file));
                    UUID uuid = UUID.fromString(file.getFileName().toString().replace(".yml", ""));
                    int value = Integer.parseInt(stats.getProperty(stat, "0"));
                    leaderboard.add(new AbstractMap.SimpleEntry<>(uuid, value));
                } catch (IOException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return leaderboard;
    }


    public String serializeHelmet(ItemStack helmet) {
        if (helmet == null) return "none";
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(helmet.serialize()); // Serialize the ItemStack properly
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "none";
        }
    }

    public ItemStack deserializeHelmet(String serializedHelmet) {
        if (serializedHelmet.equals("none")) return null;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializedHelmet));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            @SuppressWarnings("unchecked")
            Map<String, Object> serialized = (Map<String, Object>) dataInput.readObject();
            return ItemStack.deserialize(serialized); // Deserialize properly
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


}
