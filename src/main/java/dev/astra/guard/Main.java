package dev.astra.guard;

import com.github.retrooper.packetevents.PacketEvents;
import dev.astra.guard.commands.AstraCommand;
import dev.astra.guard.commands.AstraTabCompleter;
import dev.astra.guard.listeners.GUIListener;
import dev.astra.guard.listeners.JoinListener;
import dev.astra.guard.managers.AlertManager;
import dev.astra.guard.checks.CheckManager;
import dev.astra.guard.config.ConfigManager;
import dev.astra.guard.listeners.PlayerListener;
import dev.astra.guard.utils.TaskUtil;
import dev.astra.guard.webhook.WebhookConfig;
import dev.astra.guard.webhook.WebhookUtil;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public final class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private AlertManager alertManager;
    private CheckManager checkManager;

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        String version = readPluginVersion();
        new UpdateChecker(this).checkAndUpdate().whenComplete((res, ex) -> {
            if (ex != null) {
                getLogger().severe("Update check failed: " + ex.getMessage());
            }
        });

        saveDefaultConfig();



        initManagers();
        registerCommands();
        registerListeners();
      new TaskUtil(this);

        PacketEvents.getAPI().init();
        getLogger().info("AstraGuard v" + version + " enabled successfully.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        WebhookUtil.shutdown();
    }

    private void initManagers() {
        File file = new File(getDataFolder(), "webhook.yml");
        if (!file.exists()) {
            saveResource("webhook.yml", false);
        }
        configManager = new ConfigManager(this);
        configManager.init();
        WebhookConfig.load(getDataFolder());

        alertManager = new AlertManager();
        checkManager = new CheckManager();
    }

    private void registerCommands() {
        AstraCommand astraCommand = new AstraCommand(this);
        Objects.requireNonNull(getCommand("astra")).setExecutor(astraCommand);
        Objects.requireNonNull(getCommand("astra")).setTabCompleter(new AstraTabCompleter());
    }

    private void registerListeners() {
        PacketEvents.getAPI().getEventManager()
                .registerListener(new PlayerListener(checkManager));
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(alertManager), this);
    }


    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    private String readPluginVersion() {
        try (InputStream resource = getResource("plugin.yml")) {
            if (resource == null) {
                getLogger().warning("plugin.yml not found inside the plugin JAR.");
                return "unknown";
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(resource));
            return config.getString("version", "unknown");
        } catch (Exception e) {
            getLogger().warning("Could not read version from plugin.yml: " + e.getMessage());
            return "unknown";
        }
    }
}
