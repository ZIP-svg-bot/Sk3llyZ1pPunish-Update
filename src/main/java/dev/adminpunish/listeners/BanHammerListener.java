package dev.adminpunish.listeners;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class BanHammerListener implements Listener {

    private final AdminPunish plugin;

    public BanHammerListener(AdminPunish plugin) {
        this.plugin = plugin;
    }

    // Right-click a player to punish them
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) return;

        ItemStack hand = staff.getInventory().getItemInMainHand();
        if (!plugin.getBanHammerManager().isBanHammer(hand)) return;

        event.setCancelled(true);

        String offenseKey = plugin.getBanHammerManager().getHammerOffense(hand);
        if (offenseKey == null) return;

        // Permanent offenses require a second confirming right-click within the
        // configured timeout, to prevent an accidental perma-ban from one click.
        boolean requireConfirm = plugin.getConfig().getBoolean("require-confirm-for-permanent", true);
        boolean isPermanent = plugin.getOffenseManager().isPermanent(offenseKey);

        if (requireConfirm && isPermanent && !plugin.getBanHammerManager().isConfirmed(staff, target)) {
            int timeout = plugin.getConfig().getInt("confirm-timeout-seconds", 8);
            plugin.getBanHammerManager().requestConfirm(staff, target, timeout);
            staff.sendMessage(color("&c&lPERMANENT offense! &7Right-click " + target.getName()
                    + " again within " + timeout + "s to confirm."));
            staff.playSound(staff.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.7f);
            return;
        }
        plugin.getBanHammerManager().clearConfirm(staff);

        String staffName = staff.getName();
        String playerIp = target.getAddress() != null
                ? target.getAddress().getAddress().getHostAddress() : "Unknown";

        Offense offense = plugin.getPunishExecutor().execute(
                target, target.getName(), target.getUniqueId().toString(), playerIp,
                offenseKey, staffName, null
        );

        if (offense == null) {
            staff.sendMessage(color("&cFailed to apply offense."));
            return;
        }

        // Remove hammer and restore inventory
        plugin.getBanHammerManager().removeHammer(staff);

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        staff.sendMessage(color(prefix + "&a" + target.getName() + " has been punished for &e" + offense.getOffenseDisplay() + "&a."));
        staff.playSound(staff.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
        staff.sendTitle(color("&c&lPUNISHED"), color("&7" + target.getName() + " &7- &e" + offense.getOffenseDisplay()), 5, 30, 10);
    }

    // Prevent dropping the hammer
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getBanHammerManager().isBanHammer(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Prevent picking up items while holding hammer (keeps inv clean)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getBanHammerManager().hasHammer(player)) {
            event.setCancelled(true);
        }
    }

    // Prevent moving hammer in inventory
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getBanHammerManager().hasHammer(player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (plugin.getBanHammerManager().isBanHammer(current)
                || plugin.getBanHammerManager().isBanHammer(cursor)) {
            event.setCancelled(true);
        }
    }

    // Prevent dragging hammer
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getBanHammerManager().isBanHammer(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    // Restore inventory if player logs out with hammer
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getBanHammerManager().hasHammer(event.getPlayer())) {
            plugin.getBanHammerManager().removeHammer(event.getPlayer());
        }
        plugin.getBanHammerManager().clearConfirm(event.getPlayer());
    }

    // Prevent throwing with Q key or other interactions
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!plugin.getBanHammerManager().isBanHammer(hand)) return;

        Action action = event.getAction();
        // Block left/right clicking air or blocks — only entity interaction should go through
        if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR
                || action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            player.sendMessage(color("&cRight-click a player to punish them!"));
        }
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
