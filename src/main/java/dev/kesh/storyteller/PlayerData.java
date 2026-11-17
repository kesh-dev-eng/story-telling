package dev.kesh.storyteller;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerData {
    private final StoryTellerPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public PlayerData(StoryTellerPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private String key(UUID id, String field) {
        return "players." + id + "." + field;
    }

    public synchronized boolean hasJoined(UUID id) {
        return yaml.getBoolean(key(id, "joined"));
    }

    public synchronized void setJoined(UUID id) {
        yaml.set(key(id, "joined"), true);
        save();
    }

    public synchronized boolean hasSeen(UUID id, String story) {
        return yaml.getStringList(key(id, "seen")).contains(story);
    }

    public synchronized List<String> seen(UUID id) {
        return new ArrayList<>(yaml.getStringList(key(id, "seen")));
    }

    public synchronized void markSeen(UUID id, String story) {
        List<String> list = new ArrayList<>(yaml.getStringList(key(id, "seen")));
        if (!list.contains(story)) {
            list.add(story);
            yaml.set(key(id, "seen"), list);
            save();
        }
    }

    public synchronized List<String> pending(UUID id) {
        return new ArrayList<>(yaml.getStringList(key(id, "pending")));
    }

    public synchronized void addPending(UUID id, String story) {
        List<String> list = new ArrayList<>(yaml.getStringList(key(id, "pending")));
        if (!list.contains(story)) {
            list.add(story);
            yaml.set(key(id, "pending"), list);
            save();
        }
    }

    public synchronized void removePending(UUID id, String story) {
        List<String> list = new ArrayList<>(yaml.getStringList(key(id, "pending")));
        if (list.remove(story)) {
            yaml.set(key(id, "pending"), list);
            save();
        }
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }
}
