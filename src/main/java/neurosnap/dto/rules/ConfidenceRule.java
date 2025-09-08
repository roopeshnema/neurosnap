package neurosnap.dto.rules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfidenceRule
{
    private String confidence; // ">80", "Between 50 to 80", "<50"
    private double rateDelta; // e.g. -0.5, 0, +1
}
