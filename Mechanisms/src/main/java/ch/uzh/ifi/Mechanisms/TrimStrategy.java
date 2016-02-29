package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;

/*
 * The class implements a PC-TRIM payment correction rule which trims out all negative discounts by fixing them to be 
 * equal to 0.
 */
public class TrimStrategy implements IPaymentCorrectionStrategy
{

	/*
	 * A simple constructor.
	 * @param vcgDiscounts - a list of VCG discounts to be corrected.
	 */
	public TrimStrategy(List<Double> vcgDiscounts)
	{
		_vcgDiscounts = vcgDiscounts;
		_vcgDiscountsCorrected = new LinkedList<Double>();
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPaymentCorrectionStrategy#getCorrectedVCGDiscounts()
	 */
	@Override
	public List<Double> getCorrectedVCGDiscounts() 
	{
		return _vcgDiscountsCorrected;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPaymentCorrectionStrategy#correctPayments()
	 */
	@Override
	public void correctPayments()
	{
		for(int i = 0; i < _vcgDiscounts.size(); ++i)
			if( _vcgDiscounts.get(i) < 0 )
				_vcgDiscountsCorrected.add(0.0);
			else
				_vcgDiscountsCorrected.add(_vcgDiscounts.get(i));
	}

	private List<Double> _vcgDiscounts;														//VCG discounts
	private List<Double> _vcgDiscountsCorrected;											//VCG discounts
}
