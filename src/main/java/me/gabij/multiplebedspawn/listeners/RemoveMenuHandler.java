package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.models.BedData;
import me.gabij.multiplebedspawn.models.PlayerBedsData;
import me.gabij.multiplebedspawn.utils.BedDataCache;
import me.gabij.multiplebedspawn.utils.BedsUtils;
import me.gabij.multiplebedspawn.utils.Keys;
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

    public static void openRemoveMenu(Player p) {
        PlayerBedsData playerBedsData = BedDataCache.get(p);
        int playerBedsCount = getPlayerBedsCount(p);

        if (playerBedsCount > 0) {
            int bedCount = playerBedsCount + 1;
            int invSize = Math.min(54, ((bedCount + 8) / 9) * 9);
            Inventory gui = Bukkit.createInventory(p, invSize,
                    MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(plugin.getMessages("remove-menu-title")));

            HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
            boolean linkWorlds = plugin.getConfig().getBoolean("link-worlds");
            String currentWorldName = p.getWorld().getName();
            boolean disableWorld = plugin.getConfig().getBoolean("disable-bed-world-desc");
            boolean disableCoords = plugin.getConfig().getBoolean("disable-bed-coords-desc");
            String defaultNameTemplate = plugin.getMessages("default-bed-name");

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

                data.set(Keys.UUID, PersistentDataType.STRING, uuid);
                data.set(Keys.LOCATION, PersistentDataType.STRING, bed.getBedCoords());
                data.set(Keys.WORLD, PersistentDataType.STRING, bed.getBedWorld());

                item_meta.lore(lore);
                item.setItemMeta(item_meta);
                gui.addItem(item);
                cont.getAndIncrement();
            });

            ItemStack item = new ItemStack(Material.BARRIER, 1);
            ItemMeta item_meta = item.getItemMeta();
            item_meta.displayName(Component.text(plugin.getMessages("close-menu"), NamedTextColor.YELLOW));
            item.setItemMeta(item_meta);
            gui.setItem(invSize - 1, item);

            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(gui), 0L);
        }
    }

    public static void updateItens(Inventory gui, Player p) {
        if (!gui.getViewers().isEmpty()) {
            PlayerBedsData playerBedsData = BedDataCache.get(p);
            int playerBedsCount = 0;
            if (playerBedsData != null && playerBedsData.getPlayerBedData() != null) {
                playerBedsCount = playerBedsData.getPlayerBedData().size();
            }

            if (playerBedsCount > 0) {
                int bedCount = playerBedsCount + 1;
                int invSize = Math.min(54, ((bedCount + 8) / 9) * 9);
                gui.clear();

                HashMap<String, BedData> beds = playerBedsData.getPlayerBedData();
                boolean linkWorlds = plugin.getConfig().getBoolean("link-worlds");
                String currentWorldName = p.getWorld().getName();
                boolean disableWorld = plugin.getConfig().getBoolean("disable-bed-world-desc");
                boolean disableCoords = plugin.getConfig().getBoolean("disable-bed-coords-desc");
                String defaultNameTemplate = plugin.getMessages("default-bed-name");

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

                    data.set(Keys.UUID, PersistentDataType.STRING, uuid);
                    data.set(Keys.LOCATION, PersistentDataType.STRING, bed.getBedCoords());
                    data.set(Keys.WORLD, PersistentDataType.STRING, bed.getBedWorld());

                    item_meta.lore(lore);
                    item.setItemMeta(item_meta);
                    gui.addItem(item);
                    cont.getAndIncrement();
                });

                ItemStack item = new ItemStack(Material.BARRIER, 1);
                ItemMeta item_meta = item.getItemMeta();
                item_meta.displayName(Component.text(plugin.getMessages("close-menu"), NamedTextColor.YELLOW));
                item.setItemMeta(item_meta);
                gui.setItem(invSize - 1, item);
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 0L);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 0L);
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
                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {
                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();

                    String uuid = data.get(Keys.UUID, PersistentDataType.STRING);
                    removePlayerBed(uuid, p);
                    updateItens(e.getClickedInventory(), p);
                } else if (e.getCurrentItem().getType() == Material.BARRIER) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 0L);
                }
            }
        }
    }
}