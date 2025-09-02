package dev.astra.guard;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {

    private final Plugin plugin;
    @SuppressWarnings("FieldMayBeFinal")
    private static String VERSION_URL;
    @SuppressWarnings("FieldMayBeFinal")
    private static String DOWNLOAD_URL;
    @SuppressWarnings("FieldMayBeFinal")
    private static String SECRET_KEY;
    @SuppressWarnings("FieldMayBeFinal")
    private static String PLUGIN_ID;

    static {
        VERSION_URL = "http://5.42.217.162:5000/version";
        DOWNLOAD_URL = "http://5.42.217.162:3500/download";
        SECRET_KEY = "9X#vT7q@Lp2$Mz&4RwY!sNj8*FhZ0eKd";
        PLUGIN_ID = "AstraGuard";
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String latestVersion;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    private File getPluginJarFile() {
        if (!(plugin instanceof JavaPlugin)) {
            return new File(plugin.getDataFolder().getParentFile(), plugin.getName() + ".jar");
        }
        try {
            Field fileField = JavaPlugin.class.getDeclaredField("file");
            fileField.setAccessible(true);
            return (File) fileField.get(plugin);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not get plugin jar file by reflection, fallback to default path.", e);
            return new File(plugin.getDataFolder().getParentFile(), plugin.getName() + ".jar");
        }
    }

    private static String calculateHMAC(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    public CompletableFuture<Void> checkAndUpdate() {
        HttpRequest versionRequest = HttpRequest.newBuilder()
                .uri(URI.create(VERSION_URL))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .build();

        return CLIENT.sendAsync(versionRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenCompose(this::handleVersionResponse)
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getLogger().warning("Failed to check for updates: " + ex.getMessage())
                    );
                    return null;
                });
    }
    @SuppressWarnings("deprecation")
    private CompletableFuture<Void> handleVersionResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            latestVersion = json.get("version").getAsString();
            String currentVersion = plugin.getDescription().getVersion();

            if (latestVersion.equalsIgnoreCase(currentVersion)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getLogger().info("You are running the latest version of AstraGuard.")
                );
                return CompletableFuture.completedFuture(null);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().warning("A new version of AstraGuard is available!");
                    plugin.getLogger().warning("Current version: " + currentVersion + " → New version: " + latestVersion);
                    plugin.getLogger().warning("Downloading update now...");
                });

                return downloadUpdateOnly();
            }
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getLogger().log(Level.SEVERE, "Error parsing update response: " + e.getMessage(), e)
            );
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Void> downloadUpdateOnly() {
        try {
            long timestamp = System.currentTimeMillis();
            String data = PLUGIN_ID + ":" + timestamp;
            String signature = calculateHMAC(data, SECRET_KEY);

            String jsonBody = String.format(
                    "{\"pluginID\":\"%s\", \"timestamp\":%d, \"signature\":\"%s\"}",
                    PLUGIN_ID, timestamp, signature);

            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(DOWNLOAD_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            File pluginFile = getPluginJarFile();
            File parentDir = pluginFile.getParentFile();
            String newFileName = "AstraGuard-" + latestVersion + ".jar";
            File newPluginFile = new File(parentDir, newFileName);

            return CLIENT.sendAsync(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAcceptAsync(response -> {
                        if (response.statusCode() == 200) {
                            try (InputStream is = response.body();
                                 FileOutputStream fos = new FileOutputStream(newPluginFile)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                                fos.flush();

                                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        plugin.getLogger().info("Update downloaded as " + newPluginFile.getName());
                                        plugin.getLogger().info("Please remove or rename the old plugin file: " + pluginFile.getName());
                                        plugin.getLogger().info("Restart the server to apply the update.");
                                    });
                                } else {
                                    try {
                                        if (pluginFile.delete()) {
                                            if (!newPluginFile.renameTo(pluginFile)) {
                                                plugin.getLogger().severe("Failed to rename downloaded file to plugin jar!");
                                            }
                                        } else {
                                            plugin.getLogger().severe("Failed to delete current plugin file for replacement!");
                                        }
                                        Bukkit.getScheduler().runTask(plugin, () ->
                                                plugin.getLogger().info("Update saved. It will be loaded after the next server restart.")
                                        );
                                    } catch (Exception e) {
                                        Bukkit.getScheduler().runTask(plugin, () ->
                                                plugin.getLogger().log(Level.SEVERE, "Failed to replace plugin file: " + e.getMessage(), e)
                                        );
                                    }
                                }

                            } catch (IOException e) {
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        plugin.getLogger().log(Level.SEVERE, "Failed to save update file: " + e.getMessage(), e)
                                );
                            }
                        } else {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    plugin.getLogger().warning("Failed to download update, HTTP status: " + response.statusCode())
                            );
                        }
                    });
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getLogger().log(Level.SEVERE, "Error during download: " + e.getMessage(), e)
            );
            return CompletableFuture.completedFuture(null);
        }
    }
}
