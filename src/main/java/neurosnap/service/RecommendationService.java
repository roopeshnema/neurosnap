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
import neurosnap.dto.rules.GoalRule;
import neurosnap.dto.rules.IncomeRule;
import neurosnap.dto.rules.PaymentHistoryRule;
import neurosnap.util.RefiCalculator;
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

    private final double maxLoanAmountLimit = 5000;

    private final double minLoanAmountLimit = 300;

    public RecommendationService( PersonaReaderService personaReaderService, RulesReaderService rulesReaderService, ChatGptClient chatGptClient )
    {
        this.personaReaderService = personaReaderService;
        this.rulesReaderService = rulesReaderService;
        this.chatGptClient = chatGptClient;
    }

    public RecommendOptionsResponse getRecommendations(RecommendRequest request, String personaId ) throws Exception
    {

        List<Persona> personaList = personaReaderService.readPersonasFromExcel( "persona.xlsx" );
        Optional<Persona> personaResult = Optional.ofNullable( personaList.stream()
                .filter( p -> p.getPersonaId().equals( personaId ) )
                .findFirst()
                .orElseThrow( () -> new BadRequestException( "Unknown persona-id: " + personaId ) ) );
        Persona persona = personaResult.get();


        //rulesReaderService.loadRules( "rules.xlsx" );
        //return generatePlans(persona, request);
        return generatePlansUsingAI( persona, request);

    }

    public RecommendOptionsResponse getExamples(RecommendRequest request, String personaId ) throws Exception
    {
        //  return Arrays.asList(option1, option2, option3);
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


    public RecommendOptionsResponse generatePlans(Persona persona, RecommendRequest request) throws Exception
    {
        List<RecommendOption> plans = new ArrayList<>();

        // Calculating Fee
        double fees = processingFee + taxFee;

        // Validating user input amount with minRequired amount.
        double minRequired = persona.getExistingPendingAmount() + fees;
        if (request.getLoanAmount() < minRequired) {
            throw new BadRequestException("Requested amount must cover pending balance + fees (min $" + Math.round(minRequired) + ")");
        }


        // Calculating Principal
        double principal = Math.max(minRequired, Math.min(request.getLoanAmount(), maxLoanAmountLimit));
        principal = Math.max(principal, minLoanAmountLimit);

        // normalizeTenure
        int baseTenure = normalizeTenure(request.getTenure());

        // Calculate APR
        double apr = baseApr(persona) + adjustAprByPaymentHistory(persona.getPaymentHistory())
                + adjustAprByIncomeBand(persona.getIncome());

        double aprFaster = Math.max(0, apr - 0.5); // incentive

        // Baseline (balanced)
        int tBalanced = Math.max( 6,baseTenure - 3);
        double emiBalanced = RefiCalculator.calculateEMI(principal, apr, tBalanced);

        // LOWER_EMI
        int tLower = 12;
        double emiLower = RefiCalculator.calculateEMI(principal, apr, tLower);
        double savLower = Math.max(0, emiBalanced - emiLower);
        plans.add(RecommendOption.builder()
                .planId("REFI1")
                .goal( RecommendOption.GoalType.LOWER_EMI).tenure(tLower)
                .interestRate(round2(apr)).emi((int)Math.round(emiLower))
                .savingsPerMonth(round2(savLower))
                .totalSavings(round2(savLower * tLower))
                //.fees(round2(fees))
                //.breakEvenMonths((int)Math.ceil(fees / Math.max(1.0, savLower)))
                .confidence(calcConfidence(persona, "LOWER_EMI"))
                .best(false)
                //.reason(chatGptClient.sendPrompt(reasonTemplate("LOWER_EMI", persona.getPaymentHistory(), savLower, tLower)))
                .reason(reasonTemplate("LOWER_EMI", persona.getPaymentHistory(), savLower, tLower))
                .build());

        // BALANCED
        plans.add(RecommendOption.builder()
                .planId("REFI2")
                .goal( RecommendOption.GoalType.BALANCED)
                .tenure(tBalanced)
                .interestRate(round2(apr))
                .emi((int)Math.round(emiBalanced))
                .savingsPerMonth(0)
                .totalSavings(0)
                //.fees(round2(fees))
                //.breakEvenMonths(0)
                .confidence(calcConfidence(persona, "BALANCED"))
                .best(false)
                //.reason(chatGptClient.sendPrompt(reasonTemplate("BALANCED", persona.getPaymentHistory(), 0, tBalanced)))
                .reason( reasonTemplate("BALANCED", persona.getPaymentHistory(), 0, tBalanced))
                .build());

        // FASTER_CLOSURE
        int tFaster = Math.max(6, baseTenure - 12);
        double emiFaster = RefiCalculator.calculateEMI(principal, aprFaster, tFaster);
        double savFaster = Math.max(0, emiBalanced - emiFaster); // usually 0 because EMI is higher
        plans.add(RecommendOption.builder()
                .planId("REFI3")
                .goal( RecommendOption.GoalType.FASTER_CLOSURE)
                .tenure(tFaster)
                .interestRate(round2(aprFaster))
                .emi((int)Math.round(emiFaster))
                .savingsPerMonth(round2(savFaster))
                .totalSavings(round2(savFaster * tFaster))
                //.fees(round2(fees))
                //.breakEvenMonths((int)Math.ceil(fees / Math.max(1.0, savFaster)))
                .confidence(calcConfidence(persona, "FASTER_CLOSURE"))
                .best(false)
                //.reason(chatGptClient.sendPrompt(reasonTemplate("FASTER_CLOSURE", persona.getPaymentHistory(), savFaster, tFaster)))
                        .reason( reasonTemplate("FASTER_CLOSURE", persona.getPaymentHistory(), savFaster, tFaster))
                .build());


        // best-fit
        RecommendOption best = plans.get(1); // default BALANCED
        if ("LOW".equals( persona.getIncome() ) || "IRREGULAR".equals(persona.getPaymentHistory())) best = plans.get(0);
        else if ("HIGH".equals( persona.getIncome() ) && "DISCIPLINED".equals(persona.getPaymentHistory())) best = plans.get(2);
        best.setBest( true );

        return RecommendOptionsResponse.builder()
                .personaId(persona.getPersonaId())
                .modelVersion("v1.0.0")
                .recommendations(plans.toArray(new RecommendOption[0]))
                .build();
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


        // Calculating Principal
        //double principal = Math.max(minRequired, Math.min(request.getLoanAmount(), maxLoanAmountLimit));
        //principal = Math.max(principal, minLoanAmountLimit);

        double principal = request.getLoanAmount();

        // normalizeTenure
        int baseTenure = normalizeTenure(request.getTenure());

        // Calculate APR
        double apr = baseApr(persona);

        ObjectMapper mapper = new ObjectMapper();
        String personaJson = mapper.writeValueAsString(persona);

        rulesReaderService.loadRules( "rules.xlsx" );
        Map<String, IncomeRule> incomerules = rulesReaderService.getIncomeRules();
        Map<String, ConfidenceRule> confidenceRules = rulesReaderService.getConfidenceRules();
        Map<String, PaymentHistoryRule> paymentHistoryRules = rulesReaderService.getPaymentHistoryRules();
        Map<String, AprScoreRule> aprScoreRules = rulesReaderService.getAprScoreRules();

        String incomeRulesJson = mapper.writeValueAsString(incomerules);
        String paymentHistoryRuleJson = mapper.writeValueAsString(paymentHistoryRules);
        String confidenceRulesJson = mapper.writeValueAsString(confidenceRules);
        String aprScoreRulesJson = mapper.writeValueAsString(aprScoreRules);

       /* String response  = chatGptClient.sendPrompt( "suggest 3 Refinance options for Lower EMI, Faster Closure, Balanced considering below inputs " +
                "input :" + request +
                "persona :" + personaJson +
                "tenure : " + baseTenure +
                "principal : " + principal  +
                "interestRate :" + apr +
                "rules :" +  incomeRulesJson + paymentHistoryRuleJson + confidenceRulesJson + aprScoreRulesJson  +
                "output format JSON only:" + "{\n" +
                "  \"modelVersion\": \"v1.0.0\",\n" +
                "  \"requestId\":" +  UUID.randomUUID().toString() + ",\n" +
                "  \"personaId\":" + persona.getPersonaId() +",\n" +
                "  \"demoMode\": true,\n" +
                "  \"recommendations\": [\n" +
                "    {\n" +
                "      \"planId\": \"$$\",\n" +
                "      \"goal\": \"$Possible values LOWER_EMI or BALANCED or FASTER_CLOSURE$\",\n" +
                "      \"emi\":   (principal * (interestRate / 12.0 / 100.0) * (Math.pow(1 + (interestRate / 12.0 / 100.0), tenure))) / ((Math.pow(1 + (interestRate / 12.0 / 100.0), tenure)) - 1),\n" +
                "      \"principal\":" +  principal + ",\n" +
                "      \"tenure\": $tenure will be 1 to 12 in int$,\n" +
                "      \"interestRate\": $$,\n" +
                "      \"savingsPerMonth\": $$,\n" +
                "      \"totalSavings\": $$,\n" +
                "      \"totalLoanAmount\": $$,\n" +
                "      \"disburseAmount\": $principal - existing pending amount from persona$,\n" +
                "      \"breakEvenMonths\": $,\n" +
                "      \"confidence\": $confidence % (0–100), based on persona + repayment behavior$,\n" +
                "      \"best\": $boolean true or false$,\n" +
                "      \"reason\": \"$$\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");*/

        //String prompt =
                /*"You are a financial assistant. Suggest 3 refinance options based on the following goals:\n" +
                        "1. LOWER_EMI: Lowest possible EMI using maximum allowed tenure (even if total interest is higher).\n" +
                        "2. FASTER_CLOSURE: Shortest tenure possible to minimize total interest, even with higher EMI.\n" +
                        "3. BALANCED: Middle-ground between EMI and tenure — reasonable EMI and interest.\n\n" +

                        "INPUT DATA:\n" +
                        "Principal Amount: " + principal + "\n" +
                        "Base Tenure: " + baseTenure + " months\n" +
                        "Persona JSON: " + personaJson + "\n" +
                        "Rules: " + incomeRulesJson + paymentHistoryRuleJson + confidenceRulesJson + aprScoreRulesJson + "\n\n" +
                        "calculate interest rate based on aprScoreRulesJson "+
                        "Use the **standard reducing balance EMI formula** for all calculations:\n" +
                        "EMI = [P × R × (1 + R)^N] / [(1 + R)^N - 1]\n" +
                        "Where:\n" +
                        "- P = Principal\n" +
                        "- R = Monthly rate = APR / (12 × 100)\n" +
                        "- N = Tenure in months (ranging from 6 to 12)\n\n" +
                        "For each plan, compute:\n" +
                        "- EMI (2 decimal places)\n" +
                        "- TotalLoanAmount = EMI × Tenure\n" +
                        "- SavingsPerMonth = Existing EMI from persona - New EMI\n" +
                        "- TotalSavings = SavingsPerMonth × Tenure\n" +
                        "- DisburseAmount = " + principal + " - (persona.pendingAmount + 110)\n" +
                        "- BreakEvenMonths (rounded to nearest month)\n" +
                        "- Confidence (0–100), based on persona’s repayment behavior\n\n" +
                        "- Use the actual formula with numbers.\n" +
                        "- Do not guess or simplify values. Use correct math.\n" +
                        " - For LOWER_EMI: set savingsPerMonth = 0 \n" +
                        " - For other plans: \n" +
                        " - savingsPerMonth = LOWER_EMI_EMI - current_plan_emi \n" +
                        " - totalSavings = savingsPerMonth × tenure \n" +
                        " A negative savingsPerMonth is acceptable if the customer pays more EMI to save on interest (e.g., Faster Closure) \n" +
                        " - If totalLoanAmount < principal, correct EMI accordingly, never below principal \n" +
                        "- For SavingsPerMonth:\n" +
                        "   savingsPerMonth = LOWER_EMI_EMI - current_plan_emi\n" +
                        "- For TotalSavings:\n" +
                        "   totalSavings = (LOWER_EMI_EMI × LOWER_EMI_Tenure) - (current_plan_emi × current_plan_tenure)\n" +
                        "- Do NOT compute totalSavings as savingsPerMonth × tenure unless savingsPerMonth > 0.\n" +
                        "- totalSavings must reflect overall interest saved, even if EMI is higher.\n" +
                         "Do not use placeholders like \"Computed\". Always return a numeric value\n" +
                        "Output only valid JSON matching this format:\n" +
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
                        "      \"principal\": " + principal + ",\n" +
                        "      \"tenure\": <tenure>,\n" +
                        "      \"interestRate\": " + apr + ",\n" +
                        "      \"savingsPerMonth\": <savings_per_month>,\n" +
                        "      \"totalSavings\": <total_savings>,\n" +
                        "      \"totalLoanAmount\": <emi_times_tenure>,\n" +
                        "      \"disburseAmount\": <computed_disburse_amount>,\n" +
                        "      \"breakEvenMonths\": <months>,\n" +
                        "      \"confidence\": <confidence>,\n" +
                        "      \"best\": <true_or_false>,\n" +
                        "      \"reason\": \"<reasoning>\"\n" +
                        "    },\n" +
                        "    ... (repeat for FASTER_CLOSURE and BALANCED)\n" +
                        "  ]\n" +
                        "}";*/
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

    private String getConfidence( int creditScore )
    {
        if(creditScore >= 80) {
            return "HIGH";
        }
        else if ( creditScore < 80 && creditScore > 50 )
        {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private double getAdjustedRate( double rate, String paymentHistory, String confidence )
    {
        ConfidenceRule confidenceRule = rulesReaderService.getConfidenceRules(confidence);
        PaymentHistoryRule paymentHistoryRule = rulesReaderService.getPaymentHistoryRule( paymentHistory );

        rate = rate + ( confidenceRule != null ? confidenceRule.getRateDelta() : 0);
        rate = rate + ( paymentHistoryRule != null ? paymentHistoryRule.getRateDelta() : 0);

        return rate;
    }

    private int getAdjustedTenure( int tenure, String goal, String incomeBand )
    {

        GoalRule goalRule = rulesReaderService.getGoalRule( goal );

        tenure = tenure + (goalRule != null ? goalRule.getTenureDelta() : 0);

        IncomeRule incomeRule = rulesReaderService.getIncomeRule( incomeBand );

        tenure = tenure + (incomeRule != null ? incomeRule.getTenureDelta() : 0);

        return tenure;
    }


    private int calculateConfidence(Persona persona, String goal) {
        // Simple logic: higher credit score + disciplined behavior => higher %
        int base = persona.getCreditScore() / 10; // scale 0–100
        if (persona.getPaymentHistory().equals( "DISCIPLINED" )) base += 10;
        if (persona.getIncome().equals( "HIGH" )) base += 5;
        return Math.min(100, base);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public List<Persona> getPersonas()
    {
        return personaReaderService.readPersonasFromExcel( "persona.xlsx" );
    }

    private int getTotalSavings () {
       // Current loan monthly payment: $2,000
      //  Remaining term: 20 years (240 months)
      //  Refinanced monthly payment: $1,700
      //  New loan term: 30 years (360 months)
     //   savingsPerMonth = 2000 - 1700 = $300
    //return( (2000 * 240) - (1700 * 360));
    //         = 480,000 - 612,000
   //             = -$132,000 (Loss)
   //even though you save $300/month, you're paying for 10 extra years, resulting in no total savings, but actually a loss. This is why total savings must consider full loan lifetime.
   //else  totalSavings = savingsPerMonth × refinancedLoanTermInMonths
     return 300*60;
    }

    private double getSavingsPerMonth () {
        // Current loan monthly payment: $2,000
        //  Refinanced monthly payment: $1,700
        return 2000 - 1700;

    }
    private int getBreakEvenMonths(){
        //  breakEvenMonths = fees -totalRefinanceCosts / savingsPerMonth
        return  500 / 300;
    }

}
