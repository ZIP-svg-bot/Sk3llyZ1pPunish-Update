package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import dev.adminpunish.models.Offense.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class OffenseManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // UUID -> active offense
    private final Map<String, Offense> activePunishments = new HashMap<>();
    // IP -> active IPBAN offense
    private final Map<String, Offense> ipBans = new HashMap<>();

    public OffenseManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "offenses.json");
        loadData();
    }

    // ---- Config offense definitions ----

    public static class OffenseDef {
        public final String key;
        public final String display;
        public final PunishmentType type;
        public final long durationMinutes;

        public OffenseDef(String key, String display, PunishmentType type, long durationMinutes) {
            this.key = key;
            this.display = display;
            this.type = type;
            this.durationMinutes = durationMinutes;
        }
    }

    public Map<String, OffenseDef> getOffenseDefs() {
        Map<String, OffenseDef> defs = new LinkedHashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("offenses");
        if (sec == null) return defs;
        for (String key : sec.getKeys(false)) {
            String display = sec.getString(key + ".display", key);
            String typeStr = sec.getString(key + ".type", "BAN");
            long duration = sec.getLong(key + ".duration", 1440);
            PunishmentType type;
            try { type = PunishmentType.valueOf(typeStr); }
            catch (Exception e) { type = PunishmentType.BAN; }
            defs.put(key.toUpperCase(), new OffenseDef(key.toUpperCase(), display, type, duration));
        }
        return defs;
    }

    public OffenseDef getOffenseDef(String key) {
        return getOffenseDefs().get(key.toUpperCase());
    }

    public boolean isPermanent(String offenseKey) {
        OffenseDef def = getOffenseDef(offenseKey);
        return def != null && def.durationMinutes == -1;
    }

    // ---- Punish ----

    public Offense punish(String playerName, String playerUUID, String playerIp,
                          String offenseKey, String staffName) {
        return punish(playerName, playerUUID, playerIp, offenseKey, staffName, null);
    }

    public Offense punish(String playerName, String playerUUID, String playerIp,
                          String offenseKey, String staffName, String note) {
        OffenseDef def = getOffenseDef(offenseKey);
        if (def == null) return null;

        Offense offense = new Offense(
                playerName, playerUUID, playerIp,
                def.key, def.display, def.type,
                def.durationMinutes, staffName, note
        );

        if (def.type == PunishmentType.IPBAN) {
            ipBans.put(playerIp, offense);
        }
        activePunishments.put(playerUUID, offense);
        saveData();
        return offense;
    }

    // ---- Checks ----

    public boolean isBanned(String uuid) {
        Offense o = activePunishments.get(uuid);
        if (o == null) return false;
        if (o.getType() == PunishmentType.MUTE) return false;
        if (o.isExpired()) { activePunishments.remove(uuid); saveData(); return false; }
        return true;
    }

    public boolean isIpBanned(String ip) {
        Offense o = ipBans.get(ip);
        if (o == null) return false;
        if (o.isExpired()) { ipBans.remove(ip); saveData(); return false; }
        return true;
    }

    public boolean isMuted(String uuid) {
        Offense o = activePunishments.get(uuid);
        if (o == null) return false;
        if (o.getType() != PunishmentType.MUTE) return false;
        if (o.isExpired()) { activePunishments.remove(uuid); saveData(); return false; }
        return true;
    }

    public Offense getActivePunishment(String uuid) {
        Offense o = activePunishments.get(uuid);
        if (o != null && o.isExpired()) { activePunishments.remove(uuid); saveData(); return null; }
        return o;
    }

    public Offense getIpBan(String ip) {
        Offense o = ipBans.get(ip);
        if (o != null && o.isExpired()) { ipBans.remove(ip); saveData(); return null; }
        return o;
    }

    public void pardon(String uuid) {
        pardon(uuid, null);
    }

    /**
     * Removes the active punishment for this UUID. Also clears any IP ban tied
     * to the punishment's stored IP, AND (if provided) the player's current
     * known IP - covering the case where their IP changed since the ban was issued.
     */
    public void pardon(String uuid, String currentKnownIp) {
        Offense o = activePunishments.remove(uuid);
        if (o != null) {
            ipBans.remove(o.getPlayerIp());
        }
        if (currentKnownIp != null) {
            ipBans.remove(currentKnownIp);
        }
        saveData();
    }

    // Pardon by player name for offline players (kept for internal/API use)
    public boolean pardonByName(String playerName) {
        Offense found = null;
        String foundUuid = null;
        for (Map.Entry<String, Offense> entry : activePunishments.entrySet()) {
            if (entry.getValue().getPlayerName().equalsIgnoreCase(playerName)) {
                found = entry.getValue();
                foundUuid = entry.getKey();
                break;
            }
        }
        if (found == null) return false;
        activePunishments.remove(foundUuid);
        ipBans.remove(found.getPlayerIp());
        saveData();
        return true;
    }

    public Collection<Offense> getAllActive() {
        List<Offense> list = new ArrayList<>();
        for (Offense o : activePunishments.values()) {
            if (!o.isExpired()) list.add(o);
        }
        return list;
    }

    // ---- Persistence ----

    private static class StorageData {
        List<OffenseData> offenses = new ArrayList<>();
        List<OffenseData> ipBans = new ArrayList<>();
    }

    private static class OffenseData {
        String playerName, playerUUID, playerIp, offenseKey, offenseDisplay, type, staffName;
        long startTime, endTime;
    }

    public void saveData() {
        StorageData data = new StorageData();
        for (Offense o : activePunishments.values()) {
            if (!o.isExpired()) data.offenses.add(toData(o));
        }
        for (Offense o : ipBans.values()) {
            if (!o.isExpired()) data.ipBans.add(toData(o));
        }
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save offense data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null) return;
            if (data.offenses != null) {
                for (OffenseData d : data.offenses) {
                    Offense o = fromData(d);
                    if (!o.isExpired()) activePunishments.put(o.getPlayerUUID(), o);
                }
            }
            if (data.ipBans != null) {
                for (OffenseData d : data.ipBans) {
                    Offense o = fromData(d);
                    if (!o.isExpired()) ipBans.put(o.getPlayerIp(), o);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load offense data: " + e.getMessage());
        }
    }

    private OffenseData toData(Offense o) {
        OffenseData d = new OffenseData();
        d.playerName = o.getPlayerName(); d.playerUUID = o.getPlayerUUID();
        d.playerIp = o.getPlayerIp(); d.offenseKey = o.getOffenseKey();
        d.offenseDisplay = o.getOffenseDisplay(); d.type = o.getType().name();
        d.startTime = o.getStartTime(); d.endTime = o.getEndTime();
        d.staffName = o.getStaffName();
        return d;
    }

    private Offense fromData(OffenseData d) {
        return new Offense(d.playerName, d.playerUUID, d.playerIp,
                d.offenseKey, d.offenseDisplay,
                PunishmentType.valueOf(d.type), d.startTime, d.endTime, d.staffName);
    }
}
