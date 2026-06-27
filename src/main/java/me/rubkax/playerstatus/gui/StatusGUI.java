package me.rubkax.playerstatus.gui;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.config.ConfigManager;
import me.rubkax.playerstatus.model.PlayerData;
import me.rubkax.playerstatus.model.Status;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StatusGUI implements Listener {

    private final PlayerStatus plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    private final Set<Inventory> openInventories = Collections.newSetFromMap(new WeakHashMap<>());

    public StatusGUI(PlayerStatus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        ConfigManager config = plugin.getConfigManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        if (playerData == null) {

            plugin.getPlayerDataManager().loadPlayerData(player.getUniqueId()).thenAccept(data -> {
                Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, page, data));
            });
            return;
        }

        openGUI(player, page, playerData);
    }

    private void openGUI(Player player, int page, PlayerData playerData) {
        ConfigManager config = plugin.getConfigManager();
        int size = config.getInt("gui.size", 54);
        Component title = PlayerStatus.parse(
            config.getString("gui.title", "<gradient:#FF6B6B:#4ECDC4>Player Status Menu</gradient>")
        );

        Inventory inv = Bukkit.createInventory(null, size, title);
        playerPages.put(player.getUniqueId(), page);

        if (config.getBoolean("gui.fill.enabled", true)) {
            Material fillMat = Material.valueOf(config.getString("gui.fill.material", "GRAY_STAINED_GLASS_PANE"));
            ItemStack filler = new ItemStack(fillMat);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.displayName(PlayerStatus.parse(config.getString("gui.fill.name", " ")));
            filler.setItemMeta(fillerMeta);
            for (int i = 0; i < size; i++) {
                inv.setItem(i, filler);
            }
        }

        List<Status> pageStatuses = plugin.getStatusManager().getStatusesByPage(page);
        for (Status status : pageStatuses) {
            ItemStack item = createStatusItem(status, playerData);
            if (status.slot() >= 0 && status.slot() < size) {
                inv.setItem(status.slot(), item);
            }
        }

        placeButton(inv, config, "gui.buttons.info");
        placeButton(inv, config, "gui.buttons.close");
        placeButton(inv, config, "gui.buttons.remove-status");

        if (page > 1) {
            placeButton(inv, config, "gui.buttons.previous-page");
        }

        if (page < plugin.getStatusManager().getMaxPage()) {
            placeButton(inv, config, "gui.buttons.next-page");
        }

        openInventories.add(inv);
        player.openInventory(inv);

        playSound(player, config.getString("sounds.open-gui", "BLOCK_CHEST_OPEN"));
    }

    private ItemStack createStatusItem(Status status, PlayerData playerData) {
        ConfigManager config = plugin.getConfigManager();
        ItemStack item = new ItemStack(status.icon());
        ItemMeta meta = item.getItemMeta();

        meta.displayName(PlayerStatus.parse(status.display()));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        lore.add(PlayerStatus.parse(config.getRawMessage("lore.description-line")
            .replace("%description%", status.description())));
        lore.add(Component.empty());

        lore.add(PlayerStatus.parse(config.getRawMessage("lore.category-line")
            .replace("%category%", status.category())));

        if (!status.isFree()) {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.price-line")
                .replace("%price%", String.format("%.0f", status.price()))));
        } else {
            lore.add(PlayerStatus.parse("<green>Free"));
        }

        lore.add(Component.empty());

        boolean owned = playerData.ownsStatus(status.id());
        boolean equipped = status.id().equals(playerData.getEquippedStatus());

        if (equipped) {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.status-equipped")));

            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (owned) {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.status-owned")));
        } else {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.status-locked")));
        }

        if (!status.requirements().isEmpty() && !owned) {
            lore.add(Component.empty());
            for (String req : status.requirements()) {
                lore.add(PlayerStatus.parse(config.getRawMessage("lore.requirement-line")
                    .replace("%requirement%", req)));
            }
        }

        lore.add(Component.empty());

        if (owned && !equipped) {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.click-to-equip")));
        } else if (!owned) {
            lore.add(PlayerStatus.parse(config.getRawMessage("lore.click-to-purchase")));
        }
        lore.add(PlayerStatus.parse(config.getRawMessage("lore.click-to-preview")));

        meta.lore(lore);

        if (status.customModelData() > 0) {
            meta.setCustomModelData(status.customModelData());
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void placeButton(Inventory inv, ConfigManager config, String path) {
        int slot = config.getInt(path + ".slot", 0);
        Material mat = Material.valueOf(config.getString(path + ".material", "STONE"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(PlayerStatus.parse(config.getString(path + ".name", "")));

        List<String> loreStrings = config.getConfig().getStringList(path + ".lore");
        if (!loreStrings.isEmpty()) {
            meta.lore(loreStrings.stream().map(PlayerStatus::parse).toList());
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (!openInventories.contains(inv)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        ConfigManager config = plugin.getConfigManager();
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

        int closeSlot = config.getInt("gui.buttons.close.slot", 50);
        int removeSlot = config.getInt("gui.buttons.remove-status.slot", 49);
        int prevSlot = config.getInt("gui.buttons.previous-page.slot", 45);
        int nextSlot = config.getInt("gui.buttons.next-page.slot", 53);

        if (slot == closeSlot) {
            player.closeInventory();
            return;
        }

        if (slot == removeSlot) {
            handleRemoveStatus(player);
            return;
        }

        if (slot == prevSlot && currentPage > 1) {
            playSound(player, config.getString("sounds.page-turn", "ITEM_BOOK_PAGE_TURN"));
            open(player, currentPage - 1);
            return;
        }

        if (slot == nextSlot && currentPage < plugin.getStatusManager().getMaxPage()) {
            playSound(player, config.getString("sounds.page-turn", "ITEM_BOOK_PAGE_TURN"));
            open(player, currentPage + 1);
            return;
        }

        Status status = findStatusAtSlot(slot, currentPage);
        if (status == null) return;

        if (event.getClick() == ClickType.RIGHT) {
            handlePreview(player, status);
        } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
            handlePurchaseOrEquip(player, status);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            Inventory inv = event.getInventory();
            if (openInventories.remove(inv)) {
                playerPages.remove(player.getUniqueId());
            }
        }
    }

    private Status findStatusAtSlot(int slot, int page) {
        for (Status status : plugin.getStatusManager().getStatusesByPage(page)) {
            if (status.slot() == slot) return status;
        }
        return null;
    }

    private void handlePreview(Player player, Status status) {
        ConfigManager config = plugin.getConfigManager();
        player.sendMessage(PlayerStatus.parse(
            config.getMessage("preview-message")
                .replace("%player%", player.getName())
                .replace("%status%", status.display())
        ));
    }

    private void handlePurchaseOrEquip(Player player, Status status) {
        ConfigManager config = plugin.getConfigManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        if (status.hasPermission() && !player.hasPermission(status.permission())) {
            player.sendMessage(PlayerStatus.parse(config.getMessage("no-permission")));
            playSound(player, config.getString("sounds.error", "ENTITY_VILLAGER_NO"));
            return;
        }

        if (!checkRequirements(player, status)) {
            player.sendMessage(PlayerStatus.parse(config.getMessage("requirements-not-met")));
            playSound(player, config.getString("sounds.error", "ENTITY_VILLAGER_NO"));
            return;
        }

        if (playerData.ownsStatus(status.id())) {

            if (status.id().equals(playerData.getEquippedStatus())) {
                player.sendMessage(PlayerStatus.parse(config.getMessage("already-equipped")));
                return;
            }
            playerData.setEquippedStatus(status.id());
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            player.sendMessage(PlayerStatus.parse(
                config.getMessage("status-equipped").replace("%status%", status.display())
            ));
            playSound(player, config.getString("sounds.equip", "ITEM_ARMOR_EQUIP_DIAMOND"));

            if (plugin.getTabHook() != null) {
                plugin.getTabHook().updatePlayer(player);
            }

            open(player, playerPages.getOrDefault(player.getUniqueId(), 1));

        } else {

            if (status.isFree()) {

                playerData.addStatus(status.id());
                playerData.setEquippedStatus(status.id());
                plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
                player.sendMessage(PlayerStatus.parse(
                    config.getMessage("status-purchased").replace("%status%", status.display())
                ));
                playSound(player, config.getString("sounds.purchase", "ENTITY_PLAYER_LEVELUP"));

                if (plugin.getTabHook() != null) {
                    plugin.getTabHook().updatePlayer(player);
                }
                open(player, playerPages.getOrDefault(player.getUniqueId(), 1));
                return;
            }

            if (plugin.getEconomyHook() == null) {
                player.sendMessage(PlayerStatus.parse(config.getMessage("economy-not-available")));
                playSound(player, config.getString("sounds.error", "ENTITY_VILLAGER_NO"));
                return;
            }

            if (!plugin.getEconomyHook().has(player, status.price())) {
                double balance = plugin.getEconomyHook().getBalance(player);
                double needed = status.price() - balance;
                player.sendMessage(PlayerStatus.parse(
                    config.getMessage("not-enough-money")
                        .replace("%price%", String.format("%.0f", needed))
                ));
                playSound(player, config.getString("sounds.error", "ENTITY_VILLAGER_NO"));
                return;
            }

            plugin.getEconomyHook().withdraw(player, status.price());
            playerData.addStatus(status.id());
            playerData.setEquippedStatus(status.id());
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
            player.sendMessage(PlayerStatus.parse(
                config.getMessage("status-purchased").replace("%status%", status.display())
            ));
            playSound(player, config.getString("sounds.purchase", "ENTITY_PLAYER_LEVELUP"));

            if (plugin.getTabHook() != null) {
                plugin.getTabHook().updatePlayer(player);
            }
            open(player, playerPages.getOrDefault(player.getUniqueId(), 1));
        }
    }

    private void handleRemoveStatus(Player player) {
        ConfigManager config = plugin.getConfigManager();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        if (playerData.getEquippedStatus() == null) {
            player.sendMessage(PlayerStatus.parse(config.getMessage("no-status-equipped")));
            return;
        }

        playerData.setEquippedStatus(null);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        player.sendMessage(PlayerStatus.parse(config.getMessage("status-removed")));
        playSound(player, config.getString("sounds.unequip", "ITEM_ARMOR_EQUIP_GENERIC"));

        if (plugin.getTabHook() != null) {
            plugin.getTabHook().updatePlayer(player);
        }

        open(player, playerPages.getOrDefault(player.getUniqueId(), 1));
    }

    private boolean checkRequirements(Player player, Status status) {
        for (String req : status.requirements()) {
            String[] parts = req.split(":", 2);
            if (parts.length != 2) continue;

            switch (parts[0].toLowerCase()) {
                case "level" -> {
                    try {
                        int requiredLevel = Integer.parseInt(parts[1]);
                        if (player.getLevel() < requiredLevel) return false;
                    } catch (NumberFormatException ignored) {}
                }
                case "permission" -> {
                    if (!player.hasPermission(parts[1])) return false;
                }
            }
        }
        return true;
    }

    private void playSound(Player player, String soundName) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(Sound.sound(
                sound.key(),
                Sound.Source.MASTER,
                1.0f, 1.0f
            ));
        } catch (IllegalArgumentException ignored) {}
    }
}
