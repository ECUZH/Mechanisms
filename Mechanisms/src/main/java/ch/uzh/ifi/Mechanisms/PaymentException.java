package ch.uzh.ifi.Mechanisms;

import java.util.List;

public class PaymentException extends Exception
{
	public PaymentException(String str, double socialWelfare)
	{
		super(str);
		_socialWelfare = socialWelfare;
	}
	
	public PaymentException(String str, double socialWelfare, List<Double> payments)
	{
		super(str);
		_payments = payments;
		_socialWelfare = socialWelfare;
	}
	
	public List<Double> getPayments()
	{
		return _payments;
	}
	
	private double _socialWelfare;
	private List<Double> _payments;
}
