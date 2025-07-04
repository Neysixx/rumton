package fr.esgi.color_run.business;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    //!// La classe Admin utlise l'annotation @Data de lombok, et JaCoCo ne sait pas faire la correspondance
    // entre le code source et le bytecode généré par Lombok pour la couverture “ligne” et “méthode”,
    // les résultats de coverage ne reflètenet donc pas la portée des tests pour cette classe, Merci de votre compréhension :)

    @Test
    @DisplayName("Test construction via builder")
    void testBuilder() {
        Admin admin = Admin.builder()
                .idAdmin(1)
                .nom("Durand")
                .prenom("Alice")
                .email("alice@esgi.fr")
                .motDePasse("secret")
                .urlProfile("/img/profil1.png")
                .build();

        assertEquals(1, admin.getIdAdmin());
        assertEquals("Durand", admin.getNom());
        assertEquals("Alice", admin.getPrenom());
        assertEquals("alice@esgi.fr", admin.getEmail());
        assertEquals("secret", admin.getMotDePasse());
        assertEquals("/img/profil1.png", admin.getUrlProfile());
    }

    @Test
    @DisplayName("Test constructeur complet")
    void testFullConstructor() {
        Admin admin = new Admin(1, "nom", "prenom", "email", "mdp", "profile");
        assertNotNull(admin);
        assertEquals("nom", admin.getNom());
    }

    @Test
    @DisplayName("Test getUrlProfile avec valeur null")
    void testGetUrlProfileDefault() {
        Admin admin = new Admin();
        admin.setUrlProfile(null);
        assertEquals("/color_run_war_exploded/assets/img/defaultProfile.png", admin.getUrlProfile());
    }

    @Test
    @DisplayName("Test getUrlProfile avec valeur non null")
    void testGetUrlProfileCustom() {
        Admin admin = new Admin();
        admin.setUrlProfile("/custom/path.png");
        assertEquals("/custom/path.png", admin.getUrlProfile());
    }

    @Test
    @DisplayName("Test equals et hashCode")
    void testEqualityAndHashCode() {
        Admin a1 = new Admin(1, "Nom", "Prenom", "email", "mdp", "/img.png");
        Admin a2 = new Admin(1, "Nom", "Prenom", "email", "mdp", "/img.png");
        Admin a3 = new Admin(2, "X", "Y", "Z", "X", null);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, a3);
    }

    @Test
    @DisplayName("Test toString non null et contient nom")
    void testToStringNotNull() {
        Admin admin = Admin.builder().nom("Durand").build();
        String s = admin.toString();
        assertNotNull(s);
        assertTrue(s.contains("Durand"));
    }

    @Test
    @DisplayName("Test setters")
    void testSetters() {
        Admin admin = new Admin();
        admin.setIdAdmin(10);
        admin.setNom("Toto");
        admin.setPrenom("Tata");
        admin.setEmail("mail@test.fr");
        admin.setMotDePasse("1234");
        admin.setUrlProfile("url");

        assertEquals(10, admin.getIdAdmin());
        assertEquals("Toto", admin.getNom());
        assertEquals("Tata", admin.getPrenom());
        assertEquals("mail@test.fr", admin.getEmail());
        assertEquals("1234", admin.getMotDePasse());
        assertEquals("url", admin.getUrlProfile());
    }
}
