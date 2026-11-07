package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checksIfBedExists;

public class PlayerUtils {

    static MultipleBedSpawn plugin = MultipleBedSpawn.getInstance();

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

    public static void playTeleportEffects(Location loc) {
        if (loc != null && loc.getWorld() != null) {
            World world = loc.getWorld();
            boolean enableParticles = plugin.getConfig().getBoolean("magic-mirror.enable-particles", true);
            boolean enableSounds = plugin.getConfig().getBoolean("magic-mirror.enable-sounds", true);
            boolean enableLightning = plugin.getConfig().getBoolean("magic-mirror.enable-lightning", true);
            float volume = Math.clamp((float) plugin.getConfig().getDouble("magic-mirror.sound-effects-volume", 1.0), 0.0f, 1.0f);

            if (enableLightning) {
                world.strikeLightningEffect(loc);
            }
            if (enableParticles) {
                world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 100, 0.5, 1.0, 0.5, 0.2);
                world.spawnParticle(Particle.GLOW, loc.clone().add(0, 1, 0), 50, 0.5, 1.0, 0.5, 0.1);
            }
            if (enableSounds) {
                world.playSound(loc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, volume, 1.2f);
                world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, volume * 0.8f, 1.5f);
            }
        }
    }

    public static void setPropPlayer(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (!playerData.has(Keys.HAS_PROP, PersistentDataType.BOOLEAN)) {
            playerData.set(Keys.HAS_PROP, PersistentDataType.BOOLEAN, true);
            playerData.set(Keys.LAST_WALKSPEED, PersistentDataType.FLOAT, p.getWalkSpeed());
            p.setWalkSpeed(0f);

            if (p.isDead()) {
                p.setInvulnerable(true);
                playerData.set(Keys.IS_INVISIBLE, PersistentDataType.BOOLEAN, p.isInvisible());
                p.setInvisible(true);

                playerData.set(Keys.CAN_PICKUP_ITEMS, PersistentDataType.BOOLEAN, p.getCanPickupItems());
                p.setCanPickupItems(false);

                playerData.set(Keys.LAST_GAMEMODE, PersistentDataType.STRING, p.getGameMode().name());
                p.setGameMode(GameMode.SPECTATOR);

                if (plugin.getConfig().getBoolean("spawn-on-sky")) {
                    playerData.set(Keys.ALLOW_FLY, PersistentDataType.BOOLEAN, p.getAllowFlight());
                    p.setAllowFlight(true);
                    p.setFlying(true);
                }
            } else {
                // Alive Player: Stay in Survival, vulnerable, but fully immobilized
                p.setInvulnerable(false);
                playerData.set(Keys.CAN_PICKUP_ITEMS, PersistentDataType.BOOLEAN, p.getCanPickupItems());
                p.setCanPickupItems(false);

                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 72000, 255, false, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 72000, 200, false, false, false));
            }
        }
    }

    public static void undoPropPlayer(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (playerData.has(Keys.HAS_PROP, PersistentDataType.BOOLEAN)) {
            playerData.remove(Keys.HAS_PROP);

            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            p.setInvulnerable(false);

            if (playerData.has(Keys.IS_INVISIBLE, PersistentDataType.BOOLEAN)) {
                p.setInvisible(playerData.get(Keys.IS_INVISIBLE, PersistentDataType.BOOLEAN));
                playerData.remove(Keys.IS_INVISIBLE);
            } else {
                p.setInvisible(false);
            }

            if (playerData.has(Keys.CAN_PICKUP_ITEMS, PersistentDataType.BOOLEAN)) {
                p.setCanPickupItems(playerData.get(Keys.CAN_PICKUP_ITEMS, PersistentDataType.BOOLEAN));
                playerData.remove(Keys.CAN_PICKUP_ITEMS);
            } else {
                p.setCanPickupItems(true);
            }

            if (playerData.has(Keys.LAST_WALKSPEED, PersistentDataType.FLOAT)) {
                p.setWalkSpeed(playerData.get(Keys.LAST_WALKSPEED, PersistentDataType.FLOAT));
                playerData.remove(Keys.LAST_WALKSPEED);
            } else {
                p.setWalkSpeed(0.2f);
            }

            if (playerData.has(Keys.LAST_GAMEMODE, PersistentDataType.STRING)) {
                String gmName = playerData.get(Keys.LAST_GAMEMODE, PersistentDataType.STRING);
                try {
                    p.setGameMode(GameMode.valueOf(gmName));
                } catch (Exception e) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
                playerData.remove(Keys.LAST_GAMEMODE);
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                p.setGameMode(GameMode.SURVIVAL);
            }

            if (plugin.getConfig().getBoolean("spawn-on-sky") && p.getGameMode() == GameMode.SPECTATOR) {
                if (playerData.has(Keys.ALLOW_FLY, PersistentDataType.BOOLEAN)) {
                    p.setAllowFlight(playerData.get(Keys.ALLOW_FLY, PersistentDataType.BOOLEAN));
                    playerData.remove(Keys.ALLOW_FLY);
                } else {
                    p.setAllowFlight(false);
                }
                p.setFlying(false);
            }

            p.closeInventory();
        }
    }

    public static void teleportPlayer(Player p, PersistentDataContainer data, PersistentDataContainer playerData,
                                      PlayerBedsData playerBedsData, String uuid) {
        boolean isOkayToTP = true;

        if (data.has(Keys.COOLDOWN, PersistentDataType.LONG)
                && data.has(Keys.UUID, PersistentDataType.STRING)) {

            long cooldown = data.get(Keys.COOLDOWN, PersistentDataType.LONG);
            if (cooldown > System.currentTimeMillis()) {
                isOkayToTP = false;
            }
        }

        if (isOkayToTP) {
            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            undoPropPlayer(p);
            String[] loc = splitThree(beds.get(uuid).getBedSpawnCoords());
            World world = Bukkit.getWorld(beds.get(uuid).getBedWorld());
            Location locSpawn = new Location(world, Double.parseDouble(loc[0]), Double.parseDouble(loc[1]),
                    Double.parseDouble(loc[2]));
            if (!p.hasPermission("multiplebedspawn.skipcooldown")) {
                beds.get(uuid).setBedCooldown(
                        System.currentTimeMillis() + (plugin.getConfig().getLong("bed-cooldown") * 1000));
            }
            BedDataCache.set(p, playerBedsData);
            playerData.remove(Keys.SPAWN_LOC);

            p.teleport(locSpawn);
            playTeleportEffects(locSpawn);
        }
    }

    public static Location getPlayerRespawnLoc(Player p) {
        Location loc = p.getLocation();
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (playerData.has(Keys.SPAWN_LOC, PersistentDataType.STRING)) {
            Location playerRespawnLocation = stringToLocation(
                    playerData.get(Keys.SPAWN_LOC, PersistentDataType.STRING));
            if (playerRespawnLocation != null) {
                loc = playerRespawnLocation;
            }
        }
        return loc;
    }

    public static Integer getPlayerBedsCount(Player p) {
        PlayerBedsData playerBedsData = BedDataCache.get(p);
        int playerBedsCount = 0;
        if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            World world = getPlayerRespawnLoc(p).getWorld();
            String worldName = world.getName();
            boolean linkWorlds = plugin.getConfig().getBoolean("link-worlds");

            playerBedsCount = beds.size();
            List<String> uuids = new ArrayList<>(beds.keySet());
            for (String uuid : uuids) {
                BedData bedData = beds.get(uuid);
                if (bedData == null) continue;

                if (!linkWorlds && !bedData.getBedWorld().equalsIgnoreCase(worldName)) {
                    playerBedsCount--;
                    continue;
                }

                String[] location = splitThree(bedData.getBedCoords());
                String bedWorld = bedData.getBedWorld();
                Location bedLoc = new Location(Bukkit.getWorld(bedWorld), Double.parseDouble(location[0]),
                        Double.parseDouble(location[1]), Double.parseDouble(location[2]));
                if (!checksIfBedExists(bedLoc, p, uuid)) {
                    playerBedsCount--;
                }
            }
        }
        return Math.max(0, playerBedsCount);
    }
}