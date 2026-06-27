package me.rubkax.playerstatus.model;

import org.bukkit.Material;
import java.util.List;

public record Status(
    String id,
    String display,
    String description,
    Material icon,
    double price,
    String permission,
    String category,
    int slot,
    int page,
    int customModelData,
    List<String> requirements
) {
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public boolean isFree() {
        return price <= 0;
    }
}
