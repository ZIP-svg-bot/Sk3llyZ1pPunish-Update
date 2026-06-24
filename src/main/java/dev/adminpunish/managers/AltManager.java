package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.adminpunish.AdminPunish;

import java.io.*;
import java.util.*;

public class AltManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // IP -> Set of player names that logged in from that IP
    private final Map<String, Set<String>> ipToNames = new HashMap<>();
    // PlayerName (lowercase) -> last known IP
    private final Map<String, String> nameToIp = new HashMap<>();

    public AltManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "alts.json");
        loadData();
    }

    public void recordLogin(String playerName, String ip) {
        nameToIp.put(playerName.toLowerCase(), ip);
        ipToNames.computeIfAbsent(ip, k -> new HashSet<>()).add(playerName);
        saveData();
    }

    public String getIp(String playerName) {
        return nameToIp.get(playerName.toLowerCase());
    }

    public Set<String> getAlts(String playerName) {
        String ip = nameToIp.get(playerName.toLowerCase());
        if (ip == null) return Collections.emptySet();
        Set<String> alts = new HashSet<>(ipToNames.getOrDefault(ip, Collections.emptySet()));
        alts.remove(playerName); // remove self
        return alts;
    }

    /** All names ever seen logging in from this exact IP (regardless of stored "last IP" for any one name). */
    public Set<String> getNamesOnIp(String ip) {
        return new HashSet<>(ipToNames.getOrDefault(ip, Collections.emptySet()));
    }

    /** Every player name we've ever recorded a login for, correctly-cased. */
    public Set<String> getAllKnownNames() {
        Set<String> names = new HashSet<>();
        for (Set<String> nameSet : ipToNames.values()) {
            names.addAll(nameSet);
        }
        return names;
    }

    // ---- Persistence ----

    private static class StorageData {
        Map<String, List<String>> ipToNames = new HashMap<>();
        Map<String, String> nameToIp = new HashMap<>();
    }

    public void saveData() {
        StorageData data = new StorageData();
        for (Map.Entry<String, Set<String>> e : ipToNames.entrySet()) {
            data.ipToNames.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        data.nameToIp = new HashMap<>(nameToIp);
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save alt data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null) return;
            if (data.ipToNames != null) {
                for (Map.Entry<String, List<String>> e : data.ipToNames.entrySet()) {
                    ipToNames.put(e.getKey(), new HashSet<>(e.getValue()));
                }
            }
            if (data.nameToIp != null) nameToIp.putAll(data.nameToIp);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load alt data: " + e.getMessage());
        }
    }
}
