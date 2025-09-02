package dev.astra.guard.managers;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertManager {
    private final Set<UUID> toggled;

    public AlertManager() {
        this.toggled = ConcurrentHashMap.newKeySet();
    }

    public boolean toggle(UUID uuid) {
        if (!toggled.remove(uuid)) {
            toggled.add(uuid);
            return true;
        }
        return false;
    }

    public boolean isEnabled(UUID uuid) {
        return toggled.contains(uuid);
    }

    public void reload() {
        toggled.clear();
    }
}
