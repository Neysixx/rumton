package fr.esgi.color_run.business;

import fr.esgi.color_run.enums.UserRole;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public abstract class User {
    @Getter
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse;
    private String urlProfile;
    @Getter
    private UserRole role;
    
    public String getUsername() {
        return email;
    }
}
