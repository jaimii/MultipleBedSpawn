package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checksIfBedExists;

public class PlayerUtils {

    static MultipleBedSpawn plugin = MultipleBedSpawn.getInstance();

    // Fast index-based coordinate splitting, bypassing heavy regex splits
    public static String[] splitThree(String str) {
        if (str == null) return new String[]{"0", "0", "0"};
        int first = str.indexOf(':');
        int second = str.indexOf(':', first + 1);
        if (first == -1 || second == -1) {
            return str.split(":");
        }
        return new String[] {
                str.substring(0, first),
                str.substring(first + 1, second),
                str.substring(second + 1)
        };
    }

    // Fast index-based location splitting (including world names)
    public static String[] splitFour(String str) {
        if (str == null) return new String[]{"world", "0", "0", "0"};
        int first = str.indexOf(':');
        int second = str.indexOf(':', first + 1);
        int third = str.indexOf(':', second + 1);
        if (first == -1 || second == -1 || third == -1) {
            return str.split(":");
        }
        return new String[] {
                str.substring(0, first),
                str.substring(first + 1, second),
                str.substring(second + 1, third),
                str.substring(third + 1)
        };
    }

    public static String locationToString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    public static Location stringToLocation(String locString) {
        String[] loc = splitFour(locString);
        return new Location(Bukkit.getWorld(loc[0]), Double.parseDouble(loc[1]), Double.parseDouble(loc[2]),
                Double.parseDouble(loc[3]));
    }

    public static void setPropPlayer(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (!playerData.has(new NamespacedKey(plugin, "hasProp"), PersistentDataType.BOOLEAN)) {
            p.setInvulnerable(true);

            playerData.set(new NamespacedKey(plugin, "isInvisible"), PersistentDataType.BOOLEAN, p.isInvisible());
            p.setInvisible(true);

            playerData.set(new NamespacedKey(plugin, "canPickupItems"), PersistentDataType.BOOLEAN,
                    p.getCanPickupItems());
            p.setCanPickupItems(false);

            // Saves original gamemode and sets player to spectator temporarily
            playerData.set(new NamespacedKey(plugin, "lastGameMode"), PersistentDataType.STRING, p.getGameMode().name());
            p.setGameMode(GameMode.SPECTATOR);

            if (plugin.getConfig().getBoolean("spawn-on-sky")) {
                playerData.set(new NamespacedKey(plugin, "allowFly"), PersistentDataType.BOOLEAN, p.getAllowFlight());
                p.setAllowFlight(true);
                p.setFlying(true);
            }
            playerData.set(new NamespacedKey(plugin, "lastWalkspeed"), PersistentDataType.FLOAT, p.getWalkSpeed());
            p.setWalkSpeed(0);

            playerData.set(new NamespacedKey(plugin, "hasProp"), PersistentDataType.BOOLEAN, true);
        }
    }

    public static void undoPropPlayer(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (playerData.has(new NamespacedKey(plugin, "hasProp"), PersistentDataType.BOOLEAN)) {
            playerData.remove(new NamespacedKey(plugin, "hasProp"));

            p.setInvulnerable(false);
            p.setInvisible(playerData.get(new NamespacedKey(plugin, "isInvisible"), PersistentDataType.BOOLEAN));
            p.setCanPickupItems(
                    playerData.get(new NamespacedKey(plugin, "canPickupItems"), PersistentDataType.BOOLEAN));

            playerData.remove(new NamespacedKey(plugin, "isInvisible"));
            playerData.remove(new NamespacedKey(plugin, "canPickupItems"));

            p.setWalkSpeed(playerData.get(new NamespacedKey(plugin, "lastWalkspeed"), PersistentDataType.FLOAT));
            playerData.remove(new NamespacedKey(plugin, "lastWalkspeed"));

            // Restores the original gamemode
            if (playerData.has(new NamespacedKey(plugin, "lastGameMode"), PersistentDataType.STRING)) {
                String gmName = playerData.get(new NamespacedKey(plugin, "lastGameMode"), PersistentDataType.STRING);
                try {
                    p.setGameMode(GameMode.valueOf(gmName));
                } catch (Exception e) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
                playerData.remove(new NamespacedKey(plugin, "lastGameMode"));
            }

            if (plugin.getConfig().getBoolean("spawn-on-sky")) {
                p.setAllowFlight(playerData.get(new NamespacedKey(plugin, "allowFly"), PersistentDataType.BOOLEAN));
                p.setFlying(false);

                playerData.remove(new NamespacedKey(plugin, "allowFly"));
                playerData.remove(new NamespacedKey(plugin, "isFlying"));
            }

            p.closeInventory();
        }
    }

