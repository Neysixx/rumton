package fr.esgi.color_run.service;

import fr.esgi.color_run.business.Participant;
import fr.esgi.color_run.business.Verification;

public interface VerificationService {
    Verification creerVerification(Verification verification);
    boolean verifierCode(String code, Participant participant);
}
