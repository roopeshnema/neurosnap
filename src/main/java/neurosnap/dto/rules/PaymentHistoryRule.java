package neurosnap.dto.rules;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentHistoryRule
{
    private String paymentHistory;      // IRREGULAR, DISCIPLINED, MOSTLY_DISCIPLINED
    private double tenureRate;  // e.g. +12 or -12
}
