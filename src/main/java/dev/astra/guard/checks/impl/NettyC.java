package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTList;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.astra.guard.Main;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public final class NettyC implements Check {

    private final int MAX_BURST;
    private final int SOFT_BYTES;
    private final int HARD_BYTES;
    private final int MAX_BEES;
    private final int MAX_STRIKES;
    private final int MAX_BOOK_PAGES;
    private final int MAX_CHARS_PER_PAGE;
    private final int MAX_MAP_SIZE;
    private final int MAX_TOTAL_NBT_BYTES;

    private final LoadingCache<UUID, LongAdder> bursts;
    private final LoadingCache<UUID, LongAdder> strikes;

    public NettyC() {
        var cfg = Main.getInstance().getConfigManager();

        MAX_BURST           = cfg.getInt("netty.windowClickGuard.maxBurst", 20);
        SOFT_BYTES          = cfg.getInt("netty.windowClickGuard.nbtSoft", 65_536);
        HARD_BYTES          = cfg.getInt("netty.windowClickGuard.nbtHard", 262_144);
        MAX_BEES            = cfg.getInt("netty.windowClickGuard.maxBees", 30);
        MAX_STRIKES         = cfg.getInt("netty.windowClickGuard.maxStrikes", 5);
        MAX_BOOK_PAGES      = cfg.getInt("netty.windowClickGuard.book.maxPages", 20);
        MAX_CHARS_PER_PAGE  = cfg.getInt("netty.windowClickGuard.book.maxCharsPerPage", 1_024);
        MAX_MAP_SIZE        = cfg.getInt("netty.windowClickGuard.book.maxMapSize", 50);
        MAX_TOTAL_NBT_BYTES = cfg.getInt("netty.windowClickGuard.book.maxTotalNbtBytes", 200_000);

        bursts = CacheBuilder.newBuilder()
                .expireAfterWrite(50, TimeUnit.MILLISECONDS)
                .build(new LongAdderLoader());

        strikes = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new LongAdderLoader());
    }

    @Override
    public String name() {
        return "Netty-C";
    }

    @Override
    public void handle(PacketReceiveEvent ev) {
        if (ev.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        Player player = ev.getPlayer();
        if (player == null) return;
        UUID uid = player.getUniqueId();

        WrapperPlayClientClickWindow pkt;
        try {
            pkt = new WrapperPlayClientClickWindow(ev);
        } catch (IllegalStateException ex) {
            flag(player, ev, "malformed or unsupported NBT");
            return;
        }
        WrapperPlayClientClickWindow.WindowClickType type = pkt.getWindowClickType();

        if (type == WrapperPlayClientClickWindow.WindowClickType.QUICK_MOVE
                || type == WrapperPlayClientClickWindow.WindowClickType.PICKUP_ALL
                || type == WrapperPlayClientClickWindow.WindowClickType.SWAP
                || type == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                || type == WrapperPlayClientClickWindow.WindowClickType.CLONE) {
            return;
        }

        LongAdder burstCounter = bursts.getUnchecked(uid);
        burstCounter.increment();
        if (burstCounter.intValue() > MAX_BURST) {
            flag(player, ev, "burst>" + MAX_BURST);
            return;
        }

        var stack = pkt.getCarriedItemStack();
        if (stack == null) return;

        NBTCompound tag = stack.getNBT();
        if (tag == null) return;

        NBTList<NBTCompound> pages = tag.getCompoundListTagOrNull("pages");
        if (pages != null) {
            int pageCount = pages.getTags().size();
            if (pageCount > MAX_BOOK_PAGES) {
                flag(player, ev, "pages=" + pageCount);
                return;
            }

            int totalBytes = 0;
            for (NBTCompound page : pages.getTags()) {
                String text = page.toString();
                int len = text.length();
                if (len > MAX_CHARS_PER_PAGE) {
                    flag(player, ev, "chars=" + len);
                    return;
                }
                totalBytes += text.getBytes(StandardCharsets.UTF_8).length;
            }

            Map<Integer, ?> slots = pkt.getSlots().orElse(Collections.emptyMap());
            if (slots.size() > MAX_MAP_SIZE) {
                flag(player, ev, "map=" + slots.size());
                return;
            }
            if (totalBytes > MAX_TOTAL_NBT_BYTES) {
                flag(player, ev, "nbtBytes=" + totalBytes);
                return;
            }
            if (totalBytes >= HARD_BYTES) {
                flag(player, ev, "nbt=" + totalBytes);
                return;
            }
            if (totalBytes >= SOFT_BYTES) {
                LongAdder strikeCounter = strikes.getUnchecked(uid);
                strikeCounter.increment();
                flag(player, ev, "nbt=" + totalBytes +
                        " [" + strikeCounter.intValue() + "/" + MAX_STRIKES + "]");
                return;
            }
        }

        NBTCompound block = tag.getCompoundTagOrNull("BlockEntityTag");
        if (block != null) {
            NBTList<NBTCompound> bees = block.getCompoundListTagOrNull("Bees");
            if (bees != null && bees.getTags().size() > MAX_BEES) {
                flag(player, ev, "bees=" + bees.getTags().size());
            }
        }
    }


    private void flag(Player p, PacketReceiveEvent e, String detail) {
        TaskUtil.flag(p, name(), detail);
        e.setCancelled(true);
    }

    private static final class LongAdderLoader extends CacheLoader<UUID, LongAdder> {
        @Override
        public @NotNull LongAdder load(@NotNull UUID key) {
            return new LongAdder();
        }
    }
}