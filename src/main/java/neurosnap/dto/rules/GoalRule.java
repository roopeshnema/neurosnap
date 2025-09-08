package neurosnap.dto.rules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRule
{
    private String goal;      // LOWER_EMI, BALANCED, FASTER_CLOSURE
    private int tenureDelta;  // e.g. +12 or -12
}
