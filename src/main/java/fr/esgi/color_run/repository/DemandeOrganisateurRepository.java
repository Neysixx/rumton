package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.DemandeOrganisateur;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour l'accès aux données des demandes d'organisateur
 */
public interface DemandeOrganisateurRepository {
    
    /**
     * Enregistre une demande
     * @param demande La demande à enregistrer
     * @return La demande enregistrée avec son ID généré
     */
    DemandeOrganisateur save(DemandeOrganisateur demande);
    
    /**
     * Recherche une demande par son ID
     * @param id L'ID de la demande
     * @return Un Optional contenant la demande si trouvée
     */
    Optional<DemandeOrganisateur> findById(int id);
    
    /**
     * Récupère toutes les demandes
     * @return Liste des demandes
     */
    List<DemandeOrganisateur> findAll();
    
    /**
     * Récupère les demandes d'un participant
     * @param participantId L'ID du participant
     * @return Liste des demandes du participant
     */
    List<DemandeOrganisateur> findByParticipantId(int participantId);
    
    /**
     * Récupère les demandes par statut
     * @param status Le statut des demandes à récupérer
     * @return Liste des demandes ayant le statut spécifié
     */
    List<DemandeOrganisateur> findByStatus(String status);
    
    /**
     * Vérifie si un participant a une demande en cours
     * @param participantId L'ID du participant
     * @return true si une demande en cours existe, false sinon
     */
    boolean existsEnCoursByParticipantId(int participantId);
    
    /**
     * Met à jour une demande
     * @param demande La demande à mettre à jour
     */
    void update(DemandeOrganisateur demande);
    
    /**
     * Supprime une demande
     * @param id L'ID de la demande à supprimer
     */
    void delete(int id);
}
