package neurosnap.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.*;

/**
 * RecommendRequest represents the input payload sent by the frontend/UI to the Refinance Recommendation API.
 * Fields: - loanAmount: The refinance amount requested (must be >= existing pending + fees). - tenure: Desired loan tenure in months (e.g., 6, 12). - frequency: Repayment frequency (MONTHLY or BIWEEKLY).
 * Notes: - Validation must enforce loanAmount range (300–5000 USD for microfinance). - Tenure should typically be 6 or 12 months for Hackfest demo. - Frequency is optional, but if provided, only two valid values should exist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest
{

    /** Requested refinance loan amount (USD). */
    @JsonAlias( { "loan_amount" } )
    private long loanAmount;

    /** Desired loan tenure in months (valid values: 6 or 12 for Hackfest). */
    private int tenure;

    /** Repayment frequency (MONTHLY or BIWEEKLY). */
    private RepaymentFrequency frequency;

    /**
     * Enum for repayment frequency options.
     */
    public enum RepaymentFrequency
    {
        MONTHLY,
        BIWEEKLY;

        @JsonCreator
        public static RepaymentFrequency from( String v )
        {
            if ( v == null )
                return null;
            String s = v.trim().toUpperCase();
            switch ( s )
            {
                case "MONTHLY":
                    return MONTHLY;
                case "BIWEEKLY":
                    return BIWEEKLY;
                default:
                    throw new IllegalArgumentException( "Unsupported frequency: " + v );
            }
        }

        @JsonValue
        public String toJson()
        {
            return name();
        }
    }
}