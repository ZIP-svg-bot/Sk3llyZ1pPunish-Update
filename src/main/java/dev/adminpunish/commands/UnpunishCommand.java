package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class UnpunishCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public UnpunishCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.unpunish")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /unpunish <player>"));
            return true;
        }

        String targetName = args[0];
        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        String staffName = sender instanceof Player p ? p.getName() : "Console";

        // Resolve the real UUID - prefer the online player, otherwise look up
        // their actual Mojang/offline UUID rather than guessing.
        Player target = Bukkit.getPlayer(targetName);
        String uuid = target != null ? target.getUniqueId().toString() : null;

        Offense existing = null;
        if (uuid != null) {
            existing = plugin.getOffenseManager().getActivePunishment(uuid);
        }
        // Fall back to name-based lookup for offline players (covers punishments
        // recorded before this fix, where the stored UUID may not be the real one)
        if (existing == null) {
            for (Offense o : plugin.getOffenseManager().getAllActive()) {
                if (o.getPlayerName().equalsIgnoreCase(targetName)) {
                    existing = o;
                    uuid = o.getPlayerUUID();
                    break;
                }
            }
        }
        // Still nothing? Try resolving their real offline UUID directly.
        if (existing == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            String realUuid = offline.getUniqueId().toString();
            existing = plugin.getOffenseManager().getActivePunishment(realUuid);
            if (existing != null) uuid = realUuid;
        }

        if (existing == null) {
            sender.sendMessage(color(prefix + "&e" + targetName + " &chas no active punishment."));
            return true;
        }

        String typeLabel = switch (existing.getType()) {
            case MUTE -> "mute";
            case BAN -> "ban";
            case IPBAN -> "IP ban";
        };

        // Clear both the UUID punishment and any IP ban - using both the IP stored
        // on the original offense AND the player's current known IP, in case it
        // changed since the ban was issued.
        String currentIp = plugin.getAltManager().getIp(targetName);
        plugin.getOffenseManager().pardon(uuid, currentIp);
        plugin.getHistoryManager().markRemovedEarly(uuid, staffName);

        if (target != null && target.isOnline()) {
            target.sendMessage(color("&aYour " + typeLabel + " has been removed by &b" + staffName + "&a."));
        }

        sender.sendMessage(color(prefix + "&a" + targetName + "'s &f" + typeLabel + " &ahas been removed."));

        Bukkit.broadcast(
                color(prefix + "&f" + targetName + "'s " + typeLabel + " was removed by &b" + staffName + "&f."),
                "adminpunish.unpunish"
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Offense o : plugin.getOffenseManager().getAllActive()) {
                names.add(o.getPlayerName());
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            // Widen suggestions to everyone we have ANY punishment history for,
            // not just currently-active/online players.
            names.addAll(plugin.getHistoryManager().getAllKnownPlayerNames());

            List<String> deduped = new ArrayList<>(new LinkedHashSet<>(names));
            List<String> result = new ArrayList<>();
            for (String s : deduped) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }
        return Collections.emptyList();
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
