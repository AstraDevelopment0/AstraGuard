package dev.astra.guard.webhook;

import java.util.List;

public class WebhookPayload {
    public String username;
    public String avatar_url;
    public List<Embed> embeds;

    public WebhookPayload(String username, String avatar_url, List<Embed> embeds) {
        this.username = username;
        this.avatar_url = avatar_url;
        this.embeds = embeds;
    }
}
