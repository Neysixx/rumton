package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {

    /**
     * Crée un nouveau cours.
     * @param course Le cours à créer
     */
    Course createCourse(Course course);

    /**
     * Récupère un cours par son identifiant.
     * @param id L'identifiant du cours
     * @return Un Optional contenant le cours s'il existe, ou vide sinon
     */
    Optional<Course> getCourseById(int id);

    /**
     * Récupère la liste de tous les cours enregistrés.
     * @return Une liste de cours
     */
    List<Course> getAllCourses();

    /**
     * Récupère la liste de tous les course d'un organisateur.
     * @return Une liste de cours
     */
    List<Course> getCoursesByOrgaId(int idOrga);

    /**
     * Récupère une liste de course triés par plus récent.
     * @param limit le nombre de courses souhaités
     * @return Une liste de cours
     */
    List<Course> getRecentCourses(int limit);

    /**
     * Récupère les courses d'une cause.
     * @param causeId l'id de la cause
     * @return Une liste de cours
     */
    List<Course> getCoursesByCauseId(int causeId);

    /**
     * Met à jour les informations d'un cours existant.
     * @param course Le cours avec les informations mises à jour
     */
    Course updateCourse(Course course);

    /**
     * Supprime un cours par son identifiant.
     * @param id L'identifiant du cours à supprimer
     */
    boolean deleteCourse(int id);
}