package me.twix.monitoreds;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class Monitoreds extends JavaPlugin {

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(new playerJoinListener(), this);
    }

    public class playerJoinListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player p = event.getPlayer();
            UUID playerID = p.getUniqueId();

            File playerDataFolder = new File(getDataFolder(), "playerData");
            File playerFile = new File(playerDataFolder, playerID + ".yml");

            if (!playerFile.exists()) {
                playerDataFolder.mkdirs();
                try {
                    playerFile.createNewFile();
                    YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    playerConfig.set("balance", 0);
                    playerConfig.isSet("balance");
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
