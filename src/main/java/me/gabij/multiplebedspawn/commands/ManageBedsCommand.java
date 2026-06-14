package me.gabij.multiplebedspawn.commands;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import static me.gabij.multiplebedspawn.listeners.ManageBedsMenuHandler.openManageMenu;

import java.util.ArrayList;

public class ManageBedsCommand extends BukkitCommand {
    static MultipleBedSpawn plugin;

    public ManageBedsCommand(MultipleBedSpawn plugin, String name) {
        super(name);
        ManageBedsCommand.plugin = plugin;
        this.description = "Opens a menu to manage, rename, and remove your beds";
        this.usageMessage = "/managebeds";
        this.setAliases(new ArrayList<String>());
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            openManageMenu(p);
        }
        return true;
    }
}