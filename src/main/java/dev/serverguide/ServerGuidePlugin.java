package dev.serverguide;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ServerGuidePlugin extends JavaPlugin implements Listener, TabExecutor {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<UUID, GuideSession> sessions = new HashMap<>();
    private final List<Step> steps = new ArrayList<>();
    private final Set<UUID> pending = new HashSet<>();
    private final Set<UUID> completed = new HashSet<>();
    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSteps();
        loadData();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("guide")).setExecutor(this);
        getLogger().info("ServerGuide enabled with " + steps.size() + " steps."
                + (itemsAdderPresent() ? " ItemsAdder detected." : ""));
    }

    @Override
    public void onDisable() {
        new ArrayList<>(sessions.values()).forEach(GuideSession::end);
        sessions.clear();
    }

    public List<Step> steps() {
        return steps;
    }

    public boolean itemsAdderPresent() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    // ---------------- config / data ----------------

    @SuppressWarnings("unchecked")
    private void loadSteps() {
        steps.clear();
        for (Map<?, ?> m : getConfig().getMapList("steps")) {
            List<String> text = new ArrayList<>();
            Object t = m.get("text");
            if (t instanceof List<?> l) l.forEach(o -> text.add(String.valueOf(o)));
            else if (t != null) text.add(String.valueOf(t));
            List<String> cmds = new ArrayList<>();
            Object c = m.get("commands");
            if (c instanceof List<?> l) l.forEach(o -> cmds.add(String.valueOf(o)));
            steps.add(new Step(text,
                    m.get("sound") == null ? null : String.valueOf(m.get("sound")),
                    m.get("title") == null ? null : String.valueOf(m.get("title")),
                    m.get("location") == null ? null : String.valueOf(m.get("location")),
                    cmds));
        }
    }

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        data.getStringList("pending").forEach(s -> pending.add(UUID.fromString(s)));
        data.getStringList("completed").forEach(s -> completed.add(UUID.fromString(s)));
    }

    private void saveData() {
        data.set("pending", pending.stream().map(UUID::toString).toList());
        data.set("completed", completed.stream().map(UUID::toString).toList());
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    public Location parseLocation(String s) {
        try {
            String[] p = s.split(",");
            World w = Bukkit.getWorld(p[0].trim());
            if (w == null) return null;
            return new Location(w, Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()),
                    Double.parseDouble(p[3].trim()),
                    p.length > 4 ? Float.parseFloat(p[4].trim()) : 0f,
                    p.length > 5 ? Float.parseFloat(p[5].trim()) : 0f);
        } catch (Exception e) {
            getLogger().warning("Bad location in config: " + s);
            return null;
        }
    }

    // ---------------- ItemsAdder helpers (reflection, no hard dependency) ----------------

    public String replaceFontImages(String text) {
        if (!itemsAdderPresent()) return text;
        try {
            Class<?> c = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            Object r = c.getMethod("replaceFontImages", String.class).invoke(null, text);
            return r == null ? text : r.toString();
        } catch (Throwable t) {
            return text;
        }
    }

    /** Makes the guide NPC visible only to the player who is doing the tutorial. */
    public void hideFromOthers(Player owner, Entity npc) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(owner)) p.hideEntity(this, npc);
        }
    }

    // ---------------- session control ----------------

    public void startGuide(Player player) {
        if (steps.isEmpty()) return;
        GuideSession old = sessions.remove(player.getUniqueId());
        if (old != null) old.end();
        GuideSession s = new GuideSession(this, player);
        sessions.put(player.getUniqueId(), s);
        s.start();
    }

    public void finishSession(Player player, boolean completedAll) {
        GuideSession s = sessions.remove(player.getUniqueId());
        if (s != null) s.end();
        pending.remove(player.getUniqueId());
        completed.add(player.getUniqueId());
        saveData();

        if (completedAll || getConfig().getBoolean("give-rewards-on-skip", false)) {
            for (String cmd : getConfig().getStringList("finish-commands")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("<player>", player.getName()));
            }
        }
        player.sendMessage(MM.deserialize(getConfig().getString(
                completedAll ? "messages.finished" : "messages.skipped", "")));
    }

    // ---------------- events ----------------

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // hide other people's guide NPCs from the new player
        for (GuideSession s : sessions.values()) {
            if (s.npc() != null) p.hideEntity(this, s.npc());
        }
        if (!getConfig().getBoolean("auto-start.enabled", true)) return;
        if (!p.hasPlayedBefore() && !completed.contains(p.getUniqueId())) {
            pending.add(p.getUniqueId());
            saveData();
        }
        boolean onlyNew = getConfig().getBoolean("auto-start.only-new-players", true);
        boolean should = pending.contains(p.getUniqueId())
                || (!onlyNew && !completed.contains(p.getUniqueId()));
        if (should) {
            long delay = getConfig().getLong("auto-start.delay-seconds", 3) * 20L;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (p.isOnline()) startGuide(p);
            }, delay);
        }
    }

    /** F key = next dialog, Shift+F = skip the guide (HUD mode). */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        GuideSession s = sessions.get(p.getUniqueId());
        if (s == null || !getConfig().getBoolean("hud.enabled", true)) return;
        e.setCancelled(true);
        if (p.isSneaking()) finishSession(p, false); else s.next();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        GuideSession s = sessions.remove(e.getPlayer().getUniqueId());
        if (s != null) s.end();
    }

    // ---------------- commands ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String sub = args.length == 0 ? "start" : args[0].toLowerCase();

        if (sub.equals("reload") || sub.equals("reset")) {
            if (!sender.hasPermission("serverguide.admin")) {
                sender.sendMessage(MM.deserialize("<red>No permission."));
                return true;
            }
            if (sub.equals("reload")) {
                reloadConfig();
                loadSteps();
                sender.sendMessage(MM.deserialize(getConfig().getString("messages.reloaded", "Reloaded")));
            } else if (args.length > 1) {
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) {
                    sender.sendMessage(MM.deserialize("<red>Player not online."));
                } else {
                    completed.remove(t.getUniqueId());
                    saveData();
                    sender.sendMessage(MM.deserialize("<green>Guide reset for <name>.",
                            Placeholder.unparsed("name", t.getName())));
                }
            }
            return true;
        }

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!p.hasPermission("serverguide.use")) return true;

        GuideSession s = sessions.get(p.getUniqueId());
        switch (sub) {
            case "start" -> startGuide(p);
            case "next" -> {
                if (s == null) noSession(p); else s.next();
            }
            case "back" -> {
                if (s == null) noSession(p); else s.back();
            }
            case "skip" -> {
                if (s == null) noSession(p); else finishSession(p, false);
            }
            default -> p.sendMessage(MM.deserialize("<yellow>/guide [start|next|back|skip]"));
        }
        return true;
    }

    private void noSession(Player p) {
        p.sendMessage(MM.deserialize(getConfig().getString("messages.no-session", "")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> l = new ArrayList<>(List.of("start", "next", "back", "skip"));
            if (sender.hasPermission("serverguide.admin")) l.addAll(List.of("reload", "reset"));
            return l.stream().filter(x -> x.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
