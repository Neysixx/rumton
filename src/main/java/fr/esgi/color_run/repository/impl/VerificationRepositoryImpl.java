package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.VerificationRepository;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VerificationRepositoryImpl implements VerificationRepository {

    private final ParticipantService participantService;

    public VerificationRepositoryImpl() {
        this.participantService = new ParticipantServiceImpl();
    }

    @Override
    public Verification save(Verification verification) {
        String sql = "INSERT INTO VERIFICATION (id_participant, date_time, date_time_completed) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, verification.getParticipant().getIdParticipant());
            stmt.setTimestamp(2, new Timestamp(verification.getDateTime().getTime()));
            stmt.setTimestamp(3, verification.getDateTimeCompleted() != null ? new Timestamp(verification.getDateTimeCompleted().getTime()) : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Échec de l'insertion de la vérification, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Supposons qu'on veuille garder l'ID, tu peux l'ajouter dans la classe Verification si besoin
                    // verification.setIdVerification(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Échec de la récupération de l'ID de la vérification.");
                }
            }

            return verification;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement de la vérification: " + e.getMessage());
        }
    }

    @Override
    public Optional<Verification> findById(int id) {
        String sql = "SELECT * FROM VERIFICATION WHERE id_verification = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToVerification(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Verification> findAll() {
        List<Verification> verifications = new ArrayList<>();
        String sql = "SELECT * FROM VERIFICATION";
        try (Connection connection = DatabaseConnection.getProdConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                verifications.add(mapResultSetToVerification(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return verifications;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM VERIFICATION WHERE id_verification = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de la vérification: " + e.getMessage());
        }
    }

    private Verification mapResultSetToVerification(ResultSet rs) throws SQLException {
        int idParticipant = rs.getInt("id_participant");
        Participant participant = participantService.getParticipantById(idParticipant);

        return Verification.builder()
                .participant(participant)
                .dateTime(rs.getTimestamp("date_time"))
                .dateTimeCompleted(rs.getTimestamp("date_time_completed"))
                .build();
    }
}
