package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.managers.OffenseManager;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class OffendCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public OffendCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.offend")) {
            sender.sendMessage(color("&cYou don't have permission to do that."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /offend <player> <offense> [note...]"));
            sendOffenseList(sender);
            return true;
        }

        String targetName = args[0];
        String offenseKey = args[1].toUpperCase();

        OffenseManager.OffenseDef def = plugin.getOffenseManager().getOffenseDef(offenseKey);
        if (def == null) {
            sender.sendMessage(color("&cUnknown offense: &e" + offenseKey));
            sendOffenseList(sender);
            return true;
        }

        // Require explicit confirmation for permanent offenses to avoid fat-finger perma-bans.
        boolean requireConfirm = plugin.getConfig().getBoolean("require-confirm-for-permanent", true);
        boolean isPermanent = def.durationMinutes == -1;
        boolean confirmed = args.length >= 3 && args[args.length - 1].equalsIgnoreCase("confirm");

        if (requireConfirm && isPermanent && !confirmed) {
            sender.sendMessage(color("&c&lThis is a PERMANENT offense."));
            sender.sendMessage(color("&7Re-run the command with &fconfirm &7at the end to apply it:"));
            sender.sendMessage(color("&f/offend " + targetName + " " + offenseKey + " confirm"));
            return true;
        }

        // Note is everything after the offense key, minus a trailing "confirm" token.
        int noteEnd = args.length;
        if (confirmed) noteEnd = args.length - 1;
        String note = null;
        if (noteEnd > 2) {
            note = String.join(" ", Arrays.asList(args).subList(2, noteEnd));
        }

        // Resolve the target. Always use their REAL Mojang/offline UUID (not a fake
        // name-based placeholder) so the punishment still matches them correctly
        // if they're offline now and rejoin later.
        Player target = Bukkit.getPlayer(targetName);
        String playerIp = "Unknown";
        String playerUUID;
        String resolvedName = targetName;

        if (target != null) {
            resolvedName = target.getName();
            playerUUID = target.getUniqueId().toString();
            if (target.getAddress() != null) {
                playerIp = target.getAddress().getAddress().getHostAddress();
            }
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            playerUUID = offline.getUniqueId().toString();
            if (offline.getName() != null) resolvedName = offline.getName();
            String knownIp = plugin.getAltManager().getIp(targetName);
            if (knownIp != null) playerIp = knownIp;
        }

        String staffName = sender instanceof Player ? sender.getName() : "Console";

        Offense offense = plugin.getPunishExecutor().execute(
                target, resolvedName, playerUUID, playerIp, offenseKey, staffName, note
        );

        if (offense == null) {
            sender.sendMessage(color("&cFailed to apply offense."));
            return true;
        }

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        sender.sendMessage(color(prefix + "&aOffense applied successfully."));
        return true;
    }

    private void sendOffenseList(CommandSender sender) {
        sender.sendMessage(color("&7Available offenses:"));
        for (OffenseManager.OffenseDef def : plugin.getOffenseManager().getOffenseDefs().values()) {
            sender.sendMessage(color("  &b" + def.key + " &8- &f" + def.display));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            // Also suggest anyone we have punishment history for, so offline targets autocomplete too.
            names.addAll(plugin.getHistoryManager().getAllKnownPlayerNames());
            return filter(dedupe(names), args[0]);
        }
        if (args.length == 2) {
            return filter(new ArrayList<>(plugin.getOffenseManager().getOffenseDefs().keySet()), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> dedupe(List<String> names) {
        return new ArrayList<>(new LinkedHashSet<>(names));
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }

    private String color(String s) {
        return s.replace("&", "\u00a7");
    }
}
