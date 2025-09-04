package neurosnap.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
        private long loanAmount;
        private int tenure;
        private String incomeBand;
        private String goal;

}
