package neurosnap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@JsonPropertyOrder({ "planId", "goal", "emi", "tenure", "interestRate", "savingsPerMonth", "totalSavings", "fees", "breakEvenMonths", "confidence", "isBest", "reason" })
public class RecommendOption {
    private String planId;
    private String goal;
    private int emi;
    private int tenure;
    private double interestRate;
    private double savingsPerMonth;
    private double totalSavings;
    private double fees;
    private int breakEvenMonths;
    private double confidence;
    @JsonProperty("isBest")
    private boolean isBest;
    private String reason;

}
