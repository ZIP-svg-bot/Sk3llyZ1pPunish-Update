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
import java.util.List;

/**
 * /offendgui <player> - opens a clickable GUI to punish the named player.
 * This is intentionally a SEPARATE command from /offend, which keeps working
 * exactly as before for staff who prefer typing the full command.
 */
public class OffendGuiCommand implements CommandExecutor, TabCompleter {

    private final AdminPunish plugin;

    public OffendGuiCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player staff)) {
            sender.sendMessage("This command is players only.");
            return true;
        }
        if (!staff.hasPermission("adminpunish.offend")) {
            staff.sendMessage(color("&cYou don't have permission to do that."));
            return true;
        }
        if (args.length < 1) {
            staff.sendMessage(color("&cUsage: /offendgui <player>"));
            return true;
        }

        plugin.getOffendGUI().open(staff, args[0]);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
