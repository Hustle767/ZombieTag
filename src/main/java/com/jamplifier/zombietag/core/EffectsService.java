// core/EffectsService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import org.bukkit.entity.Player;
import org.bukkit.potion.*;

public class EffectsService {
    private final MainClass plugin;
    public EffectsService(MainClass plugin) { this.plugin = plugin; }

    public void applyBlindnessAndNightVision(Player p, int blindSeconds, int nvSeconds) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, nvSeconds * 20, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindSeconds * 20, 0));
    }
}
