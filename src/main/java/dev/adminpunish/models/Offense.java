package dev.adminpunish.models;

public class Offense {
    private final String playerName;
    private final String playerUUID;
    private final String playerIp;
    private final String offenseKey;
    private final String offenseDisplay;
    private final PunishmentType type;
    private final long startTime;
    private final long endTime; // -1 = permanent
    private final String staffName;
    private final String note; // nullable

    public enum PunishmentType {
        BAN, IPBAN, MUTE
    }

    public Offense(String playerName, String playerUUID, String playerIp,
                   String offenseKey, String offenseDisplay,
                   PunishmentType type, long durationMinutes, String staffName) {
        this(playerName, playerUUID, playerIp, offenseKey, offenseDisplay, type, durationMinutes, staffName, null);
    }

    public Offense(String playerName, String playerUUID, String playerIp,
                   String offenseKey, String offenseDisplay,
                   PunishmentType type, long durationMinutes, String staffName, String note) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.playerIp = playerIp;
        this.offenseKey = offenseKey;
        this.offenseDisplay = offenseDisplay;
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.endTime = durationMinutes == -1 ? -1 : startTime + (durationMinutes * 60_000L);
        this.staffName = staffName;
        this.note = note;
    }

    // For loading from storage
    public Offense(String playerName, String playerUUID, String playerIp,
                   String offenseKey, String offenseDisplay,
                   PunishmentType type, long startTime, long endTime, String staffName) {
        this(playerName, playerUUID, playerIp, offenseKey, offenseDisplay, type, startTime, endTime, staffName, null);
    }

    public Offense(String playerName, String playerUUID, String playerIp,
                   String offenseKey, String offenseDisplay,
                   PunishmentType type, long startTime, long endTime, String staffName, String note) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.playerIp = playerIp;
        this.offenseKey = offenseKey;
        this.offenseDisplay = offenseDisplay;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.staffName = staffName;
        this.note = note;
    }

    public boolean isExpired() {
        if (endTime == -1) return false;
        return System.currentTimeMillis() > endTime;
    }

    public boolean isPermanent() { return endTime == -1; }

    public String getTimeRemaining() {
        if (endTime == -1) return "Permanent";
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long days = remaining / 86_400_000L;
        long hours = (remaining % 86_400_000L) / 3_600_000L;
        long minutes = (remaining % 3_600_000L) / 60_000L;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public String getBanMessage() {
        String header = "\n\n&c&lYou have been punished.\n";
        String reason = "&fReason: &e" + offenseDisplay + "\n";
        String duration = "&fDuration: &e" + getTimeRemaining() + "\n";
        String staff = "&fStaff: &e" + staffName + "\n";
        String footer = "\n&7Appeal at your server's discord.";
        return colorize(header + reason + duration + staff + footer);
    }

    private String colorize(String s) {
        return s.replace("&", "\u00a7");
    }

    // Getters
    public String getPlayerName() { return playerName; }
    public String getPlayerUUID() { return playerUUID; }
    public String getPlayerIp() { return playerIp; }
    public String getOffenseKey() { return offenseKey; }
    public String getOffenseDisplay() { return offenseDisplay; }
    public PunishmentType getType() { return type; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getStaffName() { return staffName; }
    public String getNote() { return note; }
}
