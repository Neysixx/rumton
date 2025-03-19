package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Cause;
import java.util.List;
import java.util.Optional;

public interface CauseRepository {
    void save(Cause cause);
    Optional<Cause> findById(int id);
    List<Cause> findAll();
    void update(Cause cause);
    void delete(int id);
}
