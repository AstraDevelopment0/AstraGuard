package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItem;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class ItemB implements Check {

    @Override
    public String name() {
        return "Item-B";
    }

    @Override
    public void handle(final PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.PICK_ITEM) return;

        final Player player = e.getPlayer();
        if (player == null) return;

        final WrapperPlayClientPickItem pickItem;
        try {
            pickItem = new WrapperPlayClientPickItem(e);
        } catch (Exception ex) {
            kick(player, e, "malformed_or_unsupported_packet");
            return;
        }

        final int slot = pickItem.getSlot();

        if (slot < 0 || slot > 8) {
            kick(player, e, "invalid_pick_slot=" + slot);
        }
    }

    private void kick(final Player player, final PacketReceiveEvent e, final String detail) {
        TaskUtil.flag(player, name(), detail);
        e.setCancelled(true);
    }
}
