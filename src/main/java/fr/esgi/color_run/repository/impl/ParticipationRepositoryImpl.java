package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.ParticipationRepository;
import fr.esgi.color_run.service.CourseService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.CourseServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du repository pour les participations
 */
public class ParticipationRepositoryImpl implements ParticipationRepository {

    private final ParticipantService participantService;
    private final CourseService courseService;

    public ParticipationRepositoryImpl() {
        this.participantService = new ParticipantServiceImpl();
        this.courseService = new CourseServiceImpl();
    }

    @Override
    public Participation save(Participation participation) {
        String sql = "INSERT INTO PARTICIPATION (id_participant, id_course, numero_dossard, date_reservation) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, participation.getParticipant().getIdParticipant());
            stmt.setInt(2, participation.getCourse().getIdCourse());
            stmt.setInt(3, participation.getNumeroDossard());
            stmt.setTimestamp(4, new Timestamp(participation.getDateReservation().getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la participation a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    participation.setIdParticipation(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la participation a échoué, aucun ID obtenu.");
                }
            }

            return participation;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement de la participation: " + e.getMessage());
        }
    }

    @Override
    public Optional<Participation> findById(int id) {
        String sql = "SELECT * FROM PARTICIPATION WHERE id_participation = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Participation> findAll() {
        List<Participation> participations = new ArrayList<>();
        String sql = "SELECT * FROM PARTICIPATION";
        try (Connection connection = DatabaseConnection.getProdConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                participations.add(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participations;
    }

    @Override
    public List<Participation> findByParticipantId(int participantId) {
        List<Participation> participations = new ArrayList<>();
        String sql = "SELECT * FROM PARTICIPATION WHERE id_participant = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                participations.add(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participations;
    }

    @Override
    public List<Participation> findByCourseId(int courseId) {
        List<Participation> participations = new ArrayList<>();
        String sql = "SELECT * FROM PARTICIPATION WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                participations.add(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participations;
    }

    @Override
    public int countByCourseId(int courseId) {
        String sql = "SELECT COUNT(*) FROM PARTICIPATION WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean existsByParticipantIdAndCourseId(int participantId, int courseId) {
        String sql = "SELECT COUNT(*) FROM PARTICIPATION WHERE id_participant = ? AND id_course = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participantId);
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getParticipationIdByCourseAndParticipant(int participantId, int courseId) {
        String sql = "SELECT id_participation FROM PARTICIPATION WHERE id_participant = ? AND id_course = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participantId);
            stmt.setInt(2, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Integer findMaxBibNumberByCourseId(int courseId) {
        String sql = "SELECT MAX(numero_dossard) FROM PARTICIPATION WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void update(Participation participation) {
        String sql = "UPDATE PARTICIPATION SET id_participant = ?, id_course = ?, numero_dossard = ?, date_reservation = ? WHERE id_participation = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participation.getParticipant().getIdParticipant());
            stmt.setInt(2, participation.getCourse().getIdCourse());
            stmt.setInt(3, participation.getNumeroDossard());
            stmt.setTimestamp(4, new Timestamp(participation.getDateReservation().getTime()));
            stmt.setInt(5, participation.getIdParticipation());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de la participation: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM PARTICIPATION WHERE id_participation = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de la participation: " + e.getMessage());
        }
    }

    private Participation mapResultSetToParticipation(ResultSet rs) throws SQLException {
        int idParticipant = rs.getInt("id_participant");
        int idCourse = rs.getInt("id_course");

        // Récupérer le participant
        Participant participant = participantService.getParticipantById(idParticipant);

        // Récupérer la course
        Course course = courseService.getCourseById(idCourse).orElse(null);

        return Participation.builder()
                .idParticipation(rs.getInt("id_participation"))
                .participant(participant)
                .course(course)
                .numeroDossard(rs.getInt("numero_dossard"))
                .dateReservation(rs.getTimestamp("date_reservation"))
                .build();
    }
}
