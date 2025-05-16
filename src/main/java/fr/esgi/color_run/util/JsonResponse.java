package fr.esgi.color_run.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe utilitaire pour créer des réponses JSON standardisées
 */
public class JsonResponse {
    
    /**
     * Crée une réponse de succès
     * @param data Données à inclure dans la réponse
     * @return Map représentant la réponse JSON
     */
    public static Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
    
    /**
     * Crée une réponse de succès avec un message
     * @param message Message de succès
     * @return Map représentant la réponse JSON
     */
    public static Map<String, Object> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
    
    /**
     * Crée une réponse d'erreur
     * @param message Message d'erreur
     * @return Map représentant la réponse JSON
     */
    public static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
    
    /**
     * Crée une réponse d'erreur avec des détails supplémentaires
     * @param message Message principal d'erreur
     * @param details Détails de l'erreur
     * @return Map représentant la réponse JSON
     */
    public static Map<String, Object> error(String message, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("details", details);
        return response;
    }
} 