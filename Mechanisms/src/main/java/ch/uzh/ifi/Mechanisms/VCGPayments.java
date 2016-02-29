package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Dmitry Moor
 *
 */
public class VCGPayments implements PaymentRule
{

	/**
	 * A Constructor.
	 * @param welfare - a total welfare of computed allocation
	 * @param allBids - a list of bids submitted by all agents
	 * @param allocatedAgents - IDs of allocated agents
	 * @param numberOfItems - the number of items in the CA
	 * @param costs costs of items (goods) 
	 */
	public VCGPayments(Allocation allocation, List<Type> allBids, List<Integer> quantitiesOfItems, int numberOfItems, List<Double> costs)
	{
		if( numberOfItems != quantitiesOfItems.size() ) throw new RuntimeException("The number of items should correspond to the size of the list.");

		_costs = costs;
		_allocation = allocation;

		_bids = allBids;
		_unitsOfItems = quantitiesOfItems;
		_numberOfItems = numberOfItems;
		_numberOfAgents = _bids.size();
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		int numberOfAllocatedBidders = _allocation.getBiddersInvolved(0).size();
		_payments = new LinkedList<Double>();
		
		for(int i = 0; i < numberOfAllocatedBidders; ++i)
		{
			//1. Compute the SW without agent i
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			double allocatedAgentValue = 0.;
			
			List<Type> bids = new LinkedList<Type>();
			for(int j = 0; j < _numberOfAgents; ++j)
				if( _bids.get(j).getAgentId() != allocatedAgentId )
					bids.add(_bids.get(j));

			CAXOR auction = new CAXOR( _numberOfAgents - 1, _numberOfItems, bids, _costs);
			if( isLLG() )
				auction.computeWinnerDeterminationLLG();
			else
				auction.computeWinnerDetermination();
			
			double expectedReducedSW = auction.getAllocation().getAllocatedWelfare();
			
			//2. Compute the SW with agent i not taking its value into account
			double realizedDecreasedSW = 0.;
			for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
			{
				int bidderId = _allocation.getBiddersInvolved(0).get(j);
				
				int itsAllocatedAtom = _allocation.getAllocatedBundlesByIndex(0).get(j);
				AtomicBid allocatedBundle = _bids.get( bidderId-1 ).getAtom( itsAllocatedAtom );
				double cost  = computeCost( allocatedBundle );
				if(bidderId != allocatedAgentId)
					realizedDecreasedSW += allocatedBundle.getValue() - cost;
				else
				{
					allocatedAgentValue = allocatedBundle.getValue();
					realizedDecreasedSW += -1* cost;
				}
			}
			if( expectedReducedSW - realizedDecreasedSW >  allocatedAgentValue)
			{
				System.out.println(">> " + allocatedAgentValue + " vcg="+ (expectedReducedSW - realizedDecreasedSW));
				System.out.println(">> bids: " + _bids.toString());
				System.out.println(">> Costs: " + _costs.toString());
			}
			_payments.add(expectedReducedSW - realizedDecreasedSW);
			
			
			
			//CAXOR ca = new CAXOR( _numberOfAgents-1, _numberOfItems, bids);		//Solve the CA without Agent i
			//MultiUnitCAXOR ca = new MultiUnitCAXOR( _numberOfAgents-1, _numberOfItems, _unitsOfItems, bids);	//Solve the CA without Agent i
			//ca.computeWinnerDetermination();
			//double Wi = ca.getAllocation().getAllocatedWelfare();												//Allocation without Agent i
			//double payment = Wi - (W - _allocation.getAgentAllocatedWelfareContribution(winnerIdx));			//VCG payment for Agent i
			
			//if( payment < - (1e-4) ) throw new RuntimeException("Negative VCG payments ");
			
			//_payments.add(payment < 0 ? 0 : payment);
			
			//winnerIdx += 1;
		}
		return _payments;
	}
	
	/*
	 * The method checks whether this is an LLG setup.
	 * @returns true if the bids come from the LLG domain
	 */
	private boolean isLLG()
	{
		if( (_numberOfAgents == 3) && (_bids.size() == 3) && (_numberOfItems == 2))
			if( _bids.get(0).getNumberOfAtoms() == 1 && _bids.get(1).getNumberOfAtoms() == 1 && _bids.get(2).getNumberOfAtoms() == 1 )
				if(_bids.get(0).getInterestingSet(0).size() == 1 && _bids.get(1).getInterestingSet(0).size() == 1 && _bids.get(0).getInterestingSet(0).get(0) != _bids.get(1).getInterestingSet(0).get(0))
					if(_bids.get(2).getInterestingSet(0).size() == 2)
						return true;
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		if( _payments == null) throw new RuntimeException("Payments are not computed yet");
		
		double totalPayment = 0.;
		for(Double p : _payments)
			totalPayment += p;
		
		return totalPayment >= 0 ? true : false;
	}
	
	/*
	 * The method computes the additive cost of a given bundle.
	 * @param atom - an atomic bid of an agent containing the bundle
	 * @return the cost of the bundle, i.e., the sum of costs of all items in the bundle
	 */
	public double computeCost(AtomicBid atom)
	{
		double cost = 0.;
		
		for(int item : atom.getInterestingSet())
			cost += _costs.get( item - 1 );
		
		return cost;
	}
	
	private List<Double> _costs;
	private List<Double> _payments;
	private List<Type> _bids;
	private List<Integer> _unitsOfItems;
	private int _numberOfItems;
	private int _numberOfAgents;
	private Allocation _allocation;
	
	//private Logger _logger;
}
