package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.DemandeOrganisateur;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.DemandeOrganisateurRepository;
import fr.esgi.color_run.service.AdminService;
import fr.esgi.color_run.service.ParticipantService;
import fr.esgi.color_run.service.impl.AdminServiceImpl;
import fr.esgi.color_run.service.impl.ParticipantServiceImpl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du repository pour les demandes d'organisateur
 */
public class DemandeOrganisateurRepositoryImpl implements DemandeOrganisateurRepository {

    private final ParticipantService participantService;
    private final AdminService adminService;

    public DemandeOrganisateurRepositoryImpl() {
        this.participantService = new ParticipantServiceImpl();
        this.adminService = new AdminServiceImpl();
    }

    @Override
    public DemandeOrganisateur save(DemandeOrganisateur demande) {
        String sql = "INSERT INTO DEMANDE_ORGANISATEUR (id_participant, motivations, status, date_creation) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, demande.getParticipant().getIdParticipant());
            stmt.setString(2, demande.getMotivations());
            stmt.setString(3, demande.getStatus());
            stmt.setTimestamp(4, new Timestamp(demande.getDateCreation().getTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de la demande a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    demande.setIdDemande(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de la demande a échoué, aucun ID obtenu.");
                }
            }

            return demande;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement de la demande: " + e.getMessage());
        }
    }

    @Override
    public Optional<DemandeOrganisateur> findById(int id) {
        String sql = "SELECT * FROM DEMANDE_ORGANISATEUR WHERE id_demande = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToDemande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<DemandeOrganisateur> findAll() {
        List<DemandeOrganisateur> demandes = new ArrayList<>();
        String sql = "SELECT * FROM DEMANDE_ORGANISATEUR ORDER BY date_creation DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return demandes;
    }

    @Override
    public List<DemandeOrganisateur> findByParticipantId(int participantId) {
        List<DemandeOrganisateur> demandes = new ArrayList<>();
        String sql = "SELECT * FROM DEMANDE_ORGANISATEUR WHERE id_participant = ? ORDER BY date_creation DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return demandes;
    }

    @Override
    public List<DemandeOrganisateur> findByStatus(String status) {
        List<DemandeOrganisateur> demandes = new ArrayList<>();
        String sql = "SELECT * FROM DEMANDE_ORGANISATEUR WHERE status = ? ORDER BY date_creation DESC";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return demandes;
    }

    @Override
    public boolean existsEnCoursByParticipantId(int participantId) {
        String sql = "SELECT COUNT(*) FROM DEMANDE_ORGANISATEUR WHERE id_participant = ? AND status = 'EN_ATTENTE'";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, participantId);
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
    public void update(DemandeOrganisateur demande) {
        String sql = "UPDATE DEMANDE_ORGANISATEUR SET motivations = ?, traite_par = ?, status = ?, reponse = ?, date_traitement = ? WHERE id_demande = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, demande.getMotivations());

            if (demande.getTraitePar().isPresent()) {
                stmt.setInt(2, demande.getTraitePar().get().getIdAdmin());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setString(3, demande.getStatus());

            if (demande.getReponse() != null) {
                stmt.setBoolean(4, demande.getReponse());
            } else {
                stmt.setNull(4, Types.BOOLEAN);
            }

            if (demande.getDateTraitement().isPresent()) {
                stmt.setTimestamp(5, new Timestamp(demande.getDateTraitement().get().getTime()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }

            stmt.setInt(6, demande.getIdDemande());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de la demande: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM DEMANDE_ORGANISATEUR WHERE id_demande = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de la demande: " + e.getMessage());
        }
    }

    private DemandeOrganisateur mapResultSetToDemande(ResultSet rs) throws SQLException {
        int idParticipant = rs.getInt("id_participant");

        // Récupérer le participant
        Participant participant = participantService.getParticipantById(idParticipant);

        // Récupérer l'admin qui a traité la demande, si présent
        Optional<Admin> traitePar = Optional.empty();
        int idAdmin = rs.getInt("traite_par");
        if (!rs.wasNull()) {
            Optional<Admin> adminOpt = Optional.ofNullable(adminService.getAdminById(idAdmin));
            if (adminOpt.isPresent()) {
                traitePar = adminOpt;
            }
        }

        // Récupérer la date de traitement, si présente
        Optional<java.util.Date> dateTraitement = Optional.empty();
        Timestamp dateTraitementTs = rs.getTimestamp("date_traitement");
        if (dateTraitementTs != null) {
            // Convertir explicitement le Timestamp en java.util.Date
            dateTraitement = Optional.of(new java.util.Date(dateTraitementTs.getTime()));
        }

        // Récupérer la réponse, si présente
        Boolean reponse = rs.getBoolean("reponse");
        if (rs.wasNull()) {
            reponse = null;
        }

        return DemandeOrganisateur.builder()
                .idDemande(rs.getInt("id_demande"))
                .participant(participant)
                .motivations(rs.getString("motivations"))
                .traitePar(traitePar)
                .status(rs.getString("status"))
                .reponse(reponse)
                .dateCreation(rs.getTimestamp("date_creation"))
                .dateTraitement(dateTraitement)
                .build();
    }
}
