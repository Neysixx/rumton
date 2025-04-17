package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.repository.AdminRepository;
import fr.esgi.color_run.repository.impl.AdminRepositoryImpl;
import fr.esgi.color_run.service.AdminService;

import java.util.List;
import java.util.Optional;

public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;

    public AdminServiceImpl() {
        this.adminRepository = new AdminRepositoryImpl();
    }

    @Override
    public Admin createAdmin(Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("L'administrateur ne peut pas être null");
        }

        if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        if (admin.getMotDePasse() == null || admin.getMotDePasse().trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }

        adminRepository.save(admin);
        return admin;
    }

    @Override
    public Optional<Admin> getAdminById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID d'administrateur invalide");
        }

        return adminRepository.findById(id);
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public Admin updateAdmin(Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("L'administrateur ne peut pas être null");
        }

        if (admin.getIdAdmin() <= 0) {
            throw new IllegalArgumentException("ID d'administrateur invalide");
        }

        Optional<Admin> existingAdmin = adminRepository.findById(admin.getIdAdmin());
        if (existingAdmin.isEmpty()) {
            throw new IllegalArgumentException("Administrateur non trouvé");
        }

        adminRepository.update(admin);
        return admin;
    }

    @Override
    public boolean deleteAdmin(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID d'administrateur invalide");
        }

        try {
            adminRepository.delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
