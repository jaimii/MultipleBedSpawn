package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.BedsDataType;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import me.gabij.multiplebedspawn.utils.PlayerUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static me.gabij.multiplebedspawn.utils.BedsUtils.checksIfBedExists;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.*;
import static me.gabij.multiplebedspawn.utils.RunCommandUtils.runCommandOnSpawn;;

@SuppressWarnings("deprecation")
public class RespawnMenuHandler implements Listener {

    static MultipleBedSpawn plugin;

    public RespawnMenuHandler(MultipleBedSpawn plugin) {
        RespawnMenuHandler.plugin = plugin;
    }

    private static String formatBiomeName(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
        }
        return sb.toString().trim();
    }

    public static void updateItens(Inventory gui, Player p) {
        if (gui.getViewers().toString().length() > 2) {
            ItemStack itens[] = gui.getContents();
            boolean hasActiveCooldown = false;
            for (ItemStack item : itens) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta item_meta = item.getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    if (data.has(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG)
                            && data.has(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING)) {

                        long cooldown = data.get(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG);
                        List<Component> lore = item_meta.lore();

                        int optionsCount = 3;
                        if (plugin.getConfig().getBoolean("disable-bed-world-desc")) {
                            optionsCount--;
                        }
                        if (plugin.getConfig().getBoolean("disable-bed-coords-desc")) {
                            optionsCount--;
                        }
                        if (cooldown > System.currentTimeMillis()) {
                            hasActiveCooldown = true;
                            long sec = (cooldown - System.currentTimeMillis()) / 1000;
                            String seconds = Long.toString(sec);
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            Component cdComp = MultipleBedSpawn.LEGACY_SERIALIZER.deserialize("&6&l" + plugin.getMessages("cooldown-text").replace("{1}", seconds));
                            if (lore.size() > optionsCount) {
                                lore.set(optionsCount, cdComp);
                            } else {
                                lore.add(cdComp);
                            }
                        } else {
                            if (lore != null && lore.size() > optionsCount) {
                                lore.remove(optionsCount);
                            }
                        }

                        item_meta.lore(lore);
                        item.setItemMeta(item_meta);
                    }
                }
            }

            if (hasActiveCooldown) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updateItens(gui, p);
                }, 10L);
            }
        }
    }

    public static void openRespawnMenu(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        PlayerBedsData playerBedsData = null;

        int playerBedsCount = getPlayerBedsCount(p);

        if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
        }

        if (playerBedsCount > 0) {
            setPropPlayer(p);

            int bedCount = playerBedsCount + 1;
            Inventory gui = Bukkit.createInventory(p, 9 * ((int) Math.ceil(bedCount / (Double) 9.0)),
                    MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("menu-title")));

            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            if (!plugin.getConfig().getBoolean("link-worlds")) {
                World world = getPlayerRespawnLoc(p).getWorld();
                HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                beds.forEach((uuid, bed) -> {
                    if (!bed.getBedWorld().equalsIgnoreCase(world.getName())) {
                        bedsT.remove(uuid);
                    }
                });
                beds = bedsT;
            }
            AtomicBoolean hasCooldown = new AtomicBoolean(false);
            AtomicInteger cont = new AtomicInteger(1);
            beds.forEach((uuid, bed) -> {
                ItemStack item = new ItemStack(bed.getBedMaterial(), 1);
                ItemMeta item_meta = item.getItemMeta();
                String bedName = plugin.getMessages("default-bed-name").replace("{1}", cont.toString());
                Component nameComp = MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(bed.getBedName() != null ? bed.getBedName() : bedName);
                item_meta.displayName(nameComp);

                PersistentDataContainer data = item_meta.getPersistentDataContainer();

                List<Component> lore = new ArrayList<>();
                if (!plugin.getConfig().getBoolean("disable-bed-world-desc")) {
                    lore.add(Component.text(bed.getBedWorld().toUpperCase(), NamedTextColor.DARK_PURPLE));
                }
                if (!plugin.getConfig().getBoolean("disable-bed-coords-desc")) {
                    String[] location = PlayerUtils.splitThree(bed.getBedCoords());
                    String locText = "X: " + location[0].substring(0, location[0].length() - 2) +
                            " Y: " + location[1].substring(0, location[1].length() - 2) +
                            " Z: " + location[2].substring(0, location[2].length() - 2);
                    lore.add(Component.text(locText, NamedTextColor.GRAY));
                }

                World world = Bukkit.getWorld(bed.getBedWorld());
                if (world != null) {
                    String[] location = PlayerUtils.splitThree(bed.getBedCoords());
                    double bx = Double.parseDouble(location[0]);
                    double by = Double.parseDouble(location[1]);
                    double bz = Double.parseDouble(location[2]);
                    org.bukkit.block.Biome biome = world.getBiome(new Location(world, bx, by, bz));
                    Component biomeComp = Component.text("Biome: ", NamedTextColor.GOLD)
                            .append(Component.text(formatBiomeName(biome.getKey().getKey()), NamedTextColor.GREEN));
                    lore.add(biomeComp);
                }

                if (bed.getBedCooldown() > 0L) {
                    long cooldown = bed.getBedCooldown();
                    if (cooldown > System.currentTimeMillis()) {
                        hasCooldown.set(true);
                        data.set(new NamespacedKey(plugin, "cooldown"), PersistentDataType.LONG, cooldown);
                    } else {
                        bed.setBedCooldown(0L);
                    }
                }

                data.set(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING, uuid);
                data.set(new NamespacedKey(plugin, "location"), PersistentDataType.STRING, bed.getBedCoords());
                data.set(new NamespacedKey(plugin, "world"), PersistentDataType.STRING, bed.getBedWorld());

                item_meta.lore(lore);
                item.setItemMeta(item_meta);
                gui.addItem(item);
                cont.getAndIncrement();
            });

            if (hasCooldown.get()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    updateItens(gui, p);
                }, 10L);
            }

            ItemStack item = new ItemStack(Material.GRASS_BLOCK, 1);
            ItemMeta item_meta = item.getItemMeta();
            item_meta.displayName(Component.text("SPAWN", NamedTextColor.YELLOW));
            item.setItemMeta(item_meta);
            gui.setItem(9 * ((int) Math.ceil(bedCount / (Double) 9.0)) - 1, item);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.openInventory(gui);
            }, 0L);
        } else {
            if (playerData.has(new NamespacedKey(plugin, "spawnLoc"))) {
                playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                undoPropPlayer(p);

                World world = p.getWorld();
                int spawnY = world.getHighestBlockYAt(0, 0);
                if (spawnY < world.getMinHeight()) {
                    spawnY = 64;
                } else {
                    spawnY += 1;
                }
                Location target = new Location(world, 0.5, spawnY, 0.5);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.teleport(target);
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        String menuTitle = PlainTextComponentSerializer.plainText().serialize(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("menu-title")));

        if (viewTitle.equalsIgnoreCase(menuTitle)) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() != null) {
                PersistentDataContainer playerData = p.getPersistentDataContainer();
                int playerBedsCount = 0;
                PlayerBedsData playerBedsData = null;
                if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                    playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                    if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                        playerBedsCount = playerBedsData.getPlayerBedData().size();
                    }
                }
                double bedCount = playerBedsCount + 1;
                int index = e.getSlot();
                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {
                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    String bedCoord[] = PlayerUtils.splitThree(data.get(new NamespacedKey(plugin, "location"), PersistentDataType.STRING));
                    String world = data.get(new NamespacedKey(plugin, "world"), PersistentDataType.STRING);
                    Location location = new Location(Bukkit.getWorld(world), Double.parseDouble(bedCoord[0]),
                            Double.parseDouble(bedCoord[1]), Double.parseDouble(bedCoord[2]));
                    String uuid = data.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);

                    if (checksIfBedExists(location, p, uuid, true)) {
                        teleportPlayer(p, data, playerData, playerBedsData, uuid);
                    } else {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.closeInventory();
                        }, 0L);
                    }
                } else if (index == 9 * ((int) Math.ceil(bedCount / (Double) 9.0)) - 1) {
                    undoPropPlayer(p);

                    World world = p.getWorld();
                    int spawnY = world.getHighestBlockYAt(0, 0);
                    if (spawnY < world.getMinHeight()) {
                        spawnY = 64;
                    } else {
                        spawnY += 1;
                    }
                    Location target = new Location(world, 0.5, spawnY, 0.5);

                    playerData.remove(new NamespacedKey(plugin, "spawnLoc"));
                    p.teleport(target);
                    runCommandOnSpawn(p);
                }
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equalsIgnoreCase(plugin.getMessages("menu-title"))) {
            Player p = (Player) e.getPlayer();
            if (!p.getCanPickupItems()) {
                openRespawnMenu(p);
            }
        }
    }
}