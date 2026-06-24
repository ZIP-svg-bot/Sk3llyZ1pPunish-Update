package dev.adminpunish;

import dev.adminpunish.commands.*;
import dev.adminpunish.gui.OffendGUI;
import dev.adminpunish.listeners.BanHammerListener;
import dev.adminpunish.listeners.ChatListener;
import dev.adminpunish.listeners.FreezeListener;
import dev.adminpunish.listeners.JoinListener;
import dev.adminpunish.managers.*;
import dev.adminpunish.utils.PunishExecutor;
import dev.adminpunish.utils.WebhookUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class AdminPunish extends JavaPlugin {

    private static AdminPunish instance;
    private OffenseManager offenseManager;
    private AltManager altManager;
    private VpnManager vpnManager;
    private WebhookUtil webhookUtil;
    private BanHammerManager banHammerManager;
    private VanishManager vanishManager;
    private HistoryManager historyManager;
    private WarnManager warnManager;
    private FreezeManager freezeManager;
    private PunishExecutor punishExecutor;
    private OffendGUI offendGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        offenseManager = new OffenseManager(this);
        altManager = new AltManager(this);
        vpnManager = new VpnManager(this);
        webhookUtil = new WebhookUtil(this);
        banHammerManager = new BanHammerManager(this);
        vanishManager = new VanishManager(this);
        historyManager = new HistoryManager(this);
        warnManager = new WarnManager(this);
        freezeManager = new FreezeManager();
        punishExecutor = new PunishExecutor(this);
        offendGUI = new OffendGUI(this);

        getCommand("offend").setExecutor(new OffendCommand(this));
        getCommand("offend").setTabCompleter(new OffendCommand(this));
        getCommand("offendgui").setExecutor(new OffendGuiCommand(this));
        getCommand("offendgui").setTabCompleter(new OffendGuiCommand(this));
        getCommand("offenses").setExecutor(new OffensesCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("history").setTabCompleter(new HistoryCommand(this));
        getCommand("alt").setExecutor(new AltCommand(this));
        getCommand("alt").setTabCompleter(new AltCommand(this));
        getCommand("unpunish").setExecutor(new UnpunishCommand(this));
        getCommand("unpunish").setTabCompleter(new UnpunishCommand(this));
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("warn").setTabCompleter(new WarnCommand(this));
        getCommand("warnhistory").setExecutor(new WarnHistoryCommand(this));
        getCommand("warnhistory").setTabCompleter(new WarnHistoryCommand(this));
        getCommand("freeze").setExecutor(new FreezeCommand(this));
        getCommand("freeze").setTabCompleter(new FreezeCommand(this));
        getCommand("banhammer").setExecutor(new BanHammerCommand(this));
        getCommand("banhammer").setTabCompleter(new BanHammerCommand(this));
        getCommand("unhammer").setExecutor(new BanHammerCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new BanHammerListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(offendGUI, this);

        getLogger().info("Sk3llyZ1ps Punishments enabled!");
    }

    @Override
    public void onDisable() {
        if (offenseManager != null) offenseManager.saveData();
        if (altManager != null) altManager.saveData();
        if (historyManager != null) historyManager.saveData();
        if (warnManager != null) warnManager.saveData();
        if (vanishManager != null) vanishManager.saveData();
        if (vpnManager != null) vpnManager.saveData();
        getLogger().info("Sk3llyZ1ps Punishments disabled.");
    }

    public static AdminPunish getInstance() { return instance; }
    public OffenseManager getOffenseManager() { return offenseManager; }
    public AltManager getAltManager() { return altManager; }
    public VpnManager getVpnManager() { return vpnManager; }
    public WebhookUtil getWebhookUtil() { return webhookUtil; }
    public BanHammerManager getBanHammerManager() { return banHammerManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public WarnManager getWarnManager() { return warnManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    public PunishExecutor getPunishExecutor() { return punishExecutor; }
    public OffendGUI getOffendGUI() { return offendGUI; }
}
