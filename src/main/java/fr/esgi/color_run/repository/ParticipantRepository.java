package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Participant;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository {

    /**
     * Enregistre un participant dans la base de données
     * @param participant Le participant à sauvegarder
     * @return Le participant avec son ID généré
     */
    Participant save(Participant participant);

    /**
     * Recherche un participant par son ID
     * @param id L'ID du participant
     * @return Un Optional contenant le participant si trouvé
     */
    Optional<Participant> findById(int id);

    /**
     * Recherche un participant par son email
     * @param email L'email du participant
     * @return Un Optional contenant le participant si trouvé
     */
    Optional<Participant> findByEmail(String email);

    /**
     * Récupère tous les participants
     * @return Liste de tous les participants
     */
    List<Participant> findAll();

    /**
     * Met à jour un participant existant
     * @param participant Le participant avec les données mises à jour
     */
    void update(Participant participant);

    /**
     * Supprime un participant
     * @param id L'ID du participant à supprimer
     */
    void delete(int id);
}