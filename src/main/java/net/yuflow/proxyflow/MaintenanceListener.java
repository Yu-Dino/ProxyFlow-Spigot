package net.yuflow.proxyflow;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MaintenanceListener implements Listener {

    private final ConfigManager configManager;

    public MaintenanceListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        if (configManager.isMaintenanceEnabled()) {
            String motd = ChatColor.translateAlternateColorCodes('&', configManager.getMaintenanceMotd());
            event.setMotd(motd);
            event.setMaxPlayers(0);
        }
    }
}