package dev.astra.guard.checks.impl;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEditBook;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientNameItem;
import dev.astra.guard.checks.Check;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ItemA implements Check {

    @Override
    public String name() {
        return "Item-A";
    }

    @Override
    public void handle(PacketReceiveEvent e) {
        PacketTypeCommon type = e.getPacketType();
        Player player = e.getPlayer();
        if (player == null) return;

        if (e.getPacketType() == PacketType.Play.Client.EDIT_BOOK) {
            WrapperPlayClientEditBook editBook;
            try {
                editBook = new WrapperPlayClientEditBook(e);
            } catch (Exception ex) {
                kick(player, e, "malformed or unsupported");
                return;
            }
            String title = editBook.getTitle();
            List<String> pages = editBook.getPages();

            if (pages == null || pages.isEmpty()) {
                kick(player, e, "Illegal book page");
                return;
            }

            int maxDataSize = e.getServerVersion().isOlderThan(ServerVersion.V_1_16) ? 8192 : 32768;
            StringBuilder sb = new StringBuilder();
            sb.append(title);
            for (String page : pages) {
                sb.append(page);
            }
            byte[] dataBytes = sb.toString().getBytes(StandardCharsets.UTF_8);

            if (dataBytes.length > maxDataSize) {
                kick(player, e, "Illegal book page");
                return;
            }
            if (title == null || title.length() > 32) {
                kick(player, e, "Illegal book-title page");
                return;
            }

            final int MAX_BOOK_PAGES = 50;
            final int MAX_PAGE_CHARACTERS = 256;
            if (pages.size() > MAX_BOOK_PAGES) {
                kick(player, e, "bytesSize: " + dataBytes.length);
                return;
            }
            for (String page : pages) {
                if (page.length() > MAX_PAGE_CHARACTERS) {
                    kick(player, e, "bytesSize: " + dataBytes.length);
                    return;
                }
            }
        } else if (type == PacketType.Play.Client.NAME_ITEM) {
            WrapperPlayClientNameItem wrapper = new WrapperPlayClientNameItem(e);
            String itemname = wrapper.getItemName();

            if (itemname != null && !itemname.isEmpty()) {
                Player p = e.getPlayer();
                org.bukkit.inventory.ItemStack bukkitItem = p.getInventory().getItemInMainHand();

                if (bukkitItem.hasItemMeta()) {
                    ItemMeta meta = bukkitItem.getItemMeta();
                    if (meta != null) {
                        try {
                            @SuppressWarnings("deprecation")
                            List<String> lore = meta.getLore();
                            if (lore != null) {
                                boolean tooManyLines = lore.size() > 30;
                                boolean tooLongLine = lore.stream()
                                        .anyMatch(line -> line.length() > 150);

                                if (tooManyLines || tooLongLine) {
                                    kick(p, e, "invalid_lore: "
                                            + (tooManyLines ? "too many lines" : "line too long"));
                                }
                            }

                        } catch (Exception ex) {
                            kick(p, e, "Invalid item lore");
                        }
                    }
                }
            }
        }


    }

    private void kick(Player p, PacketReceiveEvent e, String detail) {
        TaskUtil.flag(p, name(), detail);
        e.setCancelled(true);
    }
}
