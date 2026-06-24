package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.HistoryEntry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy HH:mm");

    public HistoryCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.history")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /history <player>"));
            return true;
        }

        String targetName = args[0];
        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");

        // Resolve UUID: online player > known offline UUID > history-name lookup.
        Player online = Bukkit.getPlayer(targetName);
        String uuid;
        if (online != null) {
            uuid = online.getUniqueId().toString();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            uuid = offline.getUniqueId().toString();
            if (plugin.getHistoryManager().getHistory(uuid).isEmpty()) {
                String byName = plugin.getHistoryManager().findUuidByName(targetName);
                if (byName != null) uuid = byName;
            }
        }

        List<HistoryEntry> history = new ArrayList<>(plugin.getHistoryManager().getHistory(uuid));

        sender.sendMessage(color(prefix + "&bPunishment History for &f" + targetName
                + " &7(" + history.size() + " total)"));

        if (history.isEmpty()) {
            sender.sendMessage(color("&7No punishment history found."));
            return true;
        }

        // Most recent first
        history.sort((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()));

        int shown = 0;
        for (HistoryEntry e : history) {
            if (shown >= 15) {
                sender.sendMessage(color("&7... and " + (history.size() - shown) + " more."));
                break;
            }
            String typeColor = switch (e.getType()) {
                case "MUTE" -> "&e";
                case "IPBAN" -> "&4";
                default -> "&c";
            };
            String duration = e.isPermanent() ? "Permanent" : formatDuration(e.getEndTime() - e.getStartTime());
            sender.sendMessage(color("&8[" + dateFormat.format(new Date(e.getStartTime())) + "] "
                    + typeColor + e.getType() + " &7- &f" + e.getOffenseDisplay()
                    + " &7(" + duration + ") &7by &b" + e.getStaffName()
                    + " &7[" + e.getStatusLabel() + "]"));
            if (e.getNote() != null && !e.getNote().isBlank()) {
                sender.sendMessage(color("    &8note: &7" + e.getNote()));
            }
            shown++;
        }

        return true;
    }

    private String formatDuration(long millis) {
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
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
