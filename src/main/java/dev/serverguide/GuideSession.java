package dev.serverguide;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;

public final class GuideSession {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ServerGuidePlugin plugin;
    private final Player player;
    private int index = 0;
    private Entity npc;
    private Object itemsAdderEntity;
    private BossBar bar;
    private BukkitTask hudTask;
    private Component hudTitle = Component.empty();
    private Component hudSub = Component.empty();
    private Component hudBar = Component.empty();

    public GuideSession(ServerGuidePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Entity npc() {
        return npc;
    }

    public void start() {
        show(true);
    }

    public void next() {
        if (index + 1 >= plugin.steps().size()) {
            plugin.finishSession(player, true);
        } else {
            index++;
            show(true);
        }
    }

    public void back() {
        if (index > 0) {
            index--;
            show(false);
        }
    }

    public void end() {
        removeNpc();
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
            player.clearTitle();
            player.sendActionBar(Component.empty());
        }
        if (bar != null) {
            player.hideBossBar(bar);
            bar = null;
        }
    }

    private void show(boolean forward) {
        removeNpc();
        FileConfiguration cfg = plugin.getConfig();
        List<Step> steps = plugin.steps();
        Step step = steps.get(index);

        Location base = player.getLocation();
        if (step.location() != null) {
            Location target = plugin.parseLocation(step.location());
            if (target != null) {
                player.teleport(target);
                base = target;
            }
        }

        if (cfg.getBoolean("hud.enabled", true)) {
            // picture + speech bubble drawn on the player's screen (no NPC in the world)
            showHud(step);
            player.sendMessage(buildButtons(index, steps.size()));
        } else {
            // place the guide in front of the player, looking at them
            Vector dir = base.getDirection().setY(0);
            if (dir.lengthSquared() < 0.01) dir = new Vector(0, 0, 1);
            Location npcLoc = base.clone().add(dir.normalize().multiply(cfg.getDouble("character.distance", 2.5)));
            npcLoc.setDirection(base.toVector().subtract(npcLoc.toVector()));
            spawnNpc(npcLoc);

            // dialog box in chat
            player.sendMessage(buildDialog(step, index, steps.size()));
        }

        // extras
        if (step.sound() != null && !step.sound().isBlank()) {
            player.playSound(player.getLocation(), step.sound(), 1f, 1f);
        }
        if (!cfg.getBoolean("hud.enabled", true) && step.title() != null && !step.title().isBlank()) {
            player.showTitle(Title.title(
                    MM.deserialize(step.title()), Component.empty(),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
        updateBar(steps.size());

        if (forward && step.commands() != null) {
            for (String cmd : step.commands()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("<player>", player.getName()));
            }
        }
    }


    // ---------------- HUD (screen overlay) ----------------

    private void showHud(Step step) {
        FileConfiguration cfg = plugin.getConfig();
        var tag = Placeholder.unparsed("player", player.getName());

        String images = cfg.getString("hud.portrait", "") + cfg.getString("hud.gap", "")
                + cfg.getString("hud.bubble", "");
        hudTitle = MM.deserialize(plugin.replaceFontImages(images));

        String shift = cfg.getString("hud.text-shift", "");
        String l1 = step.text().size() > 0 ? step.text().get(0) : "";
        String l2 = step.text().size() > 1 ? step.text().get(1) : "";
        hudSub = MM.deserialize(plugin.replaceFontImages(shift + "<white>" + l1), tag);
        hudBar = MM.deserialize(plugin.replaceFontImages(shift + "<white>" + l2), tag);

        sendHud();
        if (hudTask == null) {
            hudTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sendHud, 20L, 20L);
        }
    }

    private void sendHud() {
        if (!player.isOnline()) return;
        player.showTitle(Title.title(hudTitle, hudSub,
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ZERO)));
        player.sendActionBar(hudBar);
    }

    private Component buildButtons(int i, int total) {
        FileConfiguration cfg = plugin.getConfig();
        boolean last = i + 1 >= total;
        StringBuilder sb = new StringBuilder();
        if (i > 0) {
            sb.append("<click:run_command:'/guide back'>").append(cfg.getString("ui.back", "[Back]")).append("</click> ");
        }
        sb.append("<click:run_command:'/guide next'>")
                .append(last ? cfg.getString("ui.finish", "[Finish]") : cfg.getString("ui.next", "[Next]"))
                .append("</click> ");
        if (!last) {
            sb.append("<click:run_command:'/guide skip'>").append(cfg.getString("ui.skip", "[Skip]")).append("</click>");
        }
        sb.append(" <dark_gray>(F = next, Shift+F = skip)");
        return MM.deserialize(sb.toString());
    }

