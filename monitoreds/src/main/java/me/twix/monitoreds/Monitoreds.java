package me.twix.monitoreds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Monitoreds extends JavaPlugin {

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(new playerJoinListener(), this);
        this.getCommand("sell").setExecutor(new SellCommand());
        this.getCommand("bal").setExecutor(new BalanceCommand());
        this.getCommand("addbal").setExecutor(new BalanceGive());
        this.getCommand("delbal").setExecutor(new BalanceTake());
        this.getCommand("withdraw").setExecutor(new WithdrawBalance());
    }

    File playerDataFolder = new File(getDataFolder(), "playerData");
    public class playerJoinListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player p = event.getPlayer();
            UUID playerID = p.getUniqueId();

            File playerFile = new File(playerDataFolder, playerID + ".yml");

            if (!playerFile.exists()) {
                playerDataFolder.mkdirs();
                try {
                    playerFile.createNewFile();
                    YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    playerConfig.set("balance", 0);
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class BalanceCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player p) {
                File playerFile = new File(playerDataFolder, p.getUniqueId() + ".yml");
                YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                double playerBalance = playerConfig.getDouble("balance");
                p.sendMessage(formatMessage("&7Your current balance is: &a" + playerBalance));
                return true;
            }
            return false;
        }
    }

    public class BalanceTake implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player p) {
                if (args.length > 1) {
                    String playerName = args[0];
                    Player recipient = Bukkit.getPlayer(playerName);
                    if (recipient != null) {
                        UUID recipientUUID = recipient.getUniqueId();// Update with your player data folder path
                        File playerFile = new File(playerDataFolder, recipientUUID + ".yml");
                        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

                        double currentBalance = playerConfig.getDouble("balance");
                        double amountToRemove;
                        try {
                            amountToRemove = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            p.sendMessage(formatMessage("&7Invalid amount specified."));
                            return true;
                        }
                        if (currentBalance - amountToRemove < 1) {
                            p.sendMessage(formatMessage("&7Player does not have enough money, resetting to 0."));
                            playerConfig.set("balance", 0);
                        }
                        else {
                            double newBalance = currentBalance - amountToRemove;
                            playerConfig.set("balance", newBalance);
                        }

                        try {
                            playerConfig.save(playerFile);
                        } catch (Exception e) {
                            System.out.println("An error occurred while saving the player's balance.");
                            e.printStackTrace();
                            return true;
                        }

                        p.sendMessage(formatMessage("&7Successfully removed &a" + amountToRemove + " &7from &c" + recipient.getName() + "&7's balance."));
                    } else {
                        p.sendMessage(formatMessage("&7Player not found"));
                    }
                } else {
                    p.sendMessage(formatMessage("&7Usage: /delbal <player> <amount>"));
                }
            } else {
                sender.sendMessage("This command can only be executed by a player, not console.");
            }
            return true;
        }
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public class BalanceGive implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player p) {
                if (args.length > 1) {
                    String playerName = args[0];
                    Player recipient = Bukkit.getPlayer(playerName);
                    if (recipient != null) {
                        UUID recipientUUID = recipient.getUniqueId();// Update with your player data folder path
                        File playerFile = new File(playerDataFolder, recipientUUID + ".yml");
                        YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);

                        double currentBalance = playerConfig.getDouble("balance");
                        double amountToAdd;
                        try {
                            amountToAdd = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            p.sendMessage(formatMessage("&7Invalid amount specified."));
                            return true;
                        }
                        double newBalance = currentBalance + amountToAdd;
                        playerConfig.set("balance", newBalance);

                        try {
                            playerConfig.save(playerFile);
                        } catch (Exception e) {
                            System.out.println("An error occurred while saving the player's balance.");
                            e.printStackTrace();
                            return true;
                        }

                        p.sendMessage(formatMessage("&7Successfully added &a" + amountToAdd + " &7to &c" + recipient.getName() + "&7's balance."));
                    } else {
                        p.sendMessage(formatMessage("&7Player not found"));
                    }
                } else {
                    p.sendMessage(formatMessage("&7Usage: /addbal &c<player> &a<amount>"));
                }
            } else {
                sender.sendMessage("This command can only be executed by a player, not console.");
            }
            return true;
        }
    }

    public class WithdrawBalance implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            PlayerManager playerManager = new PlayerManager();
            BalanceManager balanceManager = new BalanceManager(playerManager);

            if (!(sender instanceof Player player)) {
                return false;
            }

            if (args.length < 1) {
                player.sendMessage(formatMessage("&7Invalid command. Usage: /withdraw <10/30/100> <amount>"));
                return true;
            }

            double withdrawAmount;
            double amount;

            try {
                withdrawAmount = Double.parseDouble(args[0]);
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(formatMessage("&cInvalid amount. Please enter a valid number."));
                return true;
            }

            Material itemMaterial;
            ItemStack item;

            if (withdrawAmount == 10) {
                itemMaterial = Material.IRON_INGOT;
            } else if (withdrawAmount == 30) {
                itemMaterial = Material.GOLD_INGOT;
            } else if (withdrawAmount == 100) {
                itemMaterial = Material.DIAMOND;
            } else {
                player.sendMessage(formatMessage("&7Invalid withdrawal amount. Valid amounts: 10, 30, 100"));
                return true;
            }

            item = new ItemStack(itemMaterial, (int) Math.min(1, amount));

            double totalWithdraw = withdrawAmount * amount;
            double playerBalance = balanceManager.getBalance(player);

            if (totalWithdraw > playerBalance) {
                player.sendMessage(formatMessage("&7Insufficient balance. You don't have enough funds to withdraw."));
                return true;
            }

            player.getInventory().addItem(item);
            balanceManager.removeFromBalance(player, totalWithdraw);
            player.sendMessage(formatMessage("&7Withdrew &a" + totalWithdraw + "&7 from your balance."));

            return true;
        }
    }


    public class PlayerManager {
        public File getPlayerFile(Player player) {
            String fileName = player.getUniqueId() + ".yml";
            return new File(playerDataFolder, fileName);
        }
        public YamlConfiguration getPlayerConfig(Player player) {
            File playerFile = getPlayerFile(player);
            return YamlConfiguration.loadConfiguration(playerFile);
        }
        public void savePlayerConfig(Player player, YamlConfiguration playerConfig) {
            File playerFile = getPlayerFile(player);
            try {
                playerConfig.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class BalanceManager {
        private static PlayerManager playerManager;
        private static final Map<Player, Double> balances = new HashMap<>();

        public BalanceManager(PlayerManager playerManager) {
            BalanceManager.playerManager = playerManager;
        }

        public static void addToBalance(Player player, double amount) {
            YamlConfiguration playerConfig = playerManager.getPlayerConfig(player);
            double currentBalance = playerConfig.getDouble("balance");
            double newBalance = currentBalance + amount;
            playerManager.savePlayerConfig(player, playerConfig);
        }

        public double getBalance(Player player) {
            YamlConfiguration playerConfig = playerManager.getPlayerConfig(player);
            return playerConfig.getDouble("balance");
        }

        public void setBalance(Player player, double amount) {
            YamlConfiguration playerConfig = playerManager.getPlayerConfig(player);
            playerConfig.set("balance", amount);
        }

        public void removeFromBalance(Player player, double amount) {
            YamlConfiguration playerConfig = playerManager.getPlayerConfig(player);
            double currentBalance = playerConfig.getDouble("balance");
            double newBalance = Math.max(0, currentBalance - amount);
            playerConfig.set("balance", newBalance);
        }
    }

    @EventHandler
    public boolean onPlayerInteract(PlayerInteractEvent event) throws IOException {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        NamespacedKey namespacedKey = new NamespacedKey(this, "Withdrawal Amount");

        if (item != null && item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta != null && itemMeta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.DOUBLE)) {
                double withdrawalAmount = itemMeta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.DOUBLE);
                player.sendMessage(formatMessage("&7Withdrew: &a" + withdrawalAmount));
                File playerFile = new File(playerDataFolder, player.getUniqueId() + ".yml");
                YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                double currentBalance = playerConfig.getDouble("balance");
                double newBalance = currentBalance + withdrawalAmount;
                playerConfig.set("balance", newBalance);
                playerConfig.save(playerFile);
                return true;
            }
            return false;
        }
        return false;
    }

    public class SellCommand implements CommandExecutor {
        PlayerManager playerManager = new PlayerManager();
        BalanceManager balanceManager = new BalanceManager(playerManager);

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be executed by a player.");
                return true;
            }

            ItemStack itemInHand = player.getItemInHand();

            if (itemInHand.getType().isAir()) {
                player.sendMessage(formatMessage("&7You are not holding any item to sell."));
                return true;
            }

            double sellPrice = getSellPrice(itemInHand);
            if (sellPrice <= 0) {
                player.sendMessage(formatMessage("&7This item cannot be sold."));
                return true;
            }

            int amount = itemInHand.getAmount();
            double totalPrice = sellPrice * amount;
            BalanceManager.addToBalance(player, totalPrice);

            player.getInventory().remove(itemInHand);
            player.sendMessage(formatMessage("&7Sold &a" + amount + " &7" + itemInHand.getType().toString() + " for &a$" + totalPrice));
            return true;
        }

        private double getSellPrice(ItemStack item) {
            Material material = item.getType();

            if (material == Material.IRON_INGOT) {
                return 10.0;
            } else if (material == Material.GOLD_INGOT) {
                return 30.0;
            } else if (material == Material.DIAMOND) {
                return 100.0;
            }

            return 0.0;
        }
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}