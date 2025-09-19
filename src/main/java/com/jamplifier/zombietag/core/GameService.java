// core/GameService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.model.PlayerState;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.stats.ConfigStats;


import java.util.*;

public class GameService {
    private final MainClass plugin;
    private final Settings settings;
    private final Spawns spawns;
    private final GameState state;
    private final HelmetService helmets;
    private final EffectsService effects;
    private final RewardService rewards;
    private final PlayerRegistry registry;
    private final ConfigStats stats;
    private StayStillService stayStill; 
    
    public GameService(MainClass plugin, Settings s, Spawns sp, GameState st,
            HelmetService h, EffectsService ef, RewardService rw,
            PlayerRegistry reg, ConfigStats stats) {
this.plugin = plugin; this.settings = s; this.spawns = sp; this.state = st;
this.helmets = h; this.effects = ef; this.rewards = rw;
this.registry = reg; this.stats = stats;
}

    public void setStayStillService(StayStillService ss) { this.stayStill = ss; }

    public void startGameFromLobby() {
    	
    	if (state.isRunning()) return;
    	state.setPhase(GamePhase.RUNNING);
    	state.setGraceEndsAtMs(System.currentTimeMillis() + settings.graceSeconds * 1000L);




        Location gs = spawns.game();
        if (gs == null) { Bukkit.getLogger().severe("Game spawn invalid!"); endGame(true); return; }

        // snapshot current lobby -> gamePlayers
        state.getGamePlayers().clear();
        state.getGamePlayers().addAll(state.getLobbyPlayers());

        // pick and mark initial zombie
        Player init = state.getGamePlayers().get((int) (Math.random() * state.getGamePlayers().size()));
        state.setInitialZombie(init.getUniqueId());

        // mark and prep everyone
        for (Player p : state.getGamePlayers()) {
        	PlayerState pm = registry.get(p.getUniqueId());
            if (pm != null) pm.setIngame(true);
            p.teleport(gs);
        }

        // initial zombie setup
        helmets.giveZombieHelmet(init);
        PlayerState zd = registry.get(init.getUniqueId());
        if (zd != null) zd.setZombie(true);
        effects.applyBlindnessAndNightVision(init, settings.graceSeconds, settings.graceSeconds);
        init.sendMessage("§cYou are the zombie! A grace period is active. Wait to start tagging!");

        // Announcements
        state.getGamePlayers().forEach(p -> p.sendMessage("§aThe game has started! The grace period will last " + settings.graceSeconds + "s."));
        if (settings.announceGameLength) {
            int minutes = Math.max(1, settings.gameLengthSeconds / 60);
            state.getGamePlayers().forEach(p -> p.sendMessage("§eThe game will last for " + minutes + " minutes."));
        }

        // end grace
        new BukkitRunnable() {
            @Override public void run() {
                state.getGamePlayers().forEach(p -> p.sendMessage("§cGrace period is over! Zombies can now tag players!"));
            }
        }.runTaskLater(plugin, settings.graceSeconds * 20L);

        // start systems
        startGameTimer();
        if (settings.stayStillEnabled) stayStill.begin();
    }

    private void startGameTimer() {
        if (state.getGameTimerTask() != null) state.getGameTimerTask().cancel();

        state.setGameTimerTask(new BukkitRunnable() {
            int timeLeft = settings.gameLengthSeconds;
            @Override public void run() {
                if (state.getPhase() != GamePhase.RUNNING) { cancel(); return; }
                if (timeLeft <= 10 && timeLeft > 0) {
                    state.getGamePlayers().forEach(p -> p.sendMessage("§eGame ends in " + timeLeft + " seconds."));
                }
                if (timeLeft-- <= 0) { endGame(false); cancel(); }
            }
        });
        state.getGameTimerTask().runTaskTimer(plugin, 0L, 20L);
    }

    public void endGame(boolean forceToLobby) {
        state.setPhase(GamePhase.ENDING);

        // survivors?
        long survivors = state.getGamePlayers().stream()
        	.map(p -> registry.get(p.getUniqueId()))
            .filter(d -> d != null && d.isIngame() && !d.isZombie())
            .count();

        if (survivors > 0) {
            state.getGamePlayers().forEach(p -> p.sendMessage("§aSurvivors win!"));
            // reward survivors
            state.getGamePlayers().forEach(p -> {
            	PlayerState d = registry.get(p.getUniqueId());
                if (d != null && !d.isZombie()) rewards.rewardIfEnabled(p);
            });
        } else {
            state.getGamePlayers().forEach(p -> p.sendMessage("§cZombies win! All players have been tagged."));
        }

        // restore helmets + teleport
        Location ls = forceToLobby ? spawns.lobby() : spawns.lobby();
        for (Player p : new ArrayList<>(state.getGamePlayers())) {
            helmets.restoreHelmet(p);
            if (ls != null) p.teleport(ls);
            PlayerState pm = registry.get(p.getUniqueId());
            if (pm != null) { pm.setIngame(false); pm.setZombie(false); }
        }

        // reset lists (keep lobby queue if you want: here we keep lobby players as-is)
        state.getGamePlayers().clear();
        state.setInitialZombie(null);

        // stop timers
        if (state.getGameTimerTask() != null) { state.getGameTimerTask().cancel(); state.setGameTimerTask(null); }
        if (state.getStayStillTask() != null) { state.getStayStillTask().cancel(); state.setStayStillTask(null); }

        state.setPhase(GamePhase.LOBBY);
    }

    public boolean areAllZombies() {
        return state.getGamePlayers().stream().allMatch(p -> {
        	PlayerState d = registry.getOrCreate(p.getUniqueId());
            return d != null && d.isZombie();
        });
    }
}
