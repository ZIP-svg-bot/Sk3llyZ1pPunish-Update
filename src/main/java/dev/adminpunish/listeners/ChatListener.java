package dev.adminpunish.listeners;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class ChatListener implements Listener {

    private final AdminPunish plugin;

    public ChatListener(AdminPunish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        if (plugin.getOffenseManager().isMuted(uuid)) {
            event.setCancelled(true);
            sendMuteMessage(event.getPlayer());
        }
    }

    /**
     * Default-deny: while muted, ALL commands are blocked except whatever is
     * explicitly listed in config under "muted-allowed-commands". This is safer
     * than a hardcoded blacklist of chat-like commands, since it can't be
     * silently bypassed by some other plugin adding a new chat-like command
     * (e.g. /shout, /global) that this plugin doesn't know to block.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        if (!plugin.getOffenseManager().isMuted(uuid)) return;

        String message = event.getMessage();
        // First token after the slash, lowercase, ignoring any plugin: prefix or args.
        String base = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = base.indexOf(':');
        if (colon != -1) base = base.substring(colon + 1);

        List<String> allowed = plugin.getConfig().getStringList("muted-allowed-commands");
        for (String allowedCmd : allowed) {
            if (base.equalsIgnoreCase(allowedCmd)) return; // allowed, let it through
        }

        event.setCancelled(true);
        sendMuteMessage(event.getPlayer());
    }

    private void sendMuteMessage(org.bukkit.entity.Player player) {
        Offense o = plugin.getOffenseManager().getActivePunishment(player.getUniqueId().toString());
        String timeLeft = o != null ? o.getTimeRemaining() : "Unknown";
        player.sendMessage(color("&cYou are muted. &7Time remaining: &e" + timeLeft));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
