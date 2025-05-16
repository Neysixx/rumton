package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Admin;
import fr.esgi.color_run.business.Participant;

import java.util.List;
import java.util.Optional;

public interface AdminRepository {

    /**
     * Enregistre un administrateur dans la base de données.
     * @param admin L'administrateur à sauvegarder
     * @return L'administrateur avec son ID généré
     */
    Admin save(Admin admin);

    /**
     * Recherche un administrateur par son ID.
     * @param id L'ID de l'administrateur
     * @return Un Optional contenant l'administrateur s'il est trouvé
     */
    Optional<Admin> findById(int id);

    /**
     * Récupère la liste de tous les administrateurs.
     * @return Une liste d'administrateurs
     */
    List<Admin> findAll();

    /**
     * Met à jour les informations d'un administrateur existant.
     * @param admin L'administrateur avec les informations mises à jour
     */
    void update(Admin admin);

    /**
     * Supprime un administrateur par son identifiant.
     * @param id L'ID de l'administrateur à supprimer
     */
    void delete(int id);

    /**
     * Recherche un participant par son email
     * @param email L'email du participant
     * @return Un Optional contenant le participant si trouvé
     */
    Optional<Admin> findByEmail(String email);
}
