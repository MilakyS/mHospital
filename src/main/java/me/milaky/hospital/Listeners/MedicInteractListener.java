package me.milaky.hospital.Listeners;

import me.milaky.hospital.Hospital;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class MedicInteractListener implements Listener {
    private final Hospital plugin;

    public MedicInteractListener(Hospital plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player medic = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player)) return;

        Player target = (Player) event.getRightClicked();

        if (plugin.isKnockedDown(target) && medic.hasPermission("Hospital.Doctor")) {
            if (plugin.isKnockedDown(medic)) {
                medic.sendMessage("§cYou cannot do this while knocked down!");
                return;
            }
            // If medic is sneaking, they carry the player
            if (medic.isSneaking()) {
                if (medic.getPassengers().isEmpty()) {
                    medic.addPassenger(target);
                    target.sendMessage(plugin.getConfig().getString("Messages.PickedUp", "§fA medic picked you up.").replace("&", "§"));
                } else if (medic.getPassengers().contains(target)) {
                    medic.removePassenger(target);
                    target.sendMessage(plugin.getConfig().getString("Messages.DroppedOff", "§fThe medic dropped you.").replace("&", "§"));
                }
            } else {
                // If not sneaking, they revive the player
                plugin.removeKnockdown(target);
                target.setHealth(20.0);
                target.sendMessage(plugin.getConfig().getString("Messages.Revived", "§aYou have been revived by a medic!").replace("&", "§"));
                medic.sendMessage(plugin.getConfig().getString("Messages.MedicRevived", "§aYou revived %player%.").replace("%player%", target.getName()).replace("&", "§"));
            }
        }
    }
}
