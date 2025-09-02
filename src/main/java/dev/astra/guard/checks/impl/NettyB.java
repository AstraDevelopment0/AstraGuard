package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

public final class NettyB implements Check {

    @Override
    public String name() {
        return "Netty-B";
    }

    @Override
    public void handle(PacketReceiveEvent e) {
        if (e.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        Player player = e.getPlayer();
        if (player == null) return;
        WrapperPlayClientPluginMessage msg = new WrapperPlayClientPluginMessage(e);

        String channel = msg.getChannelName();
        byte[] data = msg.getData();

        if (channel.length() > 16 && !channel.matches("[\\w:./-]+")) {
            kick(player, e, "bad-channel" + " ");
            return;
        }

        if (channel.equalsIgnoreCase("REGISTER") && data.length > 512) {
            kick(player, e, "oversize-register" + " ");
        }
    }

    private void kick(Player p, PacketReceiveEvent e, String reason) {
        TaskUtil.flag(p, name(), reason);
        e.setCancelled(true);
    }
}
