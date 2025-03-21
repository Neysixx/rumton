package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Admin;
import java.util.List;
import java.util.Optional;

public interface AdminRepository {
    void save(Admin admin);
    Optional<Admin> findById(int id);
    List<Admin> findAll();
    void update(Admin admin);
    void delete(int id);
}
