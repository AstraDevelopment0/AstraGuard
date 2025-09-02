package dev.astra.guard.webhook;

public class Embed {
    public String title;
    public String description;
    public int color;
    public String timestamp;

    public Embed(String title, String description, int color, String timestamp) {
        this.title = title;
        this.description = description;
        this.color = color;
        this.timestamp = timestamp;
    }
}
