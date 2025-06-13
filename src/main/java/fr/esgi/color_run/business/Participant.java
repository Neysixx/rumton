package fr.esgi.color_run.business;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {
    private int idParticipant;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String urlProfile;
    private boolean estOrganisateur;
    private Date dateCreation;
    private boolean estVerifie;

    private List<Participation> participations;

    public String getUrlProfile() {
        return urlProfile != null ? urlProfile : "/color_run_war_exploded/assets/img/defaultProfile.png";
    }
}