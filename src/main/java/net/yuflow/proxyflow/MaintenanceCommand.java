package net.yuflow.proxyflow;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MaintenanceCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final ProxyFlowSpigot plugin;

    public MaintenanceCommand(ConfigManager configManager, ProxyFlowSpigot plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean currentState = configManager.isMaintenanceEnabled();
        boolean newState = !currentState;

        configManager.setMaintenance(newState);

        if (newState) {
            sender.sendMessage(ChatColor.YELLOW + "Maintenance mode has been enabled.");

            String kickMessage = ChatColor.translateAlternateColorCodes('&', configManager.getMaintenanceKickMessage());
            String bypassPermission = configManager.getMaintenanceBypassPermission();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.hasPermission(bypassPermission)) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> player.kickPlayer(kickMessage));
                }
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + "Maintenance mode has been disabled.");
        }
        return true;
    }
}