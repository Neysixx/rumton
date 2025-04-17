package fr.esgi.color_run.util;

import java.util.Date;

/**
 * Classe utilitaire pour faciliter le debug de l'application
 */
public class DebugUtil {

    private static final boolean DEBUG_MODE = true;

    /**
     * Affiche un message de debug dans la console
     * @param message Le message à afficher
     */
    public static void log(String message) {
        if (DEBUG_MODE) {
            System.out.println("[DEBUG " + new Date() + "] " + message);
        }
    }

    /**
     * Affiche un message de debug avec le nom de la classe appelante
     * @param clazz La classe depuis laquelle le log est appelé
     * @param message Le message à afficher
     */
    public static void log(Class<?> clazz, String message) {
        if (DEBUG_MODE) {
            System.out.println("[DEBUG " + new Date() + " - " + clazz.getSimpleName() + "] " + message);
        }
    }
    
    /**
     * Affiche l'état d'un objet pour debug
     * @param obj L'objet à afficher
     */
    public static void logObject(String label, Object obj) {
        if (DEBUG_MODE) {
            System.out.println("[DEBUG " + new Date() + "] " + label + ": " + obj);
        }
    }
    
    /**
     * Affiche une trace complète de la pile d'appels
     */
    public static void logStackTrace() {
        if (DEBUG_MODE) {
            new Exception("DEBUG Stack Trace").printStackTrace();
        }
    }
}
