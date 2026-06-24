package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.managers.OffenseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BanHammerCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public BanHammerCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is players only.");
            return true;
        }
        if (!player.hasPermission("adminpunish.banhammer")) {
            player.sendMessage(color("&cYou don't have permission."));
            return true;
        }

        // /unhammer
        if (label.equalsIgnoreCase("unhammer")) {
            if (!plugin.getBanHammerManager().hasHammer(player)) {
                player.sendMessage(color("&cYou don't have the Ban Hammer equipped."));
                return true;
            }
            plugin.getBanHammerManager().removeHammer(player);
            return true;
        }

        // /banhammer <offense>
        if (args.length < 1) {
            player.sendMessage(color("&cUsage: /banhammer <offense>"));
            player.sendMessage(color("&7Available offenses:"));
            for (OffenseManager.OffenseDef def : plugin.getOffenseManager().getOffenseDefs().values()) {
                player.sendMessage(color("  &b" + def.key + " &8- &f" + def.display));
            }
            return true;
        }

        String offenseKey = args[0].toUpperCase();
        OffenseManager.OffenseDef def = plugin.getOffenseManager().getOffenseDef(offenseKey);
        if (def == null) {
            player.sendMessage(color("&cUnknown offense: &e" + offenseKey));
            return true;
        }

        // If already holding a hammer, remove it first
        if (plugin.getBanHammerManager().hasHammer(player)) {
            plugin.getBanHammerManager().removeHammer(player);
        }

        plugin.getBanHammerManager().giveHammer(player, offenseKey);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (String key : plugin.getOffenseManager().getOffenseDefs().keySet()) {
                if (key.toLowerCase().startsWith(args[0].toLowerCase())) result.add(key);
            }
            return result;
        }
        return Collections.emptyList();
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
