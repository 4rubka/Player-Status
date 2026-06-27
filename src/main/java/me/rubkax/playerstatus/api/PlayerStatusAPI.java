package me.rubkax.playerstatus.api;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PlayerStatusAPI {

    private final PlayerStatus plugin;

    public PlayerStatusAPI(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    public Optional<Status> getCurrentStatus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedStatus() == null) return Optional.empty();
        return Optional.ofNullable(plugin.getStatusManager().getStatus(data.getEquippedStatus()));
    }

    public CompletableFuture<Void> setStatus(Player player, Status status) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setEquippedStatus(status.id());
            return plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> removeStatus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setEquippedStatus(null);
            return plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }
        return CompletableFuture.completedFuture(null);
    }

    public Set<String> getOwnedStatuses(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        return data != null ? data.getOwnedStatuses() : Set.of();
    }

    public boolean hasStatus(Player player, String statusId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        return data != null && data.ownsStatus(statusId);
    }

    public CompletableFuture<Void> giveStatus(Player player, String statusId) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addStatus(statusId);
            return plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }
        return CompletableFuture.completedFuture(null);
    }
}
