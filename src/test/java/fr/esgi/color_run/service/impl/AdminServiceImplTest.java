package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    private Admin validAdmin;

    @BeforeEach
    void setup() {
        validAdmin = new Admin(1, "Nom", "Prenom", "admin@mail.com", "motdepasse", null);
    }

    @Test
    @DisplayName("createAdmin: succès avec admin valide")
    void createAdminSuccess() {
        Admin result;
        result = adminService.createAdmin(validAdmin);
        verify(adminRepository).save(validAdmin);
        assertEquals(validAdmin, result);
    }

    @Test
    @DisplayName("createAdmin: admin null")
    void createAdminNull() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.createAdmin(null));
        assertEquals("L'administrateur ne peut pas être null", ex.getMessage());
    }

    @Test
    @DisplayName("createAdmin: email vide")
    void createAdminEmailVide() {
        validAdmin.setEmail("   ");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.createAdmin(validAdmin));
        assertEquals("L'email est obligatoire", ex.getMessage());
    }

    @Test
    @DisplayName("createAdmin: mot de passe vide")
    void createAdminMotDePasseVide() {
        validAdmin.setMotDePasse("   ");
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.createAdmin(validAdmin));
        assertEquals("Le mot de passe est obligatoire", ex.getMessage());
    }

    @Test
    @DisplayName("createAdmin: mot de passe null")
    void createAdminMotDePasseNull() {
        validAdmin.setMotDePasse(null);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.createAdmin(validAdmin));
        assertEquals("Le mot de passe est obligatoire", ex.getMessage());
    }

    @Test
    @DisplayName("getAdminById: id valide")
    void getAdminByIdValid() {
        when(adminRepository.findById(1)).thenReturn(Optional.of(validAdmin));
        Admin admin = adminService.getAdminById(1);
        verify(adminRepository).findById(1);
        assertEquals(validAdmin, admin);
    }

    @Test
    @DisplayName("getAdminById: id invalide")
    void getAdminByIdInvalide() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.getAdminById(0));
        assertEquals("ID d'administrateur invalide", ex.getMessage());
    }

    @Test
    @DisplayName("getAdminById: admin non trouvé")
    void getAdminByIdNotFound() {
        when(adminRepository.findById(99)).thenReturn(Optional.empty());
        Admin result = adminService.getAdminById(99);
        verify(adminRepository).findById(99);
        assertNull(result);
    }

    @Test
    @DisplayName("existsByEmail: email valide connu")
    void existsByEmailValid() {
        when(adminRepository.findByEmail("admin@mail.com")).thenReturn(Optional.of(validAdmin));
        boolean exists = adminService.existsByEmail("admin@mail.com");
        verify(adminRepository).findByEmail("admin@mail.com");
        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByEmail: email inconnu")
    void existsByEmailInconnu() {
        when(adminRepository.findByEmail("inconnu@mail.com")).thenReturn(Optional.empty());
        boolean exists = adminService.existsByEmail("inconnu@mail.com");
        verify(adminRepository).findByEmail("inconnu@mail.com");
        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByEmail: email null ou vide")
    void existsByEmailVide() {
        assertFalse(adminService.existsByEmail(null));
        assertFalse(adminService.existsByEmail("   "));
    }

    @Test
    @DisplayName("getAllAdmins: succès")
    void getAllAdmins() {
        when(adminRepository.findAll()).thenReturn(List.of(validAdmin));
        List<Admin> result = adminService.getAllAdmins();
        verify(adminRepository).findAll();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("updateAdmin: succès")
    void updateAdminSuccess() {
        when(adminRepository.findById(validAdmin.getIdAdmin())).thenReturn(Optional.of(validAdmin));
        Admin result = adminService.updateAdmin(validAdmin);
        verify(adminRepository).update(validAdmin);
        assertEquals(validAdmin, result);
    }

    @Test
    @DisplayName("updateAdmin: admin null")
    void updateAdminNull() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.updateAdmin(null));
        assertEquals("L'administrateur ne peut pas être null", ex.getMessage());
    }

    @Test
    @DisplayName("updateAdmin: ID invalide")
    void updateAdminIdInvalide() {
        validAdmin.setIdAdmin(0);
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.updateAdmin(validAdmin));
        assertEquals("ID d'administrateur invalide", ex.getMessage());
    }

    @Test
    @DisplayName("updateAdmin: admin non trouvé")
    void updateAdminNonTrouve() {
        when(adminRepository.findById(validAdmin.getIdAdmin())).thenReturn(Optional.empty());
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.updateAdmin(validAdmin));
        assertEquals("Administrateur non trouvé", ex.getMessage());
    }

    @Test
    @DisplayName("deleteAdmin: succès")
    void deleteAdminSuccess() {
        doNothing().when(adminRepository).delete(1);
        boolean result = adminService.deleteAdmin(1);
        verify(adminRepository).delete(1);
        assertTrue(result);
    }

    @Test
    @DisplayName("deleteAdmin: ID invalide")
    void deleteAdminIdInvalide() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> adminService.deleteAdmin(0));
        assertEquals("ID d'administrateur invalide", ex.getMessage());
    }

    @Test
    @DisplayName("deleteAdmin: exception lors de la suppression")
    void deleteAdminException() {
        doThrow(RuntimeException.class).when(adminRepository).delete(1);
        boolean result = adminService.deleteAdmin(1);
        verify(adminRepository).delete(1);
        assertFalse(result);
    }
}