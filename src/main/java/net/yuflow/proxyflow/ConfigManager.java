package net.yuflow.proxyflow;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {
    private final Logger logger;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public ConfigManager(Logger logger, Path dataDirectory) {
        this.logger = logger;
        Path configFile = dataDirectory.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder().path(configFile).build();

        if (Files.notExists(configFile, new LinkOption[0])) {
            try {
                Files.createDirectories(dataDirectory);
                try (InputStream in = this.getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    } else {
                        logger.severe("Could not find the default 'config.yml' in resources!");
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not create the default configuration file!", e);
            }
        }
        loadConfig();
    }

    public void loadConfig() {
        try {
            this.root = this.loader.load();
        } catch (ConfigurateException e) {
            this.logger.log(Level.SEVERE, "Error loading configuration!", e);
        }
    }

    public void saveConfig() {
        try {
            this.loader.save(this.root);
        } catch (ConfigurateException e) {
            this.logger.log(Level.SEVERE, "Error saving configuration!", e);
        }
    }

    public void setMaintenance(boolean enabled) {
        try {
            this.root.node("maintenance", "enabled").set(enabled);
            saveConfig();
        } catch (SerializationException e) {
            logger.log(Level.SEVERE, "Could not write maintenance status to configuration!", e);
        }
    }

    public boolean isMaintenanceEnabled() {
        return this.root.node("maintenance", "enabled").getBoolean(false);
    }

    public String getMaintenanceMotd() {
        return this.root.node("maintenance", "motd").getString("&cServer is in maintenance!");
    }

    public String getMaintenanceKickMessage() {
        return this.root.node("maintenance", "kick-message").getString("&cThe server is currently in maintenance mode.");
    }

    public String getMaintenanceBypassPermission() {
        return this.root.node("maintenance", "bypass-permission").getString("proxyflow.maintenance.bypass");
    }

    public String getVpnBypassPermission() {
        return this.root.node("security", "vpn-check", "bypass-permission").getString("proxyflow.security.vpn.bypass");
    }

    public boolean isVpnCheckEnabled() {
        return this.root.node("security", "vpn-check", "enabled").getBoolean(true);
    }

    public String getVpnCheckApiKey() {
        return this.root.node("security", "vpn-check", "api-key").getString("");
    }

    public boolean isCountryBlockEnabled() {
        return this.root.node("security", "country-block", "enabled").getBoolean(false);
    }

    public String getCountryBlockMode() {
        return this.root.node("security", "country-block", "mode").getString("blacklist");
    }

    public List<String> getCountryList() {
        try {
            return this.root.node("security", "country-block", "countries").getList(String.class, Collections.emptyList());
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }
}