package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;

import static me.gabij.multiplebedspawn.utils.BedsUtils.getMaxNumberOfBeds;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.getPlayerBedsCount;

public class PlayerGetsOnBedListener implements Listener {

    MultipleBedSpawn plugin;

    public PlayerGetsOnBedListener(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerGetOnBed(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        String world = player.getWorld().getName();
        List<String> denylist = plugin.getConfig().getStringList("denylist");
        List<String> allowlist = plugin.getConfig().getStringList("allowlist");
        boolean passLists = (!denylist.contains(world)) && (allowlist.contains(world) || allowlist.isEmpty());

        if (passLists) {
            Block bed = e.getBed();
            PersistentDataContainer playerData = player.getPersistentDataContainer();

            int maxBeds = getMaxNumberOfBeds(player);
            PlayerBedsData playerBedsData = null;

            int playerBedsCount = getPlayerBedsCount(player);

            if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
            }

            if (playerBedsCount < maxBeds) {
                UUID randomUUID = UUID.randomUUID();
                BlockState blockState = bed.getState();

                if (blockState instanceof TileState tileState) {
                    PersistentDataContainer container = tileState.getPersistentDataContainer();

                    if (!container.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {
                        container.set(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING, "" + randomUUID);
                    } else {
                        randomUUID = UUID.fromString(
                                container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING));
                        if ((playerBedsData == null
                                || (playerBedsData != null && !playerBedsData.hasBed(randomUUID.toString())))
                                && plugin.getConfig().getBoolean("exclusive-bed")) {
                            player.sendMessage(Component.text(plugin.getMessages("bed-already-has-owner"), NamedTextColor.RED));
                            return;
                        }
                    }
                    tileState.update();
                }

                boolean registerBed = false;
                if (playerBedsData == null) {
                    playerBedsData = new PlayerBedsData(player, bed, randomUUID.toString());
                    registerBed = true;
                } else if (!playerBedsData.hasBed(randomUUID.toString())) {
                    playerBedsData.setNewBed(player, bed, randomUUID.toString());
                    registerBed = true;
                }

                if (registerBed) {
                    playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);
                    player.sendMessage(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(
                            plugin.getMessages("bed-registered-successfully-message")));
                }
            } else {
                player.sendMessage(Component.text(plugin.getMessages("max-beds-message"), NamedTextColor.RED));
            }
            player.setBedSpawnLocation(null);
            e.setCancelled(plugin.getConfig().getBoolean("disable-sleeping"));
        }
    }
}