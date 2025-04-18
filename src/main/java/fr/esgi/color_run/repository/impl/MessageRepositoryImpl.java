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
        this.participantService = new ParticipantServiceImpl();
        this.courseService = new CourseServiceImpl();
    }

    @Override
    public Message save(Message message) {
        String sql = "INSERT INTO MESSAGE (id_emetteur, id_course, id_message_parent, contenu, date_publication) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du message a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    message.setIdMessage(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création du message a échoué, aucun ID obtenu.");
                }
            }

            return message;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement du message: " + e.getMessage());
        }
    }

    @Override
    public Optional<Message> findById(int id) {
        String sql = "SELECT * FROM MESSAGE WHERE id_message = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Message> findAll() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE";
        try (Connection connection = DatabaseConnection.getProdConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByCourseId(int courseId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_course = ? AND id_message_parent IS NULL ORDER BY date_publication DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByEmetteurId(int emetteurId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_emetteur = ? ORDER BY date_publication DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, emetteurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public List<Message> findByParentId(int parentId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM MESSAGE WHERE id_message_parent = ? ORDER BY date_publication ASC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, parentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public void update(Message message) {
        String sql = "UPDATE MESSAGE SET contenu = ? WHERE id_message = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, message.getContenu());
            stmt.setInt(2, message.getIdMessage());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du message: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM MESSAGE WHERE id_message = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du message: " + e.getMessage());
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        int idEmetteur = rs.getInt("id_emetteur");
        int idCourse = rs.getInt("id_course");

        // Récupérer l'émetteur
        Participant emetteur = participantService.getParticipantById(idEmetteur);

        // Récupérer la course
        Course course = courseService.getCourseById(idCourse).orElse(null);

        // Récupérer le message parent si présent
        Message messageParent = null;
        int idMessageParent = rs.getInt("id_message_parent");
        if (!rs.wasNull() && idMessageParent > 0) {
            // Appel récursif pour charger le message parent, mais sans charger ses réponses
            // pour éviter une boucle infinie
            String parentSql = "SELECT * FROM MESSAGE WHERE id_message = ?";
            try (Connection connection = DatabaseConnection.getProdConnection();
                    PreparedStatement stmt = connection.prepareStatement(parentSql)) {
                stmt.setInt(1, idMessageParent);
                ResultSet parentRs = stmt.executeQuery();
                if (parentRs.next()) {
                    messageParent = new Message();
                    messageParent.setIdMessage(parentRs.getInt("id_message"));
                    messageParent.setEmetteur(participantService.getParticipantById(parentRs.getInt("id_emetteur")));
                    messageParent.setCourse(courseService.getCourseById(parentRs.getInt("id_course")).orElse(null));
                    messageParent.setContenu(parentRs.getString("contenu"));
                    messageParent.setDatePublication(parentRs.getTimestamp("date_publication"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return Message.builder()
                .idMessage(rs.getInt("id_message"))
                .emetteur(emetteur)
                .course(course)
                .messageParent(messageParent)
                .contenu(rs.getString("contenu"))
                .datePublication(rs.getTimestamp("date_publication"))
                .build();
    }
}
