package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.VerificationRepository;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.repository.impl.VerificationRepositoryImpl;
import fr.esgi.color_run.service.VerificationService;

import java.sql.Timestamp;
import java.time.Instant;

public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final ParticipantRepository participantRepository;

    public VerificationServiceImpl() {
        this.verificationRepository = new VerificationRepositoryImpl();
        this.participantRepository = new ParticipantRepositoryImpl();
    }

    @Override
    public Verification creerVerification(Verification verification) {
        if (verification == null) {
            throw new IllegalArgumentException("La vérification ne peut pas être null");
        }

        // Vérifier si le participant n'à pas déjà une vérification en cours
        Participant participant = verification.getParticipant();
        if (participant == null || participant.getIdParticipant() <= 0) {
            throw new IllegalArgumentException("Le participant doit être valide");
        }

        Verification existingVerification = verificationRepository.findByParticipantId(participant.getIdParticipant());
        if (existingVerification != null) {
            // Si une vérification existe déjà, on la supprime
            verificationRepository.deleteByParticipantId(participant.getIdParticipant());
        }

        return verificationRepository.save(verification);
    }

    @Override
    public boolean verifierCode(String code, Participant participant) {
        if (code == null || participant == null) {
            throw new IllegalArgumentException("Le code et l'email ne peuvent pas être null");
        }

        // Vérifier si le code de vérification correspond à celui du participant
        // Parmi toute les vérifications, on va chercher celle qui correspond à l'email du participant (donc à son id)
        Verification verif = verificationRepository.findByParticipantId(participant.getIdParticipant());

        if (verif != null) {
            if (!verif.getCode().equals(code)) {
                return false;
            }

            // Vérifier si la date de vérification est dépassée
            if (verif.getDateTimeCompleted() != null && verif.getDateTimeCompleted().before(Timestamp.from(Instant.now()))) {
                verificationRepository.deleteByParticipantId(participant.getIdParticipant());
                return false;
            }

            // Si le code est correct, on met à jour le participant pour le marquer comme vérifié et on supprime la vérification
            participant.setEstVerifie(true);
            participantRepository.update(participant);
            verificationRepository.deleteByParticipantId(participant.getIdParticipant());
            return true;
        }
        return false;
    }
}
