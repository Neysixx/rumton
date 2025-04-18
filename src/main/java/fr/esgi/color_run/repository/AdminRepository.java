package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;

import java.util.List;
import java.util.Optional;

public interface AdminRepository {
    void save(Admin admin);
    Optional<Admin> findById(int id);
    List<Admin> findAll();
    void update(Admin admin);
    void delete(int id);

    /**
     * Recherche un participant par son email
     * @param email L'email du participant
     * @return Un Optional contenant le participant si trouvé
     */
    Optional<Admin> findByEmail(String email);
}
