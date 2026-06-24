package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Warning;

import java.io.*;
import java.util.*;

public class WarnManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // UUID -> all warnings they've ever received, oldest first
    private final Map<String, List<Warning>> warnings = new HashMap<>();

    public WarnManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "warnings.json");
        loadData();
    }

    public Warning warn(String playerName, String playerUUID, String reason, String staffName) {
        Warning warning = new Warning(playerName, playerUUID, reason, staffName, System.currentTimeMillis());
        warnings.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(warning);
        saveData();
        return warning;
    }

    public List<Warning> getWarnings(String uuid) {
        return warnings.getOrDefault(uuid, Collections.emptyList());
    }

    public int getWarningCount(String uuid) {
        return getWarnings(uuid).size();
    }

    public String findUuidByName(String playerName) {
        for (Map.Entry<String, List<Warning>> e : warnings.entrySet()) {
            for (Warning w : e.getValue()) {
                if (w.getPlayerName().equalsIgnoreCase(playerName)) return e.getKey();
            }
        }
        return null;
    }

    public Set<String> getAllKnownPlayerNames() {
        Set<String> names = new HashSet<>();
        for (List<Warning> list : warnings.values()) {
            for (Warning w : list) names.add(w.getPlayerName());
        }
        return names;
    }

    // ---- Persistence ----

    private static class StorageData {
        Map<String, List<Warning>> warnings = new HashMap<>();
    }

    public void saveData() {
        StorageData data = new StorageData();
        data.warnings = warnings;
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save warning data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null || data.warnings == null) return;
            warnings.putAll(data.warnings);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load warning data: " + e.getMessage());
        }
    }
}