    private Component buildDialog(Step step, int i, int total) {
        FileConfiguration cfg = plugin.getConfig();
        String name = cfg.getString("character.name", "<gold>Guide");
        String prefix = cfg.getString("ui.header-prefix", "");

        StringBuilder sb = new StringBuilder();
        sb.append("\n<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n");
        sb.append(prefix).append(name).append(" <gray>» <dark_gray>(").append(i + 1).append("/").append(total).append(")\n");
        for (String line : step.text()) {
            sb.append("<white>").append(line).append("\n");
        }
        sb.append("\n");
        if (i > 0) {
            sb.append("<click:run_command:'/guide back'><hover:show_text:'Previous dialog'>")
                    .append(cfg.getString("ui.back", "[Back]")).append("</hover></click> ");
        }
        boolean last = i + 1 >= total;
        sb.append("<click:run_command:'/guide next'><hover:show_text:'")
                .append(last ? "Finish the guide" : "Next dialog").append("'>")
                .append(last ? cfg.getString("ui.finish", "[Finish]") : cfg.getString("ui.next", "[Next]"))
                .append("</hover></click> ");
        if (!last) {
            sb.append("<click:run_command:'/guide skip'><hover:show_text:'Skip the whole guide'>")
                    .append(cfg.getString("ui.skip", "[Skip]")).append("</hover></click>");
        }
        sb.append("\n<dark_gray>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

        String raw = plugin.replaceFontImages(sb.toString());
        return MM.deserialize(raw, Placeholder.unparsed("player", player.getName()));
    }

    private void updateBar(int total) {
        if (!plugin.getConfig().getBoolean("ui.show-bossbar", true)) return;
        String title = plugin.getConfig().getString("ui.bossbar-title", "Step <step>/<total>");
        Component comp = MM.deserialize(title,
                Placeholder.unparsed("step", String.valueOf(index + 1)),
                Placeholder.unparsed("total", String.valueOf(total)));
        float progress = (index + 1) / (float) total;
        if (bar == null) {
            bar = BossBar.bossBar(comp, progress, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
            player.showBossBar(bar);
        } else {
            bar.name(comp);
            bar.progress(progress);
        }
    }

    // ---------------- NPC ----------------

    private void spawnNpc(Location loc) {
        FileConfiguration cfg = plugin.getConfig();
        String type = cfg.getString("character.type", "MANNEQUIN").toUpperCase();
        Component name = MM.deserialize(cfg.getString("character.name", "<gold>Guide"));

        if (type.equals("ITEMSADDER") && plugin.itemsAdderPresent()) {
            try {
                Class<?> ce = Class.forName("dev.lone.itemsadder.api.CustomEntity");
                Object custom = ce.getMethod("spawn", String.class, Location.class)
                        .invoke(null, cfg.getString("character.itemsadder-id", ""), loc);
                if (custom != null) {
                    itemsAdderEntity = custom;
                    npc = (Entity) ce.getMethod("getEntity").invoke(custom);
                    npc.setPersistent(false);
                    plugin.hideFromOthers(player, npc);
                    return;
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("ItemsAdder custom entity failed, using fallback: " + t.getMessage());
            }
            type = "MANNEQUIN";
        }

        EntityType et;
        try {
            et = EntityType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            et = EntityType.VILLAGER;
        }

        npc = loc.getWorld().spawnEntity(loc, et, CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            e.customName(name);
            e.setCustomNameVisible(true);
            e.setInvulnerable(true);
            e.setGravity(false);
            e.setSilent(true);
            e.setPersistent(false);
            if (e instanceof LivingEntity le) {
                le.setAI(false);
                le.setCollidable(false);
            }
        });
        plugin.hideFromOthers(player, npc);
    }

    private void removeNpc() {
        if (itemsAdderEntity != null) {
            try {
                itemsAdderEntity.getClass().getMethod("despawn").invoke(itemsAdderEntity);
            } catch (Throwable ignored) {
            }
            itemsAdderEntity = null;
        }
        if (npc != null && npc.isValid()) npc.remove();
        npc = null;
    }
}
