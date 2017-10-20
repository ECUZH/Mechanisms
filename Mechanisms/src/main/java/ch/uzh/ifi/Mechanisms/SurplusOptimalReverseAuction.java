package ch.uzh.ifi.Mechanisms;

import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

/**
 * The class implements a surplus optimal reverse auction with a single item.
 * @author Dmitry Moor
 *
 */
public class SurplusOptimalReverseAuction implements Auction 
{

	private static final Logger _logger = LogManager.getLogger(SurplusOptimalReverseAuction.class);
	
	/**
	 * Constructor
	 * @param numberOfBidders number of sellers
	 * @param bids bids of sellers
	 * @param value value of the auctioneer for the item
	 */
	public SurplusOptimalReverseAuction(int numberOfBidders, List<Type> bids, double value)
	{
		_numberOfBidders = numberOfBidders;
		_bids = bids;
		_value = value;
		_allocation = new Allocation();
	}
	
	/**
	 * The method solves the winner determination problem.
	 */
	public void computeWinnerDetermination()
	{
		double maxVirtualSurplus = 0.;
		int maxIdx = -1;
		
		// Find the agent that delivers the highest virtual surplus
		for(int i = 0; i < _bids.size(); ++i)
		{
			SellerType t = (SellerType)_bids.get(i);
			
			if( (_value >= t.computeVirtualCost()) && (_value-t.computeVirtualCost() > maxVirtualSurplus) )
			{
				maxVirtualSurplus = _value - t.computeVirtualCost();
				maxIdx = i;
			}
		}
		
		// Allocate
		List<Integer> allocatedBidders = Arrays.asList( _bids.get(maxIdx).getAgentId() );
		List<Integer> allocatedBundles = _bids.get(maxIdx).getInterestingSet(0);
		List<Double> biddersValues = Arrays.asList( _bids.get(maxIdx).getAtom(0).getValue() );
		try 
		{
			_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, _value, biddersValues);
		} 
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices(List<Double> reservePrices) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double[] getPayments() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Allocation getAllocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetTypes(List<Type> agentsTypes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetPlanner(Planner planner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPaymentRule() {
		// TODO Auto-generated method stub
		return null;
	}

	private int _numberOfBidders;								// Number of bidders (sellers) in the auction
	private List<Type> _bids;									// Bids of the sellers
	private double _value;										// Value of the auctioneer for the good
	private Allocation _allocation;
}
