package dev.astra.guard.listeners;

import dev.astra.guard.managers.AlertManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final AlertManager alertManager;

    public JoinListener(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean enabled = alertManager.isEnabled(player.getUniqueId());
        if (!enabled && player.hasPermission("astraguard.admin.alerts.join")) {
            alertManager.toggle(player.getUniqueId());
        }
    }
}
