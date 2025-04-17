package fr.esgi.color_run.service.impl;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.repository.ParticipantRepository;
import fr.esgi.color_run.repository.impl.ParticipantRepositoryImpl;
import fr.esgi.color_run.service.ParticipantService;

import java.util.List;
import java.util.Optional;

public class ParticipantServiceImpl implements ParticipantService {

    private final ParticipantRepository participantRepository;

    public ParticipantServiceImpl() {
        this.participantRepository = new ParticipantRepositoryImpl();
    }

    @Override
    public Participant creerParticipant(Participant participant) {
        if (participant == null) {
            throw new IllegalArgumentException("Le participant ne peut pas être null");
        }

        if (existsByEmail(participant.getEmail())) {
            throw new IllegalArgumentException("Un participant avec cet email existe déjà");
        }

        return participantRepository.save(participant);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return participantRepository.findByEmail(email).isPresent();
    }

    @Override
    public Participant getParticipantById(int id) {
        return participantRepository.findById(id).orElse(null);
    }

    @Override
    public Participant getParticipantByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return participantRepository.findByEmail(email).orElse(null);
    }

    @Override
    public List<Participant> getAllParticipants() {
        return participantRepository.findAll();
    }

    @Override
    public Participant updateParticipant(Participant participant) {
        if (participant == null) {
            throw new IllegalArgumentException("Le participant ne peut pas être null");
        }

        if (participant.getIdParticipant() <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }

        Optional<Participant> existingParticipant = participantRepository.findById(participant.getIdParticipant());
        if (existingParticipant.isEmpty()) {
            throw new IllegalArgumentException("Participant non trouvé");
        }

        participantRepository.update(participant);
        return participant;
    }

    @Override
    public boolean deleteParticipant(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de participant invalide");
        }

        try {
            participantRepository.delete(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Participant authentifier(String email, String motDePasse) {
        if (email == null || email.trim().isEmpty() || motDePasse == null || motDePasse.trim().isEmpty()) {
            return null;
        }

        Optional<Participant> participantOpt = participantRepository.findByEmail(email);
        if (participantOpt.isPresent() && participantOpt.get().getMotDePasse().equals(motDePasse)) {
            return participantOpt.get();
        }
        return null;
    }
}