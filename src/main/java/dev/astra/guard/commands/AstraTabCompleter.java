package dev.astra.guard.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AstraTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {

        if (!(sender.hasPermission("astraguard.admin.reload") ||
                sender.hasPermission("astraguard.admin.alerts") ||
                sender.hasPermission("astraguard.admin.flags"))) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("astraguard.admin.reload")) subCommands.add("reload");
            if (sender.hasPermission("astraguard.admin.alerts")) subCommands.add("alerts");
            if (sender.hasPermission("astraguard.admin.flags")) subCommands.add("flags");
            return filterStartingWith(subCommands, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("flags") && sender.hasPermission("astraguard.admin.flags")) {
                List<String> playerNames = Bukkit.getOfflinePlayers() == null
                        ? Collections.emptyList()
                        : Stream.of(Bukkit.getOfflinePlayers())
                        .filter(p -> p.hasPlayedBefore() || p.isOnline())
                        .map(p -> p.getName() != null ? p.getName() : "")
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toList());
                return filterStartingWith(playerNames, args[1]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterStartingWith(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        String lowerPrefix = prefix.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lowerPrefix)) filtered.add(s);
        }
        return filtered;
    }
}
