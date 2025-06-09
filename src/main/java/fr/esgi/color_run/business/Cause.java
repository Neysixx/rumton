package fr.esgi.color_run.business;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Cause {
    private int idCause;
    private String intitule;
    private List<Course> courses = new ArrayList<>();


}
