package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Course;
import fr.esgi.color_run.repository.CourseRepository;
import fr.esgi.color_run.repository.impl.CourseRepositoryImpl;
import fr.esgi.color_run.service.CourseService;

import java.util.*;
import java.util.stream.Collectors;

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
        return courseRepository.findRecentCourses(limit);
    }

    @Override
    public List<Course> getCoursesByCauseId(int causeId) {
        return courseRepository.findCoursesByCauseId(causeId);
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
    @Override
    public List<Course> searchCourses(Date date, String ville, Float distance, String sortBy) {
        return getAllCourses().stream()
                .filter(c -> date == null || isSameDay(c.getDateDepart(), date))
                .filter(c -> ville == null || c.getVille().equalsIgnoreCase(ville))
                .filter(c -> distance == null || Float.compare(c.getDistance(), distance) == 0)
                .sorted(getComparator(sortBy))
                .collect(Collectors.toList());
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private Comparator<Course> getComparator(String sortBy) {
        if (sortBy == null) return Comparator.comparing(Course::getIdCourse);
        String key = sortBy.toLowerCase();
        if ("date".equals(key)) {
            return Comparator.comparing(Course::getDateDepart);
        } else if ("ville".equals(key)) {
            return Comparator.comparing(Course::getVille, String.CASE_INSENSITIVE_ORDER);
        } else if ("distance".equals(key)) {
            return Comparator.comparing(Course::getDistance);
        } else {
            return Comparator.comparing(Course::getIdCourse);
        }
    }
}
