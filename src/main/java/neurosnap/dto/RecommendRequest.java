package neurosnap.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
        @JsonAlias({"loan_amount"})

        private long loanAmount;

        private int tenure;

        @JsonAlias({"income_band"})
        private String incomeBand;

        private String goal;

}
