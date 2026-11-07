package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.utils.BedDataCache;
import me.gabij.multiplebedspawn.utils.Keys;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static me.gabij.multiplebedspawn.utils.PlayerUtils.stringToLocation;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.undoPropPlayer;

public class PlayerJoinListener implements Listener {
    MultipleBedSpawn plugin;

    public PlayerJoinListener(MultipleBedSpawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        if (plugin.getConfig().getBoolean("spawn-on-sky") && playerData.has(Keys.SPAWN_LOC, PersistentDataType.STRING)) {
            Location location = stringToLocation(playerData.get(Keys.SPAWN_LOC, PersistentDataType.STRING));
            playerData.remove(Keys.SPAWN_LOC);
            p.teleport(location);
        }
        undoPropPlayer(p);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent e) {
        // Prevents memory leaks by evicting disconnecting players from cache
        BedDataCache.invalidate(e.getPlayer().getUniqueId());
    }
}