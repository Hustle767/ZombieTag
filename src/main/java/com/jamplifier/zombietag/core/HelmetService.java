package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HelmetService {
    private final MainClass plugin;
    private final Settings settings;
    private final ConfigStats stats;

    // MainClass calls: new HelmetService(this, settings, stats)
    public HelmetService(MainClass plugin, Settings settings, ConfigStats stats) {
        this.plugin = plugin;
        this.settings = settings;
        this.stats = stats;
    }

    private Material zombieHelmet() {
        Material m = Material.matchMaterial(settings.headItemType);
        return (m != null) ? m : Material.ZOMBIE_HEAD;
    }

    /** Put the zombie helmet on and remember what the player had before (by material name). */
    public void giveZombieHelmet(Player p) {
        ItemStack cur = p.getInventory().getHelmet();
        // Only save if they're not already wearing the zombie helmet
        if (cur == null || cur.getType() != zombieHelmet()) {
            stats.saveHelmet(p.getUniqueId(), cur);  // stores material name or "none"
        }
        p.getInventory().setHelmet(new ItemStack(zombieHelmet()));
    }

    /** Restore the previously saved helmet (by material name), or clear if none. */
    public void restoreHelmet(Player p) {
        String matName = stats.loadHelmetType(p.getUniqueId()); // "none" or e.g. "DIAMOND_HELMET"
        if (!"none".equalsIgnoreCase(matName)) {
            Material m = Material.matchMaterial(matName);
            if (m != null) {
                p.getInventory().setHelmet(new ItemStack(m));
            } else {
                // Unknown material name in config; safest is to clear
                p.getInventory().setHelmet(null);
                plugin.getLogger().warning("[ZombieTag] Unknown helmet material '" + matName + "' for " + p.getName());
            }
        } else {
            // If they're still wearing the zombie helm, clear it
            ItemStack cur = p.getInventory().getHelmet();
            if (cur != null && cur.getType() == zombieHelmet()) {
                p.getInventory().setHelmet(null);
            }
        }
        // Clear stored value after restore so we don't apply it twice
        stats.saveHelmet(p.getUniqueId(), null);
    }
}
