package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Warning;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public WarnCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.warn")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /warn <player> <reason...>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.asList(args).subList(1, args.length));
        String staffName = sender instanceof Player p ? p.getName() : "Console";

        Player target = Bukkit.getPlayer(targetName);
        String uuid;
        String resolvedName = targetName;
        if (target != null) {
            uuid = target.getUniqueId().toString();
            resolvedName = target.getName();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            uuid = offline.getUniqueId().toString();
            if (offline.getName() != null) resolvedName = offline.getName();
        }

        Warning warning = plugin.getWarnManager().warn(resolvedName, uuid, reason, staffName);
        int count = plugin.getWarnManager().getWarningCount(uuid);

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");

        sender.sendMessage(color(prefix + "&a" + resolvedName + " has been warned. &7(Total warnings: &e" + count + "&7)"));

        if (target != null && target.isOnline()) {
            target.sendMessage(color("&e&lYou have been warned.\n&fReason: &7" + reason
                    + "\n&fStaff: &7" + staffName + "\n&fTotal warnings: &7" + count));
            target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        Bukkit.broadcast(
                color(prefix + "&f" + resolvedName + " was warned by &b" + staffName
                        + " &ffor &e" + reason + " &7(#" + count + ")"),
                "adminpunish.warn"
        );

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
