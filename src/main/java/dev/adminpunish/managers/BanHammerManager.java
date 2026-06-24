package dev.adminpunish.managers;

import dev.adminpunish.AdminPunish;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BanHammerManager {

    private final AdminPunish plugin;
    private final NamespacedKey hammerKey;
    private final NamespacedKey offenseKey;

    // UUID -> saved inventory contents
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    // UUID -> offense key they armed with
    private final Map<UUID, String> armedOffense = new HashMap<>();
    // Staff UUID -> pending permanent-offense confirmation (target UUID + expiry millis)
    private final Map<UUID, PendingConfirm> pendingConfirms = new HashMap<>();

    public static class PendingConfirm {
        public final UUID targetUuid;
        public final long expiresAt;
        public PendingConfirm(UUID targetUuid, long expiresAt) {
            this.targetUuid = targetUuid;
            this.expiresAt = expiresAt;
        }
    }

    public BanHammerManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.hammerKey = new NamespacedKey(plugin, "banhammer");
        this.offenseKey = new NamespacedKey(plugin, "banhammer_offense");
    }

    public ItemStack createHammer(String offense) {
        ItemStack hammer = new ItemStack(Material.MACE);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&4&lBAN HAMMER"));
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Offense: &c" + offense));
            lore.add(color("&7Right-click a player to punish them."));
            lore.add(color("&8&oUse /unhammer to put it away."));
            meta.setLore(lore);

            meta.addEnchant(Enchantment.SHARPNESS, 255, true);
            meta.addEnchant(Enchantment.UNBREAKING, 255, true);
            meta.addEnchant(Enchantment.MENDING, 255, true);
            try {
                // DENSITY is 1.21+ only
                Enchantment density = Enchantment.getByKey(NamespacedKey.minecraft("density"));
                if (density != null) meta.addEnchant(density, 255, true);
            } catch (Exception ignored) {}

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            meta.getPersistentDataContainer().set(hammerKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(offenseKey, PersistentDataType.STRING, offense);

            hammer.setItemMeta(meta);
        }
        return hammer;
    }

    public boolean isBanHammer(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(hammerKey, PersistentDataType.BYTE);
    }

    public String getHammerOffense(ItemStack item) {
        if (!isBanHammer(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(offenseKey, PersistentDataType.STRING);
    }

    public void giveHammer(Player player, String offense) {
        // Save inventory
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
        armedOffense.put(player.getUniqueId(), offense);

        // Clear inventory and give hammer in first slot
        player.getInventory().clear();
        player.getInventory().setItem(0, createHammer(offense));
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &cBan Hammer armed! &7Right-click a player to punish them for &e" + offense + "&7."));
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &7Use &c/unhammer &7to put it away."));
    }

    public void removeHammer(Player player) {
        UUID uuid = player.getUniqueId();
        // Remove any hammer items
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isBanHammer(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        // Restore inventory
        ItemStack[] saved = savedInventories.remove(uuid);
        if (saved != null) {
            player.getInventory().setContents(saved);
        }
        armedOffense.remove(uuid);
        player.sendMessage(color("&8[&bSk3llyZ1ps&8] &aBan Hammer put away. Inventory restored."));
    }

    public boolean hasHammer(Player player) {
        return armedOffense.containsKey(player.getUniqueId());
    }

    public String getArmedOffense(Player player) {
        return armedOffense.get(player.getUniqueId());
    }

    public NamespacedKey getHammerKey() { return hammerKey; }

    // ---- Permanent offense confirmation flow ----

    /**
     * Records that a staff member just right-clicked a target with a hammer
     * armed with a permanent offense, and is awaiting a second confirming click.
     */
    public void requestConfirm(Player staff, Player target, int timeoutSeconds) {
        pendingConfirms.put(staff.getUniqueId(),
                new PendingConfirm(target.getUniqueId(), System.currentTimeMillis() + timeoutSeconds * 1000L));
    }

    /**
     * Returns true if this staff member has a still-valid pending confirmation
     * for exactly this target (i.e. their previous click was on the same player).
     */
    public boolean isConfirmed(Player staff, Player target) {
        PendingConfirm pending = pendingConfirms.get(staff.getUniqueId());
        if (pending == null) return false;
        if (System.currentTimeMillis() > pending.expiresAt) {
            pendingConfirms.remove(staff.getUniqueId());
            return false;
        }
        return pending.targetUuid.equals(target.getUniqueId());
    }

    public void clearConfirm(Player staff) {
        pendingConfirms.remove(staff.getUniqueId());
    }

    private String color(String s) { return s.replace("&", "\u00a7"); }
}
