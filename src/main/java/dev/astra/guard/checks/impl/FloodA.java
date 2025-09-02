package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class FloodA implements Check {

    private static final int MAX_LENGTH = 512;

    @Override
    public String name() {
        return "Flood-A";
    }

    @Override
    public void handle(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE) return;

        Player player = event.getPlayer();
        if (player == null) return;

        WrapperPlayClientTabComplete wrapper = new WrapperPlayClientTabComplete(event);
        String input = wrapper.getText();
        int length = input.length();

        if (length > MAX_LENGTH) {
            flag(player, event, "TooLong len=" + length);
            return;
        }

        int spaceIndex = input.indexOf(' ');
        if (length > 64 && (spaceIndex == -1 || spaceIndex >= 64)) {
            flag(player, event, "SuspiciousTab text=" + input);
        }
    }

    private void flag(Player player, PacketReceiveEvent event, String detail) {
        TaskUtil.flag(player, name(), detail);
        event.setCancelled(true);
    }
}
