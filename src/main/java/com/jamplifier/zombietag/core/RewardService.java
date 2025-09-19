// core/RewardService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RewardService {
    private final Settings settings;
    public RewardService(Settings settings) { this.settings = settings; }

    public void rewardIfEnabled(Player p) {
        if (!settings.survivorRewardEnabled) return;
        String tmpl = settings.survivorRewardCommand;
        if (tmpl == null || tmpl.isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), tmpl.replace("{player}", p.getName()));
        p.sendMessage("§aA reward has been granted for surviving!");
    }
}
