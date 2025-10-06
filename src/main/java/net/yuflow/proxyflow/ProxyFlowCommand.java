package net.yuflow.proxyflow;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ProxyFlowCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public ProxyFlowCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("proxyflow.command.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                return true;
            }

            this.configManager.loadConfig();
            sender.sendMessage(ChatColor.GREEN + "ProxyFlow configuration has been reloaded!");
            return true;
        }

        sender.sendMessage(ChatColor.GRAY + "Usage: /proxyflow reload");
        return true;
    }
}