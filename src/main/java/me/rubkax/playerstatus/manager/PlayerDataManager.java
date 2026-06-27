package me.rubkax.playerstatus.manager;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.database.DatabaseManager;
import me.rubkax.playerstatus.model.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {

    private final PlayerStatus plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(PlayerStatus plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return databaseManager.loadPlayerData(uuid).thenApply(data -> {
            cache.put(uuid, data);
            return data;
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, ex);
            PlayerData fallback = new PlayerData(uuid);
            cache.put(uuid, fallback);
            return fallback;
        });
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public CompletableFuture<PlayerData> getOrLoadPlayerData(UUID uuid) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return loadPlayerData(uuid);
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return databaseManager.savePlayerData(data);
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            databaseManager.savePlayerData(data);
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            try {
                databaseManager.savePlayerData(entry.getValue()).join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + entry.getKey(), e);
            }
        }
    }

    public void invalidateAll() {
        saveAll();
        cache.clear();
    }

    public CompletableFuture<Void> resetPlayerData(UUID uuid) {
        cache.remove(uuid);
        return databaseManager.resetPlayerData(uuid);
    }
}
