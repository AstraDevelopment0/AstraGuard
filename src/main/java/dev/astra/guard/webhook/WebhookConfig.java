package dev.astra.guard.webhook;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class WebhookConfig {

    public static boolean enabled;
    public static String webhookUrl;
    public static String botUsername;
    public static String botAvatarUrl;

    public static int kickColor;
    public static int checkColor;


    public static String kickTitle;
    public static List<String> kickDescription;

    public static String checkTitle;
    public static List<String> checkDescription;

    public static void load(File dataFolder) {
        File file = new File(dataFolder, "webhook.yml");
        if (!file.exists()) {
            System.out.println("⚠️ webhook.yml not found.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        enabled = config.getBoolean("enabled", true);
        webhookUrl = config.getString("webhook_url", "");
        botUsername = config.getString("bot.username", "AstraGuard");
        botAvatarUrl = config.getString("bot.avatar_url", "");

        kickColor = parseColor(config.getString("colors.kick", "0xff0000"));
        checkColor = parseColor(config.getString("colors.check", "0xffa500"));

        kickTitle = config.getString("messages.kick.title", "Player Kicked");
        kickDescription = config.getStringList("messages.kick.description");

        checkTitle = config.getString("messages.check.title", "Anti-Crash Triggered");
        checkDescription = config.getStringList("messages.check.description");
    }

    private static int parseColor(String hex) {
        try {
            return Integer.decode(hex);
        } catch (Exception e) {
            return 0xffffff;
        }
    }
}
