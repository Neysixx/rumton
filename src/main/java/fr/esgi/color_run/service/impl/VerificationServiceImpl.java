package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;
import fr.esgi.color_run.repository.VerificationRepository;
import fr.esgi.color_run.repository.impl.VerificationRepositoryImpl;
import fr.esgi.color_run.service.VerificationService;

public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;

    public VerificationServiceImpl() {
        this.verificationRepository = new VerificationRepositoryImpl();
    }

    @Override
    public Verification creerVerification(Verification verification) {
        if (verification == null) {
            throw new IllegalArgumentException("La vérification ne peut pas être null");
        }

        return verificationRepository.save(verification);
    }

    @Override
    public boolean verifierCode(String code, Participant participant) {
        if (code == null || participant == null) {
            throw new IllegalArgumentException("Le code et l'email ne peuvent pas être null");
        }

        // Vérifier si le code de vérification correspond à celui du participant
        // Parmis toute les vérifications, on va chercher celle qui correspond à l'email du participant (donc à son id)
        boolean check = verificationRepository .verifierCode(code, participant);
        if (check) {
            // Si le code est correct, on met à jour le participant pour le marquer comme vérifié et on supprime la vérification
            participant.setEstVerifie(true);
            verificationRepository.deleteByParticipantId(participant.getIdParticipant());
        }
        return check;
    }
}
