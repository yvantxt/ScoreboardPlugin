package me.yvant;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardPlugin extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Set<UUID> activePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand cmd = getCommand("scoreboard");
        if (cmd != null) {
            cmd.setExecutor(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldEnableFor(player)) {
                activePlayers.add(player.getUniqueId());
                showScoreboard(player);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideScoreboard(player);
        }
    }

    private boolean shouldEnableFor(Player player) {
        String world = player.getWorld().getName();
        return config.getConfigurationSection("scoreboard.worlds." + world) != null &&
                config.getBoolean("scoreboard.worlds." + world + ".enabled");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (shouldEnableFor(player)) {
            activePlayers.add(player.getUniqueId());
            new BukkitRunnable() {
                @Override
                public void run() {
                    showScoreboard(player);
                }
            }.runTaskLater(this, 20L);
        }
    }

    private void showScoreboard(Player player) {
        String world = player.getWorld().getName();
        if (!config.contains("scoreboard.worlds." + world)) return;

        List<String> lines = config.getStringList("scoreboard.worlds." + world + ".lines");
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("scoreboard.worlds." + world + ".title", "&fScoreboard"));

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("stats", "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        for (String line : lines) {
            String parsedLine = PlaceholderAPI.setPlaceholders(player, ChatColor.translateAlternateColorCodes('&', line));
            obj.getScore(parsedLine).setScore(score--);
        }

        player.setScoreboard(board);
    }

    private void hideScoreboard(Player player) {
        Scoreboard empty = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(empty);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("info")) {
            player.sendMessage(ChatColor.YELLOW + "/scoreboard stats on/off" + ChatColor.GRAY + " - Toggle scoreboard");
            player.sendMessage(ChatColor.YELLOW + "/scoreboard reload" + ChatColor.GRAY + " - Reload config");
            player.sendMessage(ChatColor.DARK_GRAY + "discord: trolleryvant | telegram: yvant_dev");
            return true;
        }

        if (args[0].equalsIgnoreCase("stats")) {
            if (!player.hasPermission("scoreboard.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /scoreboard stats on|off");
                return true;
            }

            if (args[1].equalsIgnoreCase("on")) {
                activePlayers.add(player.getUniqueId());
                showScoreboard(player);
                player.sendMessage(ChatColor.GREEN + "Scoreboard enabled.");
            } else if (args[1].equalsIgnoreCase("off")) {
                activePlayers.remove(player.getUniqueId());
                hideScoreboard(player);
                player.sendMessage(ChatColor.RED + "Scoreboard disabled.");
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("scoreboard.admin")) {
                player.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }

            reloadConfig();
            config = getConfig();
            player.sendMessage(ChatColor.GREEN + "Scoreboard config reloaded.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }

        return false;
    }
}
