package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
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
	 * @param bids bids of sellers
	 * @param value value of the auctioneer for the item
	 */
	public SurplusOptimalReverseAuction(List<Type> bids, double value)
	{
		_numberOfBidders = bids.size();
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
			_logger.debug("Consider seller " + t.getAgentId() + " w. cost " + t.getAtom(0).getValue() + " and virtual cost " + t.getItsVirtualCost());
			
			if( (_value >= t.getItsVirtualCost()) && (_value - t.getItsVirtualCost() > maxVirtualSurplus) )
			{
				maxVirtualSurplus = _value - t.getItsVirtualCost();
				maxIdx = i;
			}
		}
		
		if( maxIdx >= 0 )
		{
			_logger.debug("The best seller is Id=" + _bids.get(maxIdx).getAgentId());
		
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
		else
		{
			_logger.debug("Nobody is allocated.");
		}
	}
	
	/**
	 * The method computes payments as VCG over virtual costs + reserve prices.
	 * @return payment of the winner
	 */
	public List<Double> computePayments()
	{
		List<Double> payment = new ArrayList<Double>();
		
		if(_allocation.getNumberOfAllocatedAuctioneers() < 1) throw new RuntimeException("No seller was allocated. Can't compute payments.");
		
		// Identify the index of the winner
		int winnerIdx = 0;
		for(int i = 0; i < _bids.size(); ++i)
			if( _bids.get(i).getAgentId() == _allocation.getBiddersInvolved(0).get(0) )
			{
				winnerIdx = i;
				break;
			}
		
		// Find the second smallest virtual cost
		double secondMinVirtualCost = 1e+9;
		SellerType winner = (SellerType)_bids.get(winnerIdx);
		for(int i = 0; i < _bids.size(); ++i)
		{
			SellerType bidderI = (SellerType)_bids.get(i);
			if( bidderI.getAgentId() != winner.getAgentId() )
				if( bidderI.getItsVirtualCost() < secondMinVirtualCost )
					secondMinVirtualCost = bidderI.getItsVirtualCost();
		}
		//double secondMinCost = _bids.get(winnerIdx).getAtom(0).getValue();
		//for(int i = 0; i < _bids.size(); ++i)
		//	if( _bids.get(i).getAgentId() != _allocation.getBiddersInvolved(0).get(0) )
		//		if( _bids.get(i).getAtom(0).getValue() < secondMinCost )
		//			secondMinCost = _bids.get(i).getAtom(0).getValue();
		
		// Compute the reserve price
		double secondPrice = winner.computeInverseVirtualCost( secondMinVirtualCost );
		double reservePrice = ((SellerType)_bids.get(winnerIdx)).computeInverseVirtualCost(_value);
		
		// Set the payment
		payment = Arrays.asList(Math.min(reservePrice, secondPrice));
		
		return payment;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception
	{
		computeWinnerDetermination();
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
			_payments = computePayments();
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices(List<Double> reservePrices)
	{
		// TODO Auto-generated method stub
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getPayments()
	 */
	@Override
	public double[] getPayments() throws Exception 
	{
		double[] payments = new double[_payments.size()];
		for(int i = 0; i < _payments.size(); ++i)
			payments[i] = _payments.get(i);
		return payments;
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	@Override
	public void resetTypes(List<Type> agentsTypes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetPlanner(Planner planner) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getPaymentRule()
	 */
	@Override
	public String getPaymentRule() 
	{
		return "VCG+Reserve";
	}

	private int _numberOfBidders;								// Number of bidders (sellers) in the auction
	private List<Type> _bids;									// Bids of the sellers
	private double _value;										// Value of the auctioneer for the good
	private Allocation _allocation;
	private List<Double> _payments;
}
