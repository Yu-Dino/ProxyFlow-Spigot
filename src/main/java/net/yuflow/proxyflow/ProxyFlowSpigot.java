package net.yuflow.proxyflow;

import org.bukkit.plugin.java.JavaPlugin;

public class ProxyFlowSpigot extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this.getLogger(), this.getDataFolder().toPath());

        this.getCommand("proxyflow").setExecutor(new ProxyFlowCommand(this.configManager));
        this.getCommand("maintenance").setExecutor(new MaintenanceCommand(this.configManager, this));

        this.getServer().getPluginManager().registerEvents(new ConnectionListener(this.getLogger(), this.configManager), this);
        this.getServer().getPluginManager().registerEvents(new MaintenanceListener(this.configManager), this);

        getLogger().info(" ");
        getLogger().info(" _______  ______    _______  __   __  __   __  _______  ___      _______  _     _    __   __  ____  ");
        getLogger().info("|       ||    _ |  |       ||  |_|  ||  | |  ||       ||   |    |       || | _ | |  |  | |  ||    | ");
        getLogger().info("|    _  ||   | ||  |   _   ||       ||  |_|  ||    ___||   |    |   _   || || || |  |  |_|  | |   | ");
        getLogger().info("|   |_| ||   |_||_ |  | |  ||       ||       ||   |___ |   |    |  | |  ||       |  |       | |   | ");
        getLogger().info("|    ___||    __  ||  |_|  | |     | |_     _||    ___||   |___ |  |_|  ||       |  |       | |   | ");
        getLogger().info("|   |    |   |  | ||       ||   _   |  |   |  |   |    |       ||       ||   _   |   |     |  |   | ");
        getLogger().info("|___|    |___|  |_||_______||__| |__|  |___|  |___|    |_______||_______||__| |__|    |___|   |___| ");
        getLogger().info(" ");
        getLogger().info("                      ProxyFlow v1.1.2-Spigot powered by Yu_Dino");
        getLogger().info("                     Plugin has been initialized successfully!");
        getLogger().info(" ");
    }
}