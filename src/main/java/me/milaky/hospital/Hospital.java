package me.milaky.hospital;

import me.milaky.hospital.Commands.HospitalCommand;
import me.milaky.hospital.tab.TabCompliter;
import net.milkbowl.vault.economy.Economy;



import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;



public final class Hospital extends JavaPlugin implements Listener {
    public static Hospital instance;
    public static Economy econ = null;
    BukkitTask task = null;

    private String latestVersion;
    public HashSet<Player> medicsOnCall = new HashSet<>();
    public Map<UUID, ItemStack> compasses = new HashMap<>();

    public Map<UUID, BukkitTask> healingTasks = new HashMap<>();
    public Map<UUID, BukkitTask> knockdownTasks = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!setupEconomy()) {
            getServer().getLogger().info(String.format("[%s] - Disable plugin because economy not found", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginCommand("hospital").setExecutor(new HospitalCommand(this));
        getServer().getPluginCommand("hospital").setTabCompleter(new TabCompliter());
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new me.milaky.hospital.Listeners.KnockdownListener(this), this);
        getServer().getPluginManager().registerEvents(new me.milaky.hospital.Listeners.MedicInteractListener(this), this);

        saveDefaultConfig();
        loadLatestVersion();

        Metrics metrics = new Metrics(this, 20443);
        if (metrics.isEnabled())
        {
            getLogger().info("Enabled metrics. You may opt-out by changing plugins/bStats/config.yml");
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (compasses.containsKey(p.getUniqueId())) {
            ItemStack compass = compasses.get(p.getUniqueId());
            if (compass != null && compass.getType() == Material.COMPASS) {
                Location playerLocation = p.getLocation();
                Location compassTarget = ((CompassMeta) compass.getItemMeta()).getLodestone();
                if (compassTarget != null && playerLocation.distance(compassTarget) < 2) {
                    DeleteItem(p, compass);
                    compasses.remove(p.getUniqueId());
                    p.sendMessage(this.getConfig().getString("Messages.MedicArrived", "§fYou have arrived on a call.").replace("&", "§"));
                    medicsOnCall.remove(p);
                }
            }
        }

        // If medic brings player to hospital
        if (p.hasPermission("Hospital.Doctor") && !p.getPassengers().isEmpty()) {
            Location loc = p.getLocation();
            Location hospitalLoc = getHospitalLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(hospitalLoc.getWorld()) && loc.distance(hospitalLoc) < 5) {
                for (org.bukkit.entity.Entity passenger : p.getPassengers()) {
                    if (passenger instanceof Player) {
                        Player knocked = (Player) passenger;
                        if (isKnockedDown(knocked)) {
                            p.removePassenger(knocked);
                            sendToHospital(knocked);
                            removeKnockdown(knocked);
                        }
                    }
                }
            }
        }
    }



    public void DeleteItem(Player p, ItemStack compass) {
        ItemStack[] inventoryContents = p.getInventory().getContents();
        for (int i = 0; i < inventoryContents.length; i++) {
            ItemStack item = inventoryContents[i];
            if (item != null && item.equals(compass)) {
                p.getInventory().remove(item);
                p.updateInventory();
                break;
            }
        }
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }


    public Location getHospitalLocation() {
        Float x = (float) this.getConfig().getDouble("Hospital.BlockX");
        Float y = (float) this.getConfig().getDouble("Hospital.BlockY");
        Float z = (float) this.getConfig().getDouble("Hospital.BlockZ");
        Float yaw = (float) this.getConfig().getDouble("Hospital.Yaw");
        Float pitch = (float) this.getConfig().getDouble("Hospital.Pitch");
        String world = this.getConfig().getString("Hospital.World");
        return new Location(this.getServer().getWorld(world), (double) x, (double) y, (double) z, yaw, pitch);
    }

    public boolean isKnockedDown(Player player) {
        return knockdownTasks.containsKey(player.getUniqueId());
    }

    public void knockdownPlayer(Player player) {
        if (isKnockedDown(player)) return;
        player.sendMessage(this.getConfig().getString("Messages.KnockedDown", "§cYou are knocked down! Wait for a medic or you will be sent to the hospital."));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 20 * 120, 1, false, false));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW, 20 * 120, 255, false, false));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP, 20 * 120, 128, false, false));

        // Sleep pose trick
        // Poses are read-only on many older versions, so we skip setting it directly

        // Schedule auto-teleport
        int delaySeconds = this.getConfig().getInt("Settings.KnockdownTime", 120);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    sendToHospital(player);
                }
                removeKnockdown(player);
            }
        }.runTaskLater(this, delaySeconds * 20L);
        knockdownTasks.put(player.getUniqueId(), task);

        // Notify medics
        Bukkit.broadcast(this.getConfig().getString("Messages.PlayerKnockedDown", "§cPlayer %player% is knocked down!").replace("%player%", player.getName()), "Hospital.Doctor");
    }

    public void removeKnockdown(Player player) {
        BukkitTask task = knockdownTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        if (player.isOnline()) {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP);
            // if player is passenger, eject them
            if (player.getVehicle() != null) {
                player.getVehicle().removePassenger(player);
            }
        }
    }

    public void sendToHospital(Player p) {
        p.teleport(getHospitalLocation());
        p.setHealth(1.0);
        startHealing(p);
    }

    public void startHealing(Player p) {
        if (healingTasks.containsKey(p.getUniqueId())) {
            return;
        }

        int period = this.getConfig().getInt("Settings.HealthBoostSeconds", 3);
        BukkitTask task = Bukkit.getServer().getScheduler().runTaskTimer(this, () -> {
            if (!p.isOnline() || p.getHealth() >= 20.0) {
                BukkitTask t = healingTasks.remove(p.getUniqueId());
                if (t != null) {
                    t.cancel();
                }
            } else {
                double newHealth = p.getHealth() + 1.0;
                p.setHealth(Math.min(newHealth, 20.0));
            }
        }, 20L, period * 20L);

        healingTasks.put(p.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (this.getConfig().getBoolean("Settings.RespawnInHospital")) {
            Player p = event.getPlayer();
            Server server = Bukkit.getServer();
            new BukkitRunnable(){
                @Override
                public void run() {
                    p.setHealth(1);
                }
            }.runTaskLater(this, 10L);
            int seconds = 2;
            int period = this.getConfig().getInt("Settings.HealthBoostSeconds");
            Location respawn = getHospitalLocation();
            event.setRespawnLocation(respawn);
            startHealing(p);
        }

    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e){
        if(this.getConfig().getBoolean("Settings.UpdateNotify")){
            Player p = e.getPlayer();
            if (p.isOp() && latestVersion != null) {
                // Получаем версию установленного плагина
                String currentVersion = getDescription().getVersion();

                // Сравниваем версии
                if (!latestVersion.equals(currentVersion)) {
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            p.sendMessage("§b§l[mHospital] a new update is available");
                            p.sendMessage("§b§lDownload update: https://www.spigotmc.org/resources/mhospital.109651/");
                        }
                    }.runTaskLater(this, 10L);
                }
            }
        }
    }

    private void loadLatestVersion(){
        // GET-запрос к API Spigot для получения информации о плагине
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=109651");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            latestVersion = reader.readLine();

        } catch (Exception e) {
            getLogger().warning("Error");
            latestVersion = null;
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (BukkitTask task : healingTasks.values()) {
            task.cancel();
        }
        healingTasks.clear();
        for (BukkitTask task : knockdownTasks.values()) {
            task.cancel();
        }
        knockdownTasks.clear();
    }
    public static Hospital getInstance(){
        return instance;
    }
    public static Economy getEconomy() {
        return econ;
    }


}
