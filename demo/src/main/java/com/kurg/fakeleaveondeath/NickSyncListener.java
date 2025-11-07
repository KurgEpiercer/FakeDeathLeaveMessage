package com.kurg.fakeleaveondeath;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Syncs EssentialsX nicknames to:
 * - Chat display name
 * - Tablist
 * - Above-head nametag
 * Also allows hiding/restoring nametags for fake death.
 */
public class NickSyncListener implements Listener {

    private final JavaPlugin plugin;

    public NickSyncListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateNicknameDisplay(event.getPlayer(), true);
    }

    @EventHandler
    public void onNickCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().toLowerCase().startsWith("/nick ")) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateNicknameDisplay(event.getPlayer(), true), 5L);
    }

    public void updateNicknameDisplay(Player player, boolean showNametag) {
        String nickname = PlaceholderAPI.setPlaceholders(player, "%essentials_nickname%");
        if (nickname == null || nickname.isEmpty()) nickname = player.getName();
        nickname = ChatColor.translateAlternateColorCodes('&', nickname);

        player.setDisplayName(nickname);
        player.setPlayerListName(ChatColor.stripColor(nickname));

        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "nick_" + player.getName();
        Team team = sb.getTeam(teamName);
        if (team != null) team.unregister();
        team = sb.registerNewTeam(teamName);
        team.addEntry(player.getName());
        team.setPrefix("");
        team.setSuffix("");
        team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                showNametag ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);

        player.setCustomName(nickname);
        player.setCustomNameVisible(showNametag);

        for (Player p : Bukkit.getOnlinePlayers()) p.setScoreboard(sb);

        plugin.getLogger().info((showNametag ? "[NickSync] " : "[NickSync-Hidden] ")
                + player.getName() + " → " + ChatColor.stripColor(nickname));
    }

    public void hideNametag(Player player) {
        updateNicknameDisplay(player, false);
    }

    public void showNametag(Player player) {
        updateNicknameDisplay(player, true);
    }
}
