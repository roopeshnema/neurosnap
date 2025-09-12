package neurosnap.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * RecommendOption represents a single refinance plan
 * returned by the recommendation engine.
 *
 * Each persona should get exactly 3 recommendations:
 *   - LOWER_EMI
 *   - BALANCED
 *   - FASTER_CLOSURE
 *
 * Fields are designed for both backend processing and
 * frontend/UI display during Hackfest demos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonPropertyOrder({
        "planId", "goal", "principal","interestRate","tenure","emi",
        "savingsPerMonth", "totalSavings",
        "breakEvenMonths", "confidence", "best", "reason"
})
public class RecommendOption {

    /** Unique identifier for this plan (e.g., "REFI1"). */
    private String planId;

    /** Refinance goal (LOWER_EMI, BALANCED, FASTER_CLOSURE). */
    private GoalType goal;

    /** Monthly EMI for this plan (USD). */
    private double emi;

    /** Loan tenure (in months). */
    private int tenure;

    /** Annual interest rate (APR %) for this plan. */
    private double interestRate;

    /** Monthly savings compared to current EMI (USD). */
    private double savingsPerMonth;

    /** Total lifetime savings over the loan (USD). */
    private double totalSavings;

    /** Processing/closing fees applied (USD). */
    //private double fees;

    /** Breakeven period in months (fees ÷ savingsPerMonth). */
    private int breakEvenMonths;

    /** Confidence score (0–100) assigned by AI/logic engine. */
    private double confidence;

    /** Flag indicating if this plan is the best-fit recommendation. */
    private boolean best;

    /** Human-friendly explanation of why this plan was recommended. */
    private String reason;

    private double totalLoanAmount;

    private double disburseAmount;

    private double principal;

    /**
     * Enum for refinance goals (ensures only valid values are used).
     */
    public enum GoalType {
        LOWER_EMI,
        BALANCED,
        FASTER_CLOSURE;

        @JsonCreator
        public static GoalType from(String v) {
            if (v == null) return null;
            String s = v.trim().toUpperCase().replace(" ", "_");
            switch (s) {
                case "LOWER_EMI": return LOWER_EMI;
                case "BALANCED": return BALANCED;
                case "FASTER_CLOSURE": return FASTER_CLOSURE;
                default: throw new IllegalArgumentException("Unsupported goal: " + v);
            }
        }

        @JsonValue
        public String toJson() {
            return name();
        }
    }
}
