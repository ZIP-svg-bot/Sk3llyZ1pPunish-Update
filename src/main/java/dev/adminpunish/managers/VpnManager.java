package dev.adminpunish.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.adminpunish.AdminPunish;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class VpnManager {

    private final AdminPunish plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File dataFile;

    // IP -> timestamp the result was recorded
    private final Map<String, Long> clearedIps = new HashMap<>();
    private final Map<String, Long> flaggedIps = new HashMap<>();

    public VpnManager(AdminPunish plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "vpn_cache.json");
        loadData();
    }

    public void checkPlayer(Player player, String ip) {
        if (!plugin.getConfig().getBoolean("anti-vpn.enabled", true)) return;
        if (player.hasPermission("adminpunish.bypass.vpn")) return;

        long cacheMillis = plugin.getConfig().getLong("anti-vpn.cache-days", 30) * 86_400_000L;
        long now = System.currentTimeMillis();

        Long clearedAt = clearedIps.get(ip);
        if (clearedAt != null && (now - clearedAt) < cacheMillis) return;

        Long flaggedAt = flaggedIps.get(ip);
        if (flaggedAt != null && (now - flaggedAt) < cacheMillis) {
            kickVpn(player);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                VpnResult result = checkViaProxycheck(ip);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result.isProxy) {
                        flaggedIps.put(ip, System.currentTimeMillis());
                        clearedIps.remove(ip);
                        saveData();
                        plugin.getLogger().info("[AntiVPN] Flagged " + player.getName()
                                + " (" + ip + ") — " + result.reason);
                        kickVpn(player);
                    } else {
                        clearedIps.put(ip, System.currentTimeMillis());
                        flaggedIps.remove(ip);
                        saveData();
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("VPN check failed for " + ip + ": " + e.getMessage());
            }
        });
    }

    private static class VpnResult {
        boolean isProxy;
        String reason;
        VpnResult(boolean isProxy, String reason) {
            this.isProxy = isProxy;
            this.reason = reason;
        }
    }

    private VpnResult checkViaProxycheck(String ip) throws Exception {
        String apiKey = plugin.getConfig().getString("anti-vpn.proxycheck-api-key", "");

        // vpn=3 checks VPN + residential proxies (catches liquid proxies)
        // risk=1 returns risk score
        // asn=1 returns ASN data
        // node=1 returns node info
        String urlStr = apiKey.isEmpty()
                ? "https://proxycheck.io/v2/" + ip + "?vpn=3&risk=1&asn=1&node=1"
                : "https://proxycheck.io/v2/" + ip + "?key=" + apiKey + "&vpn=3&risk=1&asn=1&node=1";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("User-Agent", "Sk3llyZ1psPunishments/1.0");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        String response = sb.toString();

        // Check VPN flag
        if (response.contains("\"proxy\":\"yes\"")) return new VpnResult(true, "VPN detected");
        if (response.contains("\"vpn\":\"yes\"")) return new VpnResult(true, "VPN detected");
        if (response.contains("\"type\":\"VPN\"")) return new VpnResult(true, "VPN type detected");

        // Check residential proxy (liquid proxies use these)
        if (response.contains("\"type\":\"Residential\"") && response.contains("\"proxy\":\"yes\""))
            return new VpnResult(true, "Residential proxy detected");

        // Check risk score - flag anything at/above the configured threshold
        int riskScore = extractRiskScore(response);
        int threshold = plugin.getConfig().getInt("anti-vpn.risk-threshold", 75);
        if (riskScore >= threshold) return new VpnResult(true, "High risk score: " + riskScore);

        // Check known datacenter/hosting ASNs commonly used for liquid proxies
        if (isDatacenterAsn(response)) return new VpnResult(true, "Datacenter/hosting IP");

        return new VpnResult(false, "clean");
    }

    private int extractRiskScore(String response) {
        try {
            int idx = response.indexOf("\"risk\":");
            if (idx == -1) return 0;
            String sub = response.substring(idx + 7);
            StringBuilder num = new StringBuilder();
            for (char c : sub.toCharArray()) {
                if (Character.isDigit(c)) num.append(c);
                else break;
            }
            return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isDatacenterAsn(String response) {
        // Common datacenter ASNs used for proxies/liquid proxies
        String[] datacenterAsns = {
            "AS14061", // DigitalOcean
            "AS16276", // OVH
            "AS24940", // Hetzner
            "AS20473", // Vultr
            "AS8100",  // QuadraNet
            "AS9009",  // M247
            "AS60068", // Datacamp Limited (very common for residential proxies)
            "AS9318",  // SK Broadband (used by some proxy services)
            "AS136787",// TEFINCOM (NordVPN)
            "AS212238",// Datacamp
        };
        for (String asn : datacenterAsns) {
            if (response.contains("\"" + asn + "\"") || response.contains(asn)) return true;
        }
        return false;
    }

    private void kickVpn(Player player) {
        if (!player.isOnline()) return;
        String msg = plugin.getConfig().getString("anti-vpn.kick-message",
                "&cVPN/Proxy connections are not allowed on this server.\n&7Please disable your VPN and reconnect.")
                .replace("&", "\u00a7");
        player.kickPlayer(msg);
    }

    // ---- Persistence ----

    private static class StorageData {
        Map<String, Long> clearedIps = new HashMap<>();
        Map<String, Long> flaggedIps = new HashMap<>();
    }

    public void saveData() {
        StorageData data = new StorageData();
        data.clearedIps = clearedIps;
        data.flaggedIps = flaggedIps;
        try (Writer w = new FileWriter(dataFile)) {
            gson.toJson(data, w);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save VPN cache: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        try (Reader r = new FileReader(dataFile)) {
            StorageData data = gson.fromJson(r, StorageData.class);
            if (data == null) return;
            if (data.clearedIps != null) clearedIps.putAll(data.clearedIps);
            if (data.flaggedIps != null) flaggedIps.putAll(data.flaggedIps);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load VPN cache: " + e.getMessage());
        }
    }
}
