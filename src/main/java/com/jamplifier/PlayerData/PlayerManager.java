package com.jamplifier.PlayerData;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.jamplifier.MainClass;

public class PlayerManager implements Listener {
    private UUID uuid;
    private boolean ingame;
    private boolean isdead;

    private MainClass plugin;

    // Constructor with plugin reference
    public PlayerManager(UUID uuid, boolean ingame, boolean isdead) {
        this.uuid = uuid;
        this.ingame = ingame;
        this.isdead = isdead;
        this.plugin = MainClass.getPlugin(MainClass.class); // Get the main plugin instance
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isIngame() {
        return ingame;
    }

    public void setIngame(boolean ingame) {
        this.ingame = ingame;
    }

    public boolean isIsdead() {
        return isdead;
    }

    public void setIsdead(boolean isdead) {
        this.isdead = isdead;
    }
    private ItemStack originalHelmet;

    public ItemStack getOriginalHelmet() {
        return originalHelmet;
    }

    public void setOriginalHelmet(ItemStack originalHelmet) {
        this.originalHelmet = originalHelmet;
    }

  

}
