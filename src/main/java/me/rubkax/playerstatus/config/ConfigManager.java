package me.rubkax.playerstatus.config;

import me.rubkax.playerstatus.PlayerStatus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {

    private final PlayerStatus plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration statusesConfig;
    private File messagesFile;
    private File statusesFile;

    public ConfigManager(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        statusesFile = new File(plugin.getDataFolder(), "statuses.yml");
        if (!statusesFile.exists()) {
            plugin.saveResource("statuses.yml", false);
        }
        statusesConfig = YamlConfiguration.loadConfiguration(statusesFile);

        plugin.getLogger().info("Configuration files loaded.");
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getStatusesConfig() {
        return statusesConfig;
    }

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "");
        String message = messagesConfig.getString(key, "<red>Missing message: " + key);
        return prefix + message;
    }

    public String getRawMessage(String key) {
        return messagesConfig.getString(key, "<red>Missing message: " + key);
    }

    public String getString(String path, String def) {
        return plugin.getConfig().getString(path, def);
    }

    public int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return plugin.getConfig().getBoolean(path, def);
    }
}
