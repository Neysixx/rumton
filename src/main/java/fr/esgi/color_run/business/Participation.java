package fr.esgi.color_run.business;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {
    private int idParticipation;
    private Participant participant; // Référence à Participant
    private Course course; // Référence à Course
    private int numeroDossard;
    private Date dateReservation;
}
