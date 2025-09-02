package dev.astra.guard.commands;

import dev.astra.guard.Main;
import dev.astra.guard.guis.FlagsGUI;
import dev.astra.guard.managers.AlertManager;
import dev.astra.guard.utils.MiniMessageLegacyParser;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AstraCommand implements CommandExecutor {
    private final Main plugin;
    private final AlertManager alertManager;

    public AstraCommand(Main plugin) {
        this.plugin = plugin;
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (args.length == 0) {
            sender.sendMessage(colorize("<yellow>Usage: /astra <reload|alerts|flags>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("astraguard.admin.reload")) {
                    sender.sendMessage(colorize("<red>You do not have permission to reload."));
                    return true;
                }
                return handleReload(sender);
            }
            case "alerts" -> {
                if (!sender.hasPermission("astraguard.admin.alerts")) {
                    sender.sendMessage(colorize("<red>You do not have permission to toggle alerts."));
                    return true;
                }
                return handleToggleAlerts(sender);
            }
            case "flags" -> {
                if (!sender.hasPermission("astraguard.admin.flags")) {
                    sender.sendMessage(colorize("<red>You do not have permission to view flags."));
                    return true;
                }
                return handleFlags(sender, args);
            }
            default -> {
                sender.sendMessage(colorize("<red>Unknown subcommand."));
                return true;
            }
        }
    }

    private boolean handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reload();
            alertManager.reload();
            sender.sendMessage(colorize("<green>Configuration reloaded."));
            plugin.getLogger().info(sender.getName() + " reloaded the configuration.");
        } catch (Exception e) {
            sender.sendMessage(colorize("<red>Reload failed: " + e.getMessage()));
            plugin.getLogger().severe("Error reloading config: " + e.getMessage());
        }
        return true;
    }

    private boolean handleToggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("<yellow>Only players can toggle alerts."));
            return false;
        }

        boolean enabled = alertManager.toggle(player.getUniqueId());
        String msg = enabled
                ? "&aAstraGuardAlerts » ENABLED"
                : "&7AstraGuardAlerts » DISABLED";
        player.sendMessage(colorize(msg));
        return true;
    }

    private boolean handleFlags(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(colorize("&cOnly players can view GUI."));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(colorize("&eUsage: /astra flags <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(colorize("&cPlayer not found."));
            return true;
        }

        new FlagsGUI(target.getName(), TaskUtil.getFlags(target.getUniqueId())).open(player);
        return true;
    }

    private String colorize(String message) {
        return MiniMessageLegacyParser.parse(message);
    }
}
