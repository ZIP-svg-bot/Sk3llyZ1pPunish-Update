package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final AdminPunish plugin;

    public VanishCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is players only.");
            return true;
        }
        if (!player.hasPermission("adminpunish.vanish")) {
            player.sendMessage(color("&cYou don't have permission."));
            return true;
        }

        if (plugin.getVanishManager().isVanished(player.getUniqueId())) {
            plugin.getVanishManager().unvanish(player);
        } else {
            plugin.getVanishManager().vanish(player);
        }
        return true;
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
