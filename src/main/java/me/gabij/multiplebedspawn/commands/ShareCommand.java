package me.gabij.multiplebedspawn.commands;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checkIfIsBed;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ShareCommand extends BukkitCommand {
    static MultipleBedSpawn plugin;

    public ShareCommand(MultipleBedSpawn plugin, String name) {
        super(name);
        ShareCommand.plugin = plugin;
        this.description = "Gives bed to another player";
        this.usageMessage = "/sharebed <player>";
        this.setAliases(new ArrayList<String>());
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {
        if (args.length != 1) {
            return false;
        }
        if (sender instanceof Player) {
            Player ownerPlayer = (Player) sender;
            Player receiverPlayer = Bukkit.getPlayer(args[0]);
            if (receiverPlayer == null) {
                ownerPlayer.sendMessage(Component.text(plugin.getMessages("player-not-found"), NamedTextColor.RED));
                return false;
            }
            if (receiverPlayer == ownerPlayer) {
                return false;
            }
            Block bed = checkIfIsBed(ownerPlayer.getTargetBlockExact(4));
            if (bed != null) {
                BlockState blockState = bed.getState();
                String bedUUID = null;
                if (blockState instanceof TileState tileState) {
                    PersistentDataContainer container = tileState.getPersistentDataContainer();
                    if (container.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {
                        bedUUID = container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);
                    }
                }

                if (bedUUID == null) {
                    ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                    return false;
                }

                PlayerBedsData playerBedsData = null;
                PersistentDataContainer playerData = ownerPlayer.getPersistentDataContainer();

                if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                    playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null
                            && playerBedsData.hasBed(bedUUID)) {
                        PersistentDataContainer receiverData = receiverPlayer.getPersistentDataContainer();
                        PlayerBedsData receiverBedsData = receiverData.has(new NamespacedKey(plugin, "beds"),
                                new BedsDataType())
                                ? receiverData.get(new NamespacedKey(plugin, "beds"), new BedsDataType())
                                : new PlayerBedsData();

                        playerBedsData.shareBed(receiverBedsData, bedUUID);
                        receiverData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), receiverBedsData);
                        playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);

                        receiverPlayer.sendMessage(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(
                                plugin.getMessages("bed-registered-successfully-message")));
                    } else {
                        ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-registered-message"), NamedTextColor.RED));
                        return false;
                    }
                }
            } else {
                plugin.getLogger().info("Not found");
                ownerPlayer.sendMessage(Component.text(plugin.getMessages("bed-not-found-message"), NamedTextColor.RED));
                return false;
            }
        }
        return true;
    }
}