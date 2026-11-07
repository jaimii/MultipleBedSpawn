package me.gabij.multiplebedspawn.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import me.gabij.multiplebedspawn.utils.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checkIfIsBed;

public class NameCommand implements BasicCommand {
    private final MultipleBedSpawn plugin;

    // Single-argument constructor for Paper Lifecycle registration
    public NameCommand(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    // Overloaded two-argument constructor for backward compatibility
    public NameCommand(MultipleBedSpawn plugin, String name) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (sender instanceof Player p) {
            String name = String.join(" ", args).trim();
            Block bed = checkIfIsBed(p.getTargetBlockExact(4));
            if (bed != null) {
                BlockState blockState = bed.getState();
                String bedUUID = null;
                if (blockState instanceof TileState tileState) {
                    PersistentDataContainer container = tileState.getPersistentDataContainer();
                    if (container.has(Keys.UUID, PersistentDataType.STRING)) {
                        bedUUID = container.get(Keys.UUID, PersistentDataType.STRING);
                    }
                    tileState.update();
                }

                if (bedUUID == null) {
                    p.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    return;
                }

                PersistentDataContainer playerData = p.getPersistentDataContainer();
                if (playerData.has(Keys.BEDS, Keys.BEDS_TYPE)) {
                    PlayerBedsData playerBedsData = playerData.get(Keys.BEDS, Keys.BEDS_TYPE);
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                            && playerBedsData.hasBed(bedUUID)) {
                        BedData bedData = playerBedsData.getPlayerBedData().get(bedUUID);
                        bedData.setBedName(name);
                        playerData.set(Keys.BEDS, Keys.BEDS_TYPE, playerBedsData);
                        p.sendMessage(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(
                                plugin.getMessages("bed-name-registered-successfully-message")));
                    } else {
                        p.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    }
                }
            } else {
                p.sendMessage(Component.text(plugin.getMessages("bed-not-found-message"), NamedTextColor.RED));
            }
        }
    }
}