package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.HistoryEntry;
import dev.adminpunish.models.Offense;

import java.io.*;
import java.util.*;

/**
 * Keeps a permanent log of every punishment ever issued, separate from
 * OffenseManager (which only tracks the currently active punishment).
 * Used by /history.
 */
public class HistoryManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // UUID -> list of every punishment they've ever received, oldest first
    private final Map<String, List<HistoryEntry>> history = new HashMap<>();

    public HistoryManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "history.json");
        loadData();
    }

    public HistoryEntry record(Offense offense) {
        HistoryEntry entry = new HistoryEntry(
                offense.getPlayerName(), offense.getPlayerUUID(), offense.getPlayerIp(),
                offense.getOffenseKey(), offense.getOffenseDisplay(), offense.getType().name(),
                offense.getStaffName(), offense.getNote(), offense.getStartTime(), offense.getEndTime()
        );
        history.computeIfAbsent(offense.getPlayerUUID(), k -> new ArrayList<>()).add(entry);
        saveData();
        return entry;
    }

    /**
     * Marks the most recent active (not already removed/expired) entry for this
     * UUID as removed early, e.g. when /unpunish is used. Returns true if a
     * matching entry was found and updated.
     */
    public boolean markRemovedEarly(String uuid, String removedBy) {
        List<HistoryEntry> entries = history.get(uuid);
        if (entries == null) return false;
        // Search from the end (most recent first)
        for (int i = entries.size() - 1; i >= 0; i--) {
            HistoryEntry e = entries.get(i);
            if (e.isCurrentlyActive()) {
                e.setRemovedEarly(true);
                e.setRemovedBy(removedBy);
                e.setRemovedAt(System.currentTimeMillis());
                saveData();
                return true;
            }
        }
        return false;
    }

    public List<HistoryEntry> getHistory(String uuid) {
        return history.getOrDefault(uuid, Collections.emptyList());
    }

    /**
     * Finds a UUID by player name (case-insensitive) by scanning history records.
     * Useful for /history lookups on players who've never been online this session.
     */
    public String findUuidByName(String playerName) {
        for (Map.Entry<String, List<HistoryEntry>> e : history.entrySet()) {
            for (HistoryEntry entry : e.getValue()) {
                if (entry.getPlayerName().equalsIgnoreCase(playerName)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /** Returns true if this player currently has an active (non-expired, non-removed) BAN or IPBAN in history. */
    public boolean hasActiveBan(String uuid) {
        List<HistoryEntry> entries = history.get(uuid);
        if (entries == null) return false;
        for (HistoryEntry e : entries) {
            if (!e.isCurrentlyActive()) continue;
            if ("BAN".equals(e.getType()) || "IPBAN".equals(e.getType())) return true;
        }
        return false;
    }

    public Set<String> getAllKnownPlayerNames() {
        Set<String> names = new HashSet<>();
        for (List<HistoryEntry> entries : history.values()) {
            for (HistoryEntry e : entries) names.add(e.getPlayerName());
        }
        return names;
    }

    // ---- Persistence ----

    private static class StorageData {
        Map<String, List<HistoryEntry>> history = new HashMap<>();
    }

    public void saveData() {
        StorageData data = new StorageData();
        data.history = history;
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save history data: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null || data.history == null) return;
            history.putAll(data.history);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load history data: " + e.getMessage());
        }
    }
}
