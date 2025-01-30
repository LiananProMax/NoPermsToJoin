package dev.lianan.lobbysend;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class LobbySendPlugin extends JavaPlugin implements Listener {

    private LuckPerms luckPerms;
    private int checkTaskId;

    // 配置参数
    private int checkInterval;
    private List<String> checkedGroups;
    private String kickReason;

    @Override
    public void onEnable() {
        // 加载配置文件
        saveDefaultConfig();
        reloadConfigValues();

        // 获取LuckPerms实例
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            getLogger().severe("LuckPerms not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        luckPerms = provider.getProvider();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        // 启动周期检查任务（异步）
        startScheduledCheck();

        getLogger().info("LobbySendPlugin has been enabled!");
    }

    private void reloadConfigValues() {
        reloadConfig();

        // 读取检查间隔（秒）并转换为ticks
        checkInterval = Math.max(getConfig().getInt("interval", 3), 1) * 20;
        checkedGroups = getConfig().getStringList("checked-groups")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        kickReason = getConfig().getString("kick-reason", "您无权访问此服务器");

        // 如果checked-groups为空则使用默认值
        if (checkedGroups.isEmpty()) {
            checkedGroups.add("default");
            getLogger().warning("No checked-groups found in config, using default");
        }
    }

    private void startScheduledCheck() {
        checkTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                this::checkAllPlayersAsync,
                20L,  // 初始延迟1秒
                checkInterval  // 后续间隔
        ).getTaskId();
    }

    private void checkAllPlayersAsync() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndKickPlayer(player);
        }
    }

    private void checkAndKickPlayer(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        String primaryGroup = user.getPrimaryGroup().toLowerCase();
        if (checkedGroups.contains(primaryGroup)) {
            // 返回主线程执行踢出操作
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) {
                    player.kickPlayer(kickReason);
                    getLogger().info("Kicked player " + player.getName() + " for reason: " + kickReason);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> checkAndKickPlayer(player));
    }

    @Override
    public void onDisable() {
        // 取消周期任务
        Bukkit.getScheduler().cancelTask(checkTaskId);
        getLogger().info("LobbySendPlugin has been disabled!");
    }
}