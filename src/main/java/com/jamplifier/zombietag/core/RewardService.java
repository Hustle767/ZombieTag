// core/RewardService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RewardService {
    private final Settings settings;

    public RewardService(Settings settings) {
        this.settings = settings;
    }

    public void rewardIfEnabled(Player p) {
        if (!settings.rewardEnabled) return;

        String tmpl = settings.rewardCommand;
        if (tmpl == null || tmpl.isBlank()) return;

        String cmd = tmpl.replace("{player}", p.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        p.sendMessage("§aA reward has been granted for surviving!");
    }
}
