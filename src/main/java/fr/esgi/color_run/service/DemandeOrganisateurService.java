package fr.esgi.color_run.service;

import fr.esgi.color_run.business.DemandeOrganisateur;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour les services de gestion des demandes pour devenir organisateur
 */
public interface DemandeOrganisateurService {
    
    /**
     * Crée une nouvelle demande
     * @param demande La demande à créer
     * @return La demande créée
     */
    DemandeOrganisateur createDemande(DemandeOrganisateur demande);
    
    /**
     * Récupère une demande par son ID
     * @param id L'ID de la demande
     * @return La demande si trouvée
     */
    Optional<DemandeOrganisateur> getDemandeById(int id);
    
    /**
     * Récupère toutes les demandes
     * @return La liste de toutes les demandes
     */
    List<DemandeOrganisateur> getAllDemandes();
    
    /**
     * Récupère les demandes d'un participant
     * @param participantId L'ID du participant
     * @return La liste des demandes du participant
     */
    List<DemandeOrganisateur> getDemandesByParticipant(int participantId);
    
    /**
     * Récupère les demandes par statut
     * @param status Le statut des demandes à récupérer
     * @return La liste des demandes ayant le statut spécifié
     */
    List<DemandeOrganisateur> getDemandesByStatus(String status);
    
    /**
     * Vérifie si un participant a une demande en cours
     * @param participantId L'ID du participant
     * @return true si une demande en cours existe, false sinon
     */
    boolean hasDemandeEnCours(int participantId);
    
    /**
     * Met à jour une demande
     * @param demande La demande à mettre à jour
     * @return La demande mise à jour
     */
    DemandeOrganisateur updateDemande(DemandeOrganisateur demande);
    
    /**
     * Supprime une demande
     * @param id L'ID de la demande à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    boolean deleteDemande(int id);
}
