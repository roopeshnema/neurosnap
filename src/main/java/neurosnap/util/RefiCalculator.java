package neurosnap.util;

public class RefiCalculator
{
    public static double calculateEMI(double principal, double annualRate, int tenureMonths) {
        if (tenureMonths <= 0) throw new IllegalArgumentException("tenureMonths must be > 0");
        double monthlyRate = annualRate / 12.0 / 100.0;
        if (monthlyRate == 0) {
            return principal / tenureMonths;
        }
        double pow = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * pow) / (pow - 1);
    }


}
