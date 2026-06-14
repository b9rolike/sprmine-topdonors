package ru.sprmine.topdonors;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TopDonors extends JavaPlugin {

    private List<DonorEntry> topDonors = new ArrayList<>();
    private String dbPath;
    private int periodDays;

    public static class DonorEntry {
        public final String name;
        public final double amount;

        public DonorEntry(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.dbPath = getConfig().getString("database-path", "plugins/EasyPayments/database.db");
        this.periodDays = getConfig().getInt("period-days", 30);
        int updateInterval = getConfig().getInt("update-interval-minutes", 30);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TopDonorsExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, this::updateTopDonors);

        long ticks = updateInterval * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateTopDonors, ticks, ticks);

        getLogger().info("TopDonors enabled! Update interval: " + updateInterval + " minutes, period: " + periodDays + " days.");
    }

    public void updateTopDonors() {
        List<DonorEntry> result = new ArrayList<>();
        File serverRoot = getDataFolder().getParentFile().getParentFile();
        File dbFile = new File(serverRoot, dbPath);
        if (!dbFile.exists()) {
            File alt = new File(dbPath);
            getLogger().info("Primary path not found (" + dbFile.getAbsolutePath() + "), trying: " + alt.getAbsolutePath());
            if (alt.exists()) {
                dbFile = alt;
            }
        }
        getLogger().info("Using database at: " + dbFile.getAbsolutePath() + " (exists: " + dbFile.exists() + ")");

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        String query = "SELECT p.customer_id, SUM(pu.cost) as total " +
                "FROM easypayments_purchases pu " +
                "JOIN easypayments_payments p ON pu.payment_id = p.id " +
                "WHERE p.created_at >= datetime('now', '-" + periodDays + " days') " +
                "GROUP BY p.customer_id " +
                "ORDER BY total DESC " +
                "LIMIT 10";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String customer = rs.getString("customer_id");
                double total = rs.getDouble("total");
                result.add(new DonorEntry(customer, total));
            }

            this.topDonors = result;
            getLogger().info("Top donors updated: " + result.size() + " entries.");

        } catch (Exception e) {
            getLogger().severe("Failed to update top donors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<DonorEntry> getTopDonors() {
        return topDonors;
    }
}
