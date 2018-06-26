package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

/**
 * The class implements a surplus optimal reverse auction (BORA).
 * @author Dmitry Moor
 *
 */
public class SurplusOptimalReverseAuction implements Auction
{

	private static final Logger _logger = LogManager.getLogger(SurplusOptimalReverseAuction.class);
	
	/**
	 * Constructor
	 * @param bids bids of sellers
	 * @param inducedValues induced values of DBs for different deterministic allocations
	 */
	public SurplusOptimalReverseAuction(List<Type> bids, List< List<Double> > inducedValues)
	{
		if( inducedValues.get(0).size() < Math.pow(2, bids.size()) ) throw new RuntimeException("Not all induced values available: " + inducedValues.get(0).size() + "/"+bids.size());
		if( inducedValues.get(0).size() > Math.pow(2, bids.size()) ) throw new RuntimeException("Too many induced values specified.");
		
		_numberOfBidders = bids.size();
		_bids = bids;
		_inducedValues = inducedValues;
		_allocation = new Allocation();
	}
	
	/**
	 * The method solves the winner determination problem.
	 * @throws Exception 
	 */
	public void computeWinnerDetermination() throws Exception
	{
		if( _cplexSolver == null)
			_cplexSolver = new IloCplex();
		else
			_cplexSolver.clearModel();
		_cplexSolver.setOut(null);
		
		List<IloNumVar> sellerAllocationVars = new ArrayList<IloNumVar>();  // i-th element of the list contains a binary variable 
																			// indicating whether s_i is allocated
		//List<IloNumVar> bundleAllocationVars = new ArrayList<IloNumVar>();  // i-th element of this list contains a binary variable
																			// indicating whether the i-th group of sellers is allocated.
																			// Here, i is a binary representation of the allocation of sellers within the group
		
		//Create the optimization variables and formulate the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			IloNumVar a = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "a" + _bids.get(i).getAgentId() );
			sellerAllocationVars.add(a);
			
			double virtualCost = 2 * _bids.get(i).getAtom(0).getValue();
			IloNumExpr term = _cplexSolver.prod( -1 * virtualCost, a);
			objective = _cplexSolver.sum(objective, term);
		}
		
		int numberOfDeterministicAllocations = (int)Math.pow(2, _numberOfBidders);
		for(int i = 0; i < numberOfDeterministicAllocations; ++i)
		{
			IloNumExpr totalInducedValueI = _cplexSolver.constant(0.);
			for(int j = 0; j < _inducedValues.size(); ++j)
				totalInducedValueI = _cplexSolver.sum( _inducedValues.get(j).get(i), totalInducedValueI);
			
			
			IloNumVar z = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "z" + i );
			objective = _cplexSolver.sum(objective, _cplexSolver.prod(z, totalInducedValueI));
			
