package dev.adminpunish.gui;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class OffensesGUI implements Listener {

    private final AdminPunish plugin;
    private final NamespacedKey offenderUuidKey;

    public OffensesGUI(AdminPunish plugin) {
        this.plugin = plugin;
        this.offenderUuidKey = new NamespacedKey(plugin, "offenses_gui_offender_uuid");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Collection<Offense> offenses = plugin.getOffenseManager().getAllActive();

        int size = 54; // Always 6 rows

        String title = color(plugin.getConfig().getString("gui-title", "&8&lActive Offenses"));
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (Offense o : offenses) {
            if (slot >= size) break;
            inv.setItem(slot++, buildOffenseItem(o));
        }

        if (offenses.isEmpty()) {
            inv.setItem(4, buildInfoItem(Material.LIME_WOOL,
                    "&a&lNo Active Offenses",
                    List.of("&7The server is clean!")));
        }

        player.openInventory(inv);
    }

    private ItemStack buildOffenseItem(Offense o) {
        // Use player skull
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(o.getPlayerName()));
            meta.setDisplayName(color("&f&l" + o.getPlayerName()));

            List<String> lore = new ArrayList<>();
            lore.add(color("&8-------------------"));

            String typeColor = switch (o.getType()) {
                case MUTE -> "&e";
                case BAN -> "&c";
                case IPBAN -> "&4";
            };
            String typeLabel = switch (o.getType()) {
                case MUTE -> "MUTE";
                case BAN -> "BAN";
                case IPBAN -> "IP BAN";
            };

            lore.add(color("&7Type: " + typeColor + "&l" + typeLabel));
            lore.add(color("&7Reason: &f" + o.getOffenseDisplay()));
            lore.add(color("&7Time Left: &e" + o.getTimeRemaining()));
            lore.add(color("&7Staff: &b" + o.getStaffName()));
            if (o.getNote() != null && !o.getNote().isBlank()) {
                lore.add(color("&7Note: &f" + o.getNote()));
            }
            lore.add(color("&8-------------------"));
            lore.add(color("&eClick to view details"));

            meta.setLore(lore);
            // Store the offender's real UUID directly on the item, rather than
            // re-deriving it later by stripping color codes out of the display
            // name text (which is fragile if a name ever collides with a code).
            meta.getPersistentDataContainer().set(offenderUuidKey, PersistentDataType.STRING, o.getPlayerUUID());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack buildInfoItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) coloredLore.add(color(l));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(color(plugin.getConfig().getString("gui-title", "&8&lActive Offenses")))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String offenderUuid = meta.getPersistentDataContainer().get(offenderUuidKey, PersistentDataType.STRING);
        if (offenderUuid == null) return;

        player.closeInventory();

        Offense o = plugin.getOffenseManager().getActivePunishment(offenderUuid);
        if (o == null) {
            player.sendMessage(color("&cNo active offense found."));
            return;
        }

        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &7Showing offense for &f" + o.getPlayerName() + "&7:"));
        player.sendMessage(color("  &7Player: &f" + o.getPlayerName()));
        player.sendMessage(color("  &7Type:   &c" + o.getType().name()));
        player.sendMessage(color("  &7Reason: &f" + o.getOffenseDisplay()));
        player.sendMessage(color("  &7Left:   &e" + o.getTimeRemaining()));
        player.sendMessage(color("  &7Staff:  &b" + o.getStaffName()));
        player.sendMessage(color("  &7IP:     &f||" + o.getPlayerIp() + "||"));
        if (o.getNote() != null && !o.getNote().isBlank()) {
            player.sendMessage(color("  &7Note:   &f" + o.getNote()));
        }
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
