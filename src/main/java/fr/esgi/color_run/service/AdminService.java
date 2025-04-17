package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Admin;

import java.util.List;
import java.util.Optional;

public interface AdminService {

    /**
     * Crée un nouvel administrateur.
     * @param admin L'administrateur à créer
     * @return L'administrateur créé
     */
    Admin createAdmin(Admin admin);

    /**
     * Récupère un administrateur par son identifiant.
     * @param id L'identifiant de l'administrateur
     * @return Un Optional contenant l'administrateur s'il existe, ou vide sinon
     */
    Optional<Admin> getAdminById(int id);

    /**
     * Récupère la liste de tous les administrateurs enregistrés.
     * @return Une liste d'administrateurs
     */
    List<Admin> getAllAdmins();

    /**
     * Met à jour les informations d'un administrateur existant.
     * @param admin L'administrateur avec les informations mises à jour
     * @return L'administrateur mis à jour
     */
    Admin updateAdmin(Admin admin);

    /**
     * Supprime un administrateur par son identifiant.
     * @param id L'identifiant de l'administrateur à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    boolean deleteAdmin(int id);
}