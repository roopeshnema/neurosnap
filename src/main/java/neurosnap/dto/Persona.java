package neurosnap.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Persona
{
    private String personaId;
    private String personaName;
    private double income;
    private String paymentBehavior;
    private String refiExperience;
    private int creditScore;
    private double existingLoanAmount;
    private double existingInterestRate;
    private double existingPendingAmount;
    private String paymentHistory;
    private String dob;
    private int ssn;
}
