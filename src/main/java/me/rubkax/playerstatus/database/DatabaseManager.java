package me.rubkax.playerstatus.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.rubkax.playerstatus.PlayerStatus;
import me.rubkax.playerstatus.config.ConfigManager;
import me.rubkax.playerstatus.model.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final PlayerStatus plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private boolean isMySQL;

    public DatabaseManager(PlayerStatus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean initialize() {
        try {
            String type = configManager.getConfig().getString("database.type", "sqlite");
            isMySQL = "mysql".equalsIgnoreCase(type);

            HikariConfig config = new HikariConfig();

            if (isMySQL) {
                String host = configManager.getConfig().getString("database.mysql.host", "localhost");
                int port = configManager.getConfig().getInt("database.mysql.port", 3306);
                String database = configManager.getConfig().getString("database.mysql.database", "playerstatus");
                String username = configManager.getConfig().getString("database.mysql.username", "root");
                String password = configManager.getConfig().getString("database.mysql.password", "");

                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8");
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");

                config.setMaximumPoolSize(configManager.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
                config.setMinimumIdle(configManager.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
                config.setConnectionTimeout(configManager.getConfig().getLong("database.mysql.pool.connection-timeout", 30000));
                config.setIdleTimeout(configManager.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
                config.setMaxLifetime(configManager.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));
            } else {
                String fileName = configManager.getConfig().getString("database.sqlite.file", "database.db");
                File dbFile = new File(plugin.getDataFolder(), fileName);
                config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
                config.setConnectionTestQuery("SELECT 1");
            }

            config.setPoolName("PlayerStatus-Pool");
            dataSource = new HikariDataSource(config);

            createTables();
            plugin.getLogger().info("Database initialized (" + (isMySQL ? "MySQL" : "SQLite") + ")");
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    equipped_status VARCHAR(64) DEFAULT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_statuses (
                    uuid VARCHAR(36) NOT NULL,
                    status_id VARCHAR(64) NOT NULL,
                    purchased_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (uuid, status_id)
                )
            """);
        }
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {

                String equipped = null;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT equipped_status FROM player_data WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        equipped = rs.getString("equipped_status");
                    }
                }

                Set<String> owned = new HashSet<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status_id FROM player_statuses WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        owned.add(rs.getString("status_id"));
                    }
                }

                return new PlayerData(uuid, owned, equipped);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + uuid, e);
                return new PlayerData(uuid);
            }
        });
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);

                try {

                    String upsertSql = isMySQL
                        ? "INSERT INTO player_data (uuid, equipped_status) VALUES (?, ?) ON DUPLICATE KEY UPDATE equipped_status = VALUES(equipped_status)"
                        : "INSERT OR REPLACE INTO player_data (uuid, equipped_status) VALUES (?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                        ps.setString(1, data.getPlayerUuid().toString());
                        ps.setString(2, data.getEquippedStatus());
                        ps.executeUpdate();
                    }

                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM player_statuses WHERE uuid = ?")) {
                        ps.setString(1, data.getPlayerUuid().toString());
                        ps.executeUpdate();
                    }

                    if (!data.getOwnedStatuses().isEmpty()) {
                        String insertSql = isMySQL
                            ? "INSERT INTO player_statuses (uuid, status_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE status_id = VALUES(status_id)"
                            : "INSERT OR REPLACE INTO player_statuses (uuid, status_id) VALUES (?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            for (String statusId : data.getOwnedStatuses()) {
                                ps.setString(1, data.getPlayerUuid().toString());
                                ps.setString(2, statusId);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    conn.commit();

                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + data.getPlayerUuid(), e);
            }
        });
    }

    public CompletableFuture<Void> resetPlayerData(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_data WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player_statuses WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset data for " + uuid, e);
            }
        });
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }
}
