package neurosnap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import neurosnap.client.ChatGptClient;
import neurosnap.dto.Persona;
import neurosnap.dto.RecommendOption;
import neurosnap.dto.RecommendOptionsResponse;
import neurosnap.dto.RecommendRequest;
import neurosnap.dto.rules.AprScoreRule;
import neurosnap.dto.rules.ConfidenceRule;
import neurosnap.dto.rules.IncomeRule;
import neurosnap.dto.rules.PaymentHistoryRule;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService
{
    private final PersonaReaderService personaReaderService;

    private final RulesReaderService rulesReaderService;

    private final ChatGptClient chatGptClient;

    private final double processingFee = 100;
    private final double taxFee = processingFee / 10;

    public RecommendationService( PersonaReaderService personaReaderService, RulesReaderService rulesReaderService, ChatGptClient chatGptClient )
    {
        this.personaReaderService = personaReaderService;
        this.rulesReaderService = rulesReaderService;
        this.chatGptClient = chatGptClient;
    }

    public RecommendOptionsResponse getRecommendations(RecommendRequest request, String personaId ) throws Exception
    {

        Optional<Persona> personaResult = personaReaderService.getPersona( personaId );
        Persona persona = personaResult.get();

        if(persona.getExistingPendingAmount() > (persona.getExistingLoanAmount() / 2)) {
            throw new BadRequestException( "You need to serve the minimum tenure on your existing loan before you can apply for refinancing." ) ;
        }

        return generatePlansUsingAI( persona, request);
    }

    public RecommendOptionsResponse getExamples(RecommendRequest request, String personaId ) throws Exception
    {
        return populateRecommendOptionsResponse();
    }

    private RecommendOptionsResponse populateRecommendOptionsResponse() {
        RecommendOptionsResponse response = new RecommendOptionsResponse();
        response.setPersonaId("P_EARLY_PAYER");
        response.setModelVersion("v1.0.0");
        RecommendOption option1 = new RecommendOption(
                "REFI1", RecommendOption.GoalType.LOWER_EMI, 18000, 72, 5.9, 1200, 14400,
                  82,6,true,
                 "Because you always pay early and have strong credit, you qualify for a lower EMI with minimal risk.",
                1000,1500,4000
        );
        RecommendOption option2 = new RecommendOption(
                "REFI2", RecommendOption.GoalType.BALANCED,  20000,  60,  6.5,  0,  0,
                 74,6,  false,
                "Balanced plan keeps your payments stable while slightly reducing total interest.",
                2000,1000,3000
        );
        RecommendOption option3 = new RecommendOption(
               "REFI3", RecommendOption.GoalType.FASTER_CLOSURE, 22000,  48,  5.5,  1000,  12000,  68,6, false,
                "Higher EMI but lets you close the loan faster and save $50,000 in interest.",
                3000, 1500,2500
        );
        RecommendOption[] optionArray =  new RecommendOption[] { option1, option2, option3 };
        response.setRecommendations(optionArray);
       return response;
    }

    public RecommendOptionsResponse generatePlansUsingAI(Persona persona, RecommendRequest request) throws Exception
    {

        List<RecommendOption> plans = new ArrayList<>();

        // Calculating Fee
        double fees = processingFee + taxFee;

        // Validating user input amount with minRequired amount.
        double minRequired = persona.getExistingPendingAmount() + fees;
        if (request.getLoanAmount() < minRequired) {
            throw new BadRequestException("Requested amount must cover pending balance + fees (min $" + Math.round(minRequired) + ")");
        }

        double principal = request.getLoanAmount();

        // normalizeTenure
        int baseTenure = normalizeTenure(request.getTenure());

        // Calculate APR
        double apr = baseApr(persona);

        ObjectMapper mapper = new ObjectMapper();
        String personaJson = mapper.writeValueAsString(persona);

        Map<String, IncomeRule> incomerules = rulesReaderService.getIncomeRules();
        Map<String, ConfidenceRule> confidenceRules = rulesReaderService.getConfidenceRules();
        Map<String, PaymentHistoryRule> paymentHistoryRules = rulesReaderService.getPaymentHistoryRules();
        Map<String, AprScoreRule> aprScoreRules = rulesReaderService.getAprScoreRules();

        String incomeRulesJson = mapper.writeValueAsString(incomerules);
        String paymentHistoryRuleJson = mapper.writeValueAsString(paymentHistoryRules);
        String confidenceRulesJson = mapper.writeValueAsString(confidenceRules);
        String aprScoreRulesJson = mapper.writeValueAsString(aprScoreRules);

        String prompt =
            "You are a financial assistant. Provide exactly 3 refinance options (LOWER_EMI, FASTER_CLOSURE, BALANCED) using strict math and numeric reasoning. " +
                    "Do not guess. Use the exact formulas with numbers. Follow all instructions exactly.\n\n" +
                    "GOALS:\n" +
                    "1. LOWER_EMI: Use the longest tenure (within allowed limits, e.g. 12 months), even if total interest paid is more.\n" +
                    "2. FASTER_CLOSURE: Use the shortest tenure possible (minimum allowed, e.g. 6 months) to minimize total interest, even if EMI is high.\n" +
                    "3. BALANCED: Choose a tenure and EMI between LOWER_EMI and FASTER_CLOSURE, balancing monthly payment and interest cost.\n\n" +

                    "INPUT DATA:\n" +
                    "Principal: " + (principal-110)+ "\n" +
                    "Allowed tenure range: 6 to 12 months\n\n" +
                    "input :" + request +
                    "persona :" + personaJson +
                    "tenure : " + baseTenure +
                    // "interestRate :" + apr +
                    "rules :" +  incomeRulesJson + paymentHistoryRuleJson + confidenceRulesJson + aprScoreRulesJson  +
                    " Existing emi: "+persona.getExistingEmi()+" and remaining tenure as 6 \n" +
                    "FORMULAS AND CALCULATIONS (must apply with numbers):\n" +
                    "- Monthly interest rate R = APR / (12 × 100)\n" +
                    "- EMI = [P × R × (1 + R)^N] / [(1 + R)^N − 1]  (round to 2 decimals)\n" +
                    "- TotalLoanAmount = EMI × N  (round to 2 decimals)\n" +
                    "- TotalInterest = TotalLoanAmount − Principal  (round to 2 decimals)\n" +
                    "- SavingsPerMonth = ExistingEMI − PlanEMI\n" +
                    "  -Do NOT compute totalSavings as savingsPerMonth × tenure unless savingsPerMonth > 0.\n" +
                    "IMPORTANT CONSTRAINTS:\n" +
                    "- Ensure TotalLoanAmount >= Principal or else correct EMI/tenure so that repayment covers principal + interest.\n" +
                    "- If SavingsPerMonth is negative (because EMI > ExistingEMI), that's okay, but TotalSavings should reflect interest savings or cost correctly. \n" +
                    "- Do NOT use placeholders like \"$$\" or \"Computed\". Use real numbers everywhere.\n" +
                    "- Round all monetary values to 2 decimal places.\n\n" +
                    " Do not estimate or guess EMI. Use the EMI formula explicitly with math. Calculate using actual values.\n" +
                    " - DisburseAmount = " + (principal - (persona.getExistingPendingAmount()+110 )) +"\n" +
                    "- Use LOWER_EMI plan as baseline for savings comparison.\n" +
                    "- For LOWER_EMI: savingsPerMonth = 0.0, totalSavings = 0.0\n" +
                    "- For FASTER_CLOSURE and BALANCED: savingsPerMonth = Existing emi - current_plan_emi\n" +
                    "- totalSavings = LOWER_EMI_totalLoanAmount - current_plan_totalLoanAmount\n" +
                    "- Don't compute totalSavings as savingsPerMonth × tenure unless it's consistent with interest logic.\n" +
                    "- Use correct math. Do not guess values.\n" +
                    "- For PLAN_BALANCED:\n" +
                    "  - Set tenure to a value between the tenure of PLAN_LOWER_EMI and PLAN_FASTER_CLOSURE (e.g. if LOWER_EMI tenure = 12 months and FASTER_CLOSURE tenure = 6 months, BALANCED tenure = 8 or 9 months).\n" +
                    "  - Use the same or slightly improved interest rate as LOWER_EMI (do not increase interest rate).\n" +
                    "  - Calculate EMI accordingly.\n" +
                    "  - Ensure totalLoanAmount for BALANCED plan is strictly less than the totalLoanAmount of LOWER_EMI plan.\n" +
                    "  - Calculate savingsPerMonth = LOWER_EMI_EMI - BALANCED_EMI (can be negative if EMI is higher).\n" +
                    "  - Calculate totalSavings = LOWER_EMI_totalLoanAmount - BALANCED_totalLoanAmount (must be positive).\n" +
                    "  - If totalLoanAmount for BALANCED plan is greater than LOWER_EMI plan, adjust tenure or EMI to ensure savings.\n" +
                    "    Determine best plan based on persona characteristics:\n" +
                    "  - If persona income is high and payment history is DISCIPLINED, recommend PLAN_FASTER_CLOSURE as best for interest savings.\n" +
                    "  - If persona income is MEDIUM and payment history is MOSTLY_DISCIPLINED, recommend PLAN_BALANCED as best for a balance between affordability and interest savings.\n" +
                    "  - If persona income is low or payment history is IRREGULAR, recommend PLAN_LOWER_EMI as best to minimize monthly payment stress.\n" +
                    "  - Also consider aprScore: higher aprScore supports recommending faster closure or balanced plans; lower aprScore favors conservative lower EMI plan.\n" +
                    "  - Set the best field (true/false) accordingly in each plan's output, only one option can be true.\n" +
                    "  - Adjust confidence score based on how well the persona fits each plan.\n" +
                    "OUTPUT FORMAT (JSON only):\n" +
                    "{\n" +
                    "  \"modelVersion\": \"v1.0.0\",\n" +
                    "  \"requestId\": \"" + UUID.randomUUID().toString() + "\",\n" +
                    "  \"personaId\": \"" + persona.getPersonaId() + "\",\n" +
                    "  \"demoMode\": true,\n" +
                    "  \"recommendations\": [\n" +
                    "    {\n" +
                    "      \"planId\": \"PLAN_LOWER_EMI\",\n" +
                    "      \"goal\": \"LOWER_EMI\",\n" +
                    "      \"emi\": <calculated_emi>,\n" +
                    "      \"principal\": " +( principal-110) + ",\n" +
                    "      \"tenure\": <tenure_in_months>,\n" +
                    "      \"interestRate\": " + apr + ",\n" +
                    "      \"totalLoanAmount\": <emi × tenure>  in double,\n" +
                    "      \"disburseAmount\": <computed_disburse_amount>,\n" +
                    "      \"savingsPerMonth\": <existingEMI − emi>,\n" +
                    "      \"totalSavings\": <(existingEMI × existingTenure) − totalLoanAmount>,\n" +
                    "      \"breakEvenMonths\": <computed_integer_months>,\n" +
                    "      \"confidence\": <0–100>,\n" +
                    "      \"best\": <true_or_false based on persona income band, payment behaviour and apr rules provided>,\n" +
                    "      \"reason\": \"<reasoning>\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"planId\": \"PLAN_FASTER_CLOSURE\",\n" +
                    "      // same fields\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"planId\": \"PLAN_BALANCED\",\n" +
                    "      // same fields\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

        String response  = chatGptClient.sendPrompt(prompt);

        RecommendOptionsResponse finalResponse = mapper.readValue(response, RecommendOptionsResponse.class);

        return finalResponse;
    }

    private String reasonTemplate(String goal, String behavior, double savingsPerMonth, int tenure) {
        if ("LOWER_EMI".equals(goal))
            return "Lower EMI saves $" + Math.round(savingsPerMonth) + "/month; fits your " + behavior.toLowerCase() + " history.";
        if ("FASTER_CLOSURE".equals(goal))
            return "Higher EMI closes in " + tenure + " months; reduces total interest.";
        return "Keeps payments steady while controlling overall interest.";
    }

    private double round2( double apr )
    {
        return Math.round(apr*100.0)/100.0;
    }

    private double baseApr( Persona persona )
    {
        if (persona.getCreditScore() >= 750 && PaymentHistoryRule.PaymentHistoryType.DISCIPLINED.toString().equals(persona.getPaymentHistory())) return 10.5;
        if (persona.getCreditScore() >= 650) return 16.0;
        return 24.0;

    }

    private double adjustAprByIncomeBand(String incomeBand) {

        return switch (incomeBand.toUpperCase()){
            case "LOW" -> +0.7; case "HIGH" -> -0.7; default -> 0;
        };
    }
    private double adjustAprByPaymentHistory(String history){
        return switch (history){
            case "IRREGULAR" -> +1.5;
            case "MOSTLY_DISCIPLINED" -> +0.5;
            default -> 0;
        };
    }
    private int calcConfidence(Persona persona, String goal) {
        int base = clamp(persona.getCreditScore() / 10, 40, 95);
        if ("IRREGULAR".equals(persona.getPaymentHistory())) base -= 6;
        if ("LOWER_EMI".equals(goal) && "LOW".equals( persona.getIncome())) base += 5;
        if ("FASTER_CLOSURE".equals(goal) && "HIGH".equals( persona.getIncome()) ) base += 5;
        return clamp(base, 40, 95);
    }

    private int clamp(int v, int lo, int hi){ return Math.max(lo, Math.min(hi, v)); }

    private int normalizeTenure( int tenure )
    {
        return (tenure==6||tenure==12) ? tenure : 12;
    }

    public List<Persona> getPersonas()
    {
        return personaReaderService.getAllPersona();
    }
}
