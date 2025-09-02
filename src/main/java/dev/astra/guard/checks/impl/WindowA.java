package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class WindowA implements Check {

    @Override
    public String name() {
        return "Window-A";
    }

    @Override
    public void handle(PacketReceiveEvent e) {
        if (e.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_5)) return;
        if (e.getPacketType() != PacketType.Play.Client.CLICK_WINDOW)         return;

        Player player = e.getPlayer();
        if (player == null) return;

        WrapperPlayClientClickWindow click;
        try {
            click = new WrapperPlayClientClickWindow(e);
        } catch (IllegalStateException ex) {
            flag(player, e, "malformed or unsupported NBT");
            return;
        }

        int clickType = click.getWindowClickType().ordinal();
        int button    = click.getButton();
        int windowId  = click.getWindowId();

        if ((clickType == 1 || clickType == 2) && windowId >= 0 && button < 0) {
            flag(player, e, "type=" + clickType + ", win=" + windowId + ", btn=" + button);
        }
    }

    private void flag(Player p, PacketReceiveEvent ev, String detail) {
        TaskUtil.flag(p, name(), detail);
        ev.setCancelled(true);
    }
}