package dev.astra.guard.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;

public interface Check {
    String name();

    void handle(PacketReceiveEvent e);
}
