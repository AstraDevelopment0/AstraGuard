package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSelectBundleItem;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class BundleA implements Check {

    private static final int MIN_INDEX = -1;
    private static final int MAX_INDEX = 127;

    @Override
    public String name() {
        return "Bundle-A";
    }

    @Override
    public void handle(PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.SELECT_BUNDLE_ITEM) return;
        if (!e.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)) return;

        Player player = e.getPlayer();
        if (player == null) return;

        try {
            WrapperPlayClientSelectBundleItem packet = new WrapperPlayClientSelectBundleItem(e);
            int index = packet.getSelectedItemIndex();

            if (index < MIN_INDEX || index > MAX_INDEX) {
                flag(player, e, "invalid_index=" + index);
            }

        } catch (IllegalArgumentException ex) {
            flag(player, e, "malformed_packet");
        }
    }

    private void flag(Player player, PacketReceiveEvent e, String detail) {
        TaskUtil.flag(player, name(), detail);
        e.setCancelled(true);
    }
}
