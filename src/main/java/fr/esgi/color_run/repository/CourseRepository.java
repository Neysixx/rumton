package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Course;

import java.util.*;

public interface CourseRepository {
    void save(Course course);
    Optional<Course> findById(int id);
    List<Course> findAll();
    void update(Course course);
    void delete(int id);
}