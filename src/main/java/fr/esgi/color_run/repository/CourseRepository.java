package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Course;

import java.util.*;

public interface CourseRepository {
    void save(Course course);
    Optional<Course> findById(int id);
    List<Course> findAll();
    List<Course> findByOrgaId(int orgaId);
    List<Course> findRecentCourses(int limit);
    List<Course> findCoursesByCauseId(int causeId);
    void update(Course course);
    void delete(int id);
}