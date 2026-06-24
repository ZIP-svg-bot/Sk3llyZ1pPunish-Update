package dev.adminpunish.gui;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.managers.OffenseManager;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * /offendgui <player> opens this. Clicking an offense item applies it to the
 * stored target. This listener is registered ONCE (in AdminPunish#onEnable)
 * rather than re-registered per open, so it doesn't leak listener registrations
 * the way creating "new OffensesGUI(plugin)" on every command call does.
 *
 * GUI identity is tracked by comparing the actual open Inventory object
 * reference (not by parsing the title text), so it can't be confused by
 * player names that happen to contain color codes or similar text.
 */
public class OffendGUI implements Listener {

    private final AdminPunish plugin;
    private final NamespacedKey offenseItemKey;

    private enum Screen { MAIN, CONFIRM }

    private final Map<UUID, PendingTarget> pendingTargets = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    private final Map<UUID, Screen> openScreens = new HashMap<>();

    public OffendGUI(AdminPunish plugin) {
        this.plugin = plugin;
        this.offenseItemKey = new NamespacedKey(plugin, "offendgui_offense_key");
    }

    private static class PendingTarget {
        final String targetName;
        final String targetUuid;
        final String targetIp;
        PendingTarget(String targetName, String targetUuid, String targetIp) {
            this.targetName = targetName;
            this.targetUuid = targetUuid;
            this.targetIp = targetIp;
        }
    }

    public void open(Player staff, String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        String resolvedName = targetName;
        String targetUuid;
        String targetIp = "Unknown";

        if (onlineTarget != null) {
            resolvedName = onlineTarget.getName();
            targetUuid = onlineTarget.getUniqueId().toString();
            if (onlineTarget.getAddress() != null) {
                targetIp = onlineTarget.getAddress().getAddress().getHostAddress();
            }
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            targetUuid = offline.getUniqueId().toString();
            if (offline.getName() != null) resolvedName = offline.getName();
            String knownIp = plugin.getAltManager().getIp(targetName);
            if (knownIp != null) targetIp = knownIp;
        }

        pendingTargets.put(staff.getUniqueId(), new PendingTarget(resolvedName, targetUuid, targetIp));
        openMainMenu(staff, resolvedName);
    }

    private void openMainMenu(Player staff, String targetName) {
        Collection<OffenseManager.OffenseDef> defs = plugin.getOffenseManager().getOffenseDefs().values();
        int size = Math.max(9, (int) (Math.ceil(defs.size() / 9.0) * 9));
        size = Math.min(size, 54);

        String rawTitle = plugin.getConfig().getString("offend-gui-title", "&8&lPunish Player");
        String title = color(rawTitle + " &7- &f" + targetName);

        Inventory inv = Bukkit.createInventory(null, size, title);
        for (OffenseManager.OffenseDef def : defs) {
            inv.addItem(buildOffenseItem(def));
        }

        openInventories.put(staff.getUniqueId(), inv);
        openScreens.put(staff.getUniqueId(), Screen.MAIN);
        staff.openInventory(inv);
    }

    private void openConfirmMenu(Player staff, String targetName, String offenseKey) {
        Inventory inv = Bukkit.createInventory(null, 9, color("&4&lConfirm PERMANENT punishment"));

        OffenseManager.OffenseDef def = plugin.getOffenseManager().getOffenseDef(offenseKey);
        String display = def != null ? def.display : offenseKey;

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta cMeta = confirm.getItemMeta();
        if (cMeta != null) {
            cMeta.setDisplayName(color("&a&lCONFIRM"));
            cMeta.setLore(List.of(color("&7Permanently apply &f" + display),
                    color("&7to &f" + targetName + "&7.")));
            cMeta.getPersistentDataContainer().set(offenseItemKey, PersistentDataType.STRING, "CONFIRM:" + offenseKey);
            confirm.setItemMeta(cMeta);
        }
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta xMeta = cancel.getItemMeta();
        if (xMeta != null) {
            xMeta.setDisplayName(color("&c&lCANCEL"));
            xMeta.getPersistentDataContainer().set(offenseItemKey, PersistentDataType.STRING, "CANCEL");
            cancel.setItemMeta(xMeta);
        }

        inv.setItem(3, confirm);
        inv.setItem(5, cancel);

        openInventories.put(staff.getUniqueId(), inv);
        openScreens.put(staff.getUniqueId(), Screen.CONFIRM);
        staff.openInventory(inv);
    }

    private ItemStack buildOffenseItem(OffenseManager.OffenseDef def) {
        Material mat = switch (def.type) {
            case MUTE -> Material.PAPER;
            case IPBAN -> Material.NETHERITE_SWORD;
            case BAN -> Material.IRON_SWORD;
        };
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&f&l" + def.display));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Type: &f" + def.type.name()));
            lore.add(color("&7Duration: &f" + (def.durationMinutes == -1 ? "Permanent" : def.durationMinutes + "m")));
            lore.add(color("&7Key: &8" + def.key));
            if (def.durationMinutes == -1) lore.add(color("&c&oRequires confirmation"));
            lore.add(color("&eClick to apply"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(offenseItemKey, PersistentDataType.STRING, def.key);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player staff)) return;

        Inventory openForThisStaff = openInventories.get(staff.getUniqueId());
        if (openForThisStaff == null || !event.getView().getTopInventory().equals(openForThisStaff)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getItemMeta() == null) return;
        String tag = clicked.getItemMeta().getPersistentDataContainer().get(offenseItemKey, PersistentDataType.STRING);
        if (tag == null) return;

        PendingTarget target = pendingTargets.get(staff.getUniqueId());
        if (target == null) {
            staff.closeInventory();
            staff.sendMessage(color("&cThis menu expired. Run /offendgui again."));
            return;
        }

        Screen screen = openScreens.get(staff.getUniqueId());

        if (screen == Screen.CONFIRM) {
            if (tag.equals("CANCEL")) {
                staff.closeInventory();
                staff.sendMessage(color("&7Punishment cancelled."));
                cleanup(staff);
                return;
            }
            if (tag.startsWith("CONFIRM:")) {
                String offenseKey = tag.substring("CONFIRM:".length());
                applyOffense(staff, target, offenseKey);
            }
            return;
        }

        // Main menu click
        String offenseKey = tag;
        boolean requireConfirm = plugin.getConfig().getBoolean("require-confirm-for-permanent", true);
        if (requireConfirm && plugin.getOffenseManager().isPermanent(offenseKey)) {
            openConfirmMenu(staff, target.targetName, offenseKey);
            return;
        }
        applyOffense(staff, target, offenseKey);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player staff)) return;
        Inventory openForThisStaff = openInventories.get(staff.getUniqueId());
        if (openForThisStaff != null && event.getInventory().equals(openForThisStaff)) {
            // Only clear the screen/inventory tracking, not pendingTargets yet -
            // a confirm-menu close (e.g. clicking CONFIRM, which also closes the
            // inventory) still needs target info available to applyOffense().
            openInventories.remove(staff.getUniqueId());
            openScreens.remove(staff.getUniqueId());
        }
    }

    private void applyOffense(Player staff, PendingTarget target, String offenseKey) {
        staff.closeInventory();
        cleanup(staff);

        Player onlineTarget = null;
        try {
            onlineTarget = Bukkit.getPlayer(UUID.fromString(target.targetUuid));
        } catch (IllegalArgumentException ignored) {}

        Offense offense = plugin.getPunishExecutor().execute(
                onlineTarget, target.targetName, target.targetUuid, target.targetIp,
                offenseKey, staff.getName(), null
        );

        if (offense == null) {
            staff.sendMessage(color("&cFailed to apply offense."));
            return;
        }

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        staff.sendMessage(color(prefix + "&a" + target.targetName + " has been punished for &e" + offense.getOffenseDisplay() + "&a."));
    }

    private void cleanup(Player staff) {
        pendingTargets.remove(staff.getUniqueId());
        openInventories.remove(staff.getUniqueId());
        openScreens.remove(staff.getUniqueId());
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
