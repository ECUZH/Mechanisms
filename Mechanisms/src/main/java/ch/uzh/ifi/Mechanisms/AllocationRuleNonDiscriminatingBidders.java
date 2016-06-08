package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class AllocationRuleNonDiscriminatingBidders extends AllocationRuleProbabilistic
{

	private static final Logger _logger = LogManager.getLogger(AllocationRuleNonDiscriminatingBidders.class);

	/**
	 * Constructor.
	 * @param bids submitted bids of bidders
	 * @param costs per-good costs
	 * @param jpmf joint probability mass function
	 */
	AllocationRuleNonDiscriminatingBidders(List<Type> bids, List<Double> costs, JointProbabilityMass jpmf, int numberOfGoods, List<int[][]> binaryBids)
	{
		super(bids, costs, jpmf);
		_numberOfBidders = _bids.size();
		_numberOfGoods = numberOfGoods;
		_binaryBids = binaryBids;
		_cplexSolver = null;
	}
	
	/**
	 * The method sets up the CPLEX solver to be used for solving the WDP.
	 * @param solver CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
	}
	
	/**
	 * (non-Javadoc)
	 * @throws IloException 
	 * @see ch.uzh.ifi.Mechanisms.IAllocationRule#computeAllocation(java.util.List, java.util.List)
	 */
	@Override
	public void computeAllocation(List<Integer> allocatedGoods, List<Double> realizedAvailabilities) throws IloException 
	{
		_logger.debug("-> computeAllocation(allocatedGoods="+ (allocatedGoods!=null?allocatedGoods.toString():"") + ", realizedAvailabilities="+ (realizedAvailabilities!=null?realizedAvailabilities.toString():"") + ")");
		_cplexSolver.clearModel();
		_cplexSolver.setOut(null);
		List<List<IloNumVar> > variables = new ArrayList<List<IloNumVar> >();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		//Create the optimization variables and set the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
				
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new ArrayList<IloNumVar>();				//Create a new variable for every atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )
			{
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				double expectedMarginalAvailability = computeExpectedMarginalAvailability( bid.getAtom(j), allocatedGoods, realizedAvailabilities );
				
				IloNumExpr term = _cplexSolver.prod((value - cost) * expectedMarginalAvailability, x);
				objective = _cplexSolver.sum(objective, term);				
			}
			variables.add(varI);
		}
		
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfGoods; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			
			for(int j = 0; j < _numberOfBidders; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);						//Binary bid of agent j
				List<IloNumVar> varI = (List<IloNumVar>)(variables.get(j));

				for( int q = 0; q < varI.size(); ++q )			//Find an atom which contains the i-th item ( itemId = i+1)
					if( binaryBid[q][i] > 0)
					{
						IloNumExpr term = _cplexSolver.prod(binaryBid[q][i], varI.get(q));
						constraint = _cplexSolver.sum(constraint, term);
					}
			}
			lp.addRow( _cplexSolver.ge(1.0, constraint, "Item_"+i) );
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			double upperBound = 1.;
			
			List<IloNumVar> varI = variables.get(i);
			for(int q = 0; q < varI.size(); ++q)
				constraint = _cplexSolver.sum(constraint, varI.get(q));
			
			lp.addRow( _cplexSolver.ge(upperBound, constraint, "Bidder"+i));
		}
		
		//Launch CPLEX to solve the problem:
		_cplexSolver.setParam(IloCplex.Param.RootAlgorithm, 2);
		try 
		{
			long t1 = System.currentTimeMillis();
			_cplexSolver.solve();
			long t2 = System.currentTimeMillis();
			//System.out.println(t2-t1);
		} 
		catch (IloException e1) 
		{
			_logger.error("MIP: " + _cplexSolver.toString());
			_logger.error("Bids: " + _bids.toString());
			e1.printStackTrace();
		}

		_allocation = new AllocationEC();
		
		List<Integer> allocatedBidders    = new ArrayList<Integer>();
		List<Integer> allocatedBundles    = new ArrayList<Integer>();
		List<Double> buyersExpectedValues = new ArrayList<Double>();
		List<Double> realizedRandomVars   = new ArrayList<Double>();
		List<Double> allocatedBiddersValues = new ArrayList<Double>();
		List<Double> realizedRVsPerGood = new ArrayList<Double>();
		
		double sellerExpectedCost = 0.;
		double[] realizedSample = _jpmf.getSample();
		for(Double rRV : realizedSample)
			realizedRVsPerGood.add(rRV);
		
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
			{
				if( Math.abs( _cplexSolver.getValue(variables.get(i).get(j)) - 1.0 ) < 1e-6 )			//if allocated
				{
					AtomicBid allocatedAtom = _bids.get(i).getAtom(j);
					double value = allocatedAtom.getValue();
					double cost = allocatedAtom.computeCost(_costs);
					double expectedMarginalAvailability = computeExpectedMarginalAvailability( allocatedAtom, allocatedGoods, realizedAvailabilities );
					sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
										allocatedBiddersValues, realizedSample, allocatedAtom, value, cost, expectedMarginalAvailability, j);
					break;
				}
			}
		}
		
		if(allocatedBidders.size() > 0)
			try
			{
				_allocation.addAllocatedAgents( 0, allocatedBidders, allocatedBundles, sellerExpectedCost, buyersExpectedValues, false);
				_allocation.addRealizedRVs(realizedRandomVars);
				_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
				_allocation.setAllocatedBiddersValues(allocatedBiddersValues);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		_logger.debug("<- computeAllocation(...)");
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.IAllocationRule#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	private int _numberOfBidders;					//Number of bidders
	private int _numberOfGoods;						//Number of goods
	private List<int[][]> _binaryBids;				//Bids converted into a binary matrix format

	private AllocationEC _allocation;				//An allocation object
	
	private IloCplex _cplexSolver;					//CPLEX solver
}
