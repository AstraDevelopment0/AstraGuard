package dev.astra.guard.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.CacheLoader;
import dev.astra.guard.LibraryDownloader;
import dev.astra.guard.LibraryLoader;
import dev.astra.guard.Main;
import dev.astra.guard.config.ConfigManager;
import dev.astra.guard.modules.CrashFlag;
import dev.astra.guard.webhook.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class TaskUtil {

    private static Main plugin;
    private static LoadingCache<UUID, LongAdder> violations;
    private static Connection connection;

    public TaskUtil(Main plugin) {
        TaskUtil.plugin = plugin;
        loadCache();
        connectSQLite();
        createTable();
    }

    private void connectSQLite() {
        File dbFile = new File(plugin.getDataFolder(), "flags.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException ex) {
            if (ex.getMessage().contains("No suitable driver found")) {
                try {
                    File libFolder = new File(plugin.getDataFolder(), "libraries");
                    if (!libFolder.exists() && !libFolder.mkdirs()) {
                        plugin.getLogger().severe("Failed to create 'libraries' folder for SQLite JDBC!");
                        return;
                    }

                    LibraryDownloader.downloadIfNotExists(libFolder);

                    LibraryLoader loader = new LibraryLoader(plugin);
                    loader.loadAllLibraries();

                    Class.forName("org.sqlite.JDBC");

                    connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load SQLite JDBC dynamically: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().severe("Failed to connect to SQLite: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }


    private void createTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS flags (" +
                    "uuid TEXT NOT NULL," +
                    "reason TEXT NOT NULL," +
                    "timestamp LONG NOT NULL," +
                    "clientBrand TEXT NOT NULL" +
                    ");");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }

    private void loadCache() {
        ConfigManager cfg = plugin.getConfigManager();
        violations = CacheBuilder.newBuilder()
                .expireAfterAccess(cfg.getViolationResetMinutes(), TimeUnit.MINUTES)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull LongAdder load(@NotNull UUID key) {
                        return new LongAdder();
                    }
                });
    }

    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    public static void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runLater(Runnable task, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public static void flag(Player player, String check, String detail) {
        if (plugin == null || violations == null) return;
        if (player.hasPermission("astraguard.bypass")) return;

        ConfigManager cfg = plugin.getConfigManager();
        int max = cfg.getMaxViolations();
        UUID uuid = player.getUniqueId();

        LongAdder counter;
        try {
            counter = violations.get(uuid);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load violation counter for " + player.getName());
            return;
        }

        int rawCount = counter.intValue();
        if (rawCount < max) {
            counter.increment();
            rawCount++;
        }

        if (rawCount >= max) {
            violations.invalidate(uuid);

            String logMsg = cfg.formatLog(
                    cfg.getFlagLogFormat(),
                    player.getName(), check, detail,
                    max, max
            );
            plugin.getLogger().warning(logMsg.replaceAll("§.", ""));

            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("astraguard.alerts"))
                    .filter(p -> plugin.getAlertManager().isEnabled(p.getUniqueId()))
                    .forEach(p -> p.sendMessage(logMsg));

            WebhookUtil.sendPlayerKickedWebhook(player.getName(), check, detail);

            runSync(() -> {
                if (player.isOnline()) {
                    String kickMsg = cfg.getKickMessage().replace("{check}", check);
                    player.kickPlayer(kickMsg);
                    addFlag(player, check + " | " + detail, player.getClientBrandName());
                }
            });
            return;
        }

        String logMsg = cfg.formatLog(
                cfg.getFlagLogFormat(),
                player.getName(), check, detail,
                rawCount, max
        );
        plugin.getLogger().warning(logMsg.replaceAll("§.", ""));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("astraguard.alerts"))
                .filter(p -> plugin.getAlertManager().isEnabled(p.getUniqueId()))
                .forEach(p -> p.sendMessage(logMsg));

        WebhookUtil.sendCheckTriggeredWebhook(player.getName(), check, detail, rawCount, max);
    }

    public void reloadConfig() {
        loadCache();
    }

    public static void addFlag(Player player, String reason, String clientBrand) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flags (uuid, reason, timestamp, clientBrand) VALUES (?, ?, ?, ?)")) {
            stmt.setString(1, player.getUniqueId().toString());
            stmt.setString(2, reason);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, clientBrand != null ? clientBrand : "Unknown");
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert flag: " + e.getMessage());
        }
    }

    public static List<CrashFlag> getFlags(UUID uuid) {
        List<CrashFlag> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT reason, timestamp, clientBrand FROM flags WHERE uuid=?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new CrashFlag(
                            rs.getString("reason") + " (Client: " + rs.getString("clientBrand") + ")",
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load flags: " + e.getMessage());
        }
        return list;
    }

    public static String formatTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }
}
