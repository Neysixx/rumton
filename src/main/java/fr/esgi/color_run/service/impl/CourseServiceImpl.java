package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.repository.CourseRepository;
import fr.esgi.color_run.repository.impl.CourseRepositoryImpl;
import fr.esgi.color_run.service.CourseService;

import java.util.List;
import java.util.Optional;

public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    public CourseServiceImpl() {
        this.courseRepository = new CourseRepositoryImpl();
    }

    @Override
    public Course createCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("La course ne peut pas être null");
        }

        if (course.getNom() == null || course.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la course est obligatoire");
        }

        if (course.getDistance() <= 0) {
            throw new IllegalArgumentException("La distance de la course doit être supérieure à 0");
        }

        if (course.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("Le nombre maximum de participants doit être supérieur à 0");
        }

        if (course.getDateDepart() == null || course.getDateDepart().before(new java.util.Date())) {
            throw new IllegalArgumentException("La date de départ doit être dans le futur");
        }

        if (course.getVille() == null || course.getVille().trim().isEmpty()) {
            throw new IllegalArgumentException("La ville est obligatoire");
        }

        if (course.getPrixParticipation() < 0) {
            throw new IllegalArgumentException("Le prix de participation ne peut pas être négatif");
        }

        courseRepository.save(course);
        return course;
    }

    @Override
    public Optional<Course> getCourseById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        return courseRepository.findById(id);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public List<Course> getCoursesByOrgaId(int idOrga) {
        return courseRepository.findByOrgaId(idOrga);
    }

    @Override
    public List<Course> getRecentCourses(int limit) {
        return courseRepository.GetRecentCourses(limit);
    }

    @Override
    public Course updateCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("La course ne peut pas être null");
        }

        if (course.getNom() == null || course.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la course est obligatoire");
        }

        if (course.getDistance() <= 0) {
            throw new IllegalArgumentException("La distance de la course doit être supérieure à 0");
        }

        if (course.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("Le nombre maximum de participants doit être supérieur à 0");
        }

        if (course.getDateDepart() == null || course.getDateDepart().before(new java.util.Date())) {
            throw new IllegalArgumentException("La date de départ doit être dans le futur");
        }

        if (course.getVille() == null || course.getVille().trim().isEmpty()) {
            throw new IllegalArgumentException("La ville est obligatoire");
        }

        if (course.getPrixParticipation() < 0) {
            throw new IllegalArgumentException("Le prix de participation ne peut pas être négatif");
        }

        if (course.getIdCourse() <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        Optional<Course> existingCourse = courseRepository.findById(course.getIdCourse());
        if (existingCourse.isEmpty()) {
            throw new IllegalArgumentException("Course non trouvée");
        }

        courseRepository.update(course);
        return course;
    }

    @Override
    public boolean deleteCourse(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        try {
            courseRepository.delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
