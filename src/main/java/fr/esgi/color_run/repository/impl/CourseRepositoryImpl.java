package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.CourseRepository;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class CourseRepositoryImpl implements CourseRepository {
    @Override
    public void save(Course course) {
        String sql = "INSERT INTO COURSE (nom, description, date_depart, ville, code_postal, adresse, distance, max_participants, prix_participation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getTestConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, course.getNom());
            stmt.setString(2, course.getDescription());
            stmt.setTimestamp(3, new Timestamp(course.getDateDepart().getTime()));
            stmt.setString(4, course.getVille());
            stmt.setInt(5, course.getCodePostal());
            stmt.setString(6, course.getAdresse());
            stmt.setFloat(7, course.getDistance());
            stmt.setInt(8, course.getMaxParticipants());
            stmt.setFloat(9, course.getPrixParticipation());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Course> findById(int id) {
        String sql = "SELECT * FROM COURSE WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getTestConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToCourse(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Course> findAll() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM COURSE";
        try (Connection connection = DatabaseConnection.getTestConnection();
             Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                courses.add(mapResultSetToCourse(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    @Override
    public void update(Course course) {
        String sql = "UPDATE COURSE SET nom = ?, description = ?, date_depart = ?, ville = ?, code_postal = ?, adresse = ?, distance = ?, max_participants = ?, prix_participation = ? WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getTestConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, course.getNom());
            stmt.setString(2, course.getDescription());
            stmt.setTimestamp(3, new Timestamp(course.getDateDepart().getTime()));
            stmt.setString(4, course.getVille());
            stmt.setInt(5, course.getCodePostal());
            stmt.setString(6, course.getAdresse());
            stmt.setFloat(7, course.getDistance());
            stmt.setInt(8, course.getMaxParticipants());
            stmt.setFloat(9, course.getPrixParticipation());
            stmt.setInt(10, course.getIdCourse());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM COURSE WHERE id_course = ?";
        try (Connection connection = DatabaseConnection.getTestConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        return Course.builder()
                .idCourse(rs.getInt("id_course"))
                .nom(rs.getString("nom"))
                .description(rs.getString("description"))
                .dateDepart(rs.getTimestamp("date_depart") != null ? new Date(rs.getTimestamp("date_depart").getTime()) : null)
                .ville(rs.getString("ville"))
                .codePostal(rs.getInt("code_postal"))
                .adresse(rs.getString("adresse"))
                .distance(rs.getFloat("distance"))
                .maxParticipants(rs.getInt("max_participants"))
                .prixParticipation(rs.getFloat("prix_participation"))
                .obstacles(rs.getString("obstacles"))
                .build();
    }
}