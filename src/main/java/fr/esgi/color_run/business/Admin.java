package fr.esgi.color_run.business;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
    private int idAdmin;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String urlProfile;
}