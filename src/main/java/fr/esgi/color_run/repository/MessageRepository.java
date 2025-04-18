package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Message;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour l'accès aux données des messages
 */
public interface MessageRepository {
    
    /**
     * Enregistre un message
     * @param message Le message à enregistrer
     * @return Le message enregistré avec son ID généré
     */
    Message save(Message message);
    
    /**
     * Recherche un message par son ID
     * @param id L'ID du message
     * @return Un Optional contenant le message si trouvé
     */
    Optional<Message> findById(int id);
    
    /**
     * Récupère tous les messages
     * @return Liste des messages
     */
    List<Message> findAll();
    
    /**
     * Récupère les messages d'une course
     * @param courseId L'ID de la course
     * @return Liste des messages de la course
     */
    List<Message> findByCourseId(int courseId);
    
    /**
     * Récupère les messages d'un émetteur
     * @param emetteurId L'ID de l'émetteur
     * @return Liste des messages de l'émetteur
     */
    List<Message> findByEmetteurId(int emetteurId);
    
    /**
     * Récupère les réponses à un message parent
     * @param parentId L'ID du message parent
     * @return Liste des réponses au message parent
     */
    List<Message> findByParentId(int parentId);
    
    /**
     * Met à jour un message
     * @param message Le message à mettre à jour
     */
    void update(Message message);
    
    /**
     * Supprime un message
     * @param id L'ID du message à supprimer
     */
    void delete(int id);
}
