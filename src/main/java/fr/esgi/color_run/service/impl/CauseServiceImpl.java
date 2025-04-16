package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Cause;
import fr.esgi.color_run.repository.CauseRepository;
import fr.esgi.color_run.repository.impl.CauseRepositoryImpl;
import fr.esgi.color_run.service.CauseService;

import java.util.List;
import java.util.Optional;

public class CauseServiceImpl implements CauseService {

    private final CauseRepository causeRepository;

    public CauseServiceImpl() {
        this.causeRepository = new CauseRepositoryImpl();
    }

    @Override
    public Cause createCause(Cause cause) {
        if (cause == null) {
            throw new IllegalArgumentException("La cause ne peut pas être null");
        }

        if (cause.getIntitule() == null || cause.getIntitule().trim().isEmpty()) {
            throw new IllegalArgumentException("L'intitulé de la cause est obligatoire");
        }

        causeRepository.save(cause);
        return cause;
    }

    @Override
    public Optional<Cause> getCauseById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de cause invalide");
        }

        return causeRepository.findById(id);
    }

    @Override
    public List<Cause> getAllCauses() {
        return causeRepository.findAll();
    }

    @Override
    public Cause updateCause(Cause cause) {
        if (cause == null) {
            throw new IllegalArgumentException("La cause ne peut pas être null");
        }

        if (cause.getIdCause() <= 0) {
            throw new IllegalArgumentException("ID de cause invalide");
        }

        Optional<Cause> existingCause = causeRepository.findById(cause.getIdCause());
        if (existingCause.isEmpty()) {
            throw new IllegalArgumentException("Cause non trouvée");
        }

        causeRepository.update(cause);
        return cause;
    }

    @Override
    public boolean deleteCause(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de cause invalide");
        }

        try {
            causeRepository.delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}