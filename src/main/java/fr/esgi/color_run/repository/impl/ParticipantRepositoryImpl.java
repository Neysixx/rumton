package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.ParticipantRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParticipantRepositoryImpl implements ParticipantRepository {

    @Override
    public Participant save(Participant participant) {
        String sql = "INSERT INTO PARTICIPANT (nom, prenom, email, mot_de_passe, url_profile, est_organisateur, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, participant.getNom());
            stmt.setString(2, participant.getPrenom());
            stmt.setString(3, participant.getEmail());
            stmt.setString(4, participant.getMotDePasse());
            stmt.setString(5, participant.getUrlProfile());
            stmt.setBoolean(6, participant.isEstOrganisateur());
            stmt.setTimestamp(7, new Timestamp(participant.getDateCreation().getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création du participant a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    participant.setIdParticipant(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création du participant a échoué, aucun ID obtenu.");
                }
            }

            return participant;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement du participant: " + e.getMessage());
        }
    }

    @Override
    public Optional<Participant> findById(int id) {
        String sql = "SELECT * FROM PARTICIPANT WHERE id_participant = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Participant> findByEmail(String email) {
        String sql = "SELECT * FROM PARTICIPANT WHERE email = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Participant> findAll() {
        List<Participant> participants = new ArrayList<>();
        String sql = "SELECT * FROM PARTICIPANT";
        try (Connection connection = DatabaseConnection.getProdConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                participants.add(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return participants;
    }

    @Override
    public void update(Participant participant) {
        String sql = "UPDATE PARTICIPANT SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, " +
                "url_profile = ?, est_organisateur = ? WHERE id_participant = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, participant.getNom());
            stmt.setString(2, participant.getPrenom());
            stmt.setString(3, participant.getEmail());
            stmt.setString(4, participant.getMotDePasse());
            stmt.setString(5, participant.getUrlProfile());
            stmt.setBoolean(6, participant.isEstOrganisateur());
            stmt.setInt(7, participant.getIdParticipant());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour du participant: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM PARTICIPANT WHERE id_participant = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du participant: " + e.getMessage());
        }
    }

    private Participant mapResultSetToParticipant(ResultSet rs) throws SQLException {
        return Participant.builder()
                .idParticipant(rs.getInt("id_participant"))
                .nom(rs.getString("nom"))
                .prenom(rs.getString("prenom"))
                .email(rs.getString("email"))
                .motDePasse(rs.getString("mot_de_passe"))
                .urlProfile(rs.getString("url_profile"))
                .estOrganisateur(rs.getBoolean("est_organisateur"))
                .dateCreation(rs.getTimestamp("date_creation"))
                .build();
    }
}