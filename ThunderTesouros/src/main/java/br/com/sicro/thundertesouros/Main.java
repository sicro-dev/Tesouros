package br.com.sicro.thundertesouros;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin implements CommandExecutor, Listener {

    private FileConfiguration treasuresConfig;
    private FileConfiguration sairConfig;
    private File treasuresFile;
    private File sairFile;
    private Map<String, Boolean> playerTreasureMode = new HashMap<>();
    private Map<String, Location> playerSairLoc = new HashMap<>();
    private final List<Material> allowedBlocks = Arrays.asList(Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE);
    private Map<Location, Material> brokenBlocks = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("tesourosetloc").setExecutor(this);
        getCommand("tesouro").setExecutor(this);
        getCommand("sair").setExecutor(this);
        getCommand("tesourosetsair").setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(new BlockBreakListener(), this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        startBlockRegenerator();

        treasuresFile = new File(getDataFolder(), "tesouro.yml");
        if (!treasuresFile.exists()) {
            treasuresFile.getParentFile().mkdirs();
            saveResource("tesouro.yml", false);
        }
        treasuresConfig = YamlConfiguration.loadConfiguration(treasuresFile);

        sairFile = new File(getDataFolder(), "sair.yml");
        if (!sairFile.exists()) {
            sairFile.getParentFile().mkdirs();
            saveResource("sair.yml", false);
        }
        sairConfig = YamlConfiguration.loadConfiguration(sairFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("sair")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores!");
                return true;
            }

            Player player = (Player) sender;

            if (!playerTreasureMode.containsKey(player.getName()) || !playerTreasureMode.get(player.getName())) {
                return true;
            }

            if (!playerSairLoc.containsKey(player.getName())) {
                if (sairConfig.contains("sair.location")) {
                    playerSairLoc.put(player.getName(), deserializeLocation(sairConfig.getString("sair.location")));
                } else {
                    player.sendMessage(ChatColor.RED + "A localização de saída ainda não foi definida!");
                    return true;
                }
            }

            Location sairLoc = playerSairLoc.get(player.getName());
            player.teleport(sairLoc);
            player.sendMessage("§cRetornando ao spawn..");
            playerTreasureMode.put(player.getName(), false);
            playerSairLoc.remove(player.getName());
            return true;

        } else if (cmd.getName().equalsIgnoreCase("tesourosetloc")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("thundertesouros.setloc")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para definir a localização do tesouro!");
                return true;
            }

            Location playerLoc = player.getLocation();
            treasuresConfig.set("treasure.location", serializeLocation(playerLoc));
            try {
                treasuresConfig.save(treasuresFile);
                player.sendMessage(ChatColor.GREEN + "Localização do tesouro definida com sucesso!");
            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Ocorreu um erro ao salvar a localização do tesouro.");
                e.printStackTrace();
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("tesouro")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores!");
                return true;
            }

            Player player = (Player) sender;

            if (!treasuresConfig.contains("treasure.location")) {
                player.sendMessage(ChatColor.RED + "A localização do tesouro ainda não foi definida!");
                return true;
            }

            Location treasureLoc = deserializeLocation(treasuresConfig.getString("treasure.location"));
            player.teleport(treasureLoc);
            player.sendMessage("§e§lWOW! §eVocê entrou no mundo de tesouros com sucesso!");
            playerTreasureMode.put(player.getName(), true);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("tesourosetsair")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores!");
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("thundertesouros.setloc")) {
                player.sendMessage(ChatColor.RED + "Você não tem permissão para definir a localização de saída!");
                return true;
            }

            Location playerLoc = player.getLocation();
            playerSairLoc.put(player.getName(), playerLoc);
            player.sendMessage(ChatColor.GREEN + "Localização de saída definida com sucesso!");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (playerTreasureMode.containsKey(player.getName()) && playerTreasureMode.get(player.getName())) {
            if (!allowedBlocks.contains(event.getBlock().getType())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (playerTreasureMode.containsKey(player.getName()) && playerTreasureMode.get(player.getName())) {
            String message = event.getMessage().toLowerCase();
            String[] allowedCommands = {"/tesourosetloc", "/luz", "/lanterna", "/vanish", "/c", "/.", "/l", "/megafone", "/bolao", "/quiz", "/fastclick", "/r", "/a", "/g", "/tell", "/picareta", "/sc", "/darkit", "/sair", "/tesourosetsair"};

            boolean allowed = false;
            for (String cmd : allowedCommands) {
                if (message.equals(cmd.toLowerCase()) || message.startsWith(cmd.toLowerCase() + " ")) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                player.sendMessage("§cNão é possível utilizar esse comando nesse local, use /sair!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerTreasureMode.put(player.getName(), false);
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String s) {
        String[] parts = s.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private class BlockBreakListener implements org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler
        public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
            Block block = event.getBlock();
            if (block.getType().isSolid()) {
                brokenBlocks.put(block.getLocation(), block.getType());
            }
        }
    }

    private void startBlockRegenerator() {
        new BukkitRunnable() {
            @Override
            public void run() {
                regenerateBlocks();
            }
        }.runTaskTimer(this, 2400L, 2400L);
    }

    private void regenerateBlocks() {
        for (Map.Entry<Location, Material> entry : brokenBlocks.entrySet()) {
            Location location = entry.getKey();
            Material material = entry.getValue();
            if (location.getBlock().getType() == Material.AIR) {
                location.getBlock().setType(material);
            }
        }
    }
}
