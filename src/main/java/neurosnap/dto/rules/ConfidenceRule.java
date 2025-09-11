package neurosnap.dto.rules;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfidenceRule
{

    private ConfidenceLevel confidence;  // The evaluated confidence category

    private double rateDelta;            // Numeric adjustment to APR

    private String reason;               // Why this adjustment was applied (for AI explainability)

    public ConfidenceRule( ConfidenceLevel confidence, double rateDelta, String reason )
    {
        this.confidence = confidence;
        this.rateDelta = rateDelta;
        this.reason = reason;
    }

    public enum ConfidenceLevel
    {
        HIGH,      // Borrower is very reliable (e.g., strong credit history, high score).
        MEDIUM,    // Borrower is average risk (some late payments but overall okay).
        LOW        // Borrower is high risk (irregular payments, low score).
    }
}




