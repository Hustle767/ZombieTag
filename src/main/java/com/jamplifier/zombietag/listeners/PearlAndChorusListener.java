package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.core.GameState;
import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Blocks:
 * - Ender pearl throws
 * - Chorus fruit teleports
 *
 * Only applies if:
 * - GameState is RUNNING
 * - PlayerState.isIngame() == true
 * - restriction toggle in config is true
 */
public class PearlAndChorusListener implements Listener {

    private final MainClass plugin;
    private final GameState gameState;
    private final PlayerRegistry registry;

    public PearlAndChorusListener(MainClass plugin,
                                  GameState gameState,
                                  PlayerRegistry registry) {
        this.plugin = plugin;
        this.gameState = gameState;
        this.registry = registry;
    }

    /**
     * Block the attempt to USE pearls or chorus fruit (right click).
     * Handles main hand and off hand.
     *
     * Note: we intentionally do NOT use ignoreCancelled=true here,
     * because some plugins touch this event first and we still
     * want to be able to cancel it hard.
     */
    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        // Figure out what item they're actually trying to use.
        // On modern Paper: event.getItem() can be null for off-hand,
        // so we check which hand fired this event.
        ItemStack used;
        if (event.getHand() == EquipmentSlot.HAND) {
            used = player.getInventory().getItemInMainHand();
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            used = player.getInventory().getItemInOffHand();
        } else {
            used = event.getItem(); // fallback, should basically never matter
        }

        if (used == null) return;
        Material type = used.getType();

        boolean isPearl  = (type == Material.ENDER_PEARL);
        boolean isChorus = (type == Material.CHORUS_FRUIT);

        // Not something we care about? bail.
        if (!isPearl && !isChorus) return;

        // Respect toggles in config
        if (isPearl && !plugin.getConfig().getBoolean("restrictions.block_enderpearls", true)) {
            return;
        }
        if (isChorus && !plugin.getConfig().getBoolean("restrictions.block_chorus_fruit", true)) {
            return;
        }

        // Only if the round is actually active
        if (!gameState.isRunning()) return;

        PlayerState ps = registry.get(player.getUniqueId());
        if (ps == null || !ps.isIngame()) return;

        // Stop the use
        event.setCancelled(true);

        // Force-resync inventory client-side so they don't "lose" the pearl/chfruit visually
        player.updateInventory();

        // Tell them no
        plugin.getLang().actionBar(player, "restrictions.blocked");
    }

    /**
     * Chorus fruit teleport isn't 100% stopped just by cancelling interact.
     * Minecraft will fire a PlayerTeleportEvent with cause == CHORUS_FRUIT.
     * We kill it here too.
     */
    @EventHandler
    public void onChorusTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }

        Player player = event.getPlayer();

        // Check toggle
        if (!plugin.getConfig().getBoolean("restrictions.block_chorus_fruit", true)) {
            return;
        }

        // Only if game is running
        if (!gameState.isRunning()) return;

        PlayerState ps = registry.get(player.getUniqueId());
        if (ps == null || !ps.isIngame()) return;

        // Kill the teleport
        event.setCancelled(true);

        plugin.getLang().actionBar(player, "restrictions.blocked");
    }
}
