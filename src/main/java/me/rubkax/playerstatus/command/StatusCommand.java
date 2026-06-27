package me.rubkax.playerstatus.command;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.config.ConfigManager;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class StatusCommand implements CommandExecutor, TabCompleter {

    private final PlayerStatus plugin;

    public StatusCommand(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        ConfigManager config = plugin.getConfigManager();

        if (args.length == 0) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage(PlayerStatus.parse("<red>This command can only be used by players."));
                return true;
            }
            if (!player.hasPermission("playerstatus.use")) {
                player.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
                return true;
            }
            plugin.getStatusGUI().open(player, 1);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> handleReload(sender, config);
            case "give" -> handleGive(sender, args, config);
            case "remove" -> handleRemove(sender, args, config);
            case "reset" -> handleReset(sender, args, config);
            case "list" -> handleList(sender, config);
            default -> sender.sendMessage(PlayerStatus.parse(config.getMessage("usage")));
        }

        return true;
    }

    private void handleReload(CommandSender sender, ConfigManager config) {
        if (!sender.hasPermission("playerstatus.reload")) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            return;
        }
        plugin.reload();
        sender.sendMessage(PlayerStatus.parse(config.getMessage("reload-success")));
    }

    private void handleGive(CommandSender sender, String[] args, ConfigManager config) {
        if (!sender.hasPermission("playerstatus.give")) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("usage-give")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PlayerStatus.parse(
                config.getMessage("player-not-found").replace("%player%", args[1])
            ));
            return;
        }

        String statusId = args[2].toLowerCase();
        Status status = plugin.getStatusManager().getStatus(statusId);
        if (status == null) {
            sender.sendMessage(PlayerStatus.parse(
                config.getMessage("status-not-found").replace("%status%", statusId)
            ));
            return;
        }

        plugin.getPlayerDataManager().getOrLoadPlayerData(target.getUniqueId()).thenAccept(data -> {
            data.addStatus(statusId);
            plugin.getPlayerDataManager().savePlayerData(target.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(PlayerStatus.parse(
                    config.getMessage("status-given")
                        .replace("%status%", status.display())
                        .replace("%player%", target.getName())
                ));
                target.sendMessage(PlayerStatus.parse(
                    config.getMessage("status-given-notify")
                        .replace("%status%", status.display())
                ));
            });
        });
    }

    private void handleRemove(CommandSender sender, String[] args, ConfigManager config) {
        if (!sender.hasPermission("playerstatus.remove")) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("usage-remove")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PlayerStatus.parse(
                config.getMessage("player-not-found").replace("%player%", args[1])
            ));
            return;
        }

        String statusId = args[2].toLowerCase();
        Status status = plugin.getStatusManager().getStatus(statusId);
        if (status == null) {
            sender.sendMessage(PlayerStatus.parse(
                config.getMessage("status-not-found").replace("%status%", statusId)
            ));
            return;
        }

        plugin.getPlayerDataManager().getOrLoadPlayerData(target.getUniqueId()).thenAccept(data -> {
            data.removeStatus(statusId);
            plugin.getPlayerDataManager().savePlayerData(target.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(PlayerStatus.parse(
                    config.getMessage("status-removed-admin")
                        .replace("%status%", status.display())
                        .replace("%player%", target.getName())
                ));
                target.sendMessage(PlayerStatus.parse(
                    config.getMessage("status-removed-notify")
                        .replace("%status%", status.display())
                ));

                if (plugin.getTabHook() != null) {
                    plugin.getTabHook().updatePlayer(target);
                }
            });
        });
    }

    private void handleReset(CommandSender sender, String[] args, ConfigManager config) {
        if (!sender.hasPermission("playerstatus.admin")) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("usage-reset")));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PlayerStatus.parse(
                config.getMessage("player-not-found").replace("%player%", args[1])
            ));
            return;
        }

        plugin.getPlayerDataManager().resetPlayerData(target.getUniqueId()).thenRun(() -> {

            plugin.getPlayerDataManager().loadPlayerData(target.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(PlayerStatus.parse(
                    config.getMessage("player-reset").replace("%player%", target.getName())
                ));

                if (plugin.getTabHook() != null) {
                    plugin.getTabHook().updatePlayer(target);
                }
            });
        });
    }

    private void handleList(CommandSender sender, ConfigManager config) {
        if (!sender.hasPermission("playerstatus.admin")) {
            sender.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            return;
        }

        sender.sendMessage(PlayerStatus.parse(config.getRawMessage("list-header")));
        for (Status status : plugin.getStatusManager().getStatuses()) {
            sender.sendMessage(PlayerStatus.parse(
                config.getRawMessage("list-entry")
                    .replace("%id%", status.id())
                    .replace("%display%", status.display())
                    .replace("%price%", String.format("%.0f", status.price()))
            ));
        }
        sender.sendMessage(PlayerStatus.parse(config.getRawMessage("list-footer")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("playerstatus.admin")) {
                completions.addAll(List.of("reload", "give", "remove", "reset", "list"));
            }
            return completions.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("reset")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove")) {
                return plugin.getStatusManager().getStatusMap().keySet().stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return completions;
    }
}
