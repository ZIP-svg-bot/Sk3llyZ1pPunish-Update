package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Warning;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class WarnHistoryCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm");

    public WarnHistoryCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.warn")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /warnhistory <player>"));
            return true;
        }

        String targetName = args[0];
        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");

        Player online = Bukkit.getPlayer(targetName);
        String uuid;
        if (online != null) {
            uuid = online.getUniqueId().toString();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            uuid = offline.getUniqueId().toString();
            if (plugin.getWarnManager().getWarnings(uuid).isEmpty()) {
                String byName = plugin.getWarnManager().findUuidByName(targetName);
                if (byName != null) uuid = byName;
            }
        }

        List<Warning> warnings = new ArrayList<>(plugin.getWarnManager().getWarnings(uuid));
        warnings.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));

        sender.sendMessage(color(prefix + "&bWarning History for &f" + targetName
                + " &7(" + warnings.size() + " total)"));

        if (warnings.isEmpty()) {
            sender.sendMessage(color("&7No warnings found."));
            return true;
        }

        int shown = 0;
        for (Warning w : warnings) {
            if (shown >= 15) {
                sender.sendMessage(color("&7... and " + (warnings.size() - shown) + " more."));
                break;
            }
            sender.sendMessage(color("&8[" + dateFormat.format(new Date(w.getTime())) + "] "
                    + "&e" + w.getReason() + " &7by &b" + w.getStaffName()));
            shown++;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            names.addAll(plugin.getWarnManager().getAllKnownPlayerNames());
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
