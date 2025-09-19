// listeners/TagListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.stats.ConfigStats;
import com.jamplifier.zombietag.core.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TagListener implements Listener {
    private final MainClass plugin;
    private final GameState state;
    private final EffectsService effects;
    private final HelmetService helmets;
    private final GameService game;
    private final PlayerRegistry registry;
    private final ConfigStats stats;


    public TagListener(MainClass plugin,
            GameState st,
            EffectsService ef,
            HelmetService h,
            GameService g,
            PlayerRegistry reg,
            ConfigStats stats) {
this.plugin = plugin;
this.state = st;
this.effects = ef;
this.helmets = h;
this.game = g;
this.registry = reg;   // NEW
this.stats = stats;    // NEW
}


    @EventHandler(ignoreCancelled = true)
    public void onTag(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player tagged) || !(e.getDamager() instanceof Player tagger)) return;
        if (state.getPhase() != GamePhase.RUNNING) return;

        PlayerState td = registry.getOrCreate(tagger.getUniqueId());
        PlayerState vd = registry.getOrCreate(tagged.getUniqueId());

        if (td == null || vd == null) return;
        if (!td.isIngame() || !vd.isIngame()) return;
        //Grace Period
        if (System.currentTimeMillis() < state.getGraceEndsAtMs()) {
            tagger.sendMessage("§cYou can’t tag during grace!");
            e.setCancelled(true); return;
        }

        // Only zombies can tag
        if (!td.isZombie()) {
            tagger.sendMessage("§cYou are a survivor and cannot tag players!");
            e.setCancelled(true); return;
        }
        // Can’t tag zombies
        if (!td.isZombie()) {
            tagger.sendMessage("§cYou cannot tag another zombie!");
            e.setCancelled(true); return;
        }

        // Tag success -> turn victim
        vd.setZombie(true);
        effects.applyBlindnessAndNightVision(tagged, 10, 10);
        helmets.giveZombieHelmet(tagged);
        stats.addInt(tagger.getUniqueId(), "tags", 1);

        state.getGamePlayers().forEach(p -> p.sendMessage("§c" + tagged.getName() + " has been tagged and turned into a zombie!"));
        tagged.sendActionBar("§cYou have been tagged");

        if (game.areAllZombies()) {
            state.getGamePlayers().forEach(p -> p.sendMessage("§cAll players turned! Ending game."));
            game.endGame(false);
        }

        e.setCancelled(true);
    }
}
