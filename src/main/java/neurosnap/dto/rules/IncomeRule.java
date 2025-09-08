package neurosnap.dto.rules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncomeRule
{
    private String incomeBand; // LOW, MEDIUM, HIGH
    private int tenureDelta;  // e.g. +12 or -12
}
