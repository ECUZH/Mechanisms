package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import java.util.LinkedList;
import java.util.List;

public class VCGStrategy implements IPaymentCorrectionStrategy
{

	/*
	 * Constructor.
	 */
	public VCGStrategy( Planner planner, List<Type> types, int numberOfBuyers, int numberOfSellers)
	{
		_vcgDiscounts = new LinkedList<Double>();
		_planner = planner;
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_allBids = types;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPaymentCorrectionStrategy#getCorrectedVCGDiscounts()
	 */
	@Override
	public List<Double> getCorrectedVCGDiscounts() 
	{
		return _vcgDiscounts;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPaymentCorrectionStrategy#correctPayments()
	 */
	@Override
	public void correctPayments() 
	{
		_planner.withdrawError();
		_planner.reset(_allBids);
		_planner.makeNonInjectable();										//Make a non-injectable planner out of it
		_planner.generatePlans();											//Plans are based on statistics only
				
		DoubleSidedMarket market = new DoubleSidedMarket(_numberOfBuyers, _numberOfSellers, _allBids, _planner);
		market.setPaymentRule("VCG");
		market.setPaymentCorrectionRule("None");
		market.computeWinnerDetermination();
		
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments(market.getAllocation(), _allBids, _planner.getPlans(), _numberOfBuyers, _numberOfSellers, _planner);
		
		try 
		{
			vcgPayments.computePayments();
		}
		catch (Exception e) 
		{
			System.out.println("In VCGStrategy(): true (uncertainty-free) VCG discounts were not computed. ");
			e.printStackTrace();
			return;
		}
		_vcgDiscounts = vcgPayments.getVCGDiscounts();
		
		for( Double discount : _vcgDiscounts )
			if( discount < 0 && discount > -1e-6 ) 
				discount = 0.;
			else if( discount < 0 )
				throw new RuntimeException("Negative vcg discount: " + discount + "\n plans: " + market.getPlans().toString() +
						                   " allocation: " + market.getAllocation().toString());
	}

	private List<Double> _vcgDiscounts;
	private Planner _planner;
	private List<Type> _allBids;
	private int _numberOfBuyers;
	private int _numberOfSellers;
}
