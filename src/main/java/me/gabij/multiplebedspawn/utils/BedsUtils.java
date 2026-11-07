package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BedsUtils {
    static MultipleBedSpawn plugin = MultipleBedSpawn.getInstance();
    private static final Map<String, String> BIOME_CACHE = new ConcurrentHashMap<>();

    public static String formatBiomeName(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) return "Unknown";
        return BIOME_CACHE.computeIfAbsent(rawKey, key -> {
            String[] parts = key.split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
            return sb.toString().trim();
        });
    }

    public static void removePlayerBed(String bedUUID, Player p) {
        PlayerBedsData playerBedsData = BedDataCache.get(p);
        HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
        if (beds.containsKey(bedUUID)) {
            BedData bedData = beds.get(bedUUID);
            playerBedsData.removeBed(bedUUID);
            BedDataCache.set(p, playerBedsData);

            World world = Bukkit.getWorld(bedData.getBedWorld());
            if (world != null) {
                String[] loc = PlayerUtils.splitThree(bedData.getBedCoords());
                Location locBed = new Location(world, Double.parseDouble(loc[0]), Double.parseDouble(loc[1]),
                        Double.parseDouble(loc[2]));

                int chunkX = locBed.getBlockX() >> 4;
                int chunkZ = locBed.getBlockZ() >> 4;
                if (world.isChunkGenerated(chunkX, chunkZ)) {
                    Block bed = world.getBlockAt(locBed);
                    if (bed.getBlockData() instanceof Bed bedPart) {
                        if (bedPart.getPart().toString().equals("FOOT")) {
                            bed = bed.getRelative(bedPart.getFacing());
                        }
                    }
                    BlockState blockState = bed.getState();
                    if (blockState instanceof TileState tileState) {
                        PersistentDataContainer container = tileState.getPersistentDataContainer();
                        container.remove(Keys.UUID);
                        tileState.update();
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

        // Avoid loading unloaded chunks unless forcing on teleport click
        if (!forceLoad && !world.isChunkLoaded(chunkX, chunkZ)) {
            return true;
        }

        Block bed = world.getBlockAt(locBed);
        boolean isBed = false;
        if (bed.getBlockData() instanceof Bed bedPart) {
            if (bedPart.getPart().toString().equals("FOOT")) {
                bed = bed.getRelative(bedPart.getFacing());
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
                String uuid = container.get(Keys.UUID, PersistentDataType.STRING);

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
            if (bedPart.getPart().toString().equals("FOOT")) {
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
                if (permName.startsWith("multiplebedspawn.maxcount.") && perm.getValue()) {
                    String maxCount = permName.substring("multiplebedspawn.maxcount.".length()).trim();
                    try {
                        int max = Integer.parseInt(maxCount);
                        if (max > 53) {
                            plugin.getLogger().warning("Permission " + permName + " defaulted to 53.");
                            max = 53;
                        }
                        if (max > maxBedsByPerms) {
                            maxBedsByPerms = max;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        if (maxBeds > 53) {
            maxBeds = 53;
        }
        return maxBedsByPerms > 0 ? maxBedsByPerms : maxBeds;
    }
}