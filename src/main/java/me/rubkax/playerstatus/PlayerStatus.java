package me.rubkax.playerstatus;

import me.rubkax.playerstatus.api.PlayerStatusAPI;
import me.rubkax.playerstatus.command.StatusCommand;
import me.rubkax.playerstatus.config.ConfigManager;
import me.rubkax.playerstatus.database.DatabaseManager;
import me.rubkax.playerstatus.gui.StatusGUI;
import me.rubkax.playerstatus.hook.EconomyHook;
import me.rubkax.playerstatus.hook.PlaceholderHook;
import me.rubkax.playerstatus.hook.TabHook;
import me.rubkax.playerstatus.listener.ChatListener;
import me.rubkax.playerstatus.listener.PlayerListener;
import me.rubkax.playerstatus.manager.PlayerDataManager;
import me.rubkax.playerstatus.manager.StatusManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerStatus extends JavaPlugin {

    private static PlayerStatus instance;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private StatusManager statusManager;
    private PlayerDataManager playerDataManager;
    private StatusGUI statusGUI;
    private PlayerStatusAPI api;

    private EconomyHook economyHook;
    private PlaceholderHook placeholderHook;
    private TabHook tabHook;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  PlayerStatus v" + getDescription().getVersion());
        getLogger().info("  Author: RubkaX");
        getLogger().info("═══════════════════════════════════════");

        configManager = new ConfigManager(this);
        configManager.loadAll();

        databaseManager = new DatabaseManager(this, configManager);
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        statusManager = new StatusManager(this, configManager);
        statusManager.loadStatuses();

        playerDataManager = new PlayerDataManager(this, databaseManager);

        statusGUI = new StatusGUI(this);

        api = new PlayerStatusAPI(this);

        setupHooks();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(statusGUI, this);

        var command = getCommand("status");
        if (command != null) {
            var statusCommand = new StatusCommand(this);
            command.setExecutor(statusCommand);
            command.setTabCompleter(statusCommand);
        }

        Bukkit.getOnlinePlayers().forEach(player ->
            playerDataManager.loadPlayerData(player.getUniqueId())
        );

        getLogger().info("PlayerStatus has been enabled successfully!");
        getLogger().info("Loaded " + statusManager.getStatuses().size() + " statuses.");
    }

    @Override
    public void onDisable() {

        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        if (placeholderHook != null) {
            placeholderHook.unregister();
        }

        getLogger().info("PlayerStatus has been disabled.");
        instance = null;
    }

    private void setupHooks() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = new PlaceholderHook(this);
            placeholderHook.register();
            getLogger().info("✔ Hooked into PlaceholderAPI");
        }

        if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
            tabHook = new TabHook(this);
            getLogger().info("✔ Hooked into TAB");
        }

        if (Bukkit.getPluginManager().getPlugin("ExcellentEconomy") != null
                || Bukkit.getPluginManager().getPlugin("Vault") != null) {
            economyHook = new EconomyHook(this);
            if (economyHook.isAvailable()) {
                getLogger().info("✔ Hooked into Economy");
            } else {
                getLogger().warning("Economy plugin found but no provider available.");
                economyHook = null;
            }
        } else {
            getLogger().warning("No economy plugin found. Economy features disabled.");
        }

        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            getLogger().info("✔ Hooked into LuckPerms");
        }
    }

    public void reload() {
        configManager.loadAll();
        statusManager.loadStatuses();
        playerDataManager.invalidateAll();
        Bukkit.getOnlinePlayers().forEach(player ->
            playerDataManager.loadPlayerData(player.getUniqueId())
        );
        getLogger().info("PlayerStatus reloaded successfully.");
    }

    public static Component parse(String miniMessage) {
        return MINI_MESSAGE.deserialize(miniMessage);
    }

    public static Component parse(String miniMessage, String... replacements) {
        String result = miniMessage;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return MINI_MESSAGE.deserialize(result);
    }

    public static String parsePlain(String miniMessage) {
        return MINI_MESSAGE.stripTags(miniMessage);
    }

    public static PlayerStatus getInstance() { return instance; }
    public static MiniMessage getMiniMessage() { return MINI_MESSAGE; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public StatusManager getStatusManager() { return statusManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public StatusGUI getStatusGUI() { return statusGUI; }
    public PlayerStatusAPI getApi() { return api; }
    public EconomyHook getEconomyHook() { return economyHook; }
    public PlaceholderHook getPlaceholderHook() { return placeholderHook; }
    public TabHook getTabHook() { return tabHook; }
}
