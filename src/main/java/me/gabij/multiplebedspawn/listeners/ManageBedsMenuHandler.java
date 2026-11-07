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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static me.gabij.multiplebedspawn.utils.BedsUtils.removePlayerBed;
import static me.gabij.multiplebedspawn.utils.PlayerUtils.getPlayerBedsCount;

public class ManageBedsMenuHandler implements Listener {
    static MultipleBedSpawn plugin = MultipleBedSpawn.getInstance();

    private static final Map<UUID, String> renamingPlayers = new HashMap<>();

    public ManageBedsMenuHandler(MultipleBedSpawn plugin) {
        ManageBedsMenuHandler.plugin = plugin;
    }

    public static void openManageMenu(Player p) {
        PlayerBedsData playerBedsData = BedDataCache.get(p);
        int playerBedsCount = getPlayerBedsCount(p);

        if (playerBedsCount > 0) {
            int bedCount = playerBedsCount + 1;
            int invSize = Math.min(54, ((bedCount + 8) / 9) * 9);
            Inventory gui = Bukkit.createInventory(p, invSize,
                    Component.text("Manage Your Beds", NamedTextColor.DARK_AQUA));

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

                lore.add(Component.empty());
                lore.add(Component.text("Left-Click to Rename", NamedTextColor.GREEN));
                lore.add(Component.text("Right-Click to Delete", NamedTextColor.RED));

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
            item_meta.displayName(Component.text("Close Menu", NamedTextColor.YELLOW));
            item.setItemMeta(item_meta);
            gui.setItem(invSize - 1, item);

            Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(gui), 0L);
        } else {
            p.sendMessage(Component.text("You do not have any registered beds to manage.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        String viewTitle = PlainTextComponentSerializer.plainText().serialize(e.getView().title());
        if (viewTitle.equalsIgnoreCase("Manage Your Beds")) {
            e.setCancelled(true);
            Player p = (Player) e.getWhoClicked();
            if (e.getCurrentItem() != null) {
                if (e.getCurrentItem().getType().toString().toLowerCase().contains("bed")) {
                    ItemMeta item_meta = e.getCurrentItem().getItemMeta();
                    PersistentDataContainer data = item_meta.getPersistentDataContainer();
                    String uuid = data.get(Keys.UUID, PersistentDataType.STRING);

                    if (e.getClick() == ClickType.RIGHT) {
                        removePlayerBed(uuid, p);
                        p.sendMessage(Component.text("Bed removed successfully.", NamedTextColor.YELLOW));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> openManageMenu(p), 1L);
                    } else if (e.getClick() == ClickType.LEFT) {
                        renamingPlayers.put(p.getUniqueId(), uuid);
                        p.closeInventory();
                        p.sendMessage(Component.text("Please type the new name for this bed in chat (or type 'cancel' to abort):", NamedTextColor.YELLOW));
                    }
                } else if (e.getCurrentItem().getType() == Material.BARRIER) {
                    p.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        if (renamingPlayers.containsKey(p.getUniqueId())) {
            event.setCancelled(true);
            String bedUuid = renamingPlayers.remove(p.getUniqueId());
            String message = MultipleBedSpawn.LEGACY_SERIALIZER.serialize(event.message()).trim();

            if (message.equalsIgnoreCase("cancel")) {
                p.sendMessage(Component.text("Renaming cancelled.", NamedTextColor.RED));
                Bukkit.getScheduler().runTask(plugin, () -> openManageMenu(p));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                PlayerBedsData playerBedsData = BedDataCache.get(p);
                if (playerBedsData != null && playerBedsData.hasBed(bedUuid)) {
                    playerBedsData.getPlayerBedData().get(bedUuid).setBedName(message);
                    BedDataCache.set(p, playerBedsData);

                    Component displayName = MultipleBedSpawn.LEGACY_SERIALIZER.deserialize(message);
                    p.sendMessage(Component.text("Bed successfully renamed to: ", NamedTextColor.GREEN).append(displayName));
                }
                openManageMenu(p);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        renamingPlayers.remove(event.getPlayer().getUniqueId());
    }
}