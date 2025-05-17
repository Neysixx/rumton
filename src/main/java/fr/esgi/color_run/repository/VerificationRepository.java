package fr.esgi.color_run.repository;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;

import java.util.List;
import java.util.Optional;

public interface VerificationRepository {

    Verification save(Verification verification);

    Optional<Verification> findById(int id);

    Verification findByParticipantId(int id);

    List<Verification> findAll();

    void delete(int id);

    void deleteByParticipantId(int participantId);
}

