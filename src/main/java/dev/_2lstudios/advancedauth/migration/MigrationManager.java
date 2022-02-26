package dev._2lstudios.advancedauth.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import dev._2lstudios.advancedauth.AdvancedAuth;
import dev._2lstudios.advancedauth.migration.impl.AuthMeMigration;
import dev._2lstudios.advancedauth.player.AuthPlayerData;
import dev._2lstudios.jelly.config.Configuration;

public class MigrationManager {
    private Map<String, IMigration> migrations;
    private AdvancedAuth plugin;

    public MigrationManager(final AdvancedAuth plugin) {
        this.migrations = new HashMap<>();
        this.plugin = plugin;

        this.addMigrator(new AuthMeMigration());
    }

    public void addMigrator(final IMigration migration) {
        this.migrations.put(migration.getPlugin().toLowerCase(), migration);
    }

    public int startMigration() throws Exception {
        // Prepare stuff.
        final Configuration config = this.plugin.getMigrationConfig();

        // Connect to backend.
        Connection connection = null;
        String url = "jdbc:";
        String backend = config.getString("backend");
        String table = null;

        if (backend.equalsIgnoreCase("mysql")) {
            String host = config.getString("mysql.host");
            String port = config.getInt("mysql.port") + "";
            String username = config.getString("mysql.username");
            String password = config.getString("mysql.password");
            String database = config.getString("mysql.database");
            table = config.getString("mysql.table");
            
            url += "mysql://" + host + ":" + port + "/" + database;
            connection = DriverManager.getConnection(url, username, password);
        } 
        else if (backend.trim().equals("")) {
            throw new Exception("Please check your migration.yml file.");
        } else {
            throw new Exception("Unknown backend type " + backend);
        }

        // Get migration for specified plugin.
        final String pluginName = config.getString("plugin");
        final IMigration migration = this.migrations.get(pluginName);
        if (migration == null) {
            throw new Exception("Unknown plugin " + pluginName);
        }

        String keyEmail = migration.getEmailKey();
        String keyDisplayName = migration.getDisplayNameKey();
        String keyUsername = migration.getUsernameKey();
        String keyUUID = migration.getUUIDKey();
        String keyPassword = migration.getPasswordKey();
        String keyRegistrationIP = migration.getRegistrationIPKey();
        String keyLastLoginIP = migration.getLastLoginIPKey();

        if (keyDisplayName == null) {
            keyUsername = keyDisplayName;
        }

        // Get all players.
        PreparedStatement statement = connection.prepareStatement("select * from " + table);
        ResultSet resultSet = statement.executeQuery();

        int users = 0;

        while(resultSet.next()) {
            String displayName = resultSet.getString(keyDisplayName);
            String username = resultSet.getString(keyUsername).toLowerCase();
            String password = resultSet.getString(keyPassword);

            String uuid = null;
            if (keyUUID != null)
            uuid = resultSet.getString(keyUUID);

            String email = null;
            if (keyEmail != null)
                email = resultSet.getString(keyEmail);

            String registrationIP = null;
            if (keyRegistrationIP != null)
                registrationIP = resultSet.getString(keyRegistrationIP);

            String lastLoginIP = null;
            if (keyLastLoginIP != null)
                lastLoginIP = resultSet.getString(keyLastLoginIP);

            AuthPlayerData player = new AuthPlayerData();
            player.email = email;
            player.displayName = displayName;
            player.username = username;
            player.uuid = uuid;
            player.password = password;
            player.registrationIP = registrationIP;
            player.lastLoginIP = lastLoginIP;
            player.save();
            users++;
        }

        connection.close();
        return users;
    }
}
