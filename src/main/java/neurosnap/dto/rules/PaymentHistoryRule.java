package neurosnap.dto.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * PaymentHistoryRule defines how the customer's repayment behavior
 * affects their refinance interest rate (APR adjustment).
 *
 * Examples:
 *  - IRREGULAR:          +1.5% risk penalty on APR
 *  - MOSTLY_DISCIPLINED: +0.5% slight penalty
 *  - DISCIPLINED:        0% adjustment (base rate applies)
 *
 * Notes for engine:
 *  - This rule works alongside IncomeRule and GoalRule.
 *  - rateDelta is applied additively to the base APR.
 *  - Explanations should be shown in the UI ("Your irregular payment
 *    history increased the interest rate by 1.5%").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryRule {

    /**
     * Customer's payment history band.
     * Enum ensures only valid values are used.
     */
    private PaymentHistoryType paymentHistory;

    /**
     * Adjustment (delta) applied to the APR based on history.
     * Example: +1.5 for IRREGULAR, +0.5 for MOSTLY_DISCIPLINED, 0 for DISCIPLINED.
     */
    private double rateDelta;

    /**
     * Human-friendly explanation to support UI/AI explainability.
     * Example: "Your irregular payments added 1.5% risk adjustment."
     */
    private String reason;

    /**
     * Enum for repayment history categories, aligned with rules.xlsx config.
     */
    public enum PaymentHistoryType {
        IRREGULAR,
        MOSTLY_DISCIPLINED,
        DISCIPLINED;

        @JsonCreator
        public static PaymentHistoryType from(String v) {
            if (v == null) return null;
            String s = v.trim().toUpperCase().replace(" ", "_");
            switch (s) {
                case "IRREGULAR":           return IRREGULAR;
                case "MOSTLY_DISCIPLINED":  return MOSTLY_DISCIPLINED;
                case "DISCIPLINED":         return DISCIPLINED;
                default: throw new IllegalArgumentException("Unsupported payment history: " + v);
            }
        }

        @JsonValue
        public String toJson() {
            return name();
        }
    }
}


