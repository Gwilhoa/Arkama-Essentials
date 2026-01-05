package fr.gchatain.arkama.essentials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ArkaEssentials extends JavaPlugin {

    public static final String PREFIX = "§eArkama-§cEssentials >> §r";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String PLAYER_FILE = "Arkama/seen_player.json";

    private static final int RELOAD_COUNTDOWN_SECONDS = 5;

    public static Map<String, Date> lastSeenMap = new HashMap<>();
    public static ArkaEssentials instance;

    public static boolean explosionsEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        loadLastSeenPlayers();
        getServer().getPluginManager().registerEvents(new EssentialEvent(), this);
        getLogger().info("ArkaEssentials activé !");
    }

    @Override
    public void onDisable() {
        saveLastSeenPlayers();
        getLogger().info("ArkaEssentials désactivé !");
    }


    private void loadLastSeenPlayers() {
        File file = new File(PLAYER_FILE);

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                saveLastSeenPlayers();
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<HashMap<String, Date>>() {}.getType();
                lastSeenMap = GSON.fromJson(reader, type);

                if (lastSeenMap == null) lastSeenMap = new HashMap<>();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveLastSeenPlayers() {
        File file = new File(PLAYER_FILE);

        try {
            file.getParentFile().mkdirs();

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(lastSeenMap, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cCette commande ne peut être exécutée que par un joueur.");
            return true;
        }

        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {

            case "hat":         return cmdHat(player);
            case "invsee":      return cmdInvSee(player, args);
            case "ec":
            case "enderchest":  return cmdEnderChest(player, args);
            case "seen":        return cmdSeen(player, args);
            case "near":        return cmdNear(player, args);
            case "skull":       return cmdSkull(player, args);
            case "explode":     return cmdExplode(player);
            case "rl":  cmdReloadCountdown(); return true;

            default:
                player.sendMessage(PREFIX + "§cCommande inconnue.");
                return true;
        }
    }


    // ┌───────────────────────────────────────────────┐
    // │                 COMMAND HANDLERS               │
    // └───────────────────────────────────────────────┘

    private boolean cmdHat(Player p) {
        if (p.getInventory().getItemInMainHand().getType().isAir()) {
            p.sendMessage(PREFIX + "§cTu ne tiens rien en main !");
            return true;
        }

        p.getInventory().setHelmet(p.getInventory().getItemInMainHand());
        p.getInventory().setItemInMainHand(null);
        p.sendMessage(PREFIX + "§aTu portes maintenant ton objet en tant que chapeau !");
        return true;
    }

    private boolean cmdInvSee(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(PREFIX + "§cUtilisation : /invsee <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target != null) {
            p.openInventory(target.getInventory());
        } else {
            p.sendMessage(PREFIX + "§cCe joueur n'est pas en ligne.");
        }
        return true;
    }

    private boolean cmdEnderChest(Player p, String[] args) {
        Player target = p;

        if (args.length == 1)
            target = Bukkit.getPlayerExact(args[0]);

        if (target != null) {
            p.openInventory(target.getEnderChest());
        } else {
            p.sendMessage(PREFIX + "§cCe joueur n'est pas en ligne.");
        }
        return true;
    }

    private boolean cmdSeen(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(PREFIX + "§cUtilisation : /seen <joueur>");
            return true;
        }

        String target = args[0];

        if (Bukkit.getPlayer(target) != null) {
            p.sendMessage(PREFIX + "§a" + target + " est actuellement en ligne.");
            return true;
        }

        Date last = lastSeenMap.get(target);

        if (last == null) {
            p.sendMessage(PREFIX + "§cAucune information trouvée pour ce joueur.");
            return true;
        }

        long diff = System.currentTimeMillis() - last.getTime();

        long days = diff / 86400000;
        long hours = diff / 3600000 % 24;
        long minutes = diff / 60000 % 60;
        long seconds = diff / 1000 % 60;

        p.sendMessage(PREFIX + "§a" + target + " s’est déconnecté il y a "
                + (days > 0 ? days + "j " : "")
                + (hours > 0 ? hours + "h " : "")
                + (minutes > 0 ? minutes + "min " : "")
                + seconds + "s.");

        return true;
    }

    private boolean cmdNear(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(PREFIX + "§cUtilisation : /near <distance>");
            return true;
        }

        int radius;

        try {
            radius = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            p.sendMessage(PREFIX + "§cLa distance doit être un nombre entier.");
            return true;
        }

        boolean found = false;

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(p) || !target.getWorld().equals(p.getWorld()))
                continue;

            double dist = p.getLocation().distance(target.getLocation());

            if (dist <= radius) {
                found = true;
                p.sendMessage("§e" + target.getName() + " §7est à §a"
                        + String.format("%.1f", dist) + " blocs");
            }
        }

        if (!found)
            p.sendMessage(PREFIX + "§7Aucun joueur trouvé dans un rayon de §c" + radius + " blocs.");

        return true;
    }

    private boolean cmdSkull(Player p, String[] args) {
        if (args.length != 1) {
            p.sendMessage(PREFIX + "§cUtilisation : /skull <pseudo>");
            return true;
        }

        String name = args[0];
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name));
        meta.setDisplayName("§eTête de §6" + name);
        skull.setItemMeta(meta);

        p.getInventory().addItem(skull);
        p.sendMessage(PREFIX + "§aTu as reçu la tête de §e" + name + "§a !");
        return true;
    }

    private boolean cmdExplode(Player p) {
        if (!p.getName().equalsIgnoreCase("Gwilhoa"))
            return false;

        explosionsEnabled = !explosionsEnabled;

        if (explosionsEnabled)
            broadcast("§4§lALERTE §cLes explosions sont activées sur Ville1");
        else
            broadcast("§aSécurité remise en place, bonne journée");

        return true;
    }

    private void cmdReloadCountdown() {
        broadcast(PREFIX + "reload dans");

        for (int i = 0; i <= RELOAD_COUNTDOWN_SECONDS; i++) {
            int delay = i * 20;
            int remaining = RELOAD_COUNTDOWN_SECONDS - i;

            Bukkit.getScheduler().runTaskLater(instance, () -> {
                if (remaining > 0)
                    broadcast("§a" + remaining);
                else {
                    Bukkit.reload();
                    broadcast("§aLa mise à jour a été réalisée avec succès");
                }
            }, delay);
        }
    }


    public static void broadcast(String msg) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
        }
    }

    public static class EssentialEvent implements Listener {

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            lastSeenMap.put(e.getPlayer().getName(), new Date());
            saveLastSeenPlayers();
        }

        @EventHandler
        public void onExplode(EntityExplodeEvent e) {
            if (e.getEntity().getWorld().getName().equals("Ville1") && !explosionsEnabled) {
                e.setCancelled(true);
            }
        }
    }
}
