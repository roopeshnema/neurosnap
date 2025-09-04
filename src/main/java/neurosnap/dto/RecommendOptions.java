package neurosnap.dto;

public class RecommendOptions
{

    private String lender;

    private double interestRate;

    private int termInYears;

    public RecommendOptions( String lender, double interestRate, int termInYears )
    {
        this.lender = lender;
        this.interestRate = interestRate;
        this.termInYears = termInYears;
    }

    public String getLender()
    {
        return lender;
    }

    public void setLender( String lender )
    {
        this.lender = lender;
    }

    public double getInterestRate()
    {
        return interestRate;
    }

    public void setInterestRate( double interestRate )
    {
        this.interestRate = interestRate;
    }

    public int getTermInYears()
    {
        return termInYears;
    }

    public void setTermInYears( int termInYears )
    {
        this.termInYears = termInYears;
    }
}

