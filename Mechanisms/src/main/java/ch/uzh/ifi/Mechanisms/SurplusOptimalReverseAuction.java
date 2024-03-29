package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
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
	public SurplusOptimalReverseAuction(List<SellerType> bids, List< List<Double> > inducedValues)
	{
		List<Integer> dbIDs = new ArrayList<Integer>();
		
		//for(SellerType s : bids)
		//	if( ! dbIDs.contains(s.getAtom(0).getInterestingSet().get(0)))
		//		dbIDs.add( s.getAtom(0).getInterestingSet().get(0) );
			
		_numberOfDBs = inducedValues.size();
		if( inducedValues.get(0).size() < Math.pow(2, _numberOfDBs) ) throw new RuntimeException("Not all induced values available: " + inducedValues.get(0).size() + "/"+_numberOfDBs);
		if( inducedValues.get(0).size() > Math.pow(2, _numberOfDBs) ) throw new RuntimeException("Too many induced values specified.");
		
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
		List<IloNumVar> bundleAllocationVars = new ArrayList<IloNumVar>();  // i-th element of this list contains a binary variable
																			// indicating whether the i-th group of sellers is allocated.
																			// Here, i is a binary representation of the allocation of sellers within the group
		
		//Create the optimization variables and formulate the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		// Allocation variables per seller; Allocated negative virtual cost in the objective f-n
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			IloNumVar a = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "a" + _bids.get(i).getAgentId() );
			sellerAllocationVars.add(a);
			
			double virtualCost = ((SellerType)_bids.get(i)).getItsVirtualCost();
			IloNumExpr term = _cplexSolver.prod( -1 * virtualCost, a);
			objective = _cplexSolver.sum(objective, term);
		}
		
		int numberOfDeterministicAllocationsDBs = (int)Math.pow(2, _numberOfDBs);
		for(int i = 0; i < numberOfDeterministicAllocationsDBs; ++i)
		{
			// Total induced value of all DBs when the det. allocation of DBs is i
			IloNumExpr totalInducedValueI = _cplexSolver.constant(0.);
			for(int j = 0; j < _inducedValues.size(); ++j)
				totalInducedValueI = _cplexSolver.sum( _inducedValues.get(j).get(i), totalInducedValueI);
			
			// z indicates whether a particular set of DBs is allocated; Add the allocated value to the objective f-n
			IloNumVar z = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "z" + i );
			objective = _cplexSolver.sum(objective, _cplexSolver.prod(z, totalInducedValueI));
			bundleAllocationVars.add(z);
			
			int bit = 1;
			for(int k = 0; k < _numberOfDBs; ++k)
			{
				IloNumExpr constraintK = _cplexSolver.prod(1., z);
				for(int j = 0; j < _numberOfBidders; ++j)
				{
					int dbId = _bids.get(j).getAtom(0).getInterestingSet().get(0);
					
					if( dbId - 1 == k)
					{
						if( (i & bit) > 0 )  									// If DB_k is allocated in det. allocation i
						{
							// DB_k is allocated in a binary representation of the deterministic allocation i,
							//... then 0 >= (z - ... -a_j)
							constraintK = _cplexSolver.sum(constraintK, _cplexSolver.prod(-1, sellerAllocationVars.get(j)));
						}
						else
						{
							//Seller j is not allocated in a binary representation of the deterministic allocation i, 
							//... then (a_j - 1)
							constraintK = _cplexSolver.sum(constraintK, _cplexSolver.sum(-1, sellerAllocationVars.get(j)));
						}
					}
				}
				lp.addRow( _cplexSolver.ge(0., constraintK, "Bundle_"+i + "," + k) );
				bit = bit << 1;
			}
		}
		
		// Allocation variables per bundle; Allocation constraints connecting bundles and sellers 
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
			_logger.debug("SOL: " + sellerAllocationVars.get(i).getName() + "="+_cplexSolver.getValue(sellerAllocationVars.get(i)));
			if( Math.abs( _cplexSolver.getValue(sellerAllocationVars.get(i)) - 1.0 ) < 1e-6 )
			{
				allocatedBiddersIds.add( _bids.get(i).getAgentId());
				allocatedBundles.add( _bids.get(i).getAtom(0).getInterestingSet().get(0));
				biddersValues.add( _bids.get(i).getAtom(0).getValue() );					// Costs of sellers
				deterministicAllocation += (int)Math.pow(2, _bids.get(i).getInterestingSet(0).get(0) - 1);
			}
		}
		for(int i = 0; i < bundleAllocationVars.size(); ++i)
			_logger.debug("SOL: " + bundleAllocationVars.get(i).getName() + "="+_cplexSolver.getValue(bundleAllocationVars.get(i)));
		
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
		List<Double> payment = new ArrayList<Double>();

		// 0. Compute the virtual surplus of the optimal allocation
		double totalInducedValue = _allocation.getAuctioneersAllocatedValue(0);
		double totalVirtualCost = 0.;
		for( int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i )
			totalVirtualCost += ((SellerType)_bids.get( _allocation.getBiddersInvolved(0).get(i) - 1)).getItsVirtualCost();
		
		double virtualSurplus = totalInducedValue - totalVirtualCost;
		
		// 1. Compute payments for every allocated bidder
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			// 1.1 Remove bidder i
			List<SellerType> reducedBids = new ArrayList<SellerType>();
			_logger.debug("Remove bidder id = " + _allocation.getBiddersInvolved(0).get(i));
			for(int j = 0; j < _bids.size(); ++j)
				if( _bids.get(j).getAgentId() != _allocation.getBiddersInvolved(0).get(i) )
				{
					SellerType s = _bids.get(j);
					List<Integer> bundle = Arrays.asList(s.getAtom(0).getInterestingSet(0).get(0));
					AtomicBid atom = new AtomicBid(s.getAgentId(), bundle, s.getAtom(0).getValue());
					SellerType seller = new SellerType(atom, s.getDistribution(), s.getMean(), s.getVariance());
					//reducedBids.add(new SellerType(_bids.get(j)));
					reducedBids.add(seller);
				}	
			// 1.2 Collect induced values of all DBs
			List< List<Double> > inducedValues = new ArrayList<List<Double> >();

			// Check if there is only a single seller producing the DB
			boolean isMonopolist = true;
			int dbIdToRemove = _bids.get( _allocation.getBiddersInvolved(0).get(i) - 1 ).getInterestingSet(0).get(0);
			for(SellerType s: reducedBids)
				if( s.getAtom(0).getInterestingSet(0).get(0) == dbIdToRemove)
				{
					isMonopolist = false;
					break;
				}
			
			SurplusOptimalReverseAuction auction;
			if( isMonopolist )						//If there's a single seller producing the DB, then removal of the seller leads to the removal of the DB
			{
				for(SellerType s: reducedBids)		//Removal of the DB creates a gap in the enumeration of other DBs. This gap must be closed
					if( s.getAtom(0).getInterestingSet(0).get(0) > dbIdToRemove )
						s.getAtom(0).getInterestingSet(0).set(0, s.getAtom(0).getInterestingSet(0).get(0) - 1);
					
				for(int k = 0; k < _inducedValues.size(); ++k) 						// (For all DBs)
				{
					if( dbIdToRemove - 1 == k )
						continue;
					int numberOfDeterministicAllocationsDBs = (int)Math.pow(2, _numberOfDBs);
					List<Double> inducedValuesK = new ArrayList<Double>();
					for(int j = 0; j < numberOfDeterministicAllocationsDBs; ++j)
					{
						if( Math.floor( j/Math.pow(2, dbIdToRemove-1) ) % 2 == 0 )
							inducedValuesK.add(_inducedValues.get(k).get(j));
					}
					_logger.debug("Induced values of DB_"+k+" for different det. allocations: " + inducedValuesK.toString());
					inducedValues.add(inducedValuesK);
				}
				auction = new SurplusOptimalReverseAuction(reducedBids, inducedValues);
			}
			else			//If the seller is a not unique producer, then the induced values of DBs do not change
				auction = new SurplusOptimalReverseAuction(reducedBids, _inducedValues);
			
			// 1.3 Solve the reduced auction
			//SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(reducedBids, _inducedValues);
			auction.setSolver(_cplexSolver);
			auction.computeWinnerDetermination();
			
			double reducedTotalInducedValue = auction.getAllocation().getAuctioneersAllocatedValue(0);
			double reducedTotalVirtualCost = 0.;
			for( int j = 0; j < auction.getAllocation().getBiddersInvolved(0).size(); ++j )
				reducedTotalVirtualCost += ((SellerType)_bids.get( auction.getAllocation().getBiddersInvolved(0).get(j) - 1)).getItsVirtualCost();
			
			double reducedVirtualSurplus = reducedTotalInducedValue - reducedTotalVirtualCost;
			_logger.debug("reducedVirtualSurplus = " + reducedTotalInducedValue + " - " + reducedTotalVirtualCost + " = " + reducedVirtualSurplus);
			
			// 2. Compute the payment
			SellerType allocatedBidder = (SellerType)_bids.get(_allocation.getBiddersInvolved(0).get(i)-1); 
			double p = allocatedBidder.computeInverseVirtualCost( allocatedBidder.getItsVirtualCost() + virtualSurplus - reducedVirtualSurplus );
			_logger.debug("p=phi^{-1} (" + allocatedBidder.getItsVirtualCost() + " + " + virtualSurplus + " - " + reducedVirtualSurplus + ")=" + p);
			
			if( allocatedBidder.getDistribution() == Distribution.UNIFORM )
			{
				if( p > allocatedBidder.getMean() * 2)
				{
					p = 2 * allocatedBidder.getMean();
					_logger.debug("The payment must be within the support of the costs distribution.");
				}
			}
			else throw new RuntimeException("Not implemented for other distributions: " + allocatedBidder.getDistribution());
			
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
		return "BORA";
	}

	/**
	 * The method sets up a CPLEX solver for WDP.
	 * @param solver CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
	}
	
	private int _numberOfBidders;								// Number of bidders (sellers) in the auction
	private int _numberOfDBs;
	private List<SellerType> _bids;								// Bids of the sellers
	private List< List<Double> > _inducedValues;				// Values of the auctioneer per DB with different deterministic allocations of other DBs
	private Allocation _allocation;								// Optimal allocation of bidders
	private List<Double> _payments;								// Payments of the winners
	
	private IloCplex _cplexSolver;
}
