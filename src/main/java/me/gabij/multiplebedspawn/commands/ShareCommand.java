package me.gabij.multiplebedspawn.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import me.gabij.multiplebedspawn.utils.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checkIfIsBed;

public class ShareCommand implements BasicCommand {
    private final MultipleBedSpawn plugin;

    public ShareCommand(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /sharebed <player>", NamedTextColor.RED));
            return;
        }

        if (sender instanceof Player ownerPlayer) {
            Player receiverPlayer = Bukkit.getPlayer(args[0]);
            if (receiverPlayer == null) {
                ownerPlayer.sendMessage(Component.text(plugin.getMessages("player-not-found"), NamedTextColor.RED));
                return;
            }
            if (receiverPlayer == ownerPlayer) {
                return;
            }
            Block bed = checkIfIsBed(ownerPlayer.getTargetBlockExact(4));
            if (bed != null) {
                BlockState blockState = bed.getState();
                String bedUUID = null;
                if (blockState instanceof TileState tileState) {
                    PersistentDataContainer container = tileState.getPersistentDataContainer();
                    if (container.has(Keys.UUID, PersistentDataType.STRING)) {
                        bedUUID = container.get(Keys.UUID, PersistentDataType.STRING);
                    }
                }

                if (bedUUID == null) {
                    ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    return;
                }

                PersistentDataContainer playerData = ownerPlayer.getPersistentDataContainer();
                if (playerData.has(Keys.BEDS, Keys.BEDS_TYPE)) {
                    PlayerBedsData playerBedsData = playerData.get(Keys.BEDS, Keys.BEDS_TYPE);
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                            && playerBedsData.hasBed(bedUUID)) {
                        PersistentDataContainer receiverData = receiverPlayer.getPersistentDataContainer();
                        PlayerBedsData receiverBedsData = receiverData.has(Keys.BEDS, Keys.BEDS_TYPE)
                                ? receiverData.get(Keys.BEDS, Keys.BEDS_TYPE)
                                : new PlayerBedsData();

                        playerBedsData.shareBed(receiverBedsData, bedUUID);
                        receiverData.set(Keys.BEDS, Keys.BEDS_TYPE, receiverBedsData);
                        playerData.set(Keys.BEDS, Keys.BEDS_TYPE, playerBedsData);

                        receiverPlayer.sendMessage(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(
                                plugin.getMessages("bed-registered-successfully-message")));
                    } else {
                        ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    }
                }
            } else {
                ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-found-message"), NamedTextColor.RED));
            }
        }
    }
}