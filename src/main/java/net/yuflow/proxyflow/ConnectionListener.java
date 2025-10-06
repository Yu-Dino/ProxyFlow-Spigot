package net.yuflow.proxyflow;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ConnectionListener implements Listener {
    private final Logger logger;
    private final ConfigManager configManager;
    private final Gson gson = new Gson();
    private final Map<String, Long> connectionTimestamps = new ConcurrentHashMap<>();
    private static final long CONNECTION_TIMEOUT = 2000L;
    private static final Pattern VALID_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> tempBannedIps = new ConcurrentHashMap<>();
    private static final int MAX_VIOLATIONS = 3;
    private static final long BAN_DURATION_MINUTES = 5L;
    private final Set<String> vpnIpCache = ConcurrentHashMap.newKeySet();

    public ConnectionListener(Logger logger, ConfigManager configManager) {
        this.logger = logger;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        String ipAddress = event.getAddress().getHostAddress();
        String username = event.getName();

        if (isIpBanned(ipAddress)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ChatColor.RED + "Your IP address is temporarily banned.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastConnection = connectionTimestamps.get(ipAddress);
        if (lastConnection != null && currentTime - lastConnection < CONNECTION_TIMEOUT) {
            deny(event, "You are connecting too fast.", "[ProxyFlow] Risk found (Anti-Bot) for IP: {0}. Denying join.", ipAddress);
            incrementViolation(ipAddress);
            return;
        }
        connectionTimestamps.put(ipAddress, currentTime);

        if (!VALID_USERNAME_PATTERN.matcher(username).matches()) {
            deny(event, "Your username contains invalid characters.", "[ProxyFlow] Risk found (Invalid Name: {0}) for IP: {1}. Denying join.", username, ipAddress);
            incrementViolation(ipAddress);
            return;
        }

        performExternalChecks(event, ipAddress);
    }

    private void performExternalChecks(AsyncPlayerPreLoginEvent event, String ipAddress) {
        String apiKey = configManager.getVpnCheckApiKey();
        if ((configManager.isVpnCheckEnabled() || configManager.isCountryBlockEnabled()) && apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY_HERE")) {
            try {
                URL url = new URL("https://proxycheck.io/v2/" + ipAddress + "?key=" + apiKey + "&vpn=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != 200) {
                    logger.warning("[ProxyFlow] Error with API request to proxycheck.io (Status Code: " + connection.getResponseCode() + ")");
                    return;
                }

                JsonObject response = gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);
                if (response.has(ipAddress)) {
                    JsonObject ipInfo = response.getAsJsonObject(ipAddress);

                    if (configManager.isVpnCheckEnabled() && ipInfo.has("proxy") && "yes".equals(ipInfo.get("proxy").getAsString())) {
                        vpnIpCache.add(ipAddress);
                    }

                    if (configManager.isCountryBlockEnabled() && ipInfo.has("isocode")) {
                        String countryCode = ipInfo.get("isocode").getAsString();
                        if (countryCode != null && !countryCode.isEmpty()) {
                            String mode = configManager.getCountryBlockMode();
                            List<String> countries = configManager.getCountryList();
                            boolean isBlacklisted = "blacklist".equalsIgnoreCase(mode) && countries.contains(countryCode);
                            boolean isNotWhitelisted = "whitelist".equalsIgnoreCase(mode) && !countries.contains(countryCode);

                            if (isBlacklisted || isNotWhitelisted) {
                                deny(event, "Your country is not allowed on this server.", "[ProxyFlow] Blocked connection from country ({0}) from IP {1}.", countryCode, ipAddress);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.severe("[ProxyFlow] Error during external IP address check for " + ipAddress + ": " + e.getMessage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String playerIp = event.getAddress().getHostAddress();

        if (vpnIpCache.contains(playerIp)) {
            vpnIpCache.remove(playerIp);
            if (!player.hasPermission(configManager.getVpnBypassPermission())) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "VPNs or proxies are not allowed.");
                logger.warning("[ProxyFlow] Blocked VPN/Proxy from IP " + playerIp + " for player " + player.getName() + ".");
                return;
            }
        }

        if (configManager.isMaintenanceEnabled() && !player.hasPermission(configManager.getMaintenanceBypassPermission())) {
            String kickMessage = ChatColor.translateAlternateColorCodes('&', configManager.getMaintenanceKickMessage());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
            return;
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getAddress() != null && onlinePlayer.getAddress().getAddress().getHostAddress().equals(playerIp)) {
                if (!onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "There is already a player connected from your IP address.");
                    logger.warning("[ProxyFlow] Risk found (Multi-Account) for player: " + player.getName() + " (IP: " + playerIp + "). Player '" + onlinePlayer.getName() + "' is already online.");
                    incrementViolation(playerIp);
                    return;
                }
            }
        }
        logger.info("[ProxyFlow] Player " + player.getName() + " (IP: " + playerIp + ") has passed all checks.");
    }

    private void incrementViolation(String ipAddress) {
        int violations = violationCounts.getOrDefault(ipAddress, 0) + 1;
        if (violations >= MAX_VIOLATIONS) {
            long banUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(BAN_DURATION_MINUTES);
            tempBannedIps.put(ipAddress, banUntil);
            violationCounts.remove(ipAddress);
            logger.warning("[ProxyFlow] IP address " + ipAddress + " has been temporarily banned for " + BAN_DURATION_MINUTES + " minutes!");
        } else {
            violationCounts.put(ipAddress, violations);
        }
    }

    private boolean isIpBanned(String ipAddress) {
        Long banUntil = tempBannedIps.get(ipAddress);
        if (banUntil == null) {
            return false;
        }
        if (System.currentTimeMillis() > banUntil) {
            tempBannedIps.remove(ipAddress);
            logger.info("[ProxyFlow] Temporary ban for IP " + ipAddress + " has been lifted.");
            return false;
        }
        return true;
    }

    private void deny(AsyncPlayerPreLoginEvent event, String playerMessage, String logMessage, Object... args) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + playerMessage);
        if (logMessage != null && !logMessage.isEmpty()) {
            logger.warning(java.text.MessageFormat.format(logMessage, args));
        }
    }
}