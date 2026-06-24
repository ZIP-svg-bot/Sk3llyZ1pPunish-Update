package dev.adminpunish.utils;

import dev.adminpunish.AdminPunish;
import dev.adminpunish.models.Offense;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class WebhookUtil {

    private final AdminPunish plugin;

    public WebhookUtil(AdminPunish plugin) {
        this.plugin = plugin;
    }

    public void sendOffenseAlert(Offense offense) {
        String webhookUrl = plugin.getConfig().getString("discord-webhook", "");
        if (webhookUrl.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String color = getColorInt(offense.getType());
                String typeLabel = offense.getType() == Offense.PunishmentType.MUTE ? "🔇 MUTE" :
                        offense.getType() == Offense.PunishmentType.IPBAN ? "🔨 IP BAN" : "🔨 BAN";

                StringBuilder fields = new StringBuilder();
                fields.append("{\"name\": \"👤 Player\", \"value\": \"").append(escape(offense.getPlayerName())).append("\", \"inline\": true},")
                      .append("{\"name\": \"📋 Reason\", \"value\": \"").append(escape(offense.getOffenseDisplay())).append("\", \"inline\": true},")
                      .append("{\"name\": \"⏱ Duration\", \"value\": \"").append(escape(offense.getTimeRemaining())).append("\", \"inline\": true},")
                      .append("{\"name\": \"🌐 IP Address\", \"value\": \"||").append(escape(offense.getPlayerIp())).append("||\", \"inline\": true},")
                      .append("{\"name\": \"👮 Staff\", \"value\": \"").append(escape(offense.getStaffName())).append("\", \"inline\": true}");
                if (offense.getNote() != null && !offense.getNote().isBlank()) {
                    fields.append(",{\"name\": \"📝 Note\", \"value\": \"").append(escape(offense.getNote())).append("\", \"inline\": false}");
                }

                String json = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + typeLabel + " — " + escape(offense.getPlayerName()) + "\","
                        + "\"color\": " + color + ","
                        + "\"fields\": [" + fields + "],"
                        + "\"footer\": {\"text\": \"AdminPunish\"},"
                        + "\"timestamp\": \"" + Instant.now() + "\""
                        + "}]"
                        + "}";

                HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                conn.getResponseCode(); // fire and forget
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String getColorInt(Offense.PunishmentType type) {
        return switch (type) {
            case BAN -> "15158332";    // red
            case IPBAN -> "10038562";  // dark red
            case MUTE -> "16776960";   // yellow
        };
    }

    private String escape(String s) {
        if (s == null) return "N/A";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
