package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.AdminRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminRepositoryImpl implements AdminRepository {

    @Override
    public Admin save(Admin admin) {
        String sql = "INSERT INTO ADMIN (nom, prenom, email, mot_de_passe, url_profile) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection == null ? null : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            stmt.setString(1, admin.getNom());
            stmt.setString(2, admin.getPrenom());
            stmt.setString(3, admin.getEmail());
            stmt.setString(4, admin.getMotDePasse());
            stmt.setString(5, admin.getUrlProfile());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La création de l'admin a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    admin.setIdAdmin(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("La création de l'admin a échoué, aucun ID généré.");
                }
            }

            return admin;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de l'enregistrement de l'admin : " + e.getMessage());
        }
    }

    @Override
    public Optional<Admin> findById(int id) {
        String sql = "SELECT * FROM ADMIN WHERE id_admin = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection == null ? null : connection.prepareStatement(sql)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAdmin(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


    @Override
    public List<Admin> findAll() {
        List<Admin> admins = new ArrayList<>();
        String sql = "SELECT * FROM ADMIN";
        try (Connection connection = DatabaseConnection.getProdConnection();
             Statement stmt = connection == null ? null : connection.createStatement();
             ResultSet rs = stmt == null ? null : stmt.executeQuery(sql)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            while (rs.next()) {
                admins.add(mapResultSetToAdmin(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return admins;
    }

    @Override
    public Optional<Admin> findByEmail(String email) {
        String sql = "SELECT * FROM ADMIN WHERE email = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection == null ? null : connection.prepareStatement(sql)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAdmin(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void update(Admin admin) {
        String sql = "UPDATE ADMIN SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, url_profile = ? WHERE id_admin = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection == null ? null : connection.prepareStatement(sql)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            stmt.setString(1, admin.getNom());
            stmt.setString(2, admin.getPrenom());
            stmt.setString(3, admin.getEmail());
            stmt.setString(4, admin.getMotDePasse());
            stmt.setString(5, admin.getUrlProfile());
            stmt.setInt(6, admin.getIdAdmin());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour de l'admin : " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM ADMIN WHERE id_admin = ?";
        try (Connection connection = DatabaseConnection.getProdConnection();
             PreparedStatement stmt = connection == null ? null : connection.prepareStatement(sql)) {
            if (connection == null) {
                throw new SQLException("La connexion est nulle");
            }
            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de l'admin : " + e.getMessage());
        }
    }

    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        return Admin.builder()
                .idAdmin(rs.getInt("id_admin"))
                .nom(rs.getString("nom"))
                .prenom(rs.getString("prenom"))
                .email(rs.getString("email"))
                .motDePasse(rs.getString("mot_de_passe"))
                .urlProfile(rs.getString("url_profile"))
                .build();
    }
}
