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
import java.util.concurrent.atomic.AtomicInteger;

import static me.gabij.multiplebedspawn.utils.BedsUtils.removePlayerBed;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.getPlayerBedsCount;

public class RemoveMenuHandler implements Listener {
    static MultipleBedSpawn plugin;

    public RemoveMenuHandler(MultipleBedSpawn plugin) {
        RemoveMenuHandler.plugin = plugin;
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

    public static void openRemoveMenu(Player p) {
        PersistentDataContainer playerData = p.getPersistentDataContainer();
        PlayerBedsData playerBedsData = null;

        int playerBedsCount = getPlayerBedsCount(p);

        if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
            playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
        }

        if (playerBedsCount > 0) {
            int bedCount = playerBedsCount + 1;
            Inventory gui = Bukkit.createInventory(p, 9 * ((int) Math.ceil(bedCount / (Double) 9.0)),
                    MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("remove-menu-title")));

            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            if (!plugin.getConfig().getBoolean("link-worlds")) {
                HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                beds.forEach((uuid, bed) -> {
                    if (!bed.getBedWorld().equalsIgnoreCase(p.getWorld().getName())) {
                        bedsT.remove(uuid);
                    }
                });
                beds = bedsT;
            }
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

                data.set(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING, uuid);
                data.set(new NamespacedKey(plugin, "location"), PersistentDataType.STRING, bed.getBedCoords());
                data.set(new NamespacedKey(plugin, "world"), PersistentDataType.STRING, bed.getBedWorld());

                item_meta.lore(lore);
                item.setItemMeta(item_meta);
                gui.addItem(item);
                cont.getAndIncrement();
            });

            ItemStack item = new ItemStack(Material.BARRIER, 1);
            ItemMeta item_meta = item.getItemMeta();
            item_meta.displayName(Component.text(plugin.getMessages("close-menu"), NamedTextColor.YELLOW));
            item.setItemMeta(item_meta);
            gui.setItem(9 * ((int) Math.ceil(bedCount / (Double) 9.0)) - 1, item);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.openInventory(gui);
            }, 0L);
        }
    }

    public static void updateItens(Inventory gui, Player p) {
        if (gui.getViewers().toString().length() > 2) {
            PersistentDataContainer playerData = p.getPersistentDataContainer();
            int playerBedsCount = 0;
            PlayerBedsData playerBedsData = null;
            if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                    HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
                    if (!plugin.getConfig().getBoolean("link-worlds")) {
                        HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                        beds.forEach((uuid, bed) -> {
                            if (!bed.getBedWorld().equalsIgnoreCase(p.getWorld().getName())) {
                                bedsT.remove(uuid);
                            }
                        });
                        beds = bedsT;
                    }
                    playerBedsCount = beds.size();
                }
            }

            if (playerBedsCount > 0) {
                int bedCount = playerBedsCount + 1;
                gui.clear();
                HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
                if (!plugin.getConfig().getBoolean("link-worlds")) {
                    HashMap<String, BedData> bedsT = (HashMap<String, BedData>) beds.clone();
                    beds.forEach((uuid, bed) -> {
                        if (!bed.getBedWorld().equalsIgnoreCase(p.getWorld().getName())) {
                            bedsT.remove(uuid);
                        }
                    });
                    beds = bedsT;
                }
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

                    data.set(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING, uuid);
                    data.set(new NamespacedKey(plugin, "location"), PersistentDataType.STRING, bed.getBedCoords());
                    data.set(new NamespacedKey(plugin, "world"), PersistentDataType.STRING, bed.getBedWorld());

                    item_meta.lore(lore);
                    item.setItemMeta(item_meta);
                    gui.addItem(item);
                    cont.getAndIncrement();
                });

                ItemStack item = new ItemStack(Material.BARRIER, 1);
                ItemMeta item_meta = item.getItemMeta();
                item_meta.displayName(Component.text(plugin.getMessages("close-menu"), NamedTextColor.YELLOW));
                item.setItemMeta(item_meta);
                gui.setItem(9 * ((int) Math.ceil(bedCount / (Double) 9.0)) - 1, item);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.closeInventory();
                }, 0L);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                p.closeInventory();
            }, 0L);
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        String removeTitle = PlainTextComponentSerializer.plainText().serialize(MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("remove-menu-title")));

        if (viewTitle.equalsIgnoreCase(removeTitle)) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() != null) {
                PersistentDataContainer playerData = p.getPersistentDataContainer();
                PlayerBedsData playerBedsData = null;
                if (playerData.has(new NamespacedKey(plugin, "beds"), new BedsDataType())) {
                    playerBedsData = playerData.get(new NamespacedKey(plugin, "beds"), new BedsDataType());
                }

                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {
                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    String uuid = data.get(new NamespacedKey(plugin, "uuid"), PersistentDataType.STRING);
                    removePlayerBed(uuid, p);
                    updateItens(e.getClickedInventory(), p);
                } else if (e.getCurrentItem().getType().toString().equalsIgnoreCase("BARRIER")) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        p.closeInventory();
                    }, 0L);
                }
            }
        }
    }
}