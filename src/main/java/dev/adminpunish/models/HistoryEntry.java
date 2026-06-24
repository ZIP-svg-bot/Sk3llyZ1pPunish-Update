package dev.adminpunish.models;

/**
 * An immutable-ish log record of a punishment that was issued.
 * Unlike Offense (which only tracks the CURRENTLY active punishment for a
 * player), HistoryEntry records are kept forever so staff can review a
 * player's full punishment record with /history, even after the punishment
 * has expired or been removed with /unpunish.
 */
public class HistoryEntry {
    private String playerName;
    private String playerUUID;
    private String playerIp;
    private String offenseKey;
    private String offenseDisplay;
    private String type; // Offense.PunishmentType name
    private String staffName;
    private String note; // nullable
    private long startTime;
    private long endTime; // -1 = permanent

    private boolean removedEarly;
    private String removedBy; // nullable
    private long removedAt; // 0 if not removed early

    public HistoryEntry() {
    }

    public HistoryEntry(String playerName, String playerUUID, String playerIp,
                         String offenseKey, String offenseDisplay, String type,
                         String staffName, String note, long startTime, long endTime) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.playerIp = playerIp;
        this.offenseKey = offenseKey;
        this.offenseDisplay = offenseDisplay;
        this.type = type;
        this.staffName = staffName;
        this.note = note;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isPermanent() { return endTime == -1; }

    public boolean isCurrentlyActive() {
        if (removedEarly) return false;
        if (endTime == -1) return true;
        return System.currentTimeMillis() <= endTime;
    }

    public String getStatusLabel() {
        if (removedEarly) return "Removed early by " + removedBy;
        if (isCurrentlyActive()) return "Active";
        return "Expired";
    }

    // Getters / setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(String playerUUID) { this.playerUUID = playerUUID; }

    public String getPlayerIp() { return playerIp; }
    public void setPlayerIp(String playerIp) { this.playerIp = playerIp; }

    public String getOffenseKey() { return offenseKey; }
    public void setOffenseKey(String offenseKey) { this.offenseKey = offenseKey; }

    public String getOffenseDisplay() { return offenseDisplay; }
    public void setOffenseDisplay(String offenseDisplay) { this.offenseDisplay = offenseDisplay; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isRemovedEarly() { return removedEarly; }
    public void setRemovedEarly(boolean removedEarly) { this.removedEarly = removedEarly; }

    public String getRemovedBy() { return removedBy; }
    public void setRemovedBy(String removedBy) { this.removedBy = removedBy; }

    public long getRemovedAt() { return removedAt; }
    public void setRemovedAt(long removedAt) { this.removedAt = removedAt; }
}
