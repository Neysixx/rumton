package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Participation;
import fr.esgi.color_run.repository.ParticipationRepository;
import fr.esgi.color_run.repository.impl.ParticipationRepositoryImpl;
import fr.esgi.color_run.service.ParticipationService;
import fr.esgi.color_run.util.DebugUtil;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des participations
 */
public class ParticipationServiceImpl implements ParticipationService {

    private final ParticipationRepository participationRepository;

    public ParticipationServiceImpl() {
        this.participationRepository = new ParticipationRepositoryImpl();
    }

    @Override
    public Participation createParticipation(Participation participation) {
        if (participation == null) {
            throw new IllegalArgumentException("La participation ne peut pas être null");
        }

        if (participation.getParticipant() == null) {
            throw new IllegalArgumentException("Le participant est obligatoire");
        }

        if (participation.getCourse() == null) {
            throw new IllegalArgumentException("La course est obligatoire");
        }

        if (participation.getNumeroDossard() <= 0) {
            throw new IllegalArgumentException("Le numéro de dossard doit être positif");
        }

        // Vérifie si le participant est déjà inscrit
        if (isParticipantRegistered(participation.getParticipant().getIdParticipant(),
                participation.getCourse().getIdCourse())) {
            throw new IllegalArgumentException("Ce participant est déjà inscrit à cette course");
        }

        // Vérifie si la course n'est pas complète
        int courseId = participation.getCourse().getIdCourse();
        int maxParticipants = participation.getCourse().getMaxParticipants();
        int currentParticipants = getCountForCourse(courseId);

        if (currentParticipants >= maxParticipants) {
            throw new IllegalArgumentException("Cette course est complète");
        }

        return participationRepository.save(participation);
    }

    @Override
    public Optional<Participation> getParticipationById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de participation invalide");
        }

        return participationRepository.findById(id);
    }

    @Override
    public List<Participation> getAllParticipations() {
        return participationRepository.findAll();
    }

    @Override
    public List<Participation> getParticipationsByParticipant(int participantId) {
        if (participantId <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }

        return participationRepository.findByParticipantId(participantId);
    }

    @Override
    public List<Participation> getParticipationsByCourse(int courseId) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        return participationRepository.findByCourseId(courseId);
    }

    @Override
    public int getCountForCourse(int courseId) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        return participationRepository.countByCourseId(courseId);
    }

    @Override
    public boolean isParticipantRegistered(int participantId, int courseId) {
        if (participantId <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }

        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        return participationRepository.existsByParticipantIdAndCourseId(participantId, courseId);
    }

    @Override
    public int getParticipationIdByCourseAndParticipant(int courseId, int participantId) {
        if (participantId <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }

        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        return participationRepository.getParticipationIdByCourseAndParticipant(participantId, courseId);
    }

    @Override
    public int getLastBibNumberForCourse(int courseId) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("ID de course invalide");
        }

        Integer lastNumber = participationRepository.findMaxBibNumberByCourseId(courseId);
        return lastNumber != null ? lastNumber : 0;
    }

    @Override
    public Participation updateParticipation(Participation participation) {
        if (participation == null) {
            throw new IllegalArgumentException("La participation ne peut pas être null");
        }

        if (participation.getIdParticipation() <= 0) {
            throw new IllegalArgumentException("ID de participation invalide");
        }

        Optional<Participation> existingPart = participationRepository.findById(participation.getIdParticipation());
        if (existingPart.isEmpty()) {
            throw new IllegalArgumentException("Participation non trouvée");
        }

        participationRepository.update(participation);
        return participation;
    }

    @Override
    public boolean deleteParticipation(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de participation invalide");
        }

        try {
            participationRepository.delete(id);
            return true;
        } catch (Exception e) {
            DebugUtil.log(this.getClass(), "Erreur lors de la suppression de la participation: " + e.getMessage());
            return false;
        }
    }
}
