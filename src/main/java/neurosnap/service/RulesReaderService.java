package neurosnap.service;


import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import neurosnap.dto.rules.ConfidenceRule;
import neurosnap.dto.rules.GoalRule;
import neurosnap.dto.rules.IncomeRule;
import neurosnap.dto.rules.PaymentHistoryRule;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class RulesReaderService
{
    private Map<String, GoalRule> goalRules = new HashMap<>();
    private Map<String, ConfidenceRule> confidenceRules = new HashMap<>();
    private Map<String, IncomeRule> incomeRules = new HashMap<>();

    private Map<String, PaymentHistoryRule> paymentHistoryRules = new HashMap<>();

    public void loadRules(String rulesFile) throws Exception {
        try (InputStream is = PersonaReaderService.class.getClassLoader().getResourceAsStream(rulesFile);
             Workbook workbook = WorkbookFactory.create(is))
        {

            // Sheet 1: Goal vs Tenure
            Sheet goalSheet = workbook.getSheet("PaymentGoal");
            for ( Row row : goalSheet) {
                if (row.getRowNum() == 0) continue; // skip header
                GoalRule gr = new GoalRule();
                gr.setGoal( GoalRule.GoalType.valueOf( row.getCell(0).getStringCellValue() ) );
                gr.setTenureDelta((int) row.getCell(1).getNumericCellValue());
                goalRules.put(gr.getGoal().toString(), gr);
            }

            // Sheet 2: Confidence vs Rate
            Sheet confSheet = workbook.getSheet("ConfidenceScore");
            for (Row row : confSheet) {
                if (row.getRowNum() == 0) continue;
                ConfidenceRule cr = new ConfidenceRule();
                cr.setConfidence( ConfidenceRule.ConfidenceLevel.valueOf( row.getCell(0).getStringCellValue() ) );
                cr.setRateDelta(row.getCell(1).getNumericCellValue());
                confidenceRules.put( cr.getConfidence().toString(), cr );
            }

            // Sheet 3: Income bands
            Sheet incomeSheet = workbook.getSheet("Income");
            for (Row row : incomeSheet) {
                if (row.getRowNum() == 0) continue;
                IncomeRule ir = new IncomeRule();
                ir.setIncomeBand( IncomeRule.IncomeBand.valueOf( row.getCell(0).getStringCellValue() ) );
                ir.setTenureDelta((int) row.getCell(1).getNumericCellValue());
                incomeRules.put(ir.getIncomeBand().toString(), ir);
            }

            // Sheet 4: Payment History
            Sheet paymentHistorySheet = workbook.getSheet("PaymentHistory");
            for (Row row : paymentHistorySheet) {
                if (row.getRowNum() == 0) continue;
                PaymentHistoryRule phr = new PaymentHistoryRule();
                phr.setPaymentHistory( PaymentHistoryRule.PaymentHistoryType.valueOf( row.getCell(0).getStringCellValue() ) );
                phr.setRateDelta(row.getCell(1).getNumericCellValue());
                paymentHistoryRules.put(phr.getPaymentHistory().toString(), phr);
            }
        }
    }

    public GoalRule getGoalRule(String goal) {
        return goalRules.get(goal);
    }

    public ConfidenceRule getConfidenceRules(String confidence) {
        return confidenceRules.get( confidence );
    }

    public IncomeRule getIncomeRule(String incomeBand) {
        return incomeRules.get(incomeBand);
    }

    public PaymentHistoryRule getPaymentHistoryRule(String paymentHistory) {
        return paymentHistoryRules.get( paymentHistory );
    }

    public Map<String, IncomeRule> getIncomeRules(){
        return this.incomeRules;
    }

    public Map<String, ConfidenceRule> getConfidenceRules(){
        return this.confidenceRules;
    }

    public Map<String, PaymentHistoryRule> getPaymentHistoryRules(){
        return this.paymentHistoryRules;
    }

}
