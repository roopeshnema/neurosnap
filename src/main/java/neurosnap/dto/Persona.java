package neurosnap.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Persona represents a synthetic borrower profile used for
 * refinance recommendation demos.
 *
 * Notes:
 * - This is NOT real customer data. All values are mock/synthetic.
 * - Used to simulate borrower journeys during Hackfest and testing.
 * - Fields should map closely to real-world credit/loan features,
 *   but without exposing any PII.
 */
@Getter
@Setter
public class Persona {

    /** Unique identifier for the persona (e.g., "P_EARLY_PAYER"). */
    private String personaId;

    /** Display name of the persona (e.g., "Emily the Early Payer"). */
    private String personaName;

    /** Annual income of the persona (in USD). */
    private String income;

    /** Narrative of how the borrower behaves with payments (synthetic label). */
    private String paymentBehavior;

    /** Past refinance experience description (e.g., "No experience", "2 times"). */
    private String refiExperience;

    /** Credit score (synthetic, 300–850 range). */
    private int creditScore;

    /** Original loan amount taken (in USD). */
    private double existingLoanAmount;

    /** Existing loan’s interest rate (annual %). */
    private double existingInterestRate;

    /** Pending (outstanding) loan balance (in USD). */
    private double existingPendingAmount;

    /** Payment history band (DISCIPLINED, MOSTLY_DISCIPLINED, IRREGULAR). */
    private String paymentHistory;

    /** Date of birth (synthetic, formatted dd-MM-yyyy). */
    private String dob;

    /** Synthetic SSN for demo purposes (never real). */
    private int ssn;

    /** Synthetic mobile number (dummy, masked for demo). */
    private long mobileNumber;

    /** Verification code (OTP simulation). */
    private int verificationCode;

    /** Remaining tenure of the existing loan (in months). */
    private int existingTenure;

    /** Current monthly EMI (in USD). */
    private double existingEmi;

    /** Bank name (synthetic, e.g., "Mock National Bank"). */
    private String bankName;

    /** Synthetic card number (dummy, never real). */
    private long cardNumber;

    /** Minimum refinance amount required (covers pending + fees). */
    private double minimumRefinanceAmt;
}
