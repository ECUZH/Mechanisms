package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The class provides methods to compute VCG payments.
 * @author Dmitry Moor
 */
public class VCGPayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(VCGPayments.class);
	
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
		_cplexSolver = null;
	}
	
	/**
	 * The method sets up the solver
	 * @param solver CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		int numberOfAllocatedBidders = _allocation.getBiddersInvolved(0).size();
		_payments = new ArrayList<Double>();
		
		for(int i = 0; i < numberOfAllocatedBidders; ++i)
		{
			//1. Compute the SW without agent i
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			double allocatedAgentValue = 0.;
			
			List<Type> bids = new ArrayList<Type>();
			for(int j = 0; j < _numberOfAgents; ++j)
				if( _bids.get(j).getAgentId() != allocatedAgentId )
					bids.add(_bids.get(j));

			CAXOR auction = new CAXOR( _numberOfAgents - 1, _numberOfItems, bids, _costs);
			if(_cplexSolver != null)
				auction.setSolver(_cplexSolver);
			if( isLLG() )
				auction.computeWinnerDeterminationLLG();
			else
				auction.computeWinnerDetermination();
			
			double subgameSW = 0.;
			if( auction.getAllocation().getNumberOfAllocatedAuctioneers() > 0)
				subgameSW = auction.getAllocation().getAllocatedWelfare();
			
			//2. Compute the SW with agent i not taking its value into account
			double decreasedSW = 0.;
			for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
			{
				int bidderId = _allocation.getBiddersInvolved(0).get(j);
				int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
				AtomicBid allocatedBundle = _bids.get( bidderId-1 ).getAtom( itsAllocatedAtom );
				double cost  = computeCost( allocatedBundle );
				
				if(bidderId != allocatedAgentId)
					decreasedSW += allocatedBundle.getValue() - cost;
				else
				{
					allocatedAgentValue = allocatedBundle.getValue();
					decreasedSW += -1* cost;
				}
			}
			if( subgameSW - decreasedSW >  allocatedAgentValue + TOL)
			{
				_logger.error("IR violation: v=" + allocatedAgentValue + " p_vcg="+ (subgameSW - decreasedSW));
				_logger.error("Bids: " + _bids.toString());
				_logger.error("Costs: " + _costs.toString());
				throw new PaymentException("IR violation for VCG", 0);
			}
			_payments.add(subgameSW - decreasedSW);			
		}
		return _payments;
	}
	
	/**
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
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#isBudgetBalanced()
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
	
	/**
	 * The method computes an additive cost of a given bundle.
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
	private IloCplex _cplexSolver;
	
	private double TOL=1e-6;
}
