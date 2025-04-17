package fr.esgi.color_run.util;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class PropertiesUtil {
    private static final String PROPERTIES_FILE = "application.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("Fichier " + PROPERTIES_FILE + " non trouvé, utilisation des valeurs par défaut");
            }
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors du chargement des propriétés : " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static ArrayList<String> getProperties(ArrayList<String> keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null) {
                values.add(value);
            } else {
                System.err.println("Propriété " + key + " non trouvée");
            }
        }
        return values;
    }
}
