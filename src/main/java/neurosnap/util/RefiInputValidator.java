package neurosnap.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neurosnap.dto.RecommendRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Component
@Slf4j
@AllArgsConstructor
public class RefiInputValidator {

    public List<String>  validate( RecommendRequest requstObject, String personaId ) {
        List<String> errors = new ArrayList<>();

        if (requstObject.getLoanAmount() < 300 || requstObject.getLoanAmount()>5000 ) {
            errors.add("Loan amount must be between $300 and $5000.");
        }

        if (requstObject.getTenure() < 1 || requstObject.getTenure() > 12) {
            errors.add("Tenure must be between 1 and 12 months.");
        }

        if (personaId == null || personaId.isEmpty()) {
            errors.add("persona-id header is required");
        }

        return errors;
    }

    private boolean isValidIncomeBand(String band) {
        return Set.of("LOW", "MEDIUM", "HIGH").contains(band.toUpperCase());
    }

    private boolean isValidGoal(String goal) {
        return Set.of("LOWER_EMI", "BALANCED", "FASTER_CLOSURE").contains(goal.toUpperCase());
    }

}
