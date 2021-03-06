package org.ipvp.admintools;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibatis.common.jdbc.ScriptRunner;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.ipvp.admintools.command.*;
import org.ipvp.admintools.model.Ban;
import org.ipvp.admintools.model.IpBan;
import org.ipvp.admintools.model.Mute;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class AdminTools extends Plugin {

    private HikariDataSource hikariDataSource;
    private Configuration config;
    private Map<UUID, Mute> activeMute = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        try {
            this.config = loadConfiguration();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load config.yml", e);
            return;
        }

        try {
            loadHikariDataSource();
            upgradeDatabase();
        } catch (IOException | SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to create a connection to database", e);
            return;
        }

        getProxy().registerChannel("ConsoleBanUser");

        // Register commands
        getProxy().getPluginManager().registerCommand(this, new AltsCommand(this));
        getProxy().getPluginManager().registerCommand(this, new BanCommand(this));
        getProxy().getPluginManager().registerCommand(this, new InfoCommand(this));
        getProxy().getPluginManager().registerCommand(this, new MuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnbanCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnblacklistCommand(this));
        getProxy().getPluginManager().registerCommand(this, new UnmuteCommand(this));
        getProxy().getPluginManager().registerCommand(this, new WarnCommand(this));

        // Register listeners
        getProxy().getPluginManager().registerListener(this, new PlayerActivityListener(this));
        getProxy().getPluginManager().registerListener(this, new ServerMessageListener(this));
    }

    // Creates the Hikari database connection
    private void loadHikariDataSource() throws IOException, SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(getConfig().getString("database.url"));
        dataSource.setUsername(getConfig().getString("database.user"));
        dataSource.setPassword(getConfig().getString("database.pass"));
        dataSource.setMaximumPoolSize(getConfig().getInt("database.threads"));
        dataSource.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("hikari-sql-pool-%d").build());
        this.hikariDataSource = dataSource;
        this.upgradeDatabase();
    }

    // Creates and updates the SQL schema
    private void upgradeDatabase() throws IOException, SQLException {
        getLogger().info("Upgrading database using schema.ddl");
        // First we must get the defined schema.ddl file from
        // the plugins resources as a readable stream.
        InputStream schemaDdl = getResourceAsStream("schema.ddl");
        InputStreamReader schemaReader = new InputStreamReader(schemaDdl);

        // We grab a connection from Hikari's connection pool and use ibatis
        // to run the script for us to create/update the database.
        Connection connection = hikariDataSource.getConnection();
        ScriptRunner runner = new ScriptRunner(connection, false, false);
        runner.setLogWriter(null);
        runner.runScript(schemaReader);
        connection.close(); // Return the connection to the pool
        getLogger().info("Database successfully upgraded");
    }

    public void broadcast(String message, String permission) {
        getProxy().getPlayers().stream().filter(p -> p.hasPermission(permission))
                .forEach(p -> p.sendMessage(message));
        getProxy().getConsole().sendMessage(message);
    }

    public Configuration getConfig() {
        return config;
    }

    public DataSource getDatabase() {
        return hikariDataSource;
    }

    /* (non-Javadoc)
         * Loads the configuration file
         */
    private Configuration loadConfiguration() throws IOException {
        File file = new File(getDataFolder(), "config.yml");

        if (file.exists()) {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        }

        // Create the file to save
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();

        // Load the default provided configuration and save it to the file
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(getResourceAsStream("config.yml"));
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        return config;
    }

    public UUID getUniqueId(CommandSender sender) {
        return sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId() : null;
    }

    public String getUniqueIdSafe(CommandSender sender) {
        UUID uuid = getUniqueId(sender);
        return uuid == null ? null : uuid.toString();
    }

    public Ban getActiveBan(UUID banned) throws SQLException {
        try (Connection connection = hikariDataSource.getConnection()) {
            return getActiveBan(connection, banned);
        }
    }

    public Ban getActiveBan(Connection connection, UUID banned) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * " +
                "FROM player_active_punishment " +
                "WHERE banned_id = ? " +
                "AND type = 'ban'")) {
            ps.setString(1, banned.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String sender = rs.getString("sender_id");
                    return new Ban(rs.getInt("id"),
                            sender != null ? UUID.fromString(rs.getString("sender_id")) : null,
                            banned,
                            rs.getString("reason"),
                            rs.getTimestamp("creation_date"),
                            rs.getTimestamp("expiry_date"));
                } else {
                    return null;
                }
            }
        }
    }

    public void registerMute(UUID player, Mute mute) {
        activeMute.put(player, mute);
    }

    public void unregisterMute(ProxiedPlayer player) {
        activeMute.remove(player.getUniqueId());
    }

    public Mute getActiveMute(ProxiedPlayer player) {
        Mute mute = activeMute.get(player.getUniqueId());
        if (mute != null) {
            // Check if the mute is expired
            if (mute.getExpiry() != null && mute.getExpiry().getTime() < System.currentTimeMillis()) {
                activeMute.remove(player.getUniqueId());
                return null;
            }
            return mute;
        }
        return null;
    }

    public Mute getActiveMute(UUID banned) throws SQLException {
        try (Connection connection = hikariDataSource.getConnection()) {
            return getActiveMute(connection, banned);
        }
    }

    public Mute getActiveMute(Connection connection, UUID banned) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * " +
                "FROM player_active_punishment " +
                "WHERE banned_id = ? " +
                "AND type = 'mute'")) {
            ps.setString(1, banned.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ?
                        new Mute(rs.getInt("id"),
                                UUID.fromString(rs.getString("sender_id")),
                                banned,
                                rs.getString("reason"),
                                rs.getTimestamp("creation_date"),
                                rs.getTimestamp("expiry_date"))
                        : null;
            }
        }
    }

    public IpBan getActiveIpBan(String hostAddress) throws SQLException {
        try (Connection connection = hikariDataSource.getConnection()) {
            return getActiveIpBan(connection, hostAddress);
        }
    }

    public IpBan getActiveIpBan(Connection connection, String hostAddress) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * " +
                "FROM player_active_ip_ban " +
                "WHERE ip_address = ?")) {
            ps.setString(1, hostAddress);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ?
                        new IpBan(rs.getInt("id"),
                                UUID.fromString(rs.getString("sender_id")),
                                rs.getString("ip_address"),
                                rs.getString("reason"),
                                rs.getTimestamp("creation_date"),
                                rs.getTimestamp("expiry_date"))
                        : null;
            }
        }
    }
}
