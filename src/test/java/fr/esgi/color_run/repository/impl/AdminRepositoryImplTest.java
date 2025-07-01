package fr.esgi.color_run.repository.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.configuration.DatabaseConnection;
import fr.esgi.color_run.repository.AdminRepository;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminRepositoryImplTest {

    private AdminRepository adminRepository;

    @BeforeEach
    void setUp() {
        adminRepository = new AdminRepositoryImpl();
        adminRepository.findAll().forEach(a -> adminRepository.delete(a.getIdAdmin()));
    }

    @Test
    void testSave() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        assertTrue(admin.getIdAdmin() > 0);
    }

    @Test
    void testFindByIdExists() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test2@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        Optional<Admin> result = adminRepository.findById(admin.getIdAdmin());
        assertTrue(result.isPresent());
        assertEquals("test2@example.com", result.get().getEmail());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Admin> result = adminRepository.findById(9999);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmailExists() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test3@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        Optional<Admin> result = adminRepository.findByEmail("test3@example.com");
        assertTrue(result.isPresent());
        assertEquals("TestNom", result.get().getNom());
    }

    @Test
    void testFindByEmailNotFound() {
        Optional<Admin> result = adminRepository.findByEmail("notfound@example.com");
        assertFalse(result.isPresent());
    }

    @Test
    void testFindAllNotEmpty() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test4@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        List<Admin> all = adminRepository.findAll();
        assertFalse(all.isEmpty());
    }

    @Test
    void testFindAllEmpty() {
        List<Admin> all = adminRepository.findAll();
        assertTrue(all.isEmpty());
    }

    @Test
    void testUpdateValid() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test5@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        admin.setNom("UpdatedNom");
        adminRepository.update(admin);
        Admin updated = adminRepository.findById(admin.getIdAdmin()).orElseThrow();
        assertEquals("UpdatedNom", updated.getNom());
    }

    @Test
    void testUpdateInexistant() {
        Admin nonExistant = Admin.builder()
                .idAdmin(9999)
                .nom("Ghost")
                .email("ghost@example.com")
                .motDePasse("pass")
                .build();
        assertDoesNotThrow(() -> adminRepository.update(nonExistant));
    }

    @Test
    void testUpdateWithEmptyFields() {
        Admin admin = Admin.builder()
                .nom("Nom")
                .prenom("Prenom")
                .email("empty@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setMotDePasse("");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithMissingId() {
        Admin admin = Admin.builder()
                .nom("Nom")
                .prenom("Prenom")
                .email("missingid@example.com")
                .motDePasse("pass")
                .build();
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testFindByEmailWithWhitespace() {
        Optional<Admin> result = adminRepository.findByEmail("   ");
        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteNonNumericId() {
        assertDoesNotThrow(() -> adminRepository.delete(-42));
    }

    @Test
    void testDeleteExisting() {
        Admin admin = Admin.builder()
                .nom("TestNom")
                .prenom("TestPrenom")
                .email("test6@example.com")
                .motDePasse("123456")
                .urlProfile(null)
                .build();
        adminRepository.save(admin);
        adminRepository.delete(admin.getIdAdmin());
        Optional<Admin> result = adminRepository.findById(admin.getIdAdmin());
        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteNonExisting() {
        assertDoesNotThrow(() -> adminRepository.delete(9999));
    }

    @Test
    void testUpdateWithNullFields() {
        Admin partialAdmin = Admin.builder()
                .idAdmin(0)
                .nom(null)
                .email(null)
                .motDePasse(null)
                .build();
        assertDoesNotThrow(() -> adminRepository.update(partialAdmin));
    }

    @Test
    void testSaveNullAdmin() {
        assertThrows(NullPointerException.class, () -> adminRepository.save(null));
    }

    @Test
    void testFindByEmailNullOrEmpty() {
        Optional<Admin> resultNull = adminRepository.findByEmail(null);
        Optional<Admin> resultEmpty = adminRepository.findByEmail("");
        assertFalse(resultNull.isPresent());
        assertFalse(resultEmpty.isPresent());
    }

    @Test
    void testFindByEmailNull() {
        Optional<Admin> result = adminRepository.findByEmail(null);
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByEmailEmpty() {
        Optional<Admin> result = adminRepository.findByEmail("");
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIdInvalidNegative() {
        Optional<Admin> result = adminRepository.findById(-1);
        assertFalse(result.isPresent());
    }

    @Test
    void testSavePartialAdmin() {
        Admin partial = Admin.builder()
                .nom("Partial")
                .prenom("Test")
                .email("partial@example.com")
                .motDePasse("pass")
                .build();
        assertDoesNotThrow(() -> adminRepository.save(partial));
    }

    @Test
    void testSaveThrowsSQLException() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            Admin admin = new Admin();
            admin.setNom("Erreur");
            admin.setPrenom("Err");
            admin.setEmail("erreur@example.com");
            admin.setMotDePasse("pass");

            RuntimeException ex = assertThrows(RuntimeException.class, () -> adminRepository.save(admin));
            assertTrue(ex.getMessage().contains("Erreur lors de l'enregistrement") || ex.getMessage().contains("La connexion est nulle"));
        }
    }

    @Test
    void testFindByIdThrowsSQLException() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            Optional<Admin> result = adminRepository.findById(1);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindAllThrowsSQLException() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            List<Admin> result = adminRepository.findAll();
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testFindByEmailThrowsSQLException() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            Optional<Admin> result = adminRepository.findByEmail("test@example.com");
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testUpdateThrowsSQLExceptionMock() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            Admin admin = Admin.builder()
                    .idAdmin(1)
                    .nom("Erreur")
                    .email("erreur@example.com")
                    .motDePasse("pass")
                    .build();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> adminRepository.update(admin));
            assertTrue(ex.getMessage().contains("Erreur lors de la mise à jour") || ex.getMessage().contains("La connexion est nulle"));
        }
    }

    @Test
    void testDeleteThrowsSQLExceptionMock() {
        try (MockedStatic<DatabaseConnection> mocked = org.mockito.Mockito.mockStatic(DatabaseConnection.class)) {
            mocked.when(DatabaseConnection::getTestConnection).thenThrow(new SQLException("Erreur simulée"));
            RuntimeException ex = assertThrows(RuntimeException.class, () -> adminRepository.delete(1));
            assertTrue(ex.getMessage().contains("Erreur lors de la suppression") || ex.getMessage().contains("La connexion est nulle"));
        }
    }

    @Test
    void testUpdateWithOnlyPassword() {
        Admin admin = Admin.builder()
                .nom("Original")
                .prenom("Test")
                .email("passonly@example.com")
                .motDePasse("original")
                .build();
        adminRepository.save(admin);
        admin.setMotDePasse("newpass");
        admin.setNom("Original");
        admin.setEmail("passonly@example.com");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithOnlyEmail() {
        Admin admin = Admin.builder()
                .nom("Original")
                .prenom("Test")
                .email("initial@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setEmail("changed@example.com");
        admin.setMotDePasse("pass");
        admin.setNom("Original");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithOnlyNom() {
        Admin admin = Admin.builder()
                .nom("Original")
                .prenom("Test")
                .email("nomonly@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setNom("NouvelNom");
        admin.setEmail("nomonly@example.com");
        admin.setMotDePasse("pass");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithSameValues() {
        Admin admin = Admin.builder()
                .nom("Same")
                .prenom("Test")
                .email("same@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setNom("Same");
        admin.setEmail("same@example.com");
        admin.setMotDePasse("pass");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithOnlyPrenomChanged() {
        Admin admin = Admin.builder()
                .nom("Test")
                .prenom("OldPrenom")
                .email("prenomchange@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setPrenom("NewPrenom");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithUrlProfileUnchanged() {
        Admin admin = Admin.builder()
                .nom("Test")
                .prenom("Test")
                .email("urlunchanged@example.com")
                .motDePasse("pass")
                .urlProfile("http://example.com/img.jpg")
                .build();
        adminRepository.save(admin);
        admin.setUrlProfile("http://example.com/img.jpg");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithEmptyStringFields() {
        Admin admin = Admin.builder()
                .nom("Before")
                .prenom("Prenom")
                .email("empties@example.com")
                .motDePasse("pass")
                .build();
        adminRepository.save(admin);
        admin.setNom("");
        admin.setEmail("");
        admin.setMotDePasse("");
        assertDoesNotThrow(() -> adminRepository.update(admin));
    }

    @Test
    void testUpdateWithMixedNullAndValidFields() {
        Admin admin = Admin.builder()
                .nom("Valid")
                .prenom("Valid")
                .email("mix@example.com")
                .motDePasse("valid")
                .build();
        adminRepository.save(admin);
        admin.setNom("Valid");
        admin.setEmail("mix@example.com");
        admin.setMotDePasse("valid");
        admin.setPrenom(null); // champ obligatoire mis à null
        admin.setUrlProfile(null);
        assertThrows(RuntimeException.class, () -> adminRepository.update(admin));
    }

    @Test
    void testDeleteZeroId() {
        assertDoesNotThrow(() -> adminRepository.delete(0));
    }

    @Test
    void testSaveNoRowsAffected() throws Exception {
        try (MockedStatic<DatabaseConnection> mocked = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            when(mockConn.prepareStatement(anyString(), anyInt())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenReturn(0); // Simule aucun row affecté

            mocked.when(DatabaseConnection::getTestConnection).thenReturn(mockConn);

            Admin admin = Admin.builder()
                    .nom("Test")
                    .prenom("Test")
                    .email("norow@example.com")
                    .motDePasse("pass")
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> adminRepository.save(admin));
            assertTrue(ex.getMessage().contains("aucune ligne affectée"));
        }
    }

    @Test
    void testSaveNoGeneratedKey() throws Exception {
        try (MockedStatic<DatabaseConnection> mocked = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockKeys = mock(ResultSet.class);

            when(mockConn.prepareStatement(anyString(), anyInt())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenReturn(1);
            when(mockStmt.getGeneratedKeys()).thenReturn(mockKeys);
            when(mockKeys.next()).thenReturn(false); // Simule aucun ID généré

            mocked.when(DatabaseConnection::getTestConnection).thenReturn(mockConn);

            Admin admin = Admin.builder()
                    .nom("Test")
                    .prenom("Test")
                    .email("nokey@example.com")
                    .motDePasse("pass")
                    .build();

            RuntimeException ex = assertThrows(RuntimeException.class, () -> adminRepository.save(admin));
            assertTrue(ex.getMessage().contains("aucun ID généré"));
        }
    }

    @Test
    void testUpdateNoRowsAffected() throws Exception {
        try (MockedStatic<DatabaseConnection> mocked = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenReturn(0); // Simule aucun row affecté

            mocked.when(DatabaseConnection::getTestConnection).thenReturn(mockConn);

            Admin admin = Admin.builder()
                    .idAdmin(9999)
                    .nom("Ghost")
                    .email("ghost@example.com")
                    .motDePasse("pass")
                    .build();

            // update ne lève pas d'exception si aucune ligne affectée, donc on vérifie juste qu'il ne plante pas
            assertDoesNotThrow(() -> adminRepository.update(admin));
        }
    }

    @Test
    void testDeleteNoRowsAffected() throws Exception {
        try (MockedStatic<DatabaseConnection> mocked = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeUpdate()).thenReturn(0); // Simule aucun row affecté

            mocked.when(DatabaseConnection::getTestConnection).thenReturn(mockConn);

            // delete ne lève pas d'exception si aucune ligne affectée, donc on vérifie juste qu'il ne plante pas
            assertDoesNotThrow(() -> adminRepository.delete(9999));
        }
    }

    @Test
    void testFindByIdSQLExceptionInMapping() throws Exception {
        try (MockedStatic<DatabaseConnection> mocked = mockStatic(DatabaseConnection.class)) {
            Connection mockConn = mock(Connection.class);
            PreparedStatement mockStmt = mock(PreparedStatement.class);
            ResultSet mockRs = mock(ResultSet.class);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
            when(mockStmt.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getInt("id_admin")).thenReturn(1);
            when(mockRs.getString("nom")).thenThrow(new SQLException("Erreur mapping"));

            mocked.when(DatabaseConnection::getTestConnection).thenReturn(mockConn);

            Optional<Admin> result = adminRepository.findById(1);
            assertTrue(result.isEmpty());
        }
    }
}