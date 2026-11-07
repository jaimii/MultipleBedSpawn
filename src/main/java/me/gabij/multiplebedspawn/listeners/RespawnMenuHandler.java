package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import me.gabij.multiplebedspawn.utils.BedDataCache;
import me.gabij.multiplebedspawn.utils.BedsUtils;
import me.gabij.multiplebedspawn.utils.Keys;
import me.gabij.multiplebedspawn.utils.PlayerUtils;
import me.gabij.multiplebedspawn.utils.RunCommandUtils;
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

public class RespawnMenuHandler implements Listener {

    static MultipleBedSpawn plugin;

    public RespawnMenuHandler(MultipleBedSpawn plugin) {
        RespawnMenuHandler.plugin = plugin;
    }

    public static void updateItens(Inventory gui, Player p) {
        if (!gui.getViewers().isEmpty()) {
            ItemStack[] itens = gui.getContents();
            boolean hasActiveCooldown = false;

            boolean disableWorld = plugin.getConfig().getBoolean("disable-bed-world-desc");
            boolean disableCoords = plugin.getConfig().getBoolean("disable-bed-coords-desc");
            String cdTextTemplate = plugin.getMessages("cooldown-text");

            for (ItemStack item : itens) {
                if (item != null && item.hasItemMeta()) {
                    ItemMeta item_meta = item.getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    if (data.has(Keys.COOLDOWN, PersistentDataType.LONG)
                            && data.has(Keys.UUID, PersistentDataType.STRING)) {

                        long cooldown = data.get(Keys.COOLDOWN, PersistentDataType.LONG);
                        List<Component> lore = item_meta.lore();

                        int optionsCount = 3;
                        if (disableWorld) optionsCount--;
                        if (disableCoords) optionsCount--;

                        if (cooldown > System.currentTimeMillis()) {
                            hasActiveCooldown = true;
                            long sec = (cooldown - System.currentTimeMillis()) / 1000;
                            String seconds = Long.toString(sec);
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            Component cdComp = MultipleBedSpawn.LEGACY_SERIALIZER.deserialize("&6&l" + cdTextTemplate.replace("{1}", seconds));
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
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateItens(gui, p), 10L);
            }
        }
    }

    public static void openRespawnMenu(Player p) {
        PlayerBedsData playerBedsData = BedDataCache.get(p);
        int playerBedsCount = PlayerUtils.getPlayerBedsCount(p);

        if (playerBedsCount > 0) {
            PlayerUtils.setPropPlayer(p);

            int bedCount = playerBedsCount + 1;
            int invSize = Math.min(54, ((bedCount + 8) / 9) * 9);
            Inventory gui = Bukkit.createInventory(p, invSize,
                    MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("menu-title")));

            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            boolean linkWorlds = plugin.getConfig().getBoolean("link-worlds");
            String currentWorldName = PlayerUtils.getPlayerRespawnLoc(p).getWorld().getName();
            boolean disableWorld = plugin.getConfig().getBoolean("disable-bed-world-desc");
            boolean disableCoords = plugin.getConfig().getBoolean("disable-bed-coords-desc");
            String defaultNameTemplate = plugin.getMessages("default-bed-name");

            AtomicBoolean hasCooldown = new AtomicBoolean(false);
            AtomicInteger cont = new AtomicInteger(1);
            beds.forEach((uuid, bed) -> {
                if (!linkWorlds && !bed.getBedWorld().equalsIgnoreCase(currentWorldName)) {
                    return;
                }

                ItemStack item = new ItemStack(bed.getBedMaterial(), 1);
                ItemMeta item_meta = item.getItemMeta();
                String bedName = defaultNameTemplate.replace("{1}", cont.toString());
                Component nameComp = MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(bed.getBedName() != null ? bed.getBedName() : bedName);
                item_meta.displayName(nameComp);

                PersistentDataContainer data = item_meta.getPersistentDataContainer();

                List<Component> lore = new ArrayList<>();
                if (!disableWorld) {
                    lore.add(Component.text(bed.getBedWorld().toUpperCase(), NamedTextColor.DARK_PURPLE));
                }
                String[] location = PlayerUtils.splitThree(bed.getBedCoords());
                if (!disableCoords) {
                    String locText = "X: " + location[0].substring(0, location[0].length() - 2) +
                            " Y: " + location[1].substring(0, location[1].length() - 2) +
                            " Z: " + location[2].substring(0, location[2].length() - 2);
                    lore.add(Component.text(locText, NamedTextColor.GRAY));
                }

                World world = Bukkit.getWorld(bed.getBedWorld());
                if (world != null) {
                    int bx = (int) Double.parseDouble(location[0]);
                    int by = (int) Double.parseDouble(location[1]);
                    int bz = (int) Double.parseDouble(location[2]);
                    org.bukkit.block.Biome biome = world.getBiome(bx, by, bz);
                    Component biomeComp = Component.text("Biome: ", NamedTextColor.GOLD)
                            .append(Component.text(BedsUtils.formatBiomeName(biome.getKey().getKey()), NamedTextColor.GREEN));
                    lore.add(biomeComp);
                }

                if (bed.getBedCooldown() > 0L) {
                    long cooldown = bed.getBedCooldown();
                    if (cooldown > System.currentTimeMillis()) {
                        hasCooldown.set(true);
                        data.set(Keys.COOLDOWN, PersistentDataType.LONG, cooldown);
                    } else {
                        bed.setBedCooldown(0L);
                    }
                }

                data.set(Keys.UUID, PersistentDataType.STRING, uuid);
                data.set(Keys.LOCATION, PersistentDataType.STRING, bed.getBedCoords());
                data.set(Keys.WORLD, PersistentDataType.STRING, bed.getBedWorld());

                item_meta.lore(lore);
                item.setItemMeta(item_meta);
                gui.addItem(item);
                cont.getAndIncrement();
            });

            if (hasCooldown.get()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateItens(gui, p), 10L);
            }

            ItemStack item = new ItemStack(Material.GRASS_BLOCK, 1);
            ItemMeta item_meta = item.getItemMeta();
            item_meta.displayName(Component.text("SPAWN", NamedTextColor.YELLOW));
            item.setItemMeta(item_meta);
            gui.setItem(invSize - 1, item);

            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(gui), 0L);
        } else {
            PersistentDataContainer playerData = p.getPersistentDataContainer();
            if (playerData.has(Keys.SPAWN_LOC, PersistentDataType.STRING)) {
                playerData.remove(Keys.SPAWN_LOC);
                PlayerUtils.undoPropPlayer(p);

                World world = p.getWorld();
                int spawnY = world.getHighestBlockYAt(0, 0);
                if (spawnY < world.getMinHeight()) {
                    spawnY = 64;
                } else {
                    spawnY += 1;
                }
                Location target = new Location(world, 0.5, spawnY, 0.5);
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.teleport(target), 1L);
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
                PlayerBedsData playerBedsData = BedDataCache.get(p);
                int playerBedsCount = playerBedsData != null && playerBedsData.getPlayerBedData() != null
                        ? playerBedsData.getPlayerBedData().size() : 0;
                int invSize = Math.min(54, ((playerBedsCount + 1 + 8) / 9) * 9);
                int index = e.getSlot();

                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {
                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    String[] bedCoord = PlayerUtils.splitThree(data.get(Keys.LOCATION, PersistentDataType.STRING));
                    String world = data.get(Keys.WORLD, PersistentDataType.STRING);
                    Location location = new Location(Bukkit.getWorld(world), Double.parseDouble(bedCoord[0]),
                            Double.parseDouble(bedCoord[1]), Double.parseDouble(bedCoord[2]));
                    String uuid = data.get(Keys.UUID, PersistentDataType.STRING);

                    if (checksIfBedExists(location, p, uuid, true)) {
                        int windup = plugin.getConfig().getInt("magic-mirror.windup", 3);
                        if (!p.isDead() && windup > 0) {
                            PlayerUtils.undoPropPlayer(p);
                            p.closeInventory();
                            final PlayerBedsData finalBedsData = playerBedsData;
                            Location startLoc = p.getLocation().clone();
                            MagicMirrorListener.startWarp(p, () -> {
                                boolean enableLightning = plugin.getConfig().getBoolean("magic-mirror.enable-lightning", true);
                                if (enableLightning && startLoc.getWorld() != null) {
                                    startLoc.getWorld().strikeLightningEffect(startLoc);
                                }
                                PlayerUtils.teleportPlayer(p, data, p.getPersistentDataContainer(), finalBedsData, uuid);
                            });
                        } else {
                            PlayerUtils.teleportPlayer(p, data, p.getPersistentDataContainer(), playerBedsData, uuid);
                        }
                    } else {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 0L);
                    }
                } else if (index == invSize - 1) {
                    PlayerUtils.undoPropPlayer(p);

                    World world = p.getWorld();
                    int spawnY = world.getHighestBlockYAt(0, 0);
                    if (spawnY < world.getMinHeight()) {
                        spawnY = 64;
                    } else {
                        spawnY += 1;
                    }
                    Location target = new Location(world, 0.5, spawnY, 0.5);
                    p.getPersistentDataContainer().remove(Keys.SPAWN_LOC);

                    int windup = plugin.getConfig().getInt("magic-mirror.windup", 3);
                    Location startLoc = p.getLocation().clone();
                    if (!p.isDead() && windup > 0) {
                        p.closeInventory();
                        MagicMirrorListener.startWarp(p, () -> {
                            boolean enableLightning = plugin.getConfig().getBoolean("magic-mirror.enable-lightning", true);
                            if (enableLightning && startLoc.getWorld() != null) {
                                startLoc.getWorld().strikeLightningEffect(startLoc);
                            }
                            p.teleport(target);
                            PlayerUtils.playTeleportEffects(target);
                            RunCommandUtils.runCommandOnSpawn(p);
                        });
                    } else {
                        boolean enableLightning = plugin.getConfig().getBoolean("magic-mirror.enable-lightning", true);
                        if (enableLightning && startLoc.getWorld() != null) {
                            startLoc.getWorld().strikeLightningEffect(startLoc);
                        }
                        p.teleport(target);
                        PlayerUtils.playTeleportEffects(target);
                        RunCommandUtils.runCommandOnSpawn(p);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        String menuTitle = PlainTextComponentSerializer.plainText().serialize(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("menu-title")));

        if (viewTitle.equalsIgnoreCase(menuTitle)) {
            Player p = (Player) e.getPlayer();

            if (!p.isDead()) {
                PlayerUtils.undoPropPlayer(p);
                return;
            }

            if (!p.getCanPickupItems()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (p.isOnline() && p.isDead() && !p.getCanPickupItems()) {
                        openRespawnMenu(p);
                    }
                }, 1L);
            }
        }
    }
}