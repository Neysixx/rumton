package fr.esgi.color_run.configuration;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties properties = new Properties();

    static {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream input = classLoader.getResourceAsStream("application.properties");
            if (input != null) {
                properties.load(input);
                System.out.println("Configuration chargée : " + properties.getProperty("db.url"));
            } else {
                System.out.println("Fichier application.properties non trouvé, utilisation des valeurs par défaut");
            }

            Class.forName("org.h2.Driver");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'initialisation", e);
        }
    }

    public static Connection getProdConnection() throws SQLException {
        String url = properties.getProperty("db.url", "jdbc:h2:./db_file/database");
        String user = properties.getProperty("db.user", "sa");
        String password = properties.getProperty("db.password", "");

        return DriverManager.getConnection(url, user, password);
    }

    public static Connection getTestConnection() throws SQLException {
        String url = properties.getProperty("db.test.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        String user = properties.getProperty("db.test.user", "sa");
        String password = properties.getProperty("db.test.password", "");

        return DriverManager.getConnection(url, user, password);
    }
}