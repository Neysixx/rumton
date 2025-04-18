package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Message;

import java.util.List;
import java.util.Optional;

/**
 * Interface pour les services de gestion des messages
 */
public interface MessageService {
    
    /**
     * Crée un nouveau message
     * @param message Le message à créer
     * @return Le message créé
     */
    Message createMessage(Message message);
    
    /**
     * Récupère un message par son ID
     * @param id L'ID du message
     * @return Le message si trouvé
     */
    Optional<Message> getMessageById(int id);
    
    /**
     * Récupère tous les messages
     * @return La liste de tous les messages
     */
    List<Message> getAllMessages();
    
    /**
     * Récupère les messages d'une course
     * @param courseId L'ID de la course
     * @return La liste des messages de la course
     */
    List<Message> getMessagesByCourse(int courseId);
    
    /**
     * Récupère les messages d'un émetteur
     * @param emetteurId L'ID de l'émetteur
     * @return La liste des messages de l'émetteur
     */
    List<Message> getMessagesByEmetteur(int emetteurId);
    
    /**
     * Récupère les réponses à un message parent
     * @param parentId L'ID du message parent
     * @return La liste des réponses au message parent
     */
    List<Message> getRepliesByParent(int parentId);
    
    /**
     * Met à jour un message
     * @param message Le message à mettre à jour
     * @return Le message mis à jour
     */
    Message updateMessage(Message message);
    
    /**
     * Supprime un message
     * @param id L'ID du message à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    boolean deleteMessage(int id);
}
