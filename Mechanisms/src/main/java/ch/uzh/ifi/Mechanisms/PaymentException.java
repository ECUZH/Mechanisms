package ch.uzh.ifi.Mechanisms;

public class PaymentException extends Exception
{
	public PaymentException(String str, double socialWelfare)
	{
		super(str);
		_socialWelfare = socialWelfare;
	}
	
	private double _socialWelfare;
}
