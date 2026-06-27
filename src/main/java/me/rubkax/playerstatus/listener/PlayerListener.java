package me.rubkax.playerstatus.listener;

import me.rubkax.playerstatus.PlayerStatus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final PlayerStatus plugin;

    public PlayerListener(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer().getUniqueId())
            .thenAccept(data -> {

                if (plugin.getTabHook() != null) {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getTabHook().updatePlayer(event.getPlayer());
                    }, 20L);
                }
            });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
