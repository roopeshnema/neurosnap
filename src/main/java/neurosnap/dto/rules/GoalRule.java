package neurosnap.dto.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * GoalRule defines how a refinance goal changes the loan tenure.
 *
 * Examples:
 *  - LOWER_EMI:       extend tenure (e.g., +12 months) to reduce monthly burden.
 *  - BALANCED:        keep tenure unchanged (0 months).
 *  - FASTER_CLOSURE:  reduce tenure (e.g., -12 months) to close faster and save interest.
 *
 * Notes for engine:
 *  - The engine should clamp the final tenure to supported values (e.g., min 6 months).
 *  - Use this rule as an additive delta on top of the requested/base tenure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalRule {

    /**
     * The refinance goal (enum for type-safety).
     * JSON is accepted case-insensitively (e.g., "lower_emi", "LOWER_EMI").
     */
    private GoalType goal;

    /**
     * Number of months to add/subtract from tenure for this goal.
     * Examples: +12 for LOWER_EMI, 0 for BALANCED, -12 for FASTER_CLOSURE.
     */
    private int tenureDelta;

    /**
     * Short, human-friendly explanation to support UI/AI explainability.
     * Example: "Extended tenure to lower EMI and improve affordability."
     */
    private String reason;

    /**
     * Enum representing supported refinance goals.
     * Includes JSON helpers to be resilient to input casing/format.
     */
    public enum GoalType {
        LOWER_EMI,
        BALANCED,
        FASTER_CLOSURE;

        @JsonCreator
        public static GoalType from(String v) {
            if (v == null) return null;
            String s = v.trim().toUpperCase().replace(' ', '_');
            switch (s) {
                case "LOWER_EMI":       return LOWER_EMI;
                case "BALANCED":        return BALANCED;
                case "FASTER_CLOSURE":  return FASTER_CLOSURE;
                default: throw new IllegalArgumentException("Unsupported goal: " + v);
            }
        }

        @JsonValue
        public String toJson() {
            return name();
        }
    }
}