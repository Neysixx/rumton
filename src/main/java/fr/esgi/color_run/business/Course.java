package fr.esgi.color_run.business;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Course {
    private int idCourse;
    private Cause cause; // Référence à Cause
    private Participant organisateur; // Référence à Participant

    private String nom;
    private String description;
    private Date dateDepart;
    private String ville;
    private int codePostal;
    private String adresse;
    private float distance;
    private int maxParticipants;
    private float prixParticipation;
    private String obstacles;

    private List<Participation> participations; // Liste des participations
}