    public static void teleportPlayer(Player p, PersistentDataContainer data, PersistentDataContainer playerData,
                                      PlayerBedsData playerBedsData, String uuid) {
        boolean isOkayToTP = true;

        if (data.has(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG)
                && data.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {

            long cooldown = data.get(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG);
            if (cooldown > System.currentTimeMillis()) {
                isOkayToTP = false;
            }
        }

        if (isOkayToTP) {
            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            undoPropPlayer(p);
            String loc[] = splitThree(beds.get(uuid).getBedSpawnCoords());
            World world = Bukkit.getWorld(beds.get(uuid).getBedWorld());
            Location locSpawn = new Location(world, Double.parseDouble(loc[0]), Double.parseDouble(loc[1]),
                    Double.parseDouble(loc[2]));
            if (!p.hasPermission("multiplebedspawn.skipcooldown")) {
                beds.get(uuid).setBedCooldown(
                        System.currentTimeMillis() + (plugin.getConfig().getLong("bed-cooldown") * 1000));
            }
            playerData.set(new NamespacedKey(plugin, "beds"), new BedsDataType(), playerBedsData);
            playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
            p.teleport(locSpawn);
        }
    }

    public static Location getPlayerRespawnLoc(Player p) {
        Location loc = p.getLocation();
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (playerData.has(new NamespacedKey(plugin, "spawnLoc"), PersistentDataType.STRING)) {
            Location playerRespawnLocation = stringToLocation(
                    playerData.get(new NamespacedKey(plugin, "spawnLoc"), PersistentDataType.STRING));
            if (playerRespawnLocation != null) {
                loc = playerRespawnLocation;
            }
        }
        return loc;
    }

    public static Integer getPlayerBedsCount(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        PlayerBedsData playerBedsData = null;
        int playerBedsCount = 0;
        if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
            if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
                World world = getPlayerRespawnLoc(p).getWorld();
                String worldName = world.getName();
                if (!plugin.getConfig().getBoolean("link-worlds")) {
                    HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                    List<String> originalKeys = new ArrayList<>(beds.keySet());
                    for (String uuid : originalKeys) {
                        BedData bedData = beds.get(uuid);
                        if (bedData != null && !bedData.getBedWorld().equalsIgnoreCase(worldName)) {
                            bedsT.remove(uuid);
                        }
                    }
                    beds = bedsT;
                }
                playerBedsCount = beds.size();

                // Safe iteration over a copied list of keys to prevent ConcurrentModificationException
                List<String> uuids = new ArrayList<>(beds.keySet());
                for (String uuid : uuids) {
                    BedData bedData = beds.get(uuid);
                    if (bedData == null) continue;

                    String[] location = splitThree(bedData.getBedCoords());
                    String bedWorld = bedData.getBedWorld();
                    Location bedLoc = new Location(Bukkit.getWorld(bedWorld), Double.parseDouble(location[0]),
                            Double.parseDouble(location[1]), Double.parseDouble(location[2]));
                    if (!checksIfBedExists(bedLoc, p, uuid)) {
                        playerBedsCount--;
                    }
                }
            }
        }
        return playerBedsCount;
    }
}