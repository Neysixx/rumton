package fr.esgi.color_run.business;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    private int idMessage;
    private Participant emetteur; // Référence à Participant
    private Course course; // Référence à Course
    private Message messageParent; // Référence à un autre message (optionnel)
    private String contenu;
    private Date datePublication;
}