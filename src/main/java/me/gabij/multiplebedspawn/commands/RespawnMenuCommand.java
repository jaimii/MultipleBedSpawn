package me.gabij.multiplebedspawn.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.utils.Keys;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static me.gabij.multiplebedspawn.listeners.RespawnMenuHandler.openRespawnMenu;

public class RespawnMenuCommand implements BasicCommand {
    private final MultipleBedSpawn plugin;

    public RespawnMenuCommand(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (sender instanceof Player p) {
            PersistentDataContainer playerData = p.getPersistentDataContainer();
            if (playerData.has(Keys.HAS_PROP, PersistentDataType.BOOLEAN)) {
                openRespawnMenu(p);
            }
        }
    }
}