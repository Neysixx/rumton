package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Message;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.MessageRepository;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du repository pour les messages
 */
public class MessageRepositoryImpl implements MessageRepository {

    private final ParticipantService participantService;
    private final CourseService courseService;

    public MessageRepositoryImpl() {
        System.out.println("DEBUG - Initialisation de MessageRepositoryImpl");
        this.participantService = new ParticipantServiceImpl();
        this.courseService = new CourseServiceImpl();
    }

    @Override
    public Message save(Message message) {
        System.out.println("DEBUG - Début save message");
        String sql = "INSERT INTO MESSAGE (id_emetteur, id_course, id_message_parent, contenu, date_publication) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            System.out.println("DEBUG - Message à enregistrer: " + message);
            
            stmt.setInt(1, message.getEmetteur().getIdParticipant());
            stmt.setInt(2, message.getCourse().getIdCourse());

            if (message.getMessageParent() != null) {
                stmt.setInt(3, message.getMessageParent().getIdMessage());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setString(4, message.getContenu());

            // Correction du format de la date
            // Utiliser java.sql.Timestamp directement pour éviter les problèmes de format
            java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(message.getDatePublication().getTime());
            stmt.setTimestamp(5, sqlTimestamp);

            System.out.println("DEBUG - SQL préparé: " + stmt.toString());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du message a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setIdMessage(generatedKeys.getInt(1));
                    System.out.println("DEBUG - Message créé avec ID: " + message.getIdMessage());
                } else {
                    throw new SQLException("La création du message a échoué, aucun ID obtenu.");
                }
            }

            return message;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement du message: " + e.getMessage());
        } finally {
            System.out.println("DEBUG - Fin save message");
        }
    }

    @Override
    public Optional<Message> findById(int id) {
        System.out.println("DEBUG - Recherche message par ID: " + id);
        String sql = "SELECT * FROM MESSAGE WHERE id_message = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Message message = mapResultSetToMessage(rs);
                System.out.println("DEBUG - Message trouvé: " + message);
                return Optional.of(message);
            } else {
                System.out.println("DEBUG - Aucun message trouvé avec ID: " + id);
            }
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Message> findAll() {
        System.out.println("DEBUG - Récupération de tous les messages");
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE";
        try (Connection connection = DatabaseConnection.getProdConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG - Nombre de messages trouvés: " + messages.size());
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByCourseId(int courseId) {
        System.out.println("DEBUG - Recherche messages par courseId: " + courseId);
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_course = ? AND id_message_parent IS NULL ORDER BY date_publication DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG - Nombre de messages trouvés pour courseId " + courseId + ": " + messages.size());
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByEmetteurId(int emetteurId) {
        System.out.println("DEBUG - Recherche messages par emetteurId: " + emetteurId);
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_emetteur = ? ORDER BY date_publication DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, emetteurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG - Nombre de messages trouvés pour emetteurId " + emetteurId + ": " + messages.size());
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByParentId(int parentId) {
        System.out.println("DEBUG - Recherche réponses par parentId: " + parentId);
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_message_parent = ? ORDER BY date_publication ASC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG - Nombre de réponses trouvées pour parentId " + parentId + ": " + messages.size());
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public void update(Message message) {
        System.out.println("DEBUG - Mise à jour message ID: " + message.getIdMessage());
        String sql = "UPDATE MESSAGE SET contenu = ? WHERE id_message = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message.getContenu());
            stmt.setInt(2, message.getIdMessage());
            int affectedRows = stmt.executeUpdate();
            System.out.println("DEBUG - Lignes mises à jour: " + affectedRows);
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du message: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        System.out.println("DEBUG - Suppression message ID: " + id);
        
        // D'abord, supprimer les réponses à ce message
        try (Connection connection = DatabaseConnection.getProdConnection()) {
            // Suppression des réponses en cascade
            String sqlReplies = "DELETE FROM MESSAGE WHERE id_message_parent = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sqlReplies)) {
                stmt.setInt(1, id);
                int deleted = stmt.executeUpdate();
                System.out.println("DEBUG - Réponses supprimées: " + deleted);
            }
            
            // Ensuite, supprimer le message lui-même
            String sql = "DELETE FROM MESSAGE WHERE id_message = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, id);
                int deleted = stmt.executeUpdate();
                System.out.println("DEBUG - Message supprimé: " + (deleted > 0));
                if (deleted == 0) {
                    System.out.println("ATTENTION - Aucun message n'a été supprimé avec l'ID: " + id);
                }
            }
        } catch (SQLException e) {
            System.out.println("DEBUG - Erreur SQL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du message: " + e.getMessage());
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        int idMessage = rs.getInt("id_message");
        System.out.println("DEBUG - Mapping message ID: " + idMessage);
        
        int idEmetteur = rs.getInt("id_emetteur");
        int idCourse = rs.getInt("id_course");

        // Récupérer l'émetteur
        Participant emetteur = participantService.getParticipantById(idEmetteur);
        System.out.println("DEBUG - Émetteur: " + (emetteur != null ? emetteur.getIdParticipant() : "null"));

        // Récupérer la course
        Course course = courseService.getCourseById(idCourse).orElse(null);
        System.out.println("DEBUG - Course: " + (course != null ? course.getIdCourse() : "null"));

        // Récupérer le message parent si présent
        Message messageParent = null;
        int idMessageParent = rs.getInt("id_message_parent");
        if (!rs.wasNull() && idMessageParent > 0) {
            System.out.println("DEBUG - Message parent ID: " + idMessageParent);
            // Récupérer juste l'ID du message parent pour éviter les boucles infinies
            messageParent = new Message();
            messageParent.setIdMessage(idMessageParent);
        }

        Message message = Message.builder()
                .idMessage(idMessage)
                .emetteur(emetteur)
                .course(course)
                .messageParent(messageParent)
                .contenu(rs.getString("contenu"))
                .datePublication(rs.getTimestamp("date_publication"))
                .build();
                
        System.out.println("DEBUG - Message mappé: " + message.getIdMessage());
        return message;
    }
}
