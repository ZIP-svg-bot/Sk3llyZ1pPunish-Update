package dev.adminpunish.listeners;

import dev.adminpunish.AdminPunish;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class FreezeListener implements Listener {

    private final AdminPunish plugin;

    public FreezeListener(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getFreezeManager().isFrozen(player.getUniqueId())) return;

        Location anchor = plugin.getFreezeManager().getAnchor(player.getUniqueId());
        if (anchor == null) {
            plugin.getFreezeManager().updateAnchor(player.getUniqueId(), player.getLocation());
            return;
        }

        // Allow head movement (looking around) but cancel any actual XYZ movement.
        if (event.getTo() == null) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            Location snapBack = anchor.clone();
            snapBack.setYaw(event.getTo().getYaw());
            snapBack.setPitch(event.getTo().getPitch());
            event.setTo(snapBack);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (plugin.getFreezeManager().isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getFreezeManager().isFrozen(player.getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("freeze.disconnect-alert", true)) return;

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        Bukkit.broadcast(
                color(prefix + "&c&l" + player.getName() + " disconnected while frozen! &7Possible evasion."),
                "adminpunish.freeze"
        );
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
