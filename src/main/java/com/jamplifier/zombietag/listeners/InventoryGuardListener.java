package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryGuardListener implements Listener {
    private final PlayerRegistry registry;
    private final Material zombieHelm;

    public InventoryGuardListener(Settings settings, PlayerRegistry registry) {
        this.registry = registry;
        Material m = Material.matchMaterial(settings.headItemType);
        this.zombieHelm = (m != null) ? m : Material.ZOMBIE_HEAD;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInv(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        PlayerState d = registry.get(p.getUniqueId());
        if (d == null || !d.isIngame() || !d.isZombie()) return;

        var cur = e.getCurrentItem();
        var cursor = e.getCursor();
        boolean touching = (cur != null && cur.getType() == zombieHelm) ||
                           (cursor != null && cursor.getType() == zombieHelm);
        if (touching) e.setCancelled(true);
    }
}
