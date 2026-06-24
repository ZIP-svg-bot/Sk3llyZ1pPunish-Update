package dev.adminpunish.commands;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.gui.OffensesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OffensesCommand implements CommandExecutor {

    private final AdminPunish plugin;

    public OffensesCommand(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }
        if (!player.hasPermission("adminpunish.offenses")) {
            player.sendMessage("\u00a7cYou don't have permission to do that.");
            return true;
        }
        new OffensesGUI(plugin).open(player);
        return true;
    }
}
