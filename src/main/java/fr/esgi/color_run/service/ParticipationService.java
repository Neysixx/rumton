package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Participation;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour les services de gestion des participations
 */
public interface ParticipationService {
    
    /**
     * Crée une nouvelle participation
     * @param participation La participation à créer
     * @return La participation créée
     */
    Participation createParticipation(Participation participation);
    
    /**
     * Récupère une participation par son ID
     * @param id L'ID de la participation
     * @return La participation si trouvée
     */
    Optional<Participation> getParticipationById(int id);
    
    /**
     * Récupère toutes les participations
     * @return La liste des participations
     */
    List<Participation> getAllParticipations();
    
    /**
     * Récupère les participations d'un participant
     * @param participantId L'ID du participant
     * @return La liste des participations du participant
     */
    List<Participation> getParticipationsByParticipant(int participantId);
    
    /**
     * Récupère les participations pour une course
     * @param courseId L'ID de la course
     * @return La liste des participations à la course
     */
    List<Participation> getParticipationsByCourse(int courseId);
    
    /**
     * Compte le nombre de participants pour une course
     * @param courseId L'ID de la course
     * @return Le nombre de participants
     */
    int getCountForCourse(int courseId);
    
    /**
     * Vérifie si un participant est inscrit à une course
     * @param participantId L'ID du participant
     * @param courseId L'ID de la course
     * @return true si le participant est inscrit, false sinon
     */
    boolean isParticipantRegistered(int participantId, int courseId);
    
    /**
     * Récupère le dernier numéro de dossard utilisé pour une course
     * @param courseId L'ID de la course
     * @return Le dernier numéro de dossard utilisé
     */
    int getLastBibNumberForCourse(int courseId);
    
    /**
     * Met à jour une participation
     * @param participation La participation à mettre à jour
     * @return La participation mise à jour
     */
    Participation updateParticipation(Participation participation);
    
    /**
     * Supprime une participation
     * @param id L'ID de la participation à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    boolean deleteParticipation(int id);
}
