package me.gabij.multiplebedspawn;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.gabij.multiplebedspawn.commands.*;
import me.gabij.multiplebedspawn.listeners.*;
import me.gabij.multiplebedspawn.utils.Keys;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.List;

public final class MultipleBedSpawn extends JavaPlugin {

    private Configuration messages;
    private static MultipleBedSpawn instance;

    public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    @Override
    public void onEnable() {
        instance = this;

        // Initialize cached Keys container
        Keys.init(this);

        getConfig().options().copyDefaults(true);
        saveConfig();
        createLanguageConfig();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        getServer().getPluginManager().registerEvents(new RespawnMenuHandler(this), this);
        getServer().getPluginManager().registerEvents(new RemoveMenuHandler(this), this);
        getServer().getPluginManager().registerEvents(new ManageBedsMenuHandler(this), this);
        getServer().getPluginManager().registerEvents(new PlayerGetsOnBedListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new MagicMirrorListener(this), this);

        // Register commands natively using Paper's 26.3 Lifecycle API
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commands.register("respawnbed", "Opens a menu with saved beds", List.of(), new RespawnMenuCommand(this));
            commands.register("renamebed", "Changes the name of the bed you are looking at", List.of(), new NameCommand(this));
            commands.register("managebeds", "Opens a menu to manage, rename, and remove your beds", List.of("beds", "editbeds"), new ManageBedsCommand(this));

            if (this.getConfig().getBoolean("remove-beds-gui")) {
                commands.register("removebed", "Opens a menu to remove saved beds", List.of(), new RemoveCommand(this));
            }
            if (this.getConfig().getBoolean("bed-sharing")) {
                commands.register("sharebed", "Gives bed to another player", List.of(), new ShareCommand(this));
            }
        });

        this.getLogger().info("Commands registered via Paper Lifecycle API successfully.");
    }

    public static MultipleBedSpawn getInstance() {
        return instance;
    }

    public String getMessages(String path) {
        return this.messages.getString(path);
    }

    private void createLanguageConfig() {
        String lang = this.getConfig().getString("lang");
        InputStream input;
        try {
            input = getClass().getClassLoader().getResourceAsStream("languages/{key}.yml".replace("{key}", lang));
            this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(input));
        } catch (Exception e) {
            input = getClass().getClassLoader().getResourceAsStream("languages/enUS.yml");
            this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(input));
        }
    }
}