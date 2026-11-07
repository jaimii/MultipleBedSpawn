package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedDataType;
import me.gabij.multiplebedspawn.models.BedsDataType;
import org.bukkit.NamespacedKey;

public class Keys {
    public static NamespacedKey BEDS;
    public static NamespacedKey HAS_PROP;
    public static NamespacedKey IS_INVISIBLE;
    public static NamespacedKey CAN_PICKUP_ITEMS;
    public static NamespacedKey LAST_GAMEMODE;
    public static NamespacedKey ALLOW_FLY;
    public static NamespacedKey LAST_WALKSPEED;
    public static NamespacedKey SPAWN_LOC;
    public static NamespacedKey UUID;
    public static NamespacedKey LOCATION;
    public static NamespacedKey WORLD;
    public static NamespacedKey COOLDOWN;

    public static BedsDataType BEDS_TYPE;
    public static BedDataType BED_TYPE;

    public static void init(MultipleBedSpawn plugin) {
        BEDS = new NamespacedKey(plugin, "beds");
        HAS_PROP = new NamespacedKey(plugin, "hasProp");
        IS_INVISIBLE = new NamespacedKey(plugin, "isInvisible");
        CAN_PICKUP_ITEMS = new NamespacedKey(plugin, "canPickupItems");
        LAST_GAMEMODE = new NamespacedKey(plugin, "lastGameMode");
        ALLOW_FLY = new NamespacedKey(plugin, "allowFly");
        LAST_WALKSPEED = new NamespacedKey(plugin, "lastWalkspeed");
        SPAWN_LOC = new NamespacedKey(plugin, "spawnLoc");
        UUID = new NamespacedKey(plugin, "uuid");
        LOCATION = new NamespacedKey(plugin, "location");
        WORLD = new NamespacedKey(plugin, "world");
        COOLDOWN = new NamespacedKey(plugin, "cooldown");

        BEDS_TYPE = new BedsDataType();
        BED_TYPE = new BedDataType();
    }
}