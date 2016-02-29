package ch.uzh.ifi.Mechanisms;

import ilog.cplex.IloCplex;

import java.util.LinkedList;
import java.util.List;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class ECRVCGPayments implements PaymentRule
{

	/*
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers - the number of buyers participating in the auction
	 */
	public ECRVCGPayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf, IloCplex cplexSolver)
	{
		_allocation = allocation;
		_numberOfBuyers = numberOfBuyers;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_jpmf = jpmf;
		_cplexSolver = cplexSolver;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		int numberOfAllocatedBidders = _allocation.getBiddersInvolved(0).size();
		List<Double> payments = new LinkedList<Double>();
		
		for(int i = 0; i < numberOfAllocatedBidders; ++i)
		{
			//1. Compute the SW without agent i
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			
			List<Type> bids = new LinkedList<Type>();
			for(int j = 0; j < _numberOfBuyers; ++j)
				if( _bids.get(j).getAgentId() != allocatedAgentId )
					bids.add(_bids.get(j));
			
			ProbabilisticCAXOR auction = new ProbabilisticCAXOR( _numberOfBuyers - 1, _numberOfItems, bids, _costs, _jpmf);
			auction.setSolver(_cplexSolver);
			if( isLLG() ) 
				auction.setPaymentRule("EC-VCG_LLG");				//Faster allocation without using CPLEX.
			else
				auction.setPaymentRule("EC-VCG");
			
			List<Integer> allocatedAvailabilitiesPerGood = new LinkedList<Integer>();
			List<Double> realizationsOfAvailabilitiesPerGood = new LinkedList<Double>();
			for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
			{
				int bidderId = _allocation.getBiddersInvolved(0).get(j);				
				int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
				AtomicBid allocatedBundle = _bids.get( bidderId-1 ).getAtom( itsAllocatedAtom );
				//List<Dou> realizedMarginalAvailability = _allocation.getRealizedRVsPerGood(0);//_allocation.getRealizedRV(0, j);
				
				for(int k = 0; k < _numberOfItems; ++k)
				{
					int goodId = k+1;
					allocatedAvailabilitiesPerGood.add( goodId );
					realizationsOfAvailabilitiesPerGood.add(_allocation.getRealizedRVsPerGood(0).get(k));
				}
			}
			auction.computeWinnerDetermination(allocatedAvailabilitiesPerGood, realizationsOfAvailabilitiesPerGood);
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			double expectedReducedSW = allocation.getExpectedWelfare();
			//System.out.println("SW(x_{-i}^*)= " +expectedReducedSW + " for bids: " + _bids.toString() + 
			//		           " " + allocatedAvailabilitiesPerGood.toString() + " " + realizationsOfAvailabilitiesPerGood.toString() );
			//_logger.setLevel(_logLevel);
			//_logger.debug("P" + i + ": WDP: allocate to " + auction.getAllocation().getBiddersInvolved(0).toString() + ". E[sw]=" + expectedReducedSW);
			
			
			//2. Compute the SW with agent i not taking its value into account
			double realizedDecreasedSW = 0.;
			for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
			{
				int bidderId = _allocation.getBiddersInvolved(0).get(j);
				
				int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
				AtomicBid allocatedBundle = _bids.get( bidderId-1 ).getAtom( itsAllocatedAtom );
				double realizedMarginalAvailability = _allocation.getRealizedRV(0, j);
				double cost  = allocatedBundle.computeCost(_costs);
				if(bidderId != allocatedAgentId)
					realizedDecreasedSW += (allocatedBundle.getValue() - cost )* realizedMarginalAvailability;
				else
					realizedDecreasedSW += ( -1* cost )* realizedMarginalAvailability;
			}
			payments.add(expectedReducedSW - realizedDecreasedSW);
		}
		return payments; 

	}

	/*
	 * The method checks whether this is an LLG setup.
	 * @returns true if the bids come from the LLG domain
	 */
	private boolean isLLG()
	{
		if( (_numberOfBuyers == 3) && (_bids.size() == 3) && (_numberOfItems == 2))
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
		return true;
	}

	private int _numberOfBuyers;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;						//Resulting allocation of the auction 
	private JointProbabilityMass _jpmf;					//Joint probability mass function
	private IloCplex _cplexSolver;
}
