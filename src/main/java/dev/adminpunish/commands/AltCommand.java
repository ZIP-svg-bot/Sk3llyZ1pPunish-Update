package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AltCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public AltCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.alt")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /alt <player>"));
            return true;
        }

        String targetName = args[0];
        String ip = plugin.getAltManager().getIp(targetName);
        Set<String> alts = plugin.getAltManager().getAlts(targetName);

        String prefix = plugin.getConfig().getString("prefix", "&8[&bAdminPunish&8] &r");

        sender.sendMessage(color(prefix + "&bAlt Report for &f" + targetName));
        sender.sendMessage(color("&7IP Address: &f||" + (ip != null ? ip : "Unknown") + "||"));

        if (alts.isEmpty()) {
            sender.sendMessage(color("&7No alt accounts found."));
        } else {
            sender.sendMessage(color("&7Known alts (&e" + alts.size() + "&7):"));
            for (String alt : alts) {
                sender.sendMessage(color("  &8- &f" + alt));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Suggest from every name we've ever seen log in, not just online players,
            // so staff can /alt someone who's currently offline.
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            names.addAll(plugin.getAltManager().getAllKnownNames());

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
