// listeners/TagListener.java
package com.jamplifier.zombietag.listeners;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.stats.ConfigStats;
import com.jamplifier.zombietag.core.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import static com.jamplifier.zombietag.Util.Lang.m;

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
this.registry = reg;   
this.stats = stats;    
}

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTag(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player tagged) || !(e.getDamager() instanceof Player tagger)) return;

        // Only during running phase
        if (state.getPhase() != GamePhase.RUNNING) return;

        // Get or create states
        PlayerState td = registry.getOrCreate(tagger.getUniqueId());
        PlayerState vd = registry.getOrCreate(tagged.getUniqueId());

        // Re-sync "ingame" from authoritative gamePlayers list by UUID (not Player ref)
        boolean atkIn = state.getGamePlayers().stream().anyMatch(pl -> pl.getUniqueId().equals(tagger.getUniqueId()));
        boolean tgtIn = state.getGamePlayers().stream().anyMatch(pl -> pl.getUniqueId().equals(tagged.getUniqueId()));
        if (td.isIngame() != atkIn) td.setIngame(atkIn);
        if (vd.isIngame() != tgtIn) vd.setIngame(tgtIn);

        // Only ingame vs ingame interactions count
        if (!td.isIngame() || !vd.isIngame()) return;

        // Survivors can't tag
        if (!td.isZombie()) {
        	plugin.getLang().send(tagger, "tag.survivor_cannot_tag");
            e.setDamage(0.0);
            e.setCancelled(true);
            return;
        }

        // Grace window
        if (System.currentTimeMillis() < state.getGraceEndsAtMs()) {
        	plugin.getLang().send(tagger, "tag.no_tag_during_grace");
            e.setDamage(0.0);
            e.setCancelled(true);
            return;
        }

        // Zombies can't tag zombies
        if (vd.isZombie()) {
        	plugin.getLang().send(tagger, "tag.cannot_tag_zombie");
            e.setDamage(0.0);
            e.setCancelled(true);
            return;
        }

        // Convert victim to zombie
        vd.setZombie(true);
        effects.applyBlindnessAndNightVision(tagged, 10, 10);
        helmets.giveZombieHelmet(tagged);
        stats.addInt(tagger.getUniqueId(), "tags", 1);

        plugin.getLang().send(state.getGamePlayers(), "tag.converted_broadcast", m("victim", tagged.getName()));
        plugin.getLang().actionBar(tagged, "tag.converted_actionbar");

        // No knockback/damage
        e.setDamage(0.0);
        e.setCancelled(true);

        if (game.areAllZombies()) {
            plugin.getLang().send(state.getGamePlayers(), "game.all_turned_end");
            game.endGame(false);
        }
    }

}