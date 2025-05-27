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
                    p.sendMessage(this.getConfig().getString("Messages.MedicArrived"));
                    medicsOnCall.remove(p);
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


    /// Hospital location check
    Float x = (float) this.getConfig().getDouble("Hospital.BlockX");
    Float y = (float) this.getConfig().getDouble("Hospital.BlockY");
    Float z = (float) this.getConfig().getDouble("Hospital.BlockZ");
    Float yaw = (float) this.getConfig().getDouble("Hospital.Yaw");
    Float pitch = (float) this.getConfig().getDouble("Hospital.Pitch");
    String world = this.getConfig().getString("Hospital.World");

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
            this.task = server.getScheduler().runTaskTimer(this, () -> {
                if (p.getHealth() >= 20.0) {
                    if (this.task != null) {
                        this.task.cancel();
                    }
                } else {
                    p.setHealth(p.getHealth() + 1.0);
                }

            }, (long)seconds * 20L, (long)period * 20L);
            Location respawn = new Location(this.getServer().getWorld(this.world), (double)this.x, (double)this.y, (double)this.z, this.yaw, this.pitch);
            event.setRespawnLocation(respawn);
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
    }
    public static Hospital getInstance(){
        return instance;
    }
    public static Economy getEconomy() {
        return econ;
    }


}
