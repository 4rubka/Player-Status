package me.rubkax.playerstatus.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    private final UUID playerUuid;
    private final Set<String> ownedStatuses;
    private volatile String equippedStatus;

    public PlayerData(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.ownedStatuses = ConcurrentHashMap.newKeySet();
        this.equippedStatus = null;
    }

    public PlayerData(UUID playerUuid, Set<String> ownedStatuses, String equippedStatus) {
        this.playerUuid = playerUuid;
        this.ownedStatuses = ConcurrentHashMap.newKeySet();
        this.ownedStatuses.addAll(ownedStatuses);
        this.equippedStatus = equippedStatus;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public Set<String> getOwnedStatuses() { return Collections.unmodifiableSet(ownedStatuses); }
    public String getEquippedStatus() { return equippedStatus; }
    public void setEquippedStatus(String statusId) { this.equippedStatus = statusId; }
    public boolean ownsStatus(String statusId) { return ownedStatuses.contains(statusId); }
    public void addStatus(String statusId) { ownedStatuses.add(statusId); }

    public void removeStatus(String statusId) {
        ownedStatuses.remove(statusId);
        if (statusId.equals(equippedStatus)) {
            equippedStatus = null;
        }
    }

    public void clearStatuses() {
        ownedStatuses.clear();
        equippedStatus = null;
    }
}
