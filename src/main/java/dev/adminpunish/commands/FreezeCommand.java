package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FreezeCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public FreezeCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("adminpunish.freeze")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(color("&cUsage: /freeze <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(color("&cPlayer not found or not online."));
            return true;
        }

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        String staffName = sender instanceof Player p ? p.getName() : "Console";

        if (plugin.getFreezeManager().isFrozen(target.getUniqueId())) {
            plugin.getFreezeManager().unfreeze(target);
            sender.sendMessage(color(prefix + "&a" + target.getName() + " has been unfrozen."));
            target.sendMessage(color("&aYou have been unfrozen by &b" + staffName + "&a."));
            target.playSound(target.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f);
        } else {
            plugin.getFreezeManager().freeze(target);
            sender.sendMessage(color(prefix + "&c" + target.getName() + " has been frozen."));
            String freezeMsg = plugin.getConfig().getString("freeze.freeze-message",
                    "&c&lYou have been frozen by staff.\n&7Do not log out.");
            target.sendMessage(color(freezeMsg));
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.7f);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
