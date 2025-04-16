package fr.esgi.color_run.service;

/**
 * Interface définissant les services d'authentification
 */
public interface LoginService {
    
    /**
     * Authentifie un participant à partir de son email et mot de passe
     * @param email Email du participant
     * @param password Mot de passe en clair
     * @return Token JWT si authentification réussie, null sinon
     */
    String authenticateParticipant(String email, String password);
    
    /**
     * Authentifie un administrateur à partir de son email et mot de passe
     * @param email Email de l'admin
     * @param password Mot de passe en clair
     * @return Token JWT si authentification réussie, null sinon
     */
    String authenticateAdmin(String email, String password);
}
