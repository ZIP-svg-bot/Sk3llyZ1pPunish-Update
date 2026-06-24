package dev.adminpunish.utils;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PunishExecutor {

    private final AdminPunish plugin;

    public PunishExecutor(AdminPunish plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies an offense to a target and handles every side effect: saving the
     * punishment, recording it to history, kicking/muting the target if online,
     * firing the Discord webhook, and broadcasting to staff.
     *
     * @param target     the online Player object for the target, or null if offline
     * @param targetName the target's name
     * @param targetUUID the target's UUID as a string (should be their REAL uuid,
     *                   looked up via Bukkit.getOfflinePlayer if they're offline)
     * @param targetIp   the target's last known IP, or "Unknown"
     * @param offenseKey the offense definition key from config.yml
     * @param staffName  the name of the staff member issuing the punishment
     * @param note       an optional staff note attached to this punishment, or null
     * @return the resulting Offense, or null if the offense key was invalid
     */
    public Offense execute(Player target, String targetName, String targetUUID, String targetIp,
                            String offenseKey, String staffName, String note) {
        Offense offense = plugin.getOffenseManager().punish(targetName, targetUUID, targetIp, offenseKey, staffName, note);
        if (offense == null) return null;

        // Permanent record for /history, independent of expiry/pardon
        plugin.getHistoryManager().record(offense);

        // Apply the effect to the target if they're online
        if (target != null && target.isOnline()) {
            if (offense.getType() != Offense.PunishmentType.MUTE) {
                target.kickPlayer(offense.getBanMessage());
            } else {
                target.sendMessage(color("&c&lYou have been muted.\n&fReason: &e"
                        + offense.getOffenseDisplay() + "\n&fDuration: &e" + offense.getTimeRemaining()));
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            }
        }

        plugin.getWebhookUtil().sendOffenseAlert(offense);

        String prefix = plugin.getConfig().getString("prefix", "&8[&bSk3llyZ1ps&8] &r");
        String typeLabel = switch (offense.getType()) {
            case MUTE -> "&emuted";
            case BAN -> "&cbanned";
            case IPBAN -> "&4IP banned";
        };
        Bukkit.broadcast(
                color(prefix + "&f" + targetName + " has been " + typeLabel + " &fby &b" + staffName
                        + " &ffor &e" + offense.getOffenseDisplay()
                        + " &f(" + offense.getTimeRemaining() + ")"),
                "adminpunish.offend"
        );

        return offense;
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
