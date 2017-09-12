package org.ipvp.admintools;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibatis.common.jdbc.ScriptRunner;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class AdminTools extends Plugin implements Listener {

    private HikariDataSource hikariDataSource;
    private Configuration config;
    
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
}
