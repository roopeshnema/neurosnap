package neurosnap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import neurosnap.client.ChatGptClient;
import neurosnap.dto.Persona;
import neurosnap.dto.RecommendOption;
import neurosnap.dto.RecommendOptionsResponse;
import neurosnap.dto.RecommendRequest;
import neurosnap.dto.rules.ConfidenceRule;
import neurosnap.dto.rules.GoalRule;
import neurosnap.dto.rules.IncomeRule;
import neurosnap.dto.rules.PaymentHistoryRule;
import neurosnap.util.RefiCalculator;
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
        Optional<Persona> personaResult = personaList.stream()
                .filter(p -> p.getPersonaId().equals(personaId))
                .findFirst();
        Persona persona = personaResult.get();


        //rulesReaderService.loadRules( "rules.xlsx" );
        return generatePlans(persona, request);

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
                "REFI1", "LOWER_EMI", 18000, 72, 5.9, 1200, 14400, 500,
                 6, 82, true,
                 "Because you always pay early and have strong credit, you qualify for a lower EMI with minimal risk."
        );
        RecommendOption option2 = new RecommendOption(
                "REFI2", "BALANCED",  20000,  60,  6.5,  0,  0, 0,0,
                 74,  false,
                "Balanced plan keeps your payments stable while slightly reducing total interest."
        );
        RecommendOption option3 = new RecommendOption(
               "REFI3", "FASTER_CLOSURE", 22000,  48,  5.5,  1000,  12000,
                500, 12,  68, false,
                "Higher EMI but lets you close the loan faster and save $50,000 in interest."
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
            throw new IllegalArgumentException("Requested amount must cover pending balance + fees (min $" + Math.round(minRequired) + ")");
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
        int tBalanced = baseTenure;
        double emiBalanced = RefiCalculator.calculateEMI(principal, apr, tBalanced);

        // LOWER_EMI
        int tLower = 12;
        double emiLower = RefiCalculator.calculateEMI(principal, apr, tLower);
        double savLower = Math.max(0, emiBalanced - emiLower);
        plans.add(RecommendOption.builder()
                .planId("REFI1").goal("LOWER_EMI").tenure(tLower)
                .interestRate(round2(apr)).emi((int)Math.round(emiLower))
                .savingsPerMonth(round2(savLower))
                .totalSavings(round2(savLower * tLower))
                .fees(round2(fees)).breakEvenMonths((int)Math.ceil(fees / Math.max(1.0, savLower)))
                .confidence(calcConfidence(persona, "LOWER_EMI"))
                .isBest(false)
                .reason(chatGptClient.sendPrompt(reasonTemplate("LOWER_EMI", persona.getPaymentHistory(), savLower, tLower)))
                .build());

        // BALANCED
        plans.add(RecommendOption.builder()
                .planId("REFI2").goal("BALANCED").tenure(tBalanced)
                .interestRate(round2(apr)).emi((int)Math.round(emiBalanced))
                .savingsPerMonth(0).totalSavings(0)
                .fees(round2(fees)).breakEvenMonths(0)
                .confidence(calcConfidence(persona, "BALANCED"))
                .isBest(false)
                .reason(chatGptClient.sendPrompt(reasonTemplate("BALANCED", persona.getPaymentHistory(), 0, tBalanced)))
                .build());

        // FASTER_CLOSURE
        int tFaster = Math.max(6, baseTenure - 12);
        double emiFaster = RefiCalculator.calculateEMI(principal, aprFaster, tFaster);
        double savFaster = Math.max(0, emiBalanced - emiFaster); // usually 0 because EMI is higher
        plans.add(RecommendOption.builder()
                .planId("REFI3").goal("FASTER_CLOSURE").tenure(tFaster)
                .interestRate(round2(aprFaster)).emi((int)Math.round(emiFaster))
                .savingsPerMonth(round2(savFaster)).totalSavings(round2(savFaster * tFaster))
                .fees(round2(fees)).breakEvenMonths((int)Math.ceil(fees / Math.max(1.0, savFaster)))
                .confidence(calcConfidence(persona, "FASTER_CLOSURE"))
                .isBest(false)
                .reason(chatGptClient.sendPrompt(reasonTemplate("FASTER_CLOSURE", persona.getPaymentHistory(), savFaster, tFaster)))
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
        if (persona.getCreditScore() >= 750 && "DISCIPLINED".equals(persona.getPaymentHistory())) return 10.5;
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
        rate = rate + ( paymentHistoryRule != null ? paymentHistoryRule.getTenureRate() : 0);

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
