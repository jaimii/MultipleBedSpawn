package me.gabij.multiplebedspawn.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gabij.multiplebedspawn.MultipleBedSpawn;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.gabij.multiplebedspawn.listeners.ManageBedsMenuHandler.openManageMenu;

public class ManageBedsCommand implements BasicCommand {
    private final MultipleBedSpawn plugin;

    public ManageBedsCommand(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (sender instanceof Player p) {
            openManageMenu(p);
        }
    }
}