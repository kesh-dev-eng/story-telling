package dev.kesh.storyteller;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Builds Lyra's portrait item: a player head, an ItemsAdder item, or a plain item. */
public final class Portrait {
    private final StoryTellerPlugin plugin;

    public Portrait(StoryTellerPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack build() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("narrator.portrait.type", "skin").toLowerCase(Locale.ROOT);
        ItemStack item = null;

        if (type.equals("itemsadder")) {
            item = fromItemsAdder(config.getString("narrator.portrait.itemsadder-id", ""));
        } else if (type.equals("material")) {
            Material material = Material.matchMaterial(config.getString("narrator.portrait.material", "PAPER"));
            if (material != null && material.isItem()) {
                item = new ItemStack(material);
            }
        }
        if (item == null) {
            item = skull(config);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(config.getString("narrator.name", "<gold>Lyra the Chronicler")));
            List<Component> lore = new ArrayList<>();
            for (String line : config.getStringList("narrator.lore")) {
                lore.add(Text.item(line));
            }
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack skull(FileConfiguration config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        String texture = config.getString("narrator.portrait.texture", "");
        String uuid = config.getString("narrator.portrait.uuid", "");
        try {
            if (!texture.isBlank()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "Narrator");
                profile.setProperty(new ProfileProperty("textures", texture));
                meta.setPlayerProfile(profile);
            } else if (!uuid.isBlank()) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not apply the portrait skin: " + ex.getMessage());
        }
        head.setItemMeta(meta);
        return head;
    }

    /** ItemsAdder is used through reflection so the plugin builds without its API. */
    private ItemStack fromItemsAdder(String id) {
        if (id.isBlank() || Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return null;
        }
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method getInstance = customStack.getMethod("getInstance", String.class);
            Object stack = getInstance.invoke(null, id);
            if (stack == null) {
                return null;
            }
            return (ItemStack) customStack.getMethod("getItemStack").invoke(stack);
        } catch (Exception ex) {
            return null;
        }
    }
}
