package me.milaky.hospital.Commands;

import me.milaky.hospital.Hospital;
import net.md_5.bungee.api.ChatMessageType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;


public class HospitalCommand implements CommandExecutor {
    HashMap<Player, Player> hospitalList = new HashMap<Player, Player>();

    HashSet<Player> hospitalCall = new HashSet<Player>();
    HashMap<Player, Location> locationMap = new HashMap<>();
    BukkitTask commandTask = null;
    public Hospital plugin;




    public HospitalCommand(Hospital plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command,  String label, String[] args) {
        if(sender instanceof Player) {


            Player player = (Player) sender;
            Economy economy = Hospital.getEconomy();
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (!player.hasPermission("Hospital.Reload")) {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm").replace("&", "§"));
                    } else {
                        this.plugin.reloadConfig();
                        player.sendMessage(this.plugin.getConfig().getString("Messages.ConfigReloaded").replace("&", "§"));
                    }
                }
                if (args[0].equalsIgnoreCase("set")) {
                    if (player.hasPermission("Hospital.Set")) {
                        int x = player.getLocation().getBlockX();
                        int y = player.getLocation().getBlockY();
                        int z = player.getLocation().getBlockZ();
                        float pitch = player.getLocation().getPitch();
                        float yaw = player.getLocation().getYaw();
                        String world = player.getLocation().getWorld().getName();
                        this.plugin.getConfig().set("Hospital.BlockX", x);
                        this.plugin.getConfig().set("Hospital.BlockY", y);
                        this.plugin.getConfig().set("Hospital.BlockZ", z);
                        this.plugin.getConfig().set("Hospital.Yaw", yaw);
                        this.plugin.getConfig().set("Hospital.Pitch", pitch);
                        this.plugin.getConfig().set("Hospital.World", world);
                        this.plugin.saveConfig();
                        player.sendMessage(this.plugin.getConfig().getString("Messages.SetHospital").replace("&", "§"));
                    } else {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm"));
                    }
                }
                if (args[0].equalsIgnoreCase("accept")) {
                    if (hospitalList.containsKey(player)) {
                        double amount = this.plugin.getConfig().getDouble("Settings.cost");
                        if (economy.getBalance(player) < amount) {
                            player.sendMessage(this.plugin.getConfig().getString("Messages.EnoughMoney").replace("&", "§"));
                            Player medic = hospitalList.get(player);
                            hospitalList.remove(player, medic);
                        }
                        else {
                            Player medic = hospitalList.get(player);
                            economy.withdrawPlayer(player, amount);
                            economy.depositPlayer(medic, amount);
                            player.setHealth(20);
                            player.sendMessage(this.plugin.getConfig().getString("Messages.Accept").replace("&", "§").replace("%amount%", String.valueOf(amount)));
                            medic.sendMessage(this.plugin.getConfig().getString("Messages.AcceptMedic").replace("&", "§").replace("%player%", player.getName()).replace("%amount%", String.valueOf(amount)));
                            hospitalList.remove(player, medic);
                        }
                    }
                    else {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.HaventRequest").replace("&", "§"));
                    }
                }
                if (args[0].equalsIgnoreCase("myself")) {
                    double amount = this.plugin.getConfig().getDouble("Settings.myselfCost");
                    if (this.plugin.getConfig().getBoolean("Settings.PlayerHealMyself")) {
                        if (player.hasPermission("Hospital.Myself")) {
                            if (player.getHealth() <= 16) {
                                if (economy.getBalance(player) < amount) {
                                    player.sendMessage(this.plugin.getConfig().getString("Messages.EnoughMoney").replace("&", "§"));
                                }
                                else {
                                    economy.withdrawPlayer(player, amount);
                                    player.setHealth(20);
                                    player.sendMessage(this.plugin.getConfig().getString("Messages.YouHealMyself").replace("&", "§").replace("%amount%", String.valueOf(amount)));
                                }
                            } else {
                                player.sendMessage(this.plugin.getConfig().getString("Messages.YouHealthy").replace("&", "§"));
                            }
                        } else {
                            player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm").replace("&", "§"));
                        }

                    } else {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.MyselfDisabled").replace("&", "§"));
                    }
                }
                if (args[0].equalsIgnoreCase("decline")) {
                    if (hospitalList.containsKey(player)) {
                        Player medic = hospitalList.get(player);
                        player.sendMessage(this.plugin.getConfig().getString("Messages.Decline").replace("&", "§").replace("%medic%", medic.getName()));
                        medic.sendMessage(this.plugin.getConfig().getString("Messages.DeclineMedic").replace("&", "§").replace("%player%", player.getName()));
                        hospitalList.remove(player, medic);
                    } else {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.HaventRequest").replace("&", "§"));
                    }
                }
                if(args[0].equalsIgnoreCase("radio")){
                    if(this.plugin.getConfig().getBoolean("Settings.RadioEnabled")) {
                        if (args.length < 2) {
                            player.sendMessage(this.plugin.getConfig().getString("Messages.SmallArgs").replace("&", "§"));
                        }
                        else {
                            if (!player.hasPermission("Hospital.Doctor")) {
                                player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm").replace("&", "§"));
                            } else {
                                StringBuilder message = new StringBuilder();

                                for (int i = 1; i < args.length; ++i) {
                                    message.append(args[i]).append(" ");
                                }
                                Bukkit.broadcast(this.plugin.getConfig().getString("Messages.Radio").replace("%player%", player.getName()).replace("&", "§").replace("%message%",message), "Hospital.Doctor");
                            }
                        }
                    }
                    else{
                        player.sendMessage(this.plugin.getConfig().getString("Messages.RadioDisabled").replace("&", "§"));
                    }
                }
                if (args[0].equalsIgnoreCase("heal")) {
                    if (args.length < 2) {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.SmallArgs").replace("&", "§"));
                    } else {
                        if (args.length > 2) {
                            player.sendMessage(this.plugin.getConfig().getString("Messages.ManyArgs").replace("&", "§"));
                        } else {
                            Player target = Bukkit.getPlayer(args[1]);
                            if (target == player) {
                                player.sendMessage(this.plugin.getConfig().getString("Messages.UseYourNick").replace("&", "§"));
                            } else {
                                if (!player.hasPermission("Hospital.Doctor")) {
                                    player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm").replace("&", "§"));
                                } else {
                                    if (target == null || !target.isOnline()) {
                                        player.sendMessage(this.plugin.getConfig().getString("Messages.nullTarget").replace("&", "§"));
                                    }
                                    else {
                                        if (target.getHealth() >= 16) {
                                            player.sendMessage(this.plugin.getConfig().getString("Messages.TargetHealthy").replace("&", "§"));
                                        }
                                        else
                                        {
                                            player.sendMessage(this.plugin.getConfig().getString("Messages.MedicSendRequest").replace("&", "§").replace("%target%", target.getName()));
                                            target.sendMessage(this.plugin.getConfig().getString("Messages.YouHaveRequest").replace("&", "§").replace("%medic%", player.getName()));
                                            hospitalList.put(target, player);


                                            Server server = Bukkit.getServer();
                                            int period = 1;
                                            AtomicInteger cooldown = new AtomicInteger(this.plugin.getConfig().getInt("Settings.RequestDuration"));
                                            commandTask = server.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
                                                    int duration;
                                                    if(hospitalList.containsKey(target)) {
                                                        duration = cooldown.addAndGet(-1);
                                                        if (duration <= 0) {
                                                            hospitalList.remove(target);
                                                            commandTask.cancel();
                                                        }
                                                    }
                                                    else{
                                                        commandTask.cancel();
                                                    }
                                            }, 1, period * 20L);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(args[0].equalsIgnoreCase("call")){
                    if (args.length < 2) {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.SmallArgs").replace("&", "§"));
                    }
                    else {
                        if(hospitalCall.contains(player)){
                            player.sendMessage(this.plugin.getConfig().getString("Messages.AlreadylCall").replace("&", "§"));
                        }
                        else {
                            StringBuilder message = new StringBuilder();

                            for (int i = 1; i < args.length; ++i) {
                                message.append(args[i]).append(" ");
                            }

                            player.sendMessage(this.plugin.getConfig().getString("Messages.PlayerCall").replace("&", "§").replace("%Reason%", message));
                            Bukkit.broadcast(this.plugin.getConfig().getString("Messages.ToMedicsCall").replace("&", "§").replace("%nick%", player.getName()).replace("%Reason%", message), "Hospital.Doctor");
                            hospitalCall.add(player);
                            locationMap.put(player, player.getLocation());
                        }
                    }
                }
                if(args[0].equalsIgnoreCase("callAccept")){
                    if (!player.hasPermission("Hospital.Doctor")) {
                        player.sendMessage(this.plugin.getConfig().getString("Messages.noPerm").replace("&", "§"));
                    }
                    else {
                        if (args.length < 2) {
                            player.sendMessage(this.plugin.getConfig().getString("Messages.SmallArgs").replace("&", "§"));
                        }
                        else {
                            if (args.length > 2) {
                                player.sendMessage(this.plugin.getConfig().getString("Messages.ManyArgs").replace("&", "§"));
                            }
                            else{
                                Player target = Bukkit.getPlayer(args[1]);
                                if(!this.plugin.medicsOnCall.contains(player)) {
                                    if (hospitalCall.contains(target)) {
                                        target.sendMessage(this.plugin.getConfig().getString("Messages.CallAcceptToPlayer").replace("&", "§").replace("%nick%", player.getName()));
                                        player.sendMessage(this.plugin.getConfig().getString("Messages.CallAcceptToMedic").replace("&", "§"));
                                        hospitalCall.remove(target);

                                        Location targetPos = locationMap.get(target);

                                        ItemStack compass = new ItemStack(Material.COMPASS);
                                        CompassMeta meta = (CompassMeta) compass.getItemMeta();
                                        meta.setLodestone(targetPos);
                                        meta.setLodestoneTracked(false);
                                        meta.setDisplayName("GPS");
                                        compass.setItemMeta(meta);
                                        player.getInventory().addItem(compass);
                                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(this.plugin.getConfig().getString("Messages.ReceivedCompass").replace("&", "§")));
                                        this.plugin.compasses.put(player.getUniqueId(), compass);
                                        this.plugin.medicsOnCall.add(player);
                                        locationMap.remove(target);

                                    } else {
                                        player.sendMessage(this.plugin.getConfig().getString("Messages.NoRequest").replace("&", "§"));
                                    }
                                }
                                else{
                                    player.sendMessage(this.plugin.getConfig().getString("Messages.FullRequests").replace("&", "§"));
                                }
                            }
                        }
                    }
                }
            } else {
                player.sendMessage(this.plugin.getConfig().getString("Messages.help").replace("&", "§"));
            }
        }
        else{

        }
        return true;
    }
}
