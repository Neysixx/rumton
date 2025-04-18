package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Message;
import fr.esgi.color_run.repository.MessageRepository;
import fr.esgi.color_run.repository.impl.MessageRepositoryImpl;
import fr.esgi.color_run.service.MessageService;
import fr.esgi.color_run.util.DebugUtil;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des messages
 */
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

    public MessageServiceImpl() {
        this.messageRepository = new MessageRepositoryImpl();
    }

    @Override
    public Message createMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Le message ne peut pas être null");
        }
        
        if (message.getEmetteur() == null) {
            throw new IllegalArgumentException("L'émetteur est obligatoire");
        }
        
        if (message.getCourse() == null) {
            throw new IllegalArgumentException("La course est obligatoire");
        }
        
        if (message.getContenu() == null || message.getContenu().trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu du message est obligatoire");
        }
        
        // Vérification du message parent si spécifié
        if (message.getMessageParent() != null) {
            int parentId = message.getMessageParent().getIdMessage();
            Optional<Message> parent = getMessageById(parentId);
            
            if (parent.isEmpty()) {
                throw new IllegalArgumentException("Le message parent n'existe pas");
            }
            
            // Vérifier que le message parent appartient à la même course
            if (parent.get().getCourse().getIdCourse() != message.getCourse().getIdCourse()) {
                throw new IllegalArgumentException("Le message parent doit appartenir à la même course");
            }
        }
        
        return messageRepository.save(message);
    }

    @Override
    public Optional<Message> getMessageById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de message invalide");
        }
        
        return messageRepository.findById(id);
    }

    @Override
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    @Override
    public List<Message> getMessagesByCourse(int courseId) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }
        
        return messageRepository.findByCourseId(courseId);
    }

    @Override
    public List<Message> getMessagesByEmetteur(int emetteurId) {
        if (emetteurId <= 0) {
            throw new IllegalArgumentException("ID d'émetteur invalide");
        }
        
        return messageRepository.findByEmetteurId(emetteurId);
    }

    @Override
    public List<Message> getRepliesByParent(int parentId) {
        if (parentId <= 0) {
            throw new IllegalArgumentException("ID de message parent invalide");
        }
        
        return messageRepository.findByParentId(parentId);
    }

    @Override
    public Message updateMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Le message ne peut pas être null");
        }
        
        if (message.getIdMessage() <= 0) {
            throw new IllegalArgumentException("ID de message invalide");
        }
        
        Optional<Message> existingMsg = messageRepository.findById(message.getIdMessage());
        if (existingMsg.isEmpty()) {
            throw new IllegalArgumentException("Message non trouvé");
        }
        
        // Ne mettre à jour que le contenu du message, pas les relations
        Message existingMessage = existingMsg.get();
        existingMessage.setContenu(message.getContenu());
        
        messageRepository.update(existingMessage);
        return existingMessage;
    }

    @Override
    public boolean deleteMessage(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de message invalide");
        }
        
        try {
            // Supprimer d'abord toutes les réponses à ce message
            List<Message> replies = getRepliesByParent(id);
            for (Message reply : replies) {
                messageRepository.delete(reply.getIdMessage());
            }
            
            // Puis supprimer le message lui-même
            messageRepository.delete(id);
            return true;
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors de la suppression du message: " + e.getMessage());
            return false;
        }
    }
}
