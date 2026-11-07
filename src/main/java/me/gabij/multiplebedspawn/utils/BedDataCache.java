package me.gabij.multiplebedspawn.utils;

import me.gabij.multiplebedspawn.models.PlayerBedsData;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BedDataCache {
    private static final Map<UUID, PlayerBedsData> CACHE = new ConcurrentHashMap<>();

    public static PlayerBedsData get(Player player) {
        return CACHE.computeIfAbsent(player.getUniqueId(), uuid -> {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (pdc.has(Keys.BEDS, Keys.BEDS_TYPE)) {
                PlayerBedsData data = pdc.get(Keys.BEDS, Keys.BEDS_TYPE);
                return data != null ? data : new PlayerBedsData();
            }
            return new PlayerBedsData();
        });
    }

    public static void set(Player player, PlayerBedsData data) {
        CACHE.put(player.getUniqueId(), data);
        player.getPersistentDataContainer().set(Keys.BEDS, Keys.BEDS_TYPE, data);
    }

    public static void invalidate(UUID uuid) {
        CACHE.remove(uuid);
    }
}