package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HelmetService {
    private final MainClass plugin;
    private final Settings settings;
    private final ConfigStats stats;
    private final NamespacedKey HELM_KEY; // marks our helmet

    public HelmetService(MainClass plugin, Settings settings, ConfigStats stats) {
        this.plugin = plugin;
        this.settings = settings;
        this.stats = stats;
        this.HELM_KEY = new NamespacedKey(plugin, "zombietag_helmet");
    }

    private Material zombieHelmetType() {
        Material m = Material.matchMaterial(settings.headItemType);
        return (m != null) ? m : Material.ZOMBIE_HEAD;
    }

    /** True iff this is the *plugin's* zombie helmet (tagged). */
    private boolean isOurZombieHelmet(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getType() != zombieHelmetType()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        return Boolean.TRUE.equals(meta.getPersistentDataContainer().get(HELM_KEY, PersistentDataType.BYTE)) // 1 = true
            || Boolean.TRUE.equals(meta.getPersistentDataContainer().get(HELM_KEY, PersistentDataType.BOOLEAN));
    }

    /** Build the cursed, tagged zombie helmet. */
    private ItemStack buildZombieHelmet() {
        ItemStack z = new ItemStack(zombieHelmetType());
        ItemMeta meta = z.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);  // hard-lock
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            // tag as ours
            meta.getPersistentDataContainer().set(HELM_KEY, PersistentDataType.BYTE, (byte) 1);
            z.setItemMeta(meta);
        }
        return z;
    }

    /** Save the player's current helmet (full NBT) and put on the cursed zombie helmet. */
    public void giveZombieHelmet(Player p) {
        ItemStack current = p.getInventory().getHelmet();

        // Save whatever they're wearing UNLESS it's already our helmet (prevents saving our own)
        if (!isOurZombieHelmet(current)) {
            stats.saveHelmet(p.getUniqueId(), current); // full stack (or null)
        }

        // Always equip a fresh cursed + tagged zombie helmet
        p.getInventory().setHelmet(buildZombieHelmet());
    }

    /** Restore the exact saved helmet (with NBT). Falls back to legacy string if present. */
    public void restoreHelmet(Player p) {
        ItemStack saved = stats.loadHelmet(p.getUniqueId());

        if (saved != null) {
            p.getInventory().setHelmet(saved);  // replaces cursed helmet programmatically
        } else {
            // Legacy fallback by material string
            String legacy = stats.loadHelmetType(p.getUniqueId());
            if (legacy != null && !"none".equalsIgnoreCase(legacy)) {
                Material m = Material.matchMaterial(legacy);
                p.getInventory().setHelmet(m != null ? new ItemStack(m) : null);
                if (m == null) {
                    plugin.getLogger().warning("[ZombieTag] Unknown legacy helmet material '" + legacy + "' for " + p.getName());
                }
            } else {
                // Nothing saved: if our cursed helmet remains, clear it
                if (isOurZombieHelmet(p.getInventory().getHelmet())) {
                    p.getInventory().setHelmet(null);
                }
            }
        }

        // Clear stored value to avoid re-applying on next restore
        stats.clearHelmet(p.getUniqueId());
    }
}
