package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.adminpunish.AdminPunish;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "vanish.json");
        loadData();
    }

    public void vanish(Player player) {
        vanished.add(player.getUniqueId());
        hideFromOthers(player);
        player.setPlayerListName(null);
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &aYou are now vanished."));
        player.sendTitle(color("&b&lVANISHED"), color("&7You are now hidden from players."), 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.6f);
        saveData();
    }

    public void unvanish(Player player) {
        vanished.remove(player.getUniqueId());
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &cYou are no longer vanished."));
        player.sendTitle(color("&7&lVISIBLE"), color("&7Players can see you again."), 10, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.0f);
        saveData();
    }

    /** Hides this vanished player from every other currently-online non-op player. */
    private void hideFromOthers(Player player) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.isOp() && !other.equals(player)) {
                other.hidePlayer(plugin, player);
            }
        }
    }

    /**
     * Called when a player rejoins after a server restart - if they were vanished
     * before the restart, silently re-applies the hide-from-others effect without
     * re-sending the "you are now vanished" message/sound (avoids spamming them
     * on every reconnect).
     */
    public void reapplyVanishOnRejoin(Player player) {
        if (!isVanished(player.getUniqueId())) return;
        hideFromOthers(player);
        player.setPlayerListName(null);
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &7You're still vanished from before the restart."));
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public void applyVanishOnJoin(Player joining) {
        // When a non-op player joins, hide all vanished players from them
        for (UUID uuid : vanished) {
            Player vanishedPlayer = Bukkit.getPlayer(uuid);
            if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
                if (!joining.isOp()) {
                    joining.hidePlayer(plugin, vanishedPlayer);
                }
            }
        }
    }

    public Set<UUID> getVanished() { return vanished; }

    private String color(String s) { return s.replace("&", "\u00a7"); }

    // ---- Persistence ----

    private static class StorageData {
        List<String> vanished;
    }

    public void saveData() {
        StorageData data = new StorageData();
        data.vanished = vanished.stream().map(UUID::toString).toList();
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save vanish data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null || data.vanished == null) return;
            for (String s : data.vanished) {
                try { vanished.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load vanish data: " + e.getMessage());
        }
    }
}
