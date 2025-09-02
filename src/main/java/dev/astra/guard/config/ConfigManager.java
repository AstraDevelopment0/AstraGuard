package dev.astra.guard.config;

import dev.astra.guard.utils.MiniMessageLegacyParser;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    private int maxViolations;
    private long violationResetMinutes;
    private String kickMessage;
    private String flagLogFormat;
    private int softPayload;
    private int hardPayload;
    private int maxPayloadsPerTick;
    private int maxBookPages;
    private int maxCharsPerPage;
    private int maxMapSize;
    private int maxTotalNbtBytes;

    public int getInt(String path, int def) {
        return cfg.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return cfg.getDouble(path, def);
    }

    public long getLong(String path, long def) {
        return cfg.getLong(path, def);
    }

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    public void init() {
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
        maxViolations = cfg.getInt("violations.max", 3);
        violationResetMinutes = cfg.getLong("violations.reset-minutes", 10);
        kickMessage = toLegacy(cfg.getString("messages.kick",
                "<red>AstraGuard: Crash attempt detected <gray>({check})"));
        flagLogFormat = toLegacy(cfg.getString("messages.flag-log",
                "{player} flagged by {check} ({detail}) [{count}/{max}]"));
        softPayload = cfg.getInt("netty.payload.soft", 262_144);
        hardPayload = cfg.getInt("netty.payload.hard", 1_048_576);
        maxPayloadsPerTick = cfg.getInt("netty.payload.maxPerTick", 50);
        maxBookPages         = cfg.getInt("netty.windowClickGuard.book.maxPages", 20);
        maxCharsPerPage      = cfg.getInt("netty.windowClickGuard.book.maxCharsPerPage", 1024);
        maxMapSize           = cfg.getInt("netty.windowClickGuard.book.maxMapSize", 50);
        maxTotalNbtBytes     = cfg.getInt("netty.windowClickGuard.book.maxTotalNbtBytes", 200_000);
    }

    private String toLegacy(String s) {
        return MiniMessageLegacyParser.parse(s);
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    public long getViolationResetMinutes() {
        return violationResetMinutes;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getFlagLogFormat() {
        return flagLogFormat;
    }

    public int getSoftPayload() {
        return softPayload;
    }

    public int getHardPayload() {
        return hardPayload;
    }

    public int getMaxPayloadsPerTick() {
        return maxPayloadsPerTick;
    }
    public int getMaxBookPages()      { return maxBookPages; }
    public int getMaxCharsPerPage()   { return maxCharsPerPage; }
    public int getMaxMapSize()        { return maxMapSize; }
    public int getMaxTotalNbtBytes()  { return maxTotalNbtBytes; }


    public String formatLog(String tpl, String p, String ck, String d, int cnt, int max) {
        return tpl.replace("{player}", p).replace("{check}", ck)
                .replace("{detail}", d).replace("{count}", String.valueOf(cnt))
                .replace("{max}", String.valueOf(max));
    }
}