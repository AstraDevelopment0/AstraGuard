package dev.astra.guard.guis;

import dev.astra.guard.modules.CrashFlag;
import dev.astra.guard.utils.TaskUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class FlagsGUI {

    private final String targetName;
    private final List<CrashFlag> flags;
    private Inventory inv;

    public FlagsGUI(String targetName, List<CrashFlag> flags) {
        this.targetName = targetName;
        this.flags = flags;
    }

    @SuppressWarnings("deprecation")
    public void open(Player viewer) {
        int size = ((flags.size() / 9) + 1) * 9;
        size = Math.min(size, 54);

        FlagsInventoryHolder holder = new FlagsInventoryHolder();
        inv = Bukkit.createInventory(holder, size, "Flags of " + targetName);
        holder.setInventory(inv);

        for (int i = 0; i < flags.size() && i < size; i++) {
            CrashFlag flag = flags.get(i);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("§cReason: §f" + flag.reason());

            List<String> lore = new ArrayList<>();
            lore.add("§7Time: §e" + TaskUtil.formatTime(flag.timestamp()));
            lore.add("§cClientBrand: §e" + viewer.getClientBrandName());
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        viewer.openInventory(inv);
    }

    public Inventory getInventory() {
        return inv;
    }
}
