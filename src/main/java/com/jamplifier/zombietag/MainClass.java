package com.jamplifier.zombietag;

import com.jamplifier.zombietag.commands.CommandsRouter;
import com.jamplifier.zombietag.model.PlayerState;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import com.jamplifier.zombietag.listeners.*;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.plugin.java.JavaPlugin;
import com.jamplifier.zombietag.core.PlayerRegistry;


public class MainClass extends JavaPlugin {

    // Config/cache objects
    private Settings settings;
    private Spawns spawns;

    // Persistent stats in config.yml
    private ConfigStats stats;

    // Game state + registries/services
    private PlayerRegistry registry; 
    private GameState gameState;
    private EffectsService effectsService;
    private HelmetService helmetService;
    private RewardService rewardService;
    private StayStillService stayStillService;
    private GameService gameService;
    private LobbyService lobbyService;

    @Override
    public void onEnable() {
        getLogger().info("ZombieTag enabled");
        saveDefaultConfig();
        reloadAll();

        // Listeners
        getServer().getPluginManager().registerEvents(
            new TagListener(this, gameState, effectsService, helmetService, gameService, registry, stats), this);
        getServer().getPluginManager().registerEvents(
            new InventoryGuardListener(settings, registry), this);
        getServer().getPluginManager().registerEvents(
            new WorldGuardListener(registry, gameState), this);
        getServer().getPluginManager().registerEvents(
            new SessionListener(this, gameState, spawns, helmetService, gameService, registry), this);

        // Single root command that routes to player/admin handlers
        getCommand("zombietag").setExecutor(new CommandsRouter(this, lobbyService, gameService, stats, settings, spawns, registry));
    }

    @Override
    public void onDisable() {
        getLogger().info("ZombieTag disabled");
        // Best-effort stop timers
        if (gameState != null) gameState.clearAll();
    }

    public void reloadAll() {
        reloadConfig();

        // Config
        settings = new Settings(getConfig());
        spawns   = new Spawns(getConfig());
        stats    = new ConfigStats(this);

        // Game core
        registry      = new PlayerRegistry();
        gameState     = new GameState();
        effectsService= new EffectsService(this);
        helmetService = new HelmetService(this, settings, stats);
        rewardService = new RewardService(settings);
        gameService   = new GameService(this, settings, spawns, gameState,
                            helmetService, effectsService, rewardService, registry, stats);
        stayStillService = new StayStillService(this, settings, gameState,
                            effectsService, helmetService, gameService, registry);
        gameService.setStayStillService(stayStillService);
        lobbyService  = new LobbyService(this, settings, spawns, gameState);
    }

    // Getters if you need them elsewhere
    public Settings getSettings() { return settings; }
    public Spawns getSpawns() { return spawns; }
    public ConfigStats getStats() { return stats; }
    public PlayerRegistry getRegistry() { return registry; }
    public GameState getGameState() { return gameState; }
    public GameService getGameService() { return gameService; }
    public LobbyService getLobbyService() { return lobbyService; }
}
