package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;

/**
 * Service d'authentification qui gère la validation des tokens et la récupération des utilisateurs
 */
public interface AuthService {
    
    /**
     * Vérifie si un token JWT est valide
     * @param token Le token JWT à vérifier
     * @return true si le token est valide, false sinon
     */
    boolean isTokenValid(String token);
    
    /**
     * Extrait l'ID utilisateur d'un token JWT
     * @param token Le token JWT
     * @return L'ID de l'utilisateur
     */
    int getUserIdFromToken(String token);
    
    /**
     * Récupère le participant associé au token JWT
     * @param token Le token JWT
     * @return Le participant si le token est valide et appartient à un participant, null sinon
     */
    Participant getParticipantFromToken(String token);
    
    /**
     * Récupère l'administrateur associé au token JWT
     * @param token Le token JWT
     * @return L'administrateur si le token est valide et appartient à un admin, null sinon
     */
    Admin getAdminFromToken(String token);
    
    /**
     * Vérifie si le token appartient à un administrateur
     * @param token Le token JWT
     * @return true si le token appartient à un administrateur, false sinon
     */
    boolean isAdmin(String token);
    
    /**
     * Vérifie si le token appartient à un organisateur
     * @param token Le token JWT
     * @return true si le token appartient à un organisateur, false sinon
     */
    boolean isOrganisateur(String token);

    /**
     * Vérifie si le token appartient à un participant vérifié
     * @param token Le token JWT
     * @return true si le token appartient à un participant vérifié, false sinon
     */
    boolean isVerified(String token);

    /**
     * Extrait le token JWT d'une requête HTTP
     * @param authHeader Le header Authorization
     * @param cookieToken Le token stocké dans un cookie
     * @return Le token JWT ou null si non trouvé
     */
    String extractToken(String authHeader, String cookieToken);

    /**
     * Récupère l'email de l'utilisateur associé au token JWT
     * @param token Le token JWT
     * @return L'email de l'utilisateur
     */
    String getEmailFromToken(String token);
}
