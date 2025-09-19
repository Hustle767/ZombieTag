// core/StayStillService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.model.PlayerState;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class StayStillService {
    private final MainClass plugin;
    private final Settings settings;
    private final GameState state;
    private final EffectsService effects;
    private final HelmetService helmets;
    private final GameService game;
    private final PlayerRegistry registry;

    private final Map<UUID, Integer> timers = new HashMap<>();
    private final Map<UUID, Location> lastLoc = new HashMap<>();

    public StayStillService(MainClass plugin, Settings s, GameState st,
                            EffectsService ef, HelmetService h, GameService g, PlayerRegistry registry) {
        this.plugin = plugin; this.settings = s; this.state = st;
        this.effects = ef; this.helmets = h; this.game = g; this.registry = registry;
    }

    public void begin() {
        if (state.getStayStillTask() != null) state.getStayStillTask().cancel();

        BukkitRunnable task = new BukkitRunnable() {
            @Override public void run() {
                if (state.getPhase() != GamePhase.RUNNING) return;

                for (Player p : new ArrayList<>(state.getGamePlayers())) {
                	PlayerState d = registry.get(p.getUniqueId());
                    if (d != null && d.isZombie()) continue;

                    Location cur = p.getLocation();
                    Location last = lastLoc.get(p.getUniqueId());
                    if (last != null && cur.distanceSquared(last) < 0.01) {
                        int left = timers.getOrDefault(p.getUniqueId(), settings.stayStillSeconds);
                        if (left > 0) {
                            timers.put(p.getUniqueId(), left - 1);
                            if (left <= 7) p.sendMessage("§cMove or you'll turn into a zombie in §e" + left + " seconds!");
                        } else {
                            p.sendMessage(settings.stayStillMessage);
                            // turn into zombie (minimal inline to avoid cycles)
                            if (d != null) d.setZombie(true);
                            effects.applyBlindnessAndNightVision(p, 10, 10);
                            helmets.giveZombieHelmet(p);
                            state.getGamePlayers().forEach(gp -> gp.sendMessage("§c" + p.getName() + " has been turned into a zombie!"));
                            timers.remove(p.getUniqueId());
                            if (game.areAllZombies()) {
                                state.getGamePlayers().forEach(gp -> gp.sendMessage("§cAll players turned! Ending game."));
                                game.endGame(false);
                            }
                        }
                    } else {
                        timers.remove(p.getUniqueId());
                    }
                    lastLoc.put(p.getUniqueId(), cur.clone());
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        state.setStayStillTask(task);
    }

    public void clear() { timers.clear(); lastLoc.clear(); }
}
