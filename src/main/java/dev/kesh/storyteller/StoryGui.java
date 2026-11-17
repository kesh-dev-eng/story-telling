package dev.kesh.storyteller;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** The popup: Lyra's portrait, the current page and the buttons. */
public final class StoryGui implements Listener {
    private static final int SIZE = 27;
    private static final int PORTRAIT_SLOT = 10;
    private static final int TEXT_SLOT = 13;
    private static final int GUIDE_SLOT = 16;
    private static final int BACK_SLOT = 18;
    private static final int NEXT_SLOT = 26;
    private static final int WRAP_WIDTH = 36;

    private final StoryTellerPlugin plugin;
    private final Portrait portrait;

    public StoryGui(StoryTellerPlugin plugin) {
        this.plugin = plugin;
        this.portrait = new Portrait(plugin);
    }

    public static final class Holder implements InventoryHolder {
        private final Story story;
        private final UUID viewer;
        private final boolean preview;
        private int page;
        private boolean guide;
        private boolean finished;
        private Inventory inventory;

        private Holder(Story story, UUID viewer, boolean preview) {
            this.story = story;
            this.viewer = viewer;
            this.preview = preview;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** preview = true replays the story without saving progress or giving rewards. */
    public void open(Player player, Story story, boolean preview) {
        Holder holder = new Holder(story, player.getUniqueId(), preview);
        Component title = Text.mm(Text.fill(story.title(), player));
        Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
        holder.inventory = inventory;
        render(player, holder);
        player.openInventory(inventory);
        announce(player);
    }

    private void announce(Player player) {
        FileConfiguration config = plugin.getConfig();
        String sound = config.getString("narrator.open-sound", "");
        if (!sound.isBlank()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        if (config.getBoolean("narrator.show-title", true)) {
            player.showTitle(Title.title(
                    Text.mm(config.getString("narrator.name", "<gold>Lyra the Chronicler")),
                    Text.mm(config.getString("narrator.title-subtext", "<gray>has a message for you")),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
    }

    private void render(Player player, Holder holder) {
        Inventory inventory = holder.inventory;
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
        inventory.setItem(PORTRAIT_SLOT, portrait.build());

        if (holder.guide) {
            List<Component> lore = new ArrayList<>();
            int number = 1;
            for (String step : holder.story.guide()) {
                lore.addAll(Text.wrap("<gold>" + number + ".</gold> " + Text.fill(step, player), WRAP_WIDTH, "<gray>"));
                number++;
            }
            inventory.setItem(TEXT_SLOT, button(Material.WRITABLE_BOOK, "<aqua>Guidance", lore));
            inventory.setItem(BACK_SLOT, button(Material.ARROW, "<yellow>◀ Back to story", List.of()));
            return;
        }

        int total = holder.story.pages().size();
        String raw = Text.fill(holder.story.pages().get(holder.page), player);
        inventory.setItem(TEXT_SLOT, button(Material.BOOK,
                "<yellow>Page " + (holder.page + 1) + "/" + total,
                Text.wrap(raw, WRAP_WIDTH, "<gray>")));

        if (holder.page > 0) {
            inventory.setItem(BACK_SLOT, button(Material.ARROW, "<yellow>◀ Previous", List.of()));
        }
        if (!holder.story.guide().isEmpty()) {
            inventory.setItem(GUIDE_SLOT, button(Material.COMPASS, "<aqua>Guidance",
                    List.of(Text.item("<gray>How to do it, step by step."))));
        }
        if (holder.page < total - 1) {
            inventory.setItem(NEXT_SLOT, button(Material.ARROW, "<yellow>Next ▶", List.of()));
        } else {
            List<Component> lore = new ArrayList<>();
            boolean reward = !holder.preview
                    && !holder.story.rewards().isEmpty()
                    && !plugin.data().hasSeen(holder.viewer, holder.story.id());
            if (reward) {
                lore.add(Text.item("<gray>Claim your reward!"));
            }
            inventory.setItem(NEXT_SLOT, button(Material.LIME_DYE, "<green>✔ Finish", lore));
        }
    }

    private ItemStack button(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.item(name));
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        int total = holder.story.pages().size();

        if (holder.guide) {
            if (slot == BACK_SLOT) {
                holder.guide = false;
                refresh(player, holder);
            }
            return;
        }

        if (slot == BACK_SLOT && holder.page > 0) {
            holder.page--;
            refresh(player, holder);
        } else if (slot == GUIDE_SLOT && !holder.story.guide().isEmpty()) {
            holder.guide = true;
            refresh(player, holder);
        } else if (slot == NEXT_SLOT) {
            if (holder.page < total - 1) {
                holder.page++;
                refresh(player, holder);
            } else {
                finish(player, holder);
            }
        }
    }

    private void refresh(Player player, Holder holder) {
        render(player, holder);
        player.playSound(player.getLocation(), "minecraft:ui.button.click", 0.6f, 1.2f);
    }

    private void finish(Player player, Holder holder) {
        holder.finished = true;
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        if (holder.preview) {
            return;
        }

        PlayerData data = plugin.data();
        UUID id = player.getUniqueId();
        boolean first = !data.hasSeen(id, holder.story.id());
        data.markSeen(id, holder.story.id());
        data.removePending(id, holder.story.id());

        if (first && !holder.story.rewards().isEmpty()) {
            for (String command : holder.story.rewards()) {
                String run = command.startsWith("/") ? command.substring(1) : command;
                run = run.replace("%player%", player.getName()).replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
            }
            player.sendMessage(Text.mm("<green>Reward claimed!"));
            player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 0.8f, 1.0f);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Holder holder && !holder.finished && !holder.preview) {
            plugin.delivery().snooze(holder.viewer);
        }
    }
}
