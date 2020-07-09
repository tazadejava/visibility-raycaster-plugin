package me.tazadejava.raytracer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

//handles the player's commands for raycasting
public class CommandHandler implements CommandExecutor, TabCompleter {

    private JavaPlugin plugin;

    private HashMap<Location, BlockState> lastBlockState = new HashMap<>();
    private HashMap<Location, Player> blockPlayer = new HashMap<>();

    public CommandHandler(RayTracerPlugin plugin) {
        this.plugin = plugin;
    }

    public void restoreBlocks(CommandSender sender) {
        if(!lastBlockState.isEmpty()) {
            for(Location loc : lastBlockState.keySet()) {
                Block block = loc.getBlock();
                BlockState state = lastBlockState.get(loc);

                blockPlayer.get(block.getLocation()).sendBlockChange(block.getLocation(), state.getBlockData());
            }
            if(sender != null) {
                sender.sendMessage("Restored " + lastBlockState.size() + " blocks.");
            }

            lastBlockState.clear();
            blockPlayer.clear();
        }
    }

    //save currently visible glass blocks to json file within the plugins/VisibilityRaytracerPlugin/raycasts folder
    public void saveBlocks(CommandSender sender) {
        if(!lastBlockState.isEmpty()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            JsonObject mainObjectLocs = new JsonObject();

            JsonObject blockMapping = new JsonObject();

            JsonArray visibleBlocks = new JsonArray();
            int index = 0;
            for(Location loc : lastBlockState.keySet()) {
                Block block = loc.getBlock();
                BlockState state = lastBlockState.get(loc);

                blockPlayer.get(block.getLocation()).sendBlockChange(block.getLocation(), state.getBlockData());

                String locString = block.getLocation().getBlockX() + " " + block.getLocation().getBlockY() + " " + block.getLocation().getBlockZ();
                visibleBlocks.add(locString);

                if(index == 0) {
                    blockMapping.addProperty("world", loc.getWorld().getName());
                }

                blockMapping.addProperty(locString, index);
                index++;
            }

            mainObjectLocs.add("visibleBlocks", visibleBlocks);

            try {
                File saveFolder = new File(plugin.getDataFolder() + "/raycasts/");
                if(!saveFolder.exists()){
                    saveFolder.mkdirs();
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(":", "-");

                File saveFile = new File(plugin.getDataFolder() + "/raycasts/" + timestamp + ".json");
                if(!saveFile.exists()) {
                    saveFile.createNewFile();
                }

                FileWriter fileWriter = new FileWriter(saveFile);
                gson.toJson(mainObjectLocs, fileWriter);
                fileWriter.close();

                sender.sendMessage("Saved " + lastBlockState.size() + " blocks!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //this method will interact with the PreciseVisibleBlocksRaycaster class to obtain a list of blocks visible to the player
    // and paint them into GLASS blocks
    private void startRaycast(CommandSender commandSender, boolean restoreAfterEach, boolean loopRaycast) {
        Player player = (Player) commandSender;

        //reset the glass blocks before starting raycasting again
        restoreBlocks(player);
        lastBlockState.clear();
        blockPlayer.clear();

        //get the raycaster algorithm
        PreciseVisibleBlocksRaycaster raycaster = new PreciseVisibleBlocksRaycaster(true, true, true, 0, 255);

        BlockData defaultMaterial = Bukkit.getServer().createBlockData(Material.GLASS);

        //add block materials here to change what is seen for particular blocks; in this case victims are painted not in glass but in magma blocks or glowstone
        HashMap<Material, BlockData> customMaterials = new HashMap<>();

        customMaterials.put(Material.PRISMARINE, Bukkit.getServer().createBlockData(Material.MAGMA_BLOCK));
        customMaterials.put(Material.GOLD_BLOCK, Bukkit.getServer().createBlockData(Material.GLOWSTONE));

        //looper that will update the raycasting algorithm every 4 ticks. there are 20 ticks in a second.
        new BukkitRunnable() {

            int count = 0;
            int sneakCount = 0;

            @Override
            public void run() {
                if (restoreAfterEach) {
                    restoreBlocks(commandSender);
                }

                //get the blocks from the raycasting algorithm that are visible to the player
                Block[] blocks = raycaster.getVisibleBlocks((Player) commandSender);

                for (Block block : blocks) {
                    lastBlockState.put(block.getLocation(), block.getState());
                    blockPlayer.put(block.getLocation(), player);

                    //trick the Minecraft client into seeing glass or a custom block, but don't actually change the world to prevent glitching and destruction
                    if(customMaterials.containsKey(block.getType())) {
                        player.sendBlockChange(block.getLocation(), customMaterials.get(block.getType()));
                    } else {
                        player.sendBlockChange(block.getLocation(), defaultMaterial);
                    }
                }

                count++;

                if (player.isSneaking()) {
                    sneakCount++;
                } else {
                    if (sneakCount != 0) {
                        sneakCount = 0;
                    }
                }

                if (sneakCount >= 6) {
                    if (restoreAfterEach) {
                        restoreBlocks(commandSender);
                        player.sendMessage("Abort raycaster.");
                    } else {
                        player.sendMessage("Abort raycaster. Type in /raycast reset to restore blocks, or perform the test again.");
                    }
                    cancel();
                }

                if(!loopRaycast) {
                    player.sendMessage("Abort raycaster. Type in /raycast reset to restore blocks, or perform the test again.");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 4L);
    }

    //this method handles the command when it is typed in and entered
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if(args.length == 0) {
            commandSender.sendMessage(ChatColor.RED + "Unknown command. Options: /raycast <continuous/discrete/once/save/reset>");
        } else {
            switch(args[0].toLowerCase()) {
                case "cont": //this will draw a continuous raycast to all visible blocks.
                case "continuous":
                    startRaycast(commandSender, false, true);
                    break;
                case "discrete": //this will raycast, but will reset the raycasted before raycasting again. may flicker, watch out.
                    startRaycast(commandSender, true, true);
                    break;
                case "once": //raycast once only
                    startRaycast(commandSender, true, false);
                    break;
                case "save": //saves the current viewed blocks to JSON file
                    saveBlocks(commandSender);
                    break;
                case "reset": //resets the blocks that are converted to glass
                    restoreBlocks(commandSender);
                    break;
                default:
                    commandSender.sendMessage(ChatColor.RED + "Unknown command. Options: /raycast <continuous/discrete/once/save/reset>");
                    break;
            }
        }

        return true;
    }

    //this method will provide autocomplete suggestions when typing in the command for raycast
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], Arrays.asList("continuous", "discrete", "once", "save", "reset"), completions);
            Collections.sort(completions);

            return completions;
        }

        return null;
    }
}
