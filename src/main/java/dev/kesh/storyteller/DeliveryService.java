package dev.kesh.storyteller;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Decides when Lyra appears: queued stories are shown once the player is ready. */
public final class DeliveryService implements Listener {
    private final StoryTellerPlugin plugin;
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> snoozeUntil = new ConcurrentHashMap<>();
    private Object authApi;
    private Method authCheck;

    public DeliveryService(StoryTellerPlugin plugin) {
        this.plugin = plugin;
        hookAuthMe();
    }

    /** AuthMe is used through reflection so the plugin builds without its API. */
    private void hookAuthMe() {
        if (Bukkit.getPluginManager().getPlugin("AuthMe") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            authApi = api.getMethod("getInstance").invoke(null);
            authCheck = api.getMethod("isAuthenticated", Player.class);
            plugin.getLogger().info("AuthMe found: stories wait until players are logged in.");
        } catch (Exception ex) {
            authApi = null;
            authCheck = null;
            plugin.getLogger().warning("AuthMe found but its API could not be hooked: " + ex.getMessage());
        }
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 40L, 40L);
    }

    public void snooze(UUID id) {
        long minutes = plugin.getConfig().getLong("delivery.reopen-cooldown-minutes", 10);
        snoozeUntil.put(id, System.currentTimeMillis() + minutes * 60_000L);
    }

    private boolean authenticated(Player player) {
        if (!plugin.getConfig().getBoolean("delivery.wait-for-authme", true) || authApi == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(authCheck.invoke(authApi, player));
        } catch (Exception ex) {
            return true;
        }
    }

    public boolean isReady(Player player) {
        if (!player.isOnline()) {
            return false;
        }
        if (player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
            return false;
        }
        return authenticated(player);
    }

    /** Queues a story for a player and shows it right away if they are ready. */
    public void give(UUID id, Story story) {
        plugin.data().addPending(id, story.id());
        Player player = Bukkit.getPlayer(id);
        if (player != null && isReady(player)) {
            snoozeUntil.remove(id);
            plugin.gui().open(player, story, false);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long delayMs = plugin.getConfig().getLong("delivery.join-delay-seconds", 4) * 1000L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            List<String> pending = plugin.data().pending(id);
            if (pending.isEmpty()) {
                continue;
            }
            Long joined = joinTimes.get(id);
            if (joined != null && now - joined < delayMs) {
                continue;
            }
            Long snooze = snoozeUntil.get(id);
            if (snooze != null && now < snooze) {
                continue;
            }
            if (!isReady(player)) {
                continue;
            }
            for (String storyId : pending) {
                Story story = plugin.stories().get(storyId);
                if (story == null) {
                    plugin.data().removePending(id, storyId);
                    continue;
                }
                plugin.gui().open(player, story, false);
                break;
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        joinTimes.put(id, System.currentTimeMillis());
        snoozeUntil.remove(id);

        PlayerData data = plugin.data();
        if (!data.hasJoined(id)) {
            data.setJoined(id);
            String first = plugin.getConfig().getString("triggers.first-join-story", "");
            Story story = plugin.stories().get(first);
            if (story != null) {
                data.addPending(id, story.id());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        joinTimes.remove(id);
        snoozeUntil.remove(id);
    }
}
