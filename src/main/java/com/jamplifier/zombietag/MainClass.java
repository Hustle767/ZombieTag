package com.jamplifier.zombietag;

import com.jamplifier.zombietag.commands.CommandsRouter;
import com.jamplifier.zombietag.config.Settings;
import com.jamplifier.zombietag.config.Spawns;
import com.jamplifier.zombietag.core.*;
import com.jamplifier.zombietag.listeners.*;
import com.jamplifier.zombietag.stats.ConfigStats;
import org.bukkit.plugin.java.JavaPlugin;

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

    // Lang / messages
    private com.jamplifier.zombietag.Util.Lang lang;
    public com.jamplifier.zombietag.Util.Lang getLang() { return lang; }

    @Override
    public void onEnable() {
        getLogger().info("ZombieTag enabled");

        saveDefaultConfig();
        reloadAll();
        registerListeners();

        // /zombietag command + tab completion
        var root = getCommand("zombietag");
        if (root != null) {
            var router = new CommandsRouter(
                this,
                lobbyService,
                gameService,
                stats,
                settings,
                spawns,
                registry
            );
            root.setExecutor(router);
            root.setTabCompleter(new com.jamplifier.zombietag.Util.CommandsTabCompleter(this));
        }

        // PlaceholderAPI expansion
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.jamplifier.zombietag.Util.Placeholders(
                this,
                getStats(),
                getGameState()
            ).register();
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ZombieTag disabled");
        // Best-effort stop timers / cleanup
        if (gameState != null) {
            gameState.clearAll();
        }
    }

    /**
     * Registers all event listeners for this plugin.
     * Called once during onEnable().
     */
    public void registerListeners() {

        // Core tag/infect logic
        getServer().getPluginManager().registerEvents(
            new TagListener(
                this,
                gameState,
                effectsService,
                helmetService,
                gameService,
                registry,
                stats
            ),
            this
        );

        // "No hitting / protected areas / lobby protection"-style rules
        getServer().getPluginManager().registerEvents(
            new LobbyProtectionListener(gameState),
            this
        );

        // WorldGuard border enforcement or escape prevention
        getServer().getPluginManager().registerEvents(
            new WorldGuardListener(registry, gameState),
            this
        );

        // Join/quit/session handling, helmet restore, teleport to lobby, etc.
        getServer().getPluginManager().registerEvents(
            new SessionListener(
                this,
                gameState,
                spawns,
                helmetService,
                gameService,
                registry
            ),
            this
        );

        // Movement / mobility restrictions during active game:
        // - block ender pearls
        // - block chorus fruit
        getServer().getPluginManager().registerEvents(
            new PearlAndChorusListener(
                this,
                gameState,
                registry
            ),
            this
        );

        // - block elytra equip/glide
        getServer().getPluginManager().registerEvents(
            new ElytraListener(
                this,
                gameState,
                registry
            ),
            this
        );

        // If you want helmet locking during the round in future, re-enable this:
        /*
        getServer().getPluginManager().registerEvents(
            new HelmetSlotLockListener(
                gameState,
                registry,
                settings
            ),
            this
        );
        */
     // Command restriction during active game
        getServer().getPluginManager().registerEvents(
            new CommandBlockListener(
                this,
                gameState,
                registry,
                settings
            ),
            this
        );

    }

    /**
     * Reloads config, lang, and rebuilds all services/state.
     * Called once onEnable() and can also be called later by a /reload command if you add one.
     */
    public void reloadAll() {
        reloadConfig();

        // lang.yml manager
        if (lang == null) {
            lang = new com.jamplifier.zombietag.Util.Lang(this);
        } else {
            lang.reload();
        }

        // Load config models
        settings = new Settings(getConfig());
        spawns   = new Spawns(getConfig());
        stats    = new ConfigStats(this);

        // Game core services / registries
        registry        = new PlayerRegistry();
        gameState       = new GameState();
        effectsService  = new EffectsService(this);
        helmetService   = new HelmetService(this, settings, stats);
        rewardService   = new RewardService(settings);

        gameService     = new GameService(
            this,
            settings,
            spawns,
            gameState,
            helmetService,
            effectsService,
            rewardService,
            registry,
            stats
        );

        stayStillService = new StayStillService(
            this,
            settings,
            gameState,
            effectsService,
            helmetService,
            gameService,
            registry
        );
        gameService.setStayStillService(stayStillService);

        lobbyService = new LobbyService(
            this,
            settings,
            spawns,
            gameState
        );
        gameService.setLobbyService(lobbyService);
    }

    // Getters
    public Settings getSettings()        { return settings; }
    public Spawns getSpawns()            { return spawns; }
    public ConfigStats getStats()        { return stats; }
    public PlayerRegistry getRegistry()  { return registry; }
    public GameState getGameState()      { return gameState; }
    public GameService getGameService()  { return gameService; }
    public LobbyService getLobbyService(){ return lobbyService; }
}
