package me.rubkax.playerstatus.hook;

import me.rubkax.playerstatus.PlayerStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class EconomyHook {

    private final PlayerStatus plugin;
    private Object economyProvider;
    private boolean useVault = false;
    private boolean available = false;

    public EconomyHook(PlayerStatus plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {

        try {
            Plugin vault = Bukkit.getPluginManager().getPlugin("Vault");
            if (vault != null) {
                var rsp = Bukkit.getServicesManager().getRegistration(
                    Class.forName("net.milkbowl.vault.economy.Economy")
                );
                if (rsp != null) {
                    economyProvider = rsp.getProvider();
                    useVault = true;
                    available = true;
                    plugin.getLogger().info("Using Vault economy provider.");
                    return;
                }
            }
        } catch (ClassNotFoundException ignored) {}

        Plugin excellentEconomy = Bukkit.getPluginManager().getPlugin("ExcellentEconomy");
        if (excellentEconomy != null) {
            available = true;
            economyProvider = excellentEconomy;
            plugin.getLogger().info("Using ExcellentEconomy directly.");
        } else {
            plugin.getLogger().warning("No economy provider found!");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public double getBalance(Player player) {
        if (!available) return 0;

        try {
            if (useVault) {
                Method getBalance = economyProvider.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
                return (double) getBalance.invoke(economyProvider, player);
            }

            return getBalanceReflection(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get balance for " + player.getName(), e);
            return 0;
        }
    }

    public boolean has(Player player, double amount) {
        return getBalance(player) >= amount;
    }

    public boolean withdraw(Player player, double amount) {
        if (!available) return false;

        try {
            if (useVault) {
                Method withdrawPlayer = economyProvider.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
                Object result = withdrawPlayer.invoke(economyProvider, player, amount);
                Method transactionSuccess = result.getClass().getMethod("transactionSuccess");
                return (boolean) transactionSuccess.invoke(result);
            }
            return withdrawReflection(player, amount);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to withdraw from " + player.getName(), e);
            return false;
        }
    }

    private double getBalanceReflection(Player player) {

        try {
            if (economyProvider != null) {

                for (String methodName : new String[]{"getBalance", "getMoney", "get"}) {
                    try {
                        Method method = economyProvider.getClass().getMethod(methodName, Player.class);
                        Object result = method.invoke(economyProvider, player);
                        if (result instanceof Number num) return num.doubleValue();
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean withdrawReflection(Player player, double amount) {
        try {
            if (economyProvider != null) {
                for (String methodName : new String[]{"withdraw", "withdrawMoney", "take", "removeMoney"}) {
                    try {
                        Method method = economyProvider.getClass().getMethod(methodName, Player.class, double.class);
                        method.invoke(economyProvider, player, amount);
                        return true;
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
