package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.DemandeOrganisateur;
import fr.esgi.color_run.repository.DemandeOrganisateurRepository;
import fr.esgi.color_run.repository.impl.DemandeOrganisateurRepositoryImpl;
import fr.esgi.color_run.service.DemandeOrganisateurService;
import fr.esgi.color_run.util.DebugUtil;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des demandes pour devenir organisateur
 */
public class DemandeOrganisateurServiceImpl implements DemandeOrganisateurService {

    private final DemandeOrganisateurRepository demandeRepository;

    public DemandeOrganisateurServiceImpl() {
        this.demandeRepository = new DemandeOrganisateurRepositoryImpl();
    }

    @Override
    public DemandeOrganisateur createDemande(DemandeOrganisateur demande) {
        if (demande == null) {
            throw new IllegalArgumentException("La demande ne peut pas être null");
        }
        
        if (demande.getParticipant() == null) {
            throw new IllegalArgumentException("Le participant est obligatoire");
        }
        
        if (demande.getMotivations() == null || demande.getMotivations().trim().isEmpty()) {
            throw new IllegalArgumentException("Les motivations sont obligatoires");
        }
        
        // Vérifier qu'une demande en cours n'existe pas déjà
        if (hasDemandeEnCours(demande.getParticipant().getIdParticipant())) {
            throw new IllegalArgumentException("Le participant a déjà une demande en cours");
        }
        
        // Définir le statut initial
        demande.setStatus("EN_ATTENTE");
        
        return demandeRepository.save(demande);
    }

    @Override
    public Optional<DemandeOrganisateur> getDemandeById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de demande invalide");
        }
        
        return demandeRepository.findById(id);
    }

    @Override
    public List<DemandeOrganisateur> getAllDemandes() {
        return demandeRepository.findAll();
    }

    @Override
    public List<DemandeOrganisateur> getDemandesByParticipant(int participantId) {
        if (participantId <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }
        
        return demandeRepository.findByParticipantId(participantId);
    }

    @Override
    public List<DemandeOrganisateur> getDemandesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Le statut ne peut pas être vide");
        }
        
        return demandeRepository.findByStatus(status);
    }

    @Override
    public boolean hasDemandeEnCours(int participantId) {
        if (participantId <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }
        
        return demandeRepository.existsEnCoursByParticipantId(participantId);
    }

    @Override
    public DemandeOrganisateur updateDemande(DemandeOrganisateur demande) {
        if (demande == null) {
            throw new IllegalArgumentException("La demande ne peut pas être null");
        }
        
        if (demande.getIdDemande() <= 0) {
            throw new IllegalArgumentException("ID de demande invalide");
        }
        
        Optional<DemandeOrganisateur> existingDemande = demandeRepository.findById(demande.getIdDemande());
        if (existingDemande.isEmpty()) {
            throw new IllegalArgumentException("Demande non trouvée");
        }
        
        // Vérifier que le statut est valide
        String status = demande.getStatus();
        if (status != null && !status.equals("EN_ATTENTE") && !status.equals("ACCEPTEE") && !status.equals("REFUSEE")) {
            throw new IllegalArgumentException("Statut de demande invalide");
        }
        
        demandeRepository.update(demande);
        return demande;
    }

    @Override
    public boolean deleteDemande(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de demande invalide");
        }
        
        try {
            demandeRepository.delete(id);
            return true;
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors de la suppression de la demande: " + e.getMessage());
            return false;
        }
    }
}
