package me.gabij.multiplebedspawn.commands;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checkIfIsBed;

import java.util.ArrayList;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NameCommand extends BukkitCommand {
    static MultipleBedSpawn plugin;

    public NameCommand(MultipleBedSpawn plugin, String name) {
        super(name);
        NameCommand.plugin = plugin;
        this.description = "Changes the name of the bed you are looking at";
        this.usageMessage = "/renamebed <name of the bed>";
        this.setAliases(new ArrayList<String>());
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (sender instanceof Player) {
            String name = "";
            for (String arg : args) {
                name += arg + " ";
            }
            Player p = (Player) sender;
            Block bed = checkIfIsBed(p.getTargetBlockExact(4));
            if (bed != null) {
                BlockState blockState = bed.getState();
                String bedUUID = null;
                if (blockState instanceof TileState tileState) {
                    PersistentDataContainer container = tileState.getPersistentDataContainer();
                    if (container.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {
                        bedUUID = container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);
                    }
                    tileState.update();
                }

                if (bedUUID == null) {
                    p.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    return false;
                }

                PlayerBedsData playerBedsData = null;
                PersistentDataContainer playerData = p.getPersistentDataContainer();

                if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                    playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                            && playerBedsData.hasBed(bedUUID)) {
                        BedData bedData = playerBedsData.getPlayerBedData().get(bedUUID);
                        bedData.setBedName(name);
                        playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);
                        p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                                plugin.getMessages("bed-name-registered-successfully-message")));
                    } else {
                        p.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                        return false;
                    }
                }
            } else {
                p.sendMessage(Component.text(plugin.getMessages("bed-not-found-message"), NamedTextColor.RED));
                return false;
            }
        }
        return true;
    }
}