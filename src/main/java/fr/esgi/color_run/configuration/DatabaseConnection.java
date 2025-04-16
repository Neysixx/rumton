package fr.esgi.color_run.configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String TEST_DB_URL = "jdbc:h2:./db_file/database_test"; // Base de test
    private static final String PROD_DB_URL = "jdbc:h2:./db_file/database"; // Base de production
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    static {
        try {
            // Chargement explicite du driver H2
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver H2 non trouvé. Vérifiez vos dépendances Maven.", e);
        }
    }

    public static Connection getTestConnection() throws SQLException {
        return DriverManager.getConnection(TEST_DB_URL, USER, PASSWORD);
    }

    public static Connection getProdConnection() throws SQLException {
        return DriverManager.getConnection(PROD_DB_URL, USER, PASSWORD);
    }
}