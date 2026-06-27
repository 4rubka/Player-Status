package me.rubkax.playerstatus.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final PlayerStatus plugin;

    public ChatListener(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfigManager().getBoolean("chat.enabled", true)) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(event.getPlayer().getUniqueId());
        if (data == null || data.getEquippedStatus() == null) return;

        Status status = plugin.getStatusManager().getStatus(data.getEquippedStatus());
        if (status == null) return;

        Component statusComponent = PlayerStatus.parse(" " + status.display());

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component playerName = sourceDisplayName.append(statusComponent);
            return playerName
                .append(PlayerStatus.parse("<gray>: <white>"))
                .append(message);
        });
    }
}
