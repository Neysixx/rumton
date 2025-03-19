package fr.esgi.color_run.business;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cause {
    private int idCause;
    private String intitule;
    private List<Course> courses; // Liste des courses associées à cette cause
}