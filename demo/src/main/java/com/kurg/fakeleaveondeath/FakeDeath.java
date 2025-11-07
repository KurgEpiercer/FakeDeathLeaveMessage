package com.kurg.fakeleaveondeath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeDeath extends JavaPlugin implements Listener {

    private NickSyncListener nickSync;

    private String leaveMessageFormat;
    private long leaveDelayTicks;
    private int messageRadius;
    private int combatMemorySeconds;

    private final Map<UUID, Long> combatTracker = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        Bukkit.getPluginManager().registerEvents(this, this);
        nickSync = new NickSyncListener(this);
        Bukkit.getPluginManager().registerEvents(nickSync, this);

        getLogger().info("✅ FakeLeaveOnDeath v2.1 enabled — localized messages + EssentialsX nickname sync");
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        leaveMessageFormat = config.getString("leave-message", "&e%player% left the game");
        leaveDelayTicks = config.getLong("delay-ticks", 4L);
        messageRadius = config.getInt("death-message-radius", 50);
        combatMemorySeconds = config.getInt("combat-memory-seconds", 8);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String displayName = ChatColor.stripColor(player.getDisplayName());

        // Capture and modify death message
        String originalMessage = event.getDeathMessage();
        if (originalMessage == null || originalMessage.isEmpty()) {
            originalMessage = displayName + " died";
        } else {
            originalMessage = originalMessage.replace(player.getName(), displayName);
        }

        // Hide the nametag immediately
        if (nickSync != null) nickSync.hideNametag(player);

        // Prevent global broadcast
        event.setDeathMessage(null);

        // Send the death message locally (next tick so it’s clean)
        String finalOriginalMessage = originalMessage;
        Bukkit.getScheduler().runTask(this, () -> sendLocalizedDeathMessage(player, finalOriginalMessage));

        // Send the fake leave message later
        Bukkit.getScheduler().runTaskLater(this, () -> sendLocalizedLeaveMessage(player), leaveDelayTicks);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Restore nametag after respawn
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (nickSync != null) nickSync.showNametag(player);
        }, 20L);
    }

    /**
     * Sends the original death message to nearby or in-combat players.
     */
    private void sendLocalizedDeathMessage(Player dead, String message) {
        long now = System.currentTimeMillis();
        double radiusSq = messageRadius * messageRadius;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(dead)) continue;

            boolean sameWorld = viewer.getWorld().equals(dead.getWorld());
            boolean inRange = sameWorld && viewer.getLocation().distanceSquared(dead.getLocation()) <= radiusSq;

            Long lastCombat = combatTracker.getOrDefault(viewer.getUniqueId(), 0L);
            boolean inCombat = (now - lastCombat <= combatMemorySeconds * 1000L);

            if (inRange || inCombat) {
                viewer.sendMessage(message);
            }
        }
    }

    /**
     * Sends the fake leave message to nearby or in-combat players.
     */
    private void sendLocalizedLeaveMessage(Player dead) {
        String displayName = ChatColor.stripColor(dead.getDisplayName());
        String formatted = ChatColor.translateAlternateColorCodes('&',
                leaveMessageFormat.replace("%player%", displayName));

        long now = System.currentTimeMillis();
        double radiusSq = messageRadius * messageRadius;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(dead)) continue;

            boolean sameWorld = viewer.getWorld().equals(dead.getWorld());
            boolean inRange = sameWorld && viewer.getLocation().distanceSquared(dead.getLocation()) <= radiusSq;

            Long lastCombat = combatTracker.getOrDefault(viewer.getUniqueId(), 0L);
            boolean inCombat = (now - lastCombat <= combatMemorySeconds * 1000L);

            if (inRange || inCombat) {
                viewer.sendMessage(formatted);
            }
        }
    }

    /**
     * Used if you add combat detection (PvP tracker) later.
     */
    public void recordCombat(Player attacker, Player victim) {
        long now = System.currentTimeMillis();
        combatTracker.put(attacker.getUniqueId(), now);
        combatTracker.put(victim.getUniqueId(), now);
    }
}
