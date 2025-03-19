//package fr.esgi.color_run.repository.impl;
//
//import fr.esgi.color_run.configuration.DatabaseConnection;
//import fr.esgi.color_run.repository.AdminRepository;
//import fr.esgi.color_run.business.Admin;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//public class AdminRepositoryImpl implements AdminRepository {
//
//    @Override
//    public void save(Admin admin) {
//        String sql = "INSERT INTO ADMIN (nom, prenom, email, mot_de_passe, url_profile) VALUES (?, ?, ?, ?, ?)";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setString(1, admin.getNom());
//            stmt.setString(2, admin.getPrenom());
//            stmt.setString(3, admin.getEmail());
//            stmt.setString(4, admin.getMotDePasse());
//            stmt.setString(5, admin.getUrlProfile());
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public Optional<Admin> findById(int id) {
//        String sql = "SELECT * FROM ADMIN WHERE id_admin = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, id);
//            ResultSet rs = stmt.executeQuery();
//            if (rs.next()) {
//                return Optional.of(mapResultSetToAdmin(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return Optional.empty();
//    }
//
//    @Override
//    public List<Admin> findAll() {
//        List<Admin> admins = new ArrayList<>();
//        String sql = "SELECT * FROM ADMIN";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             Statement stmt = connection.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//            while (rs.next()) {
//                admins.add(mapResultSetToAdmin(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return admins;
//    }
//
//    @Override
//    public void update(Admin admin) {
//        String sql = "UPDATE ADMIN SET nom = ?, prenom = ?, email = ?, mot_de_passe = ?, url_profile = ? WHERE id_admin = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setString(1, admin.getNom());
//            stmt.setString(2, admin.getPrenom());
//            stmt.setString(3, admin.getEmail());
//            stmt.setString(4, admin.getMotDePasse());
//            stmt.setString(5, admin.getUrlProfile());
//            stmt.setInt(6, admin.getIdAdmin());
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void delete(int id) {
//        String sql = "DELETE FROM ADMIN WHERE id_admin = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, id);
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
//        return new Admin(
//                rs.getInt("id_admin"),
//                rs.getString("nom"),
//                rs.getString("prenom"),
//                rs.getString("email"),
//                rs.getString("mot_de_passe"),
//                rs.getString("url_profile")
//        );
//    }
//}