package neurosnap.dto.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * IncomeRule defines how the customer's income band affects tenure adjustments.
 *
 * Examples:
 *  - LOW:   extend tenure (+12 months) for affordability.
 *  - MEDIUM: keep tenure unchanged (0 months).
 *  - HIGH:  reduce tenure (-12 months) to incentivize faster closure.
 *
 * Notes for engine:
 *  - Income band is derived from persona (salary/income).
 *  - Tenure adjustments should always be clamped within valid loan ranges.
 *  - This complements GoalRule (both applied cumulatively).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeRule {

    /**
     * Income band (LOW, MEDIUM, HIGH).
     * Using enum for type-safety and validation.
     */
    private IncomeBand incomeBand;

    /**
     * Number of months to add/subtract based on income band.
     * Example: +12 for LOW, 0 for MEDIUM, -12 for HIGH.
     */
    private int tenureDelta;

    /**
     * Human-friendly explanation for UI/AI explainability.
     * Example: "Low income detected → extended tenure to ease repayment pressure."
     */
    private String reason;

    /**
     * Enum for income categories, aligned with rules.xlsx config.
     */
    public enum IncomeBand {
        LOW, MEDIUM, HIGH;

        @JsonCreator
        public static IncomeBand from(String v) {
            if (v == null) return null;
            String s = v.trim().toUpperCase();
            switch (s) {
                case "LOW":    return LOW;
                case "MEDIUM": return MEDIUM;
                case "HIGH":   return HIGH;
                default: throw new IllegalArgumentException("Unsupported income band: " + v);
            }
        }

        @JsonValue
        public String toJson() {
            return name();
        }
    }
}

