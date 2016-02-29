package ch.uzh.ifi.Mechanisms;

import java.util.List;

public interface IPaymentCorrectionStrategy 
{	
	/*
	 * The method returns the list of corrected VCG discounts.
	 * @return a list of corrected VCG discounts
	 */
	public List<Double> getCorrectedVCGDiscounts();
	
	/*
	 * The method performs payments correction.
	 */
	public void correctPayments();
}
