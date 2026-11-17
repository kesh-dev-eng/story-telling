package dev.kesh.storyteller;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class StoryTellerPlugin extends JavaPlugin {
    private StoryManager stories;
    private PlayerData data;
    private StoryGui gui;
    private DeliveryService delivery;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        stories = new StoryManager(this);
        stories.load();
        data = new PlayerData(this);
        gui = new StoryGui(this);
        delivery = new DeliveryService(this);

        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(delivery, this);
        delivery.start();

        PluginCommand command = getCommand("story");
        if (command != null) {
            StoryCommand handler = new StoryCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
    }

    public void reloadAll() {
        reloadConfig();
        stories.load();
    }

    public StoryManager stories() {
        return stories;
    }

    public PlayerData data() {
        return data;
    }

    public StoryGui gui() {
        return gui;
    }

    public DeliveryService delivery() {
        return delivery;
    }
}
