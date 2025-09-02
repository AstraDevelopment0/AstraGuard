package dev.astra.guard.webhook;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.*;

public class WebhookUtil {

    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final BlockingQueue<WebhookPayload> queue = new LinkedBlockingQueue<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final long COOLDOWN_MS = 5000L;

    private static long lastSent = 0;

    static {
        executor.submit(() -> {
            while (true) {
                try {
                    WebhookPayload payload = queue.take();
                    long now = System.currentTimeMillis();
                    long waitTime = COOLDOWN_MS - (now - lastSent);
                    if (waitTime > 0) TimeUnit.MILLISECONDS.sleep(waitTime);
                    sendNow(payload);
                    lastSent = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("❌ Error sending webhook: " + e.getMessage());
                }
            }
        });
    }

    public static void sendPlayerKickedWebhook(String playerName, String checkName, String reason) {
        if (!WebhookConfig.enabled) return;
        String description = String.join("\n", WebhookConfig.kickDescription)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%reason%", reason);
        Embed embed = new Embed(
                WebhookConfig.kickTitle,
                description,
                WebhookConfig.kickColor,
                Instant.now().toString()
        );
        enqueue(embed);
    }

    public static void sendCheckTriggeredWebhook(String playerName, String checkName, String details, int count, int max) {
        if (!WebhookConfig.enabled) return;
        String description = String.join("\n", WebhookConfig.checkDescription)
                .replace("%player%", playerName)
                .replace("%check%", checkName)
                .replace("%details%", details)
                .replace("%count%", String.valueOf(count))
                .replace("%max%", String.valueOf(max));
        Embed embed = new Embed(
                WebhookConfig.checkTitle,
                description,
                WebhookConfig.checkColor,
                Instant.now().toString()
        );
        enqueue(embed);
    }

    private static void enqueue(Embed embed) {
        WebhookPayload payload = new WebhookPayload(
                WebhookConfig.botUsername,
                WebhookConfig.botAvatarUrl,
                Collections.singletonList(embed)
        );
        if (!queue.offer(payload)) {
            System.err.println("❌ Failed to enqueue webhook payload (queue full)");
        }
    }

    private static void sendNow(WebhookPayload payload) {
        String json = gson.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WebhookConfig.webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        System.err.println("❌ Webhook error: " + response.statusCode() + " - " + response.body());
                    }
                })
                .exceptionally(e -> {
                    System.err.println("❌ Webhook failed: " + e.getMessage());
                    return null;
                });
    }

    public static void shutdown() {
        executor.shutdownNow();
    }
}
