package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class WindowC implements Check {

    @Override public String name() { return "Window-C"; }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (event.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) return;
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW)        return;

        var obj = event.getPlayer();
        if (!(obj instanceof Player player) || !player.isOnline()) return;

        WrapperPlayClientClickWindow click;
        try {
            click = new WrapperPlayClientClickWindow(event);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            flag(player, event, "malformed or unsupported NBT / item-ID");
            return;
        }

        var windowId = click.getWindowId();
        var slot     = click.getSlot();
        var button   = click.getButton();
        var type     = click.getWindowClickType();

        if (windowId < 0 || slot < -999 || button < 0 || type == null) {
            flag(player, event,
                    "win=%d, slot=%d, btn=%d, type=%s".formatted(windowId, slot, button, type));
        }
    }

    private void flag(Player player, PacketReceiveEvent event, String detail) {
        TaskUtil.flag(player, name(), detail);
        event.setCancelled(true);
    }
}