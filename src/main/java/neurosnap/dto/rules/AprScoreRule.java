package neurosnap.dto.rules;



import lombok.Getter;
import lombok.Setter;

/**
 * Represents an APR tier based on score ranges.
 *
 * Example row from rules.xlsx:
 *   Tier | MinScore | MaxScore | MinAPR | MaxAPR
 *   HIGH | 1        | 49       | 19     | 30
 *   MEDIUM | 50     | 79       | 13     | 18
 *   LOW | 80       | 100      | 8      | 12
 */
@Getter
@Setter
public class AprScoreRule {
    private String tier;       // HIGH | MEDIUM | LOW
    private int minScore;      // inclusive
    private int maxScore;      // inclusive
    private double minApr;     // inclusive
    private double maxApr;     // inclusive

    /** Check if a given normalized score falls into this tier. */
    public boolean matches(int score) {
        return score >= minScore && score <= maxScore;
    }
}




