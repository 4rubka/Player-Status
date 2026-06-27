package me.rubkax.playerstatus.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderHook extends PlaceholderExpansion {

    private final PlayerStatus plugin;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public PlaceholderHook(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playerstatus";
    }

    @Override
    public @NotNull String getAuthor() {
        return "RubkaX";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    private String toLegacy(String miniMessage) {
        return LEGACY.serialize(PlayerStatus.parse(miniMessage));
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return "";

        switch (params.toLowerCase()) {
            case "current" -> {
                if (data.getEquippedStatus() == null) return "";
                Status status = plugin.getStatusManager().getStatus(data.getEquippedStatus());
                return status != null ? toLegacy(status.display()) : "";
            }
            case "current_minimessage" -> {
                if (data.getEquippedStatus() == null) return "";
                Status status = plugin.getStatusManager().getStatus(data.getEquippedStatus());
                return status != null ? status.display() : "";
            }
            case "raw" -> {
                if (data.getEquippedStatus() == null) return "";
                Status status = plugin.getStatusManager().getStatus(data.getEquippedStatus());
                return status != null ? PlayerStatus.parsePlain(status.display()) : "";
            }
            case "id" -> {
                return data.getEquippedStatus() != null ? data.getEquippedStatus() : "";
            }
            case "count" -> {
                return String.valueOf(data.getOwnedStatuses().size());
            }
            case "total" -> {
                return String.valueOf(plugin.getStatusManager().getStatuses().size());
            }
            default -> {
                if (params.toLowerCase().startsWith("has_")) {
                    String statusId = params.substring(4).toLowerCase();
                    return String.valueOf(data.ownsStatus(statusId));
                }
            }
        }

        return null;
    }
}
