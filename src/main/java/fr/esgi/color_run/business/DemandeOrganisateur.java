package fr.esgi.color_run.business;

import lombok.*;

import java.util.Date;
import java.util.Optional;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeOrganisateur {
    private int idDemande;
    private Participant participant; // Référence à Participant
    private String motivations;
    private Optional<Admin> traitePar = Optional.empty(); // Admin optionnel
    private String status;
    private Boolean reponse;
    private Date dateCreation;
    private Optional<Date> dateTraitement = Optional.empty(); // Date optionnelle
}