package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;

import java.util.LinkedList;
import java.util.List;

public class PenaltyStrategy implements IPaymentCorrectionStrategy
{

	/*
	 * A Constructor.
	 */
	public PenaltyStrategy( Planner planner, List<Type> types, Allocation allocation, int numberOfBuyers, int numberOfSellers )
	{
		_vcgDiscountsCorrected = new LinkedList<Double>();
		_planner = planner;
		_allBids = types;
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_allocation = allocation;
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
		//Compute sw with the error injected
		double sw = _allocation.getAllocatedWelfare();
		
		//Identify sellers which injected the error
		int sellerIdx = _planner.getInjectedSeller();
		
		//Compute sw with no errors injected
		_planner.withdrawError();
		_planner.reset(_allBids);
		_planner.makeNonInjectable();
		_planner.generatePlans();
		DoubleSidedMarket dsm = new DoubleSidedMarket(_numberOfBuyers, _numberOfSellers, _allBids, _planner);
		dsm.computeWinnerDetermination();
		
		double swWithNoErrors = dsm.getAllocation().getAllocatedWelfare();
		
		try 
		{
			IPaymentCorrectionStrategy pc = new VCGStrategy( _planner, _allBids, _numberOfBuyers, _numberOfSellers );
			pc.correctPayments();
			_vcgDiscountsCorrected = pc.getCorrectedVCGDiscounts();
			_vcgDiscountsCorrected.set(1 + sellerIdx, _vcgDiscountsCorrected.get(1+sellerIdx) * (  1 - Math.abs(sw-swWithNoErrors) / ( Math.abs(sw) + Math.abs(swWithNoErrors))  ) );
		} 
		catch (Exception e) 
		{
			System.out.println("Payments were not computed.");
			e.printStackTrace();
		}
	}

	private Planner _planner;
	private Allocation _allocation;
	private List<Type> _allBids;
	private int _numberOfBuyers;
	private int _numberOfSellers;
	private List<Double> _vcgDiscountsCorrected;						//Corrected VCG discounts
}
