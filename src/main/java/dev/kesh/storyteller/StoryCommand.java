package dev.kesh.storyteller;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StoryCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN = "storyteller.admin";
    private final StoryTellerPlugin plugin;

    public StoryCommand(StoryTellerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> list(sender);
            case "replay" -> replay(sender, args);
            case "send" -> send(sender, args);
            case "preview" -> preview(sender, args);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(Text.mm("<gray>Usage: /story [list | replay <id>]"
                    + (sender.hasPermission(ADMIN) ? " | send <player|all> <id> | preview <id> | reload" : "")));
        }
        return true;
    }

    private void list(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can list their stories.");
            return;
        }
        List<String> seen = plugin.data().seen(player.getUniqueId());
        if (seen.isEmpty()) {
            player.sendMessage(Text.mm("<gray>You have not heard any stories yet."));
            return;
        }
        player.sendMessage(Text.mm("<gold>Your chronicle:"));
        for (String id : seen) {
            Story story = plugin.stories().get(id);
            if (story == null) {
                continue;
            }
            player.sendMessage(Text.mm("<gray> • ").append(
                    Text.mm(Text.fill(story.title(), player))
                            .clickEvent(ClickEvent.runCommand("/story replay " + id))
                            .hoverEvent(HoverEvent.showText(Text.mm("<yellow>Click to hear it again")))));
        }
    }

    private void replay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can replay stories.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Text.mm("<gray>Usage: /story replay <id>"));
            return;
        }
        Story story = plugin.stories().get(args[1]);
        boolean allowed = story != null
                && (player.hasPermission(ADMIN) || plugin.data().hasSeen(player.getUniqueId(), story.id()));
        if (!allowed) {
            player.sendMessage(Text.mm("<red>You have not heard that story yet."));
            return;
        }
        plugin.gui().open(player, story, true);
    }

    private void send(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(Text.mm("<red>You do not have permission."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Text.mm("<gray>Usage: /story send <player|all> <id>"));
            return;
        }
        Story story = plugin.stories().get(args[2]);
        if (story == null) {
            sender.sendMessage(Text.mm("<red>Unknown story: " + args[2]));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.delivery().give(online.getUniqueId(), story);
            }
            sender.sendMessage(Text.mm("<green>Story '" + story.id() + "' sent to everyone online."));
            return;
        }
        Player online = Bukkit.getPlayerExact(args[1]);
        if (online != null) {
            plugin.delivery().give(online.getUniqueId(), story);
            sender.sendMessage(Text.mm("<green>Story '" + story.id() + "' sent to " + online.getName() + "."));
            return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (offline == null) {
            sender.sendMessage(Text.mm("<red>Player not found: " + args[1]));
            return;
        }
        plugin.delivery().give(offline.getUniqueId(), story);
        sender.sendMessage(Text.mm("<green>" + args[1] + " is offline. Lyra will visit them when they join."));
    }

    private void preview(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(Text.mm("<red>You do not have permission."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can preview stories.");
            return;
        }
        Story story = args.length < 2 ? null : plugin.stories().get(args[1]);
        if (story == null) {
            player.sendMessage(Text.mm("<red>Usage: /story preview <id>"));
            return;
        }
        plugin.gui().open(player, story, true);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN)) {
            sender.sendMessage(Text.mm("<red>You do not have permission."));
            return;
        }
        plugin.reloadAll();
        sender.sendMessage(Text.mm("<green>StoryTeller reloaded (" + plugin.stories().all().size() + " stories)."));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission(ADMIN);
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list", "replay"));
            if (admin) {
                subs.addAll(List.of("send", "preview", "reload"));
            }
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("send") && admin) {
                List<String> names = new ArrayList<>();
                names.add("all");
                Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
                return filter(names, args[1]);
            }
            if (sub.equals("preview") && admin) {
                return filter(new ArrayList<>(plugin.stories().ids()), args[1]);
            }
            if (sub.equals("replay") && sender instanceof Player player) {
                List<String> ids = admin
                        ? new ArrayList<>(plugin.stories().ids())
                        : plugin.data().seen(player.getUniqueId());
                return filter(ids, args[1]);
            }
        }
        if (args.length == 3 && sub.equals("send") && admin) {
            return filter(new ArrayList<>(plugin.stories().ids()), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
