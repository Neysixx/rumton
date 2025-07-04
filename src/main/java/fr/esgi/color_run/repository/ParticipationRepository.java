package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Participation;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour l'accès aux données des participations
 */
public interface ParticipationRepository {
    
    /**
     * Enregistre une participation
     * @param participation La participation à enregistrer
     * @return La participation enregistrée avec son ID généré
     */
    Participation save(Participation participation);
    
    /**
     * Recherche une participation par son ID
     * @param id L'ID de la participation
     * @return Un Optional contenant la participation si trouvée
     */
    Optional<Participation> findById(int id);
    
    /**
     * Récupère toutes les participations
     * @return Liste des participations
     */
    List<Participation> findAll();
    
    /**
     * Récupère les participations d'un participant
     * @param participantId L'ID du participant
     * @return Liste des participations du participant
     */
    List<Participation> findByParticipantId(int participantId);
    
    /**
     * Récupère les participations à une course
     * @param courseId L'ID de la course
     * @return Liste des participations à la course
     */
    List<Participation> findByCourseId(int courseId);
    
    /**
     * Compte le nombre de participants à une course
     * @param courseId L'ID de la course
     * @return Le nombre de participants
     */
    int countByCourseId(int courseId);
    
    /**
     * Vérifie si un participant est inscrit à une course
     * @param participantId L'ID du participant
     * @param courseId L'ID de la course
     * @return true si le participant est inscrit, false sinon
     */
    boolean existsByParticipantIdAndCourseId(int participantId, int courseId);

    /**
     * Récupère l'id de la participation a partir le la course Id et du participant
     * @param participantId L'ID du participant
     * @param courseId L'ID de la course
     * @return l'id de la participation
     */
    int getParticipationIdByCourseAndParticipant(int participantId, int courseId);
    
    /**
     * Trouve le numéro de dossard maximum pour une course
     * @param courseId L'ID de la course
     * @return Le numéro de dossard maximum ou null si aucune participation
     */
    Integer findMaxBibNumberByCourseId(int courseId);
    
    /**
     * Met à jour une participation
     * @param participation La participation à mettre à jour
     */
    void update(Participation participation);
    
    /**
     * Supprime une participation
     * @param id L'ID de la participation à supprimer
     */
    void delete(int id);
}
