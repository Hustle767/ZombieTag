package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Blocks equipping an elytra and starting to glide with it
 * for active ZombieTag players mid-round.
 */
public class ElytraListener implements Listener {

    private final MainClass plugin;
    private final GameState gameState;
    private final PlayerRegistry registry;

    public ElytraListener(MainClass plugin,
                          GameState gameState,
                          PlayerRegistry registry) {
        this.plugin = plugin;
        this.gameState = gameState;
        this.registry = registry;
    }

    private boolean shouldBlock(Player p) {
        // config toggle first
        if (!plugin.getConfig().getBoolean("restrictions.block_elytra", true)) {
            return false;
        }

        // only if game is live
        if (!gameState.isRunning()) return false;

        PlayerState ps = registry.get(p.getUniqueId());
        return ps != null && ps.isIngame();
    }

    /**
     * Stop players from putting an Elytra in their chest slot
     * (player inventory OR armor slot UI).
     */
    @EventHandler(ignoreCancelled = true)
    public void onEquipAttempt(InventoryClickEvent event) {
        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player player)) return;
        if (!shouldBlock(player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // We only care if they're trying to place an elytra in the chest/armor slot
        // Different clients show armor slots differently, but Bukkit treats
        // the chestplate slot as EquipmentSlot.CHEST which maps to slot index 38 in the Player Inventory.
        // Easiest reliable check: is the item being moved an elytra AND
        // is the slot the chestplate slot in the player's inventory UI.

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Scenario 1: placing an elytra into armor slot
        // - cursor is Elytra, slot is chest slot
        if (cursor != null && cursor.getType() == Material.ELYTRA) {
            // check if the slot they're clicking is the armor chest slot
            if (isChestArmorSlot(event)) {
                event.setCancelled(true);
                player.updateInventory();
                plugin.getLang().actionBar(player, "restrictions.blocked");
                return;
            }
        }

        // Scenario 2: shift-clicking Elytra from inventory into armor slot automatically
        // - current is Elytra, and they're shift-clicking
        if (current != null && current.getType() == Material.ELYTRA && event.isShiftClick()) {
            // If it's a shift-click with Elytra, just hard block if we would block at all
            event.setCancelled(true);
            player.updateInventory();
            plugin.getLang().actionBar(player, "restrictions.blocked");
        }
    }

    /**
     * Stop players from actually gliding, even if they somehow got an elytra equipped.
     */
    @EventHandler(ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.isGliding()) return; // only care about starting glide
        if (!shouldBlock(player)) return;

        // block glide
        event.setCancelled(true);
        player.setGliding(false);
        plugin.getLang().actionBar(player, "restrictions.blocked");
    }

    /**
     * Helper: check if the clicked slot is the player's chest armor slot.
     *
     * Player inventory layout (0-8 hotbar, etc.) is annoying,
     * but Bukkit exposes standardized slot types.
     *
     * We can just check if the clicked slot type is ARMOR and slot is the chest slot.
     */
    private boolean isChestArmorSlot(InventoryClickEvent event) {
        // Quick path:
        // The Bukkit API lets us check the raw slot and view type.
        // Chestplate slot in a Player Inventory view is usually raw slot 38,
        // and slot type ARMOR with InventoryType.PLAYER.
        if (event.getView().getType() != InventoryType.CRAFTING
            && event.getView().getType() != InventoryType.PLAYER) {
            return false; // not their personal inventory/armor view
        }

        // Some servers/plugins remap, but vanilla player inv:
        // 36 boots, 37 leggings, 38 chest, 39 helmet (sometimes reversed helmet<->boots on some impls,
        // but Paper 1.21+ is standard boots=36 ... helmet=39).
        int raw = event.getRawSlot();
        return (raw == 38);
    }
}
