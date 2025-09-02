package dev.astra.guard.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import dev.astra.guard.checks.CheckManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener extends PacketListenerAbstract implements Listener {
    private final CheckManager mgr;

    public PlayerListener(CheckManager m) {
        super(PacketListenerPriority.HIGH);
        this.mgr = m;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent e) {
        mgr.handle(e);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
    }
}
