package dev.adminpunish.models;

public class Warning {
    private String playerName;
    private String playerUUID;
    private String reason;
    private String staffName;
    private long time;

    public Warning() {
    }

    public Warning(String playerName, String playerUUID, String reason, String staffName, long time) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.reason = reason;
        this.staffName = staffName;
        this.time = time;
    }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(String playerUUID) { this.playerUUID = playerUUID; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
}
