package me.gabij.multiplebedspawn.listeners;

import me.gabij.multiplebedspawn.MultipleBedSpawn;
import me.gabij.multiplebedspawn.utils.Keys;
import me.gabij.multiplebedspawn.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.gabij.multiplebedspawn.listeners.RespawnMenuHandler.openRespawnMenu;

public class MagicMirrorListener implements Listener {

    private static MultipleBedSpawn plugin;
    private static final Map<UUID, WarpSession> activeWarps = new HashMap<>();
    private static final String GLOW_TEAM_NAME = "MM_BlueGlow";

    public MagicMirrorListener(MultipleBedSpawn plugin) {
        MagicMirrorListener.plugin = plugin;
    }

    private static class WarpSession {
        final List<BukkitTask> tasks = new ArrayList<>();
        BukkitTask particleTask;

        void cancel(Player player) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
            tasks.clear();

            if (particleTask != null) {
                particleTask.cancel();
                particleTask = null;
            }

            disableBlueGlow(player);
            player.removePotionEffect(PotionEffectType.WITHER);
        }
    }

    private static Team getGlowTeam(Scoreboard sb) {
        if (sb == null) return null;
        Team team = sb.getTeam(GLOW_TEAM_NAME);
        if (team == null) {
            try {
                team = sb.registerNewTeam(GLOW_TEAM_NAME);
            } catch (IllegalArgumentException e) {
                team = sb.getTeam(GLOW_TEAM_NAME);
            }
        }
        if (team != null) {
            team.color(NamedTextColor.BLUE);
        }
        return team;
    }

    private static void enableBlueGlow(Player player) {
        try {
            Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
            Team mainTeam = getGlowTeam(mainSb);
            if (mainTeam != null && !mainTeam.hasEntry(player.getName())) {
                mainTeam.addEntry(player.getName());
            }

            Scoreboard playerSb = player.getScoreboard();
            if (playerSb != null && playerSb != mainSb) {
                Team pTeam = getGlowTeam(playerSb);
                if (pTeam != null && !pTeam.hasEntry(player.getName())) {
                    pTeam.addEntry(player.getName());
                }
            }

            player.setGlowing(true);
        } catch (Exception ignored) {}
    }

    private static void disableBlueGlow(Player player) {
        try {
            player.setGlowing(false);

            Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
            Team mainTeam = mainSb.getTeam(GLOW_TEAM_NAME);
            if (mainTeam != null) {
                mainTeam.removeEntry(player.getName());
            }

            Scoreboard playerSb = player.getScoreboard();
            if (playerSb != null && playerSb != mainSb) {
                Team pTeam = playerSb.getTeam(GLOW_TEAM_NAME);
                if (pTeam != null) {
                    pTeam.removeEntry(player.getName());
                }
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (!plugin.getConfig().getBoolean("magic-mirror.enabled", true)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) return;

        String requiredName = plugin.getConfig().getString("magic-mirror.item-name", "");
        if (!requiredName.isEmpty()) {
            if (!item.hasItemMeta()) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;
            String plainTextName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            if (!plainTextName.equalsIgnoreCase(requiredName)) return;
        }

        Player player = event.getPlayer();

        if (!player.hasPermission("multiplebedspawn.magicmirror")) {
            return;
        }

        event.setCancelled(true);

        if (activeWarps.containsKey(player.getUniqueId())) {
            boolean enableMessages = plugin.getConfig().getBoolean("magic-mirror.enable-messages", true);
            if (enableMessages) {
                player.sendActionBar(Component.text("You are already channeling a warp!", NamedTextColor.RED));
            }
            return;
        }

        openRespawnMenu(player);
    }

    public static boolean isWarping(Player player) {
        return activeWarps.containsKey(player.getUniqueId());
    }

    public static void startWarp(Player player, Runnable onComplete) {
        UUID uuid = player.getUniqueId();
        cancelWarp(player, false);

        int windupSeconds = Math.max(0, plugin.getConfig().getInt("magic-mirror.windup", 3));
        boolean enableMessages = plugin.getConfig().getBoolean("magic-mirror.enable-messages", true);
        boolean enableParticles = plugin.getConfig().getBoolean("magic-mirror.enable-particles", true);

        if (windupSeconds <= 0) {
            onComplete.run();
            return;
        }

        WarpSession session = new WarpSession();
        activeWarps.put(uuid, session);

        // Enable blue glowing outline
        enableBlueGlow(player);

        // Apply particle-hidden, icon-hidden WITHER potion effect during windup
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, windupSeconds * 20 + 20, 0, false, false, false));

        // Action Bar countdown colored in AQUA
        if (enableMessages) {
            player.sendActionBar(Component.text("Warping in " + windupSeconds + "...", NamedTextColor.AQUA));
        }
        playAudioEffect(player, 0.5f);

        // Zero-allocation primitive coordinate particle emissions
        if (enableParticles) {
            session.particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) return;
                double x = player.getX();
                double y = player.getY() + 1.0;
                double z = player.getZ();
                World world = player.getWorld();
                world.spawnParticle(Particle.GLOW, x, y, z, 6, 0.3, 0.6, 0.3, 0.05);
                world.spawnParticle(Particle.PORTAL, x, y, z, 8, 0.3, 0.6, 0.3, 0.1);
                world.spawnParticle(Particle.ELECTRIC_SPARK, x, y, z, 4, 0.2, 0.5, 0.2, 0.02);
            }, 0L, 2L);
        }

        for (int i = 1; i < windupSeconds; i++) {
            final int remaining = windupSeconds - i;
            final float pitch = 0.5f + ((float) i / windupSeconds) * 0.5f;

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!activeWarps.containsKey(uuid)) return;
                if (enableMessages) {
                    player.sendActionBar(Component.text("Warping in " + remaining + "...", NamedTextColor.AQUA));
                }
                playAudioEffect(player, pitch);
            }, 20L * i);
            session.tasks.add(task);
        }

        BukkitTask finalTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cancelWarp(player, false);
            if (player.isOnline() && !player.isDead()) {
                onComplete.run();
            }
        }, 20L * windupSeconds);
        session.tasks.add(finalTask);
    }

    private static void playAudioEffect(Player player, float pitch) {
        if (!player.isOnline() || player.getLocation() == null) return;

        boolean enableSounds = plugin.getConfig().getBoolean("magic-mirror.enable-sounds", true);
        float soundVolume = Math.clamp((float) plugin.getConfig().getDouble("magic-mirror.sound-effects-volume", 1.0), 0.0f, 1.0f);

        if (enableSounds) {
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, soundVolume, pitch);
        }
    }

    public static void cancelWarp(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        WarpSession session = activeWarps.remove(uuid);
        if (session != null) {
            session.cancel(player);
            boolean enableMessages = plugin.getConfig().getBoolean("magic-mirror.enable-messages", true);
            if (notify && enableMessages && player.isOnline()) {
                player.sendActionBar(Component.text("Warp cancelled due to taking damage!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Exempt wither damage caused by the channel effect itself
            if (event.getCause() == EntityDamageEvent.DamageCause.WITHER && isWarping(player)) {
                event.setCancelled(true);
                return;
            }

            // If taking external damage while warping or menu is open, cancel and restore player
            if (isWarping(player) || player.getPersistentDataContainer().has(Keys.HAS_PROP, PersistentDataType.BOOLEAN)) {
                cancelWarp(player, true);
                PlayerUtils.undoPropPlayer(player);
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            if (isWarping(player) || player.getPersistentDataContainer().has(Keys.HAS_PROP, PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancelWarp(event.getPlayer(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelWarp(event.getPlayer(), false);
    }
}