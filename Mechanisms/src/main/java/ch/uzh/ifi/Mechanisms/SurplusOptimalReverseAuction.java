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
		
		List<IloNumVar> variables = new ArrayList<IloNumVar>(); // i-th element of the list contains the list of variables 
																// corresponding to the i-th agent
		
		//Create the optimization variables and formulate the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			IloNumVar a = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "a" + _bids.get(i).getAgentId() );
			variables.add(a);
			
			double virtualCost = 2 * _bids.get(i).getAtom(0).getValue();
			IloNumExpr term = _cplexSolver.prod( -1 * virtualCost, a);
			objective = _cplexSolver.sum(objective, term);
		}
		
		int numberOfDeterministicAllocations = (int)Math.pow(2, _numberOfBidders);
		for(int i = 0; i < numberOfDeterministicAllocations; ++i)
		{
			IloNumExpr term = _cplexSolver.constant(0.);
			for(int j = 0; j < _inducedValues.size(); ++j)
			{
				term = _cplexSolver.sum( _inducedValues.get(j).get(i), term);
			}
			
			
			int bit = 1;
			for(int j = 0; j < _numberOfBidders; ++j)
			{
				if( (i & bit) > 0 )  
				{
					//Seller j is allocated in a binary representation of the deterministic allocation, i
					term = _cplexSolver.prod(variables.get(j), term);
				}
				else
				{
					//Seller j is not allocated in a binary representation of the deterministic allocation, i
					IloNumExpr t = _cplexSolver.sum(-1, variables.get(j));
					term = _cplexSolver.prod(t, term);
				}
			}
			objective = _cplexSolver.sum(objective, term);
		}
		
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
			if( Math.abs( _cplexSolver.getValue(variables.get(i)) - 1.0 ) < 1e-6 )
			{
				allocatedBiddersIds.add( _bids.get(i).getAgentId());
				allocatedBundles.add( _bids.get(i).getAtom(0).getInterestingSet().get(0));
				biddersValues.add( _bids.get(i).getAtom(0).getValue() );
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
		/*double secondPrice = winner.computeInverseVirtualCost( secondMinVirtualCost );
		double reservePrice = ((SellerType)_bids.get(winnerIdx)).computeInverseVirtualCost(_inducedValues);
		
		// Set the payment
		payment = Arrays.asList(Math.min(reservePrice, secondPrice));
		*/
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
	private List< List<Double> > _inducedValues;						// Values of the auctioneer per DB with different deterministic allocations of other DBs
	private Allocation _allocation;
	private List<Double> _payments;
	
	private IloCplex _cplexSolver;
}