			int bit = 1;
			for(int j = 0; j < _numberOfBidders; ++j)
			{
				IloNumExpr constraintJ = _cplexSolver.prod(1., z);
				if( (i & bit) > 0 )  
				{
					//Seller j is allocated in a binary representation of the deterministic allocation i
					constraintJ = _cplexSolver.sum(constraintJ, _cplexSolver.prod(-1, sellerAllocationVars.get(j)));
				}
				else
				{
					//Seller j is not allocated in a binary representation of the deterministic allocation i
					constraintJ = _cplexSolver.sum(constraintJ, _cplexSolver.sum(-1, sellerAllocationVars.get(j)));
				}
				lp.addRow( _cplexSolver.ge(0., constraintJ, "Bundle_"+i + "," + j) );
				bit = bit << 1;
			}
		}
		_logger.debug("Obj: " + objective.toString());
		_logger.debug("Constr: " + lp.toString());
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		_cplexSolver.solve();
		
		_allocation = new Allocation();
		List<Integer> allocatedBiddersIds = new ArrayList<Integer>();
		List<Integer> allocatedBundles = new ArrayList<Integer>();
		List<Double> biddersValues = new ArrayList<Double>();
		int deterministicAllocation = 0;
		double autioneerValue = 0.;
		
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			if( Math.abs( _cplexSolver.getValue(sellerAllocationVars.get(i)) - 1.0 ) < 1e-6 )
			{
				allocatedBiddersIds.add( _bids.get(i).getAgentId());
				allocatedBundles.add( _bids.get(i).getAtom(0).getInterestingSet().get(0));
				biddersValues.add( _bids.get(i).getAtom(0).getValue() );					// Costs of sellers
				deterministicAllocation += (int)Math.pow(2, i);
			}
		}
		
		for(int i = 0; i < _inducedValues.size(); ++i)
			autioneerValue += _inducedValues.get(i).get(deterministicAllocation);
		
		_allocation.addAllocatedAgent(0, allocatedBiddersIds, allocatedBundles, autioneerValue, biddersValues);
	}
	
	/**
	 * The method computes payments as VCG over virtual costs + reserve prices.
	 * @return payment of the winner
	 * @throws Exception 
	 */
	public List<Double> computePayments() throws Exception
	{
		/////!!!!!!!!!!!!!!!!!!!!!!! TODO: implement generic virtual cost
		List<Double> payment = new ArrayList<Double>();

		double totalInducedValue = _allocation.getAuctioneersAllocatedValue(0);
		double totalVirtualCost = 0.;
		for( int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i )
			totalVirtualCost += 2 * _bids.get( _allocation.getBiddersInvolved(0).get(i) - 1).getAtom(0).getValue();
		
		double virtualSurplus = totalInducedValue - totalVirtualCost;
		
		//if(_allocation.getNumberOfAllocatedAuctioneers() < 1) throw new RuntimeException("No seller was allocated. Can't compute payments.");
	
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			// 1. Remove bidder i
			List<Type> reducedBids = new ArrayList<Type>();
			for(int j = 0; j < _bids.size(); ++j)
				if( _bids.get(j).getAgentId() != _allocation.getBiddersInvolved(0).get(i) )
					reducedBids.add(_bids.get(j));
	
			// 2. Collect induced values of all DBs 
			List< List<Double> > inducedValues = new ArrayList<List<Double> >();
			for(int k = 0; k < _inducedValues.size(); ++k) 						// (For all DBs)
			{
				int numberOfDeterministicAllocations = (int)Math.pow(2, _bids.size());
				List<Double> inducedValuesK = new ArrayList<Double>();
				for(int j = 0; j < numberOfDeterministicAllocations; ++j)
					if( Math.floor( j/Math.pow(2, i) ) % 2 == 0 )
						inducedValuesK.add(_inducedValues.get(k).get(j));

				inducedValues.add(inducedValuesK);
			}
			
			// 3. Solve the reduced auction
			SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(reducedBids, inducedValues);
			auction.computeWinnerDetermination();
			
			double reducedTotalInducedValue = auction.getAllocation().getAuctioneersAllocatedValue(0);
			double reducedTotalVirtualCost = 0.;
			for( int j = 0; j < auction.getAllocation().getBiddersInvolved(0).size(); ++j )
				reducedTotalVirtualCost += 2 * _bids.get( auction.getAllocation().getBiddersInvolved(0).get(j) - 1).getAtom(0).getValue();
			
			double reducedVirtualSurplus = reducedTotalInducedValue - reducedTotalVirtualCost;
			_logger.debug("reducedVirtualSurplus = " + reducedTotalInducedValue + " - " + reducedTotalVirtualCost + " = " + reducedVirtualSurplus);
			
			// 4. Compute the payment
			double p = 0.5 * ( 2*_bids.get(_allocation.getBiddersInvolved(0).get(i)-1).getAtom(0).getValue() + virtualSurplus - reducedVirtualSurplus );
			_logger.debug("p=phi^{-1} (" + 2*_bids.get(_allocation.getBiddersInvolved(0).get(i)-1).getAtom(0).getValue() + " + " +
			              virtualSurplus + " - " + reducedVirtualSurplus + ")=" + p);
			payment.add(p);
		}
		
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
	private List< List<Double> > _inducedValues;				// Values of the auctioneer per DB with different deterministic allocations of other DBs
	private Allocation _allocation;
	private List<Double> _payments;
	
	private IloCplex _cplexSolver;
}
