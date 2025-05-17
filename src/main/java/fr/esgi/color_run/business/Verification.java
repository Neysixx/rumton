package fr.esgi.color_run.business;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.print.attribute.standard.DateTimeAtCompleted;
import javax.print.attribute.standard.DateTimeAtCreation;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verification {
    private int idVerification;
    private Participant participant;
    private String code;
    private Timestamp dateTime;
    private Timestamp dateTimeCompleted;
}
