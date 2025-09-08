package neurosnap.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    public RecommendationService( PersonaReaderService personaReaderService, RulesReaderService rulesReaderService )
    {
        this.personaReaderService = personaReaderService;
        this.rulesReaderService = rulesReaderService;
    }

    public RecommendOptionsResponse getRecommendations(RecommendRequest request, String personaId ) throws Exception
    {

        List<Persona> personaList = personaReaderService.readPersonasFromExcel( "persona.xlsx" );
        Optional<Persona> personaResult = personaList.stream()
                .filter(p -> p.getPersonaId().equals(personaId))
                .findFirst();
        Persona persona = personaResult.get();

        rulesReaderService.loadRules( "rules.xlsx" );



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


    public RecommendOptionsResponse generatePlans(Persona persona, RecommendRequest input) {
        List<RecommendOption> plans = new ArrayList<>();

        double principal = input.getLoanAmount();

        // base annual rate from persona (could be overridden by rules)
        double baseRate = persona.getExistingInterestRate();

        // base tenure
        int baseTenure = input.getTenure();

        // Apply rules and adjust the tenure
        int revisedTenure = getAdjustedTenure(baseTenure, input.getGoal(), input.getIncomeBand());

        // Apply rules and adjust the rate
        double revisedRate = getAdjustedRate(baseRate, persona.getPaymentHistory(), getConfidence(persona.getCreditScore()));

        // define candidate tenures and slight rate adjustments per goal (can be driven by rules)
        int moreTenure = revisedTenure + 12;               // extend to lower EMI
        int balancedTenure = revisedTenure;                 // keep same
        int fasterTenure = Math.max(12, revisedTenure - 12);// shorten to close faster

        double lowerRate = Math.max(0.0, revisedRate - 0.5); // small discount to incentivize refinance
        double balancedRate = revisedRate;
        double fasterRate = revisedRate; // could be same or slightly different


        double emiBalanced = RefiCalculator.calculateEMI(principal, balancedRate, balancedTenure);
        double emiFaster = RefiCalculator.calculateEMI(principal, fasterRate, fasterTenure);

        RecommendOption lowerOption  = new RecommendOption();
        lowerOption.setPlanId( "REFI-1" );
        lowerOption.setFees( 500 );
        lowerOption.setBest( false );
        lowerOption.setEmi( ( int) RefiCalculator.calculateEMI(principal, lowerRate, moreTenure) );
        lowerOption.setGoal( "LOWER_EMI" );
        lowerOption.setConfidence( calculateConfidence(persona, "LOWER_EMI") );
        lowerOption.setReason( "Extends tenor to reduce monthly burden; good for lower income-band or irregular payers." );
        lowerOption.setTenure( moreTenure );
        lowerOption.setInterestRate( lowerRate );

        plans.add(lowerOption);
        /*
        plans.add(new RefinancePlan("REFI2", "BALANCED", round(emiBalanced), balancedTenure,
                calculateConfidence(persona, "BALANCED"),
                "Keeps tenure steady and uses current rate — balances cost and affordability.", false));

        plans.add(new RefinancePlan("REFI3", "FASTER_CLOSURE", round(emiFaster), fasterTenure,
                calculateConfidence(persona, "FASTER_CLOSURE")
                "Shortens tenor to save interest — increases EMI; suitable if cashflow allows.", false));

        // mark the plan with highest confidence as best
        RecommendOption best = plans.stream()
                .max( Comparator.comparingInt(RecommendOption))
                .orElse(null);
        if (best != null) {
            best.setBest(true);
            // ensure others are false
            plans.forEach(p -> { if (!p.equals(best)) p.setBest(false); });
        }

        return new RecommendationResponse(persona.getPersonaId(), "v1.0.0", plans);
        */

        return null;
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
        if (goal.equals("LOWER_EMI") && persona.getIncome() < 50000) base += 5;
        if (goal.equals("FASTER_CLOSURE") && persona.getIncome() > 100000) base += 5;
        return Math.min(100, base);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

}
