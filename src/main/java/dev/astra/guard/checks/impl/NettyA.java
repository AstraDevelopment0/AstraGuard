package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.astra.guard.Main;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class NettyA implements Check {

    private final int SOFT;
    private final int HARD;
    private final int MAX_PER_TICK;

    private static final Cache<UUID, LongAdder> BURST =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(50, TimeUnit.MILLISECONDS)
                    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                    .build();

    public NettyA() {
        var cfg = Main.getInstance().getConfigManager();
        SOFT = cfg.getSoftPayload();
        HARD = cfg.getHardPayload();
        MAX_PER_TICK = cfg.getMaxPayloadsPerTick();
    }

    @Override
    public String name() {
        return "Netty-A";
    }

    @Override
    public void handle(PacketReceiveEvent e) {

        if (e.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) return;

        Player player = e.getPlayer();
        if (player == null) return;
        WrapperPlayClientPluginMessage pkt = new WrapperPlayClientPluginMessage(e);

        LongAdder burst = BURST.getIfPresent(player.getUniqueId());
        if (burst == null) {
            burst = new LongAdder();
            BURST.put(player.getUniqueId(), burst);
        }
        burst.increment();
        if (burst.intValue() > MAX_PER_TICK) {
            kick(player, e, "burst>" + MAX_PER_TICK);
            return;
        }

        byte[] data = pkt.getData();
        int size = data == null ? 0 : data.length;

        if (size >= HARD) {
            kick(player, e, "size=" + size + "(hard)");
            return;
        }
        if (size >= SOFT) {
            kick(player, e, "size=" + size + "(soft)");
        }
    }

    private void kick(Player p, PacketReceiveEvent e, String detail) {
        TaskUtil.flag(p, name(), detail);
        e.setCancelled(true);
    }
}
