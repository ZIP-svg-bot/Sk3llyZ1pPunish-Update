package dev.adminpunish.listeners;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;

public class JoinListener implements Listener {

    private final AdminPunish plugin;

    public JoinListener(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        String ip = event.getAddress().getHostAddress();

        // Check IP ban first
        if (plugin.getOffenseManager().isIpBanned(ip)) {
            Offense o = plugin.getOffenseManager().getIpBan(ip);
            if (o != null) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, o.getBanMessage());
                return;
            }
        }

        // Check UUID ban
        if (plugin.getOffenseManager().isBanned(uuid)) {
            Offense o = plugin.getOffenseManager().getActivePunishment(uuid);
            if (o != null) {
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED, o.getBanMessage());
                return;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String ip = player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "Unknown";

        // Ban evasion check: see if this IP is already linked to another name
        // with a currently active ban, BEFORE we record this new login (so the
        // alt set doesn't already include this session under a different cached IP).
        if (plugin.getConfig().getBoolean("ban-evasion-alerts", true) && !ip.equals("Unknown")) {
            checkBanEvasion(player, playerName, ip);
        }

        // Log alt data
        plugin.getAltManager().recordLogin(playerName, ip);

        // VPN check
        plugin.getVpnManager().checkPlayer(player, ip);

        // Apply vanish — hide vanished staff from this new player if they're not op
        plugin.getVanishManager().applyVanishOnJoin(player);

        // If THIS joining player was vanished before a server restart, re-hide them now.
        plugin.getVanishManager().reapplyVanishOnRejoin(player);
    }

    private void checkBanEvasion(Player player, String playerName, String ip) {
        Set<String> namesOnIp = plugin.getAltManager().getNamesOnIp(ip);
        for (String altName : namesOnIp) {
            if (altName.equalsIgnoreCase(playerName)) continue;
            OfflinePlayer altOffline = Bukkit.getOfflinePlayer(altName);
            String altUuid = altOffline.getUniqueId().toString();
            if (plugin.getHistoryManager().hasActiveBan(altUuid)) {
                String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
                Bukkit.broadcast(
                        color(prefix + "&c&lPOSSIBLE BAN EVASION: &f" + playerName
                                + " &7joined on the same IP as banned player &f" + altName),
                        "adminpunish.offend"
                );
                return; // one alert is enough, don't spam for every matching alt
            }
        }
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
