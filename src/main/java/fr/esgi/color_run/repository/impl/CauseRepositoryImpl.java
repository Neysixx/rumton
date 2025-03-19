//package fr.esgi.color_run.repository.impl;
//
//import fr.esgi.color_run.configuration.DatabaseConnection;
//import fr.esgi.color_run.repository.CauseRepository;
//import fr.esgi.color_run.business.Cause;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//public class CauseRepositoryImpl implements CauseRepository {
//
//    @Override
//    public void save(Cause cause) {
//        String sql = "INSERT INTO CAUSE (intitule) VALUES (?)";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            stmt.setString(1, cause.getIntitule());
//            stmt.executeUpdate();
//
//            // Récupérer l'ID généré
//            ResultSet generatedKeys = stmt.getGeneratedKeys();
//            if (generatedKeys.next()) {
//                cause.setIdCause(generatedKeys.getInt(1));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public Optional<Cause> findById(int id) {
//        String sql = "SELECT * FROM CAUSE WHERE id_cause = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, id);
//            ResultSet rs = stmt.executeQuery();
//            if (rs.next()) {
//                return Optional.of(mapResultSetToCause(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return Optional.empty();
//    }
//
//    @Override
//    public List<Cause> findAll() {
//        List<Cause> causes = new ArrayList<>();
//        String sql = "SELECT * FROM CAUSE";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             Statement stmt = connection.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//            while (rs.next()) {
//                causes.add(mapResultSetToCause(rs));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return causes;
//    }
//
//    @Override
//    public void update(Cause cause) {
//        String sql = "UPDATE CAUSE SET intitule = ? WHERE id_cause = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setString(1, cause.getIntitule());
//            stmt.setInt(2, cause.getIdCause());
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void delete(int id) {
//        String sql = "DELETE FROM CAUSE WHERE id_cause = ?";
//        try (Connection connection = DatabaseConnection.getTestConnection();
//             PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, id);
//            stmt.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private Cause mapResultSetToCause(ResultSet rs) throws SQLException {
//        return new Cause(
//                rs.getInt("id_cause"),
//                rs.getString("intitule"),
//                new ArrayList<>() // Liste des courses associées (à récupérer via une autre requête si nécessaire)
//        );
//    }
//}
