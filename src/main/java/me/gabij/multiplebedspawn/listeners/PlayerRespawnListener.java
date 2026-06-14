package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

import static me.gabij.multiplebedspawn.listeners.RespawnMenuHandler.openRespawnMenu;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.locationToString;

public class PlayerRespawnListener implements Listener {
    static MultipleBedSpawn plugin;

    public PlayerRespawnListener(MultipleBedSpawn plugin) {
        PlayerRespawnListener.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        World world = e.getRespawnLocation().getWorld();
        if (world.getEnvironment() == Environment.NETHER || world.getEnvironment() == Environment.THE_END)
            return;
        if (e.getRespawnReason() == RespawnReason.PLUGIN)
            return;
        String worldName = world.getName();
        List<String> denylist = plugin.getConfig().getStringList("denylist");
        List<String> allowlist = plugin.getConfig().getStringList("allowlist");
        boolean passLists = (!denylist.contains(worldName)) && (allowlist.contains(worldName) || allowlist.isEmpty());

        if (passLists) {
            Player p = e.getPlayer();
            PersistentDataContainer playerData = p.getPersistentDataContainer();

            // Redundant bed-checking loop has been successfully removed.
            // getPlayerBedsCount is called natively inside openRespawnMenu on the same tick,
            // entirely eliminating duplicate operations.

            Location originalRespawnLoc = e.getRespawnLocation();
            playerData.set(new NamespacedKey(plugin, "spawnLoc"), PersistentDataType.STRING, locationToString(originalRespawnLoc));

            Location tempRespawnLoc = new Location(originalRespawnLoc.getWorld(), 0.0, 300.0, 0.0);
            e.setRespawnLocation(tempRespawnLoc);

            openRespawnMenu(p);
        }
    }
}