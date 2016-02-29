package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;

public class FirstPriceAuction implements Auction
{
	/*
	 * Constructor.
	 * @param agentsTypes - types of agents willing to participate in the auction.
	 */
	public FirstPriceAuction( List<Type> agentsTypes)
	{
		resetTypes(agentsTypes);
	}
	
	@Override
	public void resetTypes( List<Type> agentsTypes )
	{
		_reportedTypes = agentsTypes;
		_winnerId = 0;
	}
	
	@Override
	public void resetPlanner(Planner planner)
	{
		//TODO: throw an exception
	}
	
	@Override
	public String getPaymentRule()
	{
		return "FP";
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String str = "First Price auction. Reported types: ";
		for(Type t : _reportedTypes)
			str += t.toString() + "; ";
		
		return str;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt()
	{
		double maxValue = 0;
		
		for(Type t : _reportedTypes)
			if( (double)t.getTypeComponent(0, AtomicBid.Value) > maxValue )
			{
				maxValue = (double)t.getTypeComponent(0, AtomicBid.Value);
				_winnerId = t.getAgentId();
				_payment = maxValue;
			}
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add(0);
		_allocation = new Allocation();
		try 
		{
			List<Integer> allocatedBidders = new LinkedList<Integer>();
			allocatedBidders.add(_winnerId);
			
			List<Integer> allocatedBundles = new LinkedList<Integer>();
			allocatedBundles.add(0);
			
			List<Double> biddersValues = new LinkedList<Double>();
			biddersValues.add(maxValue);
			
			//_allocation.addAllocatedAgent(_winnerId, maxValue, bundle);
			_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, 0., biddersValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPayments()
	 */
	@Override
	public double[] getPayments()
	{
		double[] payments = new double[1];
		payments[0] = _payment;
		return payments;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices(List<Double> reservePrices) 
	{
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isExPostIR()
	 */
	@Override
	public boolean isExPostIR() 
	{
		return true;
	}
	
	protected Allocation _allocation;
	protected List<Type> _reportedTypes;						//Types of agents participating in the auction
	protected int _winnerId;
	protected double _payment;
}
