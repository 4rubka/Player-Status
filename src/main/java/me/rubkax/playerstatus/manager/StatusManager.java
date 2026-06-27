package me.rubkax.playerstatus.manager;

import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.config.ConfigManager;
import me.rubkax.playerstatus.model.Status;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatusManager {

    private final PlayerStatus plugin;
    private final ConfigManager configManager;
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();
    private int maxPage = 1;

    public StatusManager(PlayerStatus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void loadStatuses() {
        statuses.clear();
        maxPage = 1;

        ConfigurationSection section = configManager.getStatusesConfig().getConfigurationSection("statuses");
        if (section == null) {
            plugin.getLogger().warning("No statuses found in statuses.yml!");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection statusSection = section.getConfigurationSection(id);
            if (statusSection == null) continue;

            try {
                Material icon = Material.valueOf(statusSection.getString("icon", "PAPER").toUpperCase());
                int page = statusSection.getInt("page", 1);

                Status status = new Status(
                    id,
                    statusSection.getString("display", "<white>" + id),
                    statusSection.getString("description", ""),
                    icon,
                    statusSection.getDouble("price", 0),
                    statusSection.getString("permission", ""),
                    statusSection.getString("category", "default"),
                    statusSection.getInt("slot", 0),
                    page,
                    statusSection.getInt("custom-model-data", 0),
                    statusSection.getStringList("requirements")
                );

                statuses.put(id, status);
                if (page > maxPage) maxPage = page;

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for status '" + id + "': " + statusSection.getString("icon"));
            }
        }

        plugin.getLogger().info("Loaded " + statuses.size() + " statuses across " + maxPage + " pages.");
    }

    public Status getStatus(String id) {
        return statuses.get(id);
    }

    public Collection<Status> getStatuses() {
        return Collections.unmodifiableCollection(statuses.values());
    }

    public Map<String, Status> getStatusMap() {
        return Collections.unmodifiableMap(statuses);
    }

    public List<Status> getStatusesByPage(int page) {
        return statuses.values().stream()
            .filter(s -> s.page() == page)
            .sorted(Comparator.comparingInt(Status::slot))
            .collect(Collectors.toList());
    }

    public int getMaxPage() {
        return maxPage;
    }

    public List<Status> getStatusesByCategory(String category) {
        return statuses.values().stream()
            .filter(s -> s.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    public Set<String> getCategories() {
        return statuses.values().stream()
            .map(Status::category)
            .collect(Collectors.toSet());
    }
}
