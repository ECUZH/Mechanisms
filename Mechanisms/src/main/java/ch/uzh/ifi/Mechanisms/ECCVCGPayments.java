package ch.uzh.ifi.Mechanisms;

import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

public class ECCVCGPayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(ECCVCGPayments.class);
	
	/**
	 * Constructor
	 * @param allocation allocation of the auction
	 * @param numberOfBidders number of bidders
	 * @param numberOfItems number of goods
	 * @param bids bids of bidders
	 * @param costs (additive) costs per good 
	 * @param jpmf joint probability mass function
	 * @param cplexSolver CPLEX Solver to be used by WDP
	 */
	public ECCVCGPayments(AllocationEC allocation, int numberOfBidders, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf, IloCplex cplexSolver)
	{		
		_allocation = allocation;
		_numberOfBuyers = numberOfBidders;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_jpmf = jpmf;
		_cplexSolver = cplexSolver;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		_logger.debug("-> computePayments()");
		int numberOfAllocatedBidders = _allocation.getBiddersInvolved(0).size();
		List<Double> payments = new ArrayList<Double>();
		
		for(int i = 0; i < numberOfAllocatedBidders; ++i)
		{
			//1. Compute the SW without agent i
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			_logger.debug("1. Compute the SW without agent id="+allocatedAgentId);
			
			List<Type> bids = new ArrayList<Type>();
			for(int j = 0; j < _numberOfBuyers; ++j)
				if( _bids.get(j).getAgentId() != allocatedAgentId )
					bids.add(_bids.get(j));
			
			ProbabilisticCAXOR auction = new ProbabilisticCAXOR( _numberOfBuyers - 1, _numberOfItems, bids, _costs, _jpmf);
			auction.setSolver(_cplexSolver);
			if( isLLG() ) 
				auction.setPaymentRule("EC-VCG_LLG");				//Faster allocation without using CPLEX.
			else
				auction.setPaymentRule("EC-VCG");
			
			List<Integer> allocatedAvailabilitiesPerGood = _allocation.getGoodIdsWithKnownAvailabilities(_bids, true);
			List<Double> realizationsOfAvailabilitiesPerGood = _allocation.getRealizationsOfAvailabilitiesPerGood(_bids, true);
			
			auction.computeWinnerDetermination(allocatedAvailabilitiesPerGood, realizationsOfAvailabilitiesPerGood);
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			
			double expectedReducedSW = 0.;
			if(allocation.getNumberOfAllocatedAuctioneers() > 0)
				expectedReducedSW = allocation.getExpectedWelfare();
			
			_logger.debug("P" + i + ": WDP: allocate to " + (auction.getAllocation().getNumberOfAllocatedAuctioneers() > 0 ? auction.getAllocation().getBiddersInvolved(0).toString() : "none") + ". E[sw]=" + expectedReducedSW);
			
			
			//2. Compute the SW with agent i not taking its value into account
			_logger.debug("2. Compute the SW with agent i not taking its value into account");
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
					realizedDecreasedSW += ( -1 * cost )* realizedMarginalAvailability;
			}
			
			_logger.debug("P" + i + ": Realization: " + _allocation.getRealizedRV(0, i));
			_logger.debug("P" + i + ": VCG payment = " + expectedReducedSW + " - " + realizedDecreasedSW + " = " + (expectedReducedSW - realizedDecreasedSW));
			payments.add(expectedReducedSW - realizedDecreasedSW);
		}
		_logger.debug("<- computePayments()");
		return payments; 
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/**
	 * The method checks whether this is an LLG setup.
	 * @returns true if the bids come from the LLG domain
	 */
	private boolean isLLG()
	{
		_logger.debug("Check if LLG...");
		if( (_numberOfBuyers == 3) && (_bids.size() == 3) && (_numberOfItems == 2))
			if( _bids.get(0).getNumberOfAtoms() == 1 && _bids.get(1).getNumberOfAtoms() == 1 && _bids.get(2).getNumberOfAtoms() == 1 )
				if(_bids.get(0).getInterestingSet(0).size() == 1 && _bids.get(1).getInterestingSet(0).size() == 1 && _bids.get(0).getInterestingSet(0).get(0) != _bids.get(1).getInterestingSet(0).get(0))
					if(_bids.get(2).getInterestingSet(0).size() == 2)
					{
						_logger.debug("It is LLG.");
						return true;
					}
		_logger.debug("It is not LLG.");
		return false;
	}
	
	private int _numberOfBuyers;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;					//Resulting allocation of the auction 
	private JointProbabilityMass _jpmf;					//Joint probability mass function
	private IloCplex _cplexSolver;						//CPLEX Solver
}
