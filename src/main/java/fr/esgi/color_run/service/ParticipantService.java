package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Participant;
import java.util.List;

public interface ParticipantService {

    /**
     * Crée un nouveau participant
     * @param participant Le participant à créer
     * @return Le participant créé avec son ID généré
     */
    Participant creerParticipant(Participant participant);

    /**
     * Vérifie si un participant existe avec l'email donné
     * @param email L'email à vérifier
     * @return true si un participant existe avec cet email, false sinon
     */
    boolean existsByEmail(String email);

    /**
     * Récupère un participant par son ID
     * @param id L'ID du participant
     * @return Le participant trouvé ou null
     */
    Participant getParticipantById(int id);

    /**
     * Récupère un participant par son email
     * @param email L'email du participant
     * @return Le participant trouvé ou null
     */
    Participant getParticipantByEmail(String email);

    /**
     * Récupère tous les participants
     * @return Liste de tous les participants
     */
    List<Participant> getAllParticipants();

    /**
     * Met à jour un participant existant
     * @param participant Le participant avec les données mises à jour
     * @return Le participant mis à jour
     */
    Participant updateParticipant(Participant participant);

    /**
     * Supprime un participant
     * @param id L'ID du participant à supprimer
     * @return true si supprimé avec succès, false sinon
     */
    boolean deleteParticipant(int id);

    /**
     * Authentifie un participant
     * @param email Email du participant
     * @param motDePasse Mot de passe du participant
     * @return Le participant authentifié ou null
     */
    Participant authentifier(String email, String motDePasse);
}