// core/GameService.java
package com.jamplifier.zombietag.core;

import com.jamplifier.zombietag.MainClass;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.model.PlayerState;
import static com.jamplifier.zombietag.Util.Lang.m;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.jamplifier.zombietag.core.PlayerRegistry;
import com.jamplifier.zombietag.stats.ConfigStats;

import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
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
    private LobbyService lobby;
    public void setLobbyService(LobbyService lobby) { this.lobby = lobby; }

    
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


        // Defensive: stop any leftover lobby countdown
        if (state.getLobbyCountdownTask() != null) {
            try { state.getLobbyCountdownTask().cancel(); } catch (Throwable ignored) {}
            state.setLobbyCountdownTask(null);
        }
        // Snapshot first
        List<Player> snapshot = new ArrayList<>(state.getLobbyPlayers());
        snapshot.removeIf(p -> p == null || !p.isOnline());         // safety

        if (snapshot.isEmpty()) {
            Bukkit.getLogger().warning("[ZombieTag] No players to start a game.");
            state.setPhase(GamePhase.LOBBY);
            return;
        }

        state.setPhase(GamePhase.RUNNING);
        state.setGraceEndsAtMs(System.currentTimeMillis() + settings.graceSeconds * 1000L);

        Location gs = spawns.game();
        if (gs == null) {
            Bukkit.getLogger().severe("[ZombieTag] Game spawn not set. Use /zt setspawn game");
            state.setPhase(GamePhase.LOBBY); // just revert; don't trigger end/reset
            plugin.getLang().send(state.getLobbyPlayers(), "lobby.missing_game_spawn");
            return;
        }

        // gamePlayers := snapshot, lobby := empty (clean separation)
        state.getGamePlayers().clear();
        state.getGamePlayers().addAll(snapshot);
        state.getLobbyPlayers().clear();

        // Reset everyone to a known state, heal + TP
        for (Player p : state.getGamePlayers()) {
            PlayerState ps = registry.getOrCreate(p.getUniqueId());
            ps.setIngame(true);
            ps.setZombie(false);
            healAndReset(p);
            p.teleport(gs);
        }

        // Pick zombie AFTER reset/teleport
        Player init = state.getGamePlayers().get(
                java.util.concurrent.ThreadLocalRandom.current().nextInt(state.getGamePlayers().size())
        );
        state.setInitialZombie(init.getUniqueId());

        // Give zombie gear/effects + flag
        helmets.giveZombieHelmet(init);
        PlayerState zd = registry.getOrCreate(init.getUniqueId());
        zd.setZombie(true);
        effects.applyBlindnessAndNightVision(init, settings.graceSeconds, settings.graceSeconds);

        // ... (rest of your titles/chat/grace message) ...

     // Chat message
        init.sendMessage(
            Component.text("You are the zombie! A grace period is active. Wait to start tagging!")
                    .color(NamedTextColor.RED)
        );

        // Title + subtitle
        Title.Times times = Title.Times.times(
            Duration.ofMillis(200),  // fade in
            Duration.ofSeconds(3),   // stay
            Duration.ofMillis(500)   // fade out
        );

        init.showTitle(Title.title(
            Component.text("You are the ZOMBIE!!").color(NamedTextColor.RED),
            Component.text("Tag others till there are no survivors!").color(NamedTextColor.YELLOW),
            times
        ));

     // Resolve zombie + name
        UUID zid = state.getInitialZombie();
        Player zombie = (zid != null) ? Bukkit.getPlayer(zid) : null;
        String zName = (zombie != null) ? zombie.getName() : "Unknown";

        // Build one concise chat line
        Component chat = Component.text("Game started! ", NamedTextColor.GREEN)
            .append(Component.text("Zombie: ", NamedTextColor.GOLD))
            .append(Component.text(zName, NamedTextColor.RED))
            .append(Component.text(" • Grace: " + settings.graceSeconds + "s", NamedTextColor.GRAY));

        if (settings.announceGameLength) {
            int minutes = Math.max(1, settings.gameLengthSeconds / 60);
            chat = chat.append(Component.text(" • Length: " + minutes + "m", NamedTextColor.GRAY));
        }

        // Send chat + per-player title
        Title.Times times1 = Title.Times.times(
            Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500)
        );

        for (Player p : state.getGamePlayers()) {
            p.sendMessage(chat);

            if (zombie != null && p.getUniqueId().equals(zombie.getUniqueId())) {
                // Zombie’s title
                p.showTitle(Title.title(
                    Component.text("You are the ZOMBIE!!", NamedTextColor.RED),
                    Component.text("Wait " + settings.graceSeconds + "s, then TAG!", NamedTextColor.YELLOW),
                    times1
                ));
            } else {
                // Survivors’ title
                p.showTitle(Title.title(
                    Component.text(zName + " is the ZOMBIE!", NamedTextColor.RED),
                    Component.text("Run! Grace " + settings.graceSeconds + "s", NamedTextColor.YELLOW),
                    times1
                ));
            }
        }

        // end grace
        new BukkitRunnable() {
            @Override public void run() {
            	plugin.getLang().send(state.getGamePlayers(), "game.grace_over");

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
                	plugin.getLang().send(state.getGamePlayers(), "game.ends_in", m("seconds", timeLeft));
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
        	plugin.getLang().send(state.getGamePlayers(), "game.survivors_win");

            // reward survivors
            state.getGamePlayers().forEach(p -> {
                PlayerState d = registry.get(p.getUniqueId());
                if (d != null && !d.isZombie()) rewards.rewardIfEnabled(p);
            });
        } else {
        	plugin.getLang().send(state.getGamePlayers(), "game.zombies_win");

        }

        // Snapshot who finished this round (so we can optionally merge them into the lobby)
        List<Player> returned = new ArrayList<>(state.getGamePlayers());

        // Restore helmets + teleport everyone to lobby spawn
        Location ls = spawns.lobby();
        for (Player p : returned) {
            helmets.restoreHelmet(p);
            if (ls != null) p.teleport(ls);
            PlayerState pm = registry.get(p.getUniqueId());
            if (pm != null) { pm.setIngame(false); pm.setZombie(false); }
        }

        // Clear game state
        state.getGamePlayers().clear();
        state.setInitialZombie(null);

        // Stop timers
        if (state.getGameTimerTask() != null) { state.getGameTimerTask().cancel(); state.setGameTimerTask(null); }
        if (state.getStayStillTask() != null) { state.getStayStillTask().cancel(); state.setStayStillTask(null); }

        // Back to lobby phase
        state.setPhase(GamePhase.LOBBY);

        // ---- Preserve any players who queued during the game ----
        // Clean lobby list: remove offline/null and de-dup by UUID (keep first occurrence)
        state.getLobbyPlayers().removeIf(pl -> pl == null || !pl.isOnline());
        {
            java.util.Set<java.util.UUID> seen = new java.util.HashSet<>();
            state.getLobbyPlayers().removeIf(pl -> !seen.add(pl.getUniqueId()));
        }

        // Merge returned players only when auto-rejoin is enabled
        if (settings.autoRejoin) {
            returned.removeIf(pl -> pl == null || !pl.isOnline());
            for (Player pl : returned) {
                if (state.getLobbyPlayers().stream().noneMatch(x -> x.getUniqueId().equals(pl.getUniqueId()))) {
                    state.getLobbyPlayers().add(pl);
                }
            }
        } else {
            // Auto-rejoin OFF: do NOT add returned players; just inform them how to queue again
            for (Player p : returned) {
                if (p != null && p.isOnline()) {
                	plugin.getLang().send(p, "end.queue_hint");

                }
            }
        }

        // Clear stale countdown ref if any
        if (state.getLobbyCountdownTask() != null && state.getLobbyCountdownTask().isCancelled()) {
            state.setLobbyCountdownTask(null);
        }

        // Try to start/continue countdown next tick (safe no-op if below threshold or wrong phase)
        if (lobby != null) {
            Bukkit.getScheduler().runTask(plugin, lobby::maybeStartCountdown);
        }
    }


    public boolean areAllZombies() {
        return state.getGamePlayers().stream().allMatch(p -> {
        	PlayerState d = registry.getOrCreate(p.getUniqueId());
            return d != null && d.isZombie();
        });
    }
    private void healAndReset(Player p) {
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExhaustion(0f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAbsorptionAmount(0.0);
        p.setFreezeTicks(0);
        p.getActivePotionEffects().forEach(pe -> p.removePotionEffect(pe.getType()));
    }


}
