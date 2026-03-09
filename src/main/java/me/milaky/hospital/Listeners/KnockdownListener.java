package me.milaky.hospital.Listeners;

import me.milaky.hospital.Hospital;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Pose;

public class KnockdownListener implements Listener {
    private final Hospital plugin;

    public KnockdownListener(Hospital plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.getHealth() - event.getFinalDamage() <= 0.0) {
            if (plugin.isKnockedDown(player)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            player.setHealth(1.0);
            plugin.knockdownPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.isKnockedDown(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() < to.getY()) {
                player.teleport(from);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        plugin.removeKnockdown(p);
        me.milaky.hospital.Commands.HospitalCommand cmd = (me.milaky.hospital.Commands.HospitalCommand) plugin.getServer().getPluginCommand("hospital").getExecutor();
        if (cmd != null) {
            cmd.hospitalList.remove(p);
            cmd.hospitalCall.remove(p);
            cmd.locationMap.remove(p);
        }
        plugin.medicsOnCall.remove(p);
        plugin.compasses.remove(p.getUniqueId());

        org.bukkit.scheduler.BukkitTask task = plugin.healingTasks.remove(p.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
}
