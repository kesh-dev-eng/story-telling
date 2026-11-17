package dev.kesh.storyteller;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StoryManager {
    private final StoryTellerPlugin plugin;
    private final Map<String, Story> stories = new LinkedHashMap<>();

    public StoryManager(StoryTellerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stories.clear();
        File file = new File(plugin.getDataFolder(), "stories.yml");
        if (!file.exists()) {
            plugin.saveResource("stories.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("stories");
        if (root == null) {
            plugin.getLogger().warning("stories.yml has no 'stories' section.");
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            List<String> pages = section.getStringList("pages");
            if (pages.isEmpty()) {
                plugin.getLogger().warning("Story '" + id + "' has no pages and was skipped.");
                continue;
            }
            for (int i = 0; i < pages.size(); i++) {
                if (Text.plain(pages.get(i)).length() > 320) {
                    plugin.getLogger().warning("Story '" + id + "' page " + (i + 1)
                            + " is long and may be cut off in the tooltip. Consider splitting it.");
                }
            }
            stories.put(id, new Story(
                    id,
                    section.getString("title", "<gold>" + id),
                    pages,
                    section.getStringList("guide"),
                    section.getStringList("rewards")));
        }
        plugin.getLogger().info("Loaded " + stories.size() + " stories.");
    }

    public Story get(String id) {
        return id == null ? null : stories.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Story> all() {
        return stories.values();
    }

    public Set<String> ids() {
        return stories.keySet();
    }
}
