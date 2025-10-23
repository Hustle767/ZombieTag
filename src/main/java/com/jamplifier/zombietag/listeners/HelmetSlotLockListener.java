// src/main/java/com/jamplifier/zombietag/listeners/HelmetSlotLockListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.core.GamePhase;
import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class HelmetSlotLockListener implements Listener {
    private final GameState state;
    private final PlayerRegistry registry;
    private final Material zombieHelmType;

    public HelmetSlotLockListener(GameState state, PlayerRegistry registry, Settings settings) {
        this.state = state;
        this.registry = registry;
        Material m = Material.matchMaterial(settings.headItemType);
        this.zombieHelmType = (m != null) ? m : Material.ZOMBIE_HEAD;
    }

    // ---- helpers ----
    private boolean isParticipant(Player p) {
        GamePhase phase = state.getPhase();
        PlayerState d = registry.get(p.getUniqueId());
        if (phase == GamePhase.LOBBY || phase == GamePhase.COUNTDOWN) {
            // queued in lobby gets protection
            return state.getLobbyPlayers().stream().anyMatch(lp -> lp != null && lp.getUniqueId().equals(p.getUniqueId()));
        }
        return d != null && d.isIngame(); // during RUNNING, only ingame players
    }

    private boolean isZombie(Player p) {
        PlayerState d = registry.get(p.getUniqueId());
        return d != null && d.isIngame() && d.isZombie();
    }

    private boolean isZombieHelmet(ItemStack it) {
        return it != null && it.getType() == zombieHelmType;
    }

    private static boolean isHelmet(ItemStack it) {
        if (it == null) return false;
        String n = it.getType().name();
        return n.endsWith("_HELMET") || n.endsWith("_HEAD") || n.endsWith("_SKULL");
    }


    // ============================================
    // 2) Fallback guards for tricky client actions
    //    (keep hotbar functional everywhere else)
    // ============================================

    // Slot clicks / shift-click / number key swaps
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isParticipant(p)) return;
        PlayerInventory inv = p.getInventory();
        ItemStack worn = inv.getHelmet();

        // A) Any attempt to interact with the armor head slot directly
        if (e.getSlotType() == InventoryType.SlotType.ARMOR) {
            // We only care about head: Bukkit doesn’t expose the exact subslot reliably across views,
            // so detect by the items involved.
            if (isZombieHelmet(worn) || isZombieHelmet(e.getCurrentItem()) || isHelmet(e.getCursor()) || isHelmet(e.getCurrentItem())) {
                e.setCancelled(true);
                return;
            }
        }

        // B) Shift-clicking a helmet (auto-equips to head)
        if (e.isShiftClick() && isHelmet(e.getCurrentItem())) {
            e.setCancelled(true);
            return;
        }

        // C) Number-key hotbar swap with a helmet (would target head if possible)
        if (e.getHotbarButton() != -1) {
            ItemStack hot = inv.getItem(e.getHotbarButton());
            if (isHelmet(hot)) {
                e.setCancelled(true);
                return;
            }
        }

        // D) Swap with cursor (placing helmet onto the head quickly)
        if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR && isHelmet(e.getCursor())) {
            // If they currently wear our zombie head, don’t allow swapping anything into head
            if (isZombieHelmet(worn)) {
                e.setCancelled(true);
                return;
            }
        }

        // E) Don’t allow picking up/moving our zombie head (prevents yanking it out fast)
        if (isZombieHelmet(e.getCurrentItem()) || isZombieHelmet(e.getCursor())) {
            e.setCancelled(true);
        }
    }

    // Drag attempts — cancel if dragging helmets while zombie-head is equipped
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isParticipant(p)) return;
        // Keep hotbar drags allowed unless it’s a helmet that could land on head
        if (isHelmet(e.getOldCursor())) {
            e.setCancelled(true);
        }
    }

    // Right-click equip from hand: block only when it’s a helmet & zombie head is worn
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClickEquip(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isParticipant(p)) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isHelmet(hand) && isZombieHelmet(p.getInventory().getHelmet())) {
            e.setCancelled(true);
        }
    }
}
