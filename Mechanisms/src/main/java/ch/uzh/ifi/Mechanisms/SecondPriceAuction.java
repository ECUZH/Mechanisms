package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;

import java.util.LinkedList;
import java.util.List;

public class SecondPriceAuction implements Auction
{

	/*
	 * Constructor.
	 * @param agentsTypes - types of agents willing to participate in the auction.
	 */
	public SecondPriceAuction( List<Type> agentsTypes)
	{
		resetTypes(agentsTypes);
	}
	
	/*
	 * 
	 */
	public void resetTypes( List<Type> agentsTypes )
	{
		_reportedTypes = agentsTypes;
		_winnerId = 0;
	}
	
	@Override
	public void resetPlanner(Planner planner)
	{
	
	}
	
	@Override
	public String getPaymentRule()
	{
		return "SP";
	}
	
	/*
	 * 
	 */
	public void solveIt()
	{
		double maxValue = 0;
		
		for(Type t : _reportedTypes)
			if( (double)t.getTypeComponent(0, AtomicBid.Value) > maxValue )
			{
				maxValue = (double)t.getTypeComponent(0, AtomicBid.Value);
				_winnerId = t.getAgentId();
			}
		
		double secondMaxValue = 0;
		for(Type t : _reportedTypes)
			if( ((double)t.getTypeComponent(0, AtomicBid.Value) > secondMaxValue) && ((double)t.getTypeComponent(0, AtomicBid.Value) != maxValue) )
			{
				secondMaxValue = (double)t.getTypeComponent(0, AtomicBid.Value);
				_payment = secondMaxValue;
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
			
			List<Double> allocatedValues = new LinkedList<Double>();
			allocatedValues.add(maxValue);
			
			_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, 0., allocatedValues);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
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
	
	
	Allocation _allocation;
	private List<Type> _reportedTypes;						//Types of agents participating in the auction
	private int _winnerId;
	private double _payment;
}
