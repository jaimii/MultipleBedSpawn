package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;

public class BedsUtils {
    static MultipleBedSpawn plugin = MultipleBedSpawn.getInstance();

    public static void removePlayerBed(String bedUUID, Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            PlayerBedsData playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            if (beds.containsKey(bedUUID)) {
                BedData bedData = beds.get(bedUUID);
                playerBedsData.removeBed(bedUUID);
                playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);

                World world = Bukkit.getWorld(bedData.getBedWorld());
                if (world != null) {
                    String loc[] = PlayerUtils.splitThree(bedData.getBedCoords());
                    Location locBed = new Location(world, Double.parseDouble(loc[0]), Double.parseDouble(loc[1]),
                            Double.parseDouble(loc[2]));

                    int chunkX = locBed.getBlockX() >> 4;
                    int chunkZ = locBed.getBlockZ() >> 4;
                    if (world.isChunkGenerated(chunkX, chunkZ)) {
                        Block bed = world.getBlockAt(locBed);
                        if (bed.getBlockData() instanceof Bed bedPart) {
                            if (bedPart.getPart().toString() == "FOOT") {
                                bed = (Block) bed.getRelative(bedPart.getFacing());
                            }
                        }
                        BlockState blockState = bed.getState();
                        if (blockState instanceof TileState tileState) {
                            PersistentDataContainer container = tileState.getPersistentDataContainer();
                            container.remove(new NamespacedKey(plugin, "uuid"));
                            tileState.update();
                        }
                    }
                }
            }
        }
    }

    public static boolean checksIfBedExists(Location locBed, Player p, String bedUUID) {
        return checksIfBedExists(locBed, p, bedUUID, false);
    }

    public static boolean checksIfBedExists(Location locBed, Player p, String bedUUID, boolean forceLoad) {
        World world = locBed.getWorld();
        if (world == null) {
            removePlayerBed(bedUUID, p);
            return false;
        }

        int chunkX = locBed.getBlockX() >> 4;
        int chunkZ = locBed.getBlockZ() >> 4;

        if (!world.isChunkGenerated(chunkX, chunkZ)) {
            removePlayerBed(bedUUID, p);
            String msg = plugin.getMessages("bed-no-longer-exists");
            if (msg == null || msg.isEmpty()) {
                msg = "One of your beds no longer exists (it may have been deleted or trimmed).";
            }
            p.sendMessage(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(msg).color(NamedTextColor.RED));
            return false;
        }

        if (!forceLoad && !world.isChunkLoaded(chunkX, chunkZ)) {
            return true;
        }

        Block bed = world.getBlockAt(locBed);
        boolean isBed = false;
        if (bed.getBlockData() instanceof Bed bedPart) {
            if (bedPart.getPart().toString() == "FOOT") {
                bed = (Block) bed.getRelative(bedPart.getFacing());
            }
            isBed = true;
        }

        if (!isBed) {
            removePlayerBed(bedUUID, p);
            return false;
        } else {
            BlockState blockState = bed.getState();
            if (blockState instanceof TileState tileState) {
                PersistentDataContainer container = tileState.getPersistentDataContainer();
                String uuid = container.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);

                if (container == null || uuid == null || !uuid.equalsIgnoreCase(bedUUID)) {
                    removePlayerBed(bedUUID, p);
                    return false;
                }
            }
        }
        return true;
    }

    public static Block checkIfIsBed(Block block) {
        if (block != null && block.getBlockData() instanceof Bed bedPart) {
            if (bedPart.getPart().toString() == "FOOT") {
                block = block.getRelative(bedPart.getFacing());
            }
            return block;
        }
        return null;
    }

    public static int getMaxNumberOfBeds(Player player) {
        int maxBeds = plugin.getConfig().getInt("max-beds");
        int maxBedsByPerms = 0;
        if (player.hasPermission("multiplebedspawn.maxcount")) {
            for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
                String permName = perm.getPermission();
                if (permName.contains("multiplebedspawn.maxcount.") && perm.getValue()) {
                    String maxCount = (permName.split("multiplebedspawn.maxcount."))[1].trim();
                    try {
                        int max = Integer.parseInt(maxCount);
                        if (max > 53) {
                            plugin.getLogger().warning("Permission " + permName
                                    + " is invalid! Should be lower than 53. Value defaulted to 53, please remove this permission. Warning triggered by player "
                                    + player.getName());
                            max = 53;
                        }
                        if (max > maxBedsByPerms) {
                            maxBedsByPerms = max;
                        }
                    } catch (Exception err) {
                        plugin.getLogger().warning("Permission " + permName
                                + " is invalid! Should be a number after 'maxcount.'. Warning triggered by player "
                                + player.getName());
                    }
                }
            }
        }
        if (maxBeds > 53) {
            plugin.getLogger().warning("Max bed count cant be over 53! Value defaulted to 53.");
            plugin.getConfig().set("max-beds", 53);
            plugin.saveConfig();
            maxBeds = 53;
        }
        if (maxBedsByPerms > 0) {
            maxBeds = maxBedsByPerms;
        }
        return maxBeds;
    }
}