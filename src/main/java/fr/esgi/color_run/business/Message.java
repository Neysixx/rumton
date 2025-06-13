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
    private String datePublicationStr;
    
    /**
     * Méthode toString personnalisée pour éviter les références circulaires
     * et faciliter le débogage
     */
    @Override
    public String toString() {
        return "{" +
                "\"idMessage\":" + idMessage +
                ", \"emetteur\":{\"id\":" + (emetteur != null ? emetteur.getIdParticipant() : "null") + 
                ", \"nom\":\"" + (emetteur != null ? emetteur.getNom() : "") + 
                "\", \"prenom\":\"" + (emetteur != null ? emetteur.getPrenom() : "") + "\"}" +
                ", \"course\":{\"id\":" + (course != null ? course.getIdCourse() : "null") + 
                ", \"nom\":\"" + (course != null ? course.getNom() : "") + "\"}" +
                ", \"messageParentId\":" + (messageParent != null ? messageParent.getIdMessage() : "null") +
                ", \"contenu\":\"" + contenu + "\"" +
                ", \"datePublication\":\"" + datePublication + "\"" +
                '}';
    }
}