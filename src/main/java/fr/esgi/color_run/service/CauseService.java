package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Cause;

import java.util.List;
import java.util.Optional;

public interface CauseService {

    /**
     * Crée une nouvelle cause.
     * @param cause La cause à créer
     * @return La cause créée
     */
    Cause createCause(Cause cause);

    /**
     * Récupère une cause par son identifiant.
     * @param id L'identifiant de la cause
     * @return Un Optional contenant la cause si elle existe, ou vide sinon
     */
    Optional<Cause> getCauseById(int id);

    /**
     * Récupère la liste de toutes les causes enregistrées.
     * @return Une liste de causes
     */
    List<Cause> getAllCauses();

    /**
     * Met à jour les informations d'une cause existante.
     * @param cause La cause avec les informations mises à jour
     * @return La cause mise à jour
     */
    Cause updateCause(Cause cause);

    /**
     * Supprime une cause par son identifiant.
     * @param id L'identifiant de la cause à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    boolean deleteCause(int id);
}
