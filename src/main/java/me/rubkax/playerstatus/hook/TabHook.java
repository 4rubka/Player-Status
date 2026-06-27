package me.rubkax.playerstatus.hook;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import org.bukkit.entity.Player;

public class TabHook {

    private final PlayerStatus plugin;

    public TabHook(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    public void updatePlayer(Player player) {
        try {

            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Object tabApi = tabApiClass.getMethod("getInstance").invoke(null);
            Object tabPlayer = tabApi.getClass().getMethod("getPlayer", java.util.UUID.class)
                .invoke(tabApi, player.getUniqueId());

        } catch (Exception ignored) {

        }
    }

    public String getFormattedStatus(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null || data.getEquippedStatus() == null) return "";

        Status status = plugin.getStatusManager().getStatus(data.getEquippedStatus());
        if (status == null) return "";

        return " " + status.display();
    }
}
