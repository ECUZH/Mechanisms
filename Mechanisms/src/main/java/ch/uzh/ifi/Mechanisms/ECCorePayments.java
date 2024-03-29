package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;

/**
 * The class implements a general minimum-revenue Execution-contingent core-selecting payment rule.
 * @author Dmitry
 *
 */
public class ECCorePayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(ECCorePayments.class);
	
	/*
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers - the number of buyers participating in the auction
	 */
	public ECCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf)
	{
		_allocation = allocation;
		_numberOfBuyers = numberOfBuyers;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_binaryBids = binaryBids;
		_jpmf = jpmf;
		_isExternalSolver = false;
		_cplexSolver = null;
	}
	
	/*
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers - the number of buyers participating in the auction
	 */
	public ECCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf, IloCplex solver)
	{
		_allocation = allocation;
		_numberOfBuyers = numberOfBuyers;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_binaryBids = binaryBids;
		_jpmf = jpmf;
		_isExternalSolver = false;
		_cplexSolver = null;
		
		setSolver(solver);
	}
	
	/*
	 * The method sets up the CPLEX solver.
	 * @param solver - a CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		//if( solver == null) 	throw new RuntimeException("Solver was not initialized");
		_cplexSolver = solver;
		_isExternalSolver = true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		if( _cplexSolver == null )
			try 
			{
				_cplexSolver = new IloCplex();
			}
			catch (IloException e)
			{
				e.printStackTrace();
			}

		_cplexSolver.setOut(null);
		
		if( _allocation.getNumberOfAllocatedAuctioneers() <= 0 ) 
		{
			_payments = new LinkedList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		_logger.debug("Compute EC-VCG payments: " + _bids.toString());
		PaymentRule ecvcgRule = new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver);
		List<Double> ecvcgPayments = ecvcgRule.computePayments();
		_payments = ecvcgPayments;
		_logger.debug("EC-VCG payments: " + _payments.toString());
		
		List<Integer> blockingCoalition = new LinkedList<Integer>();
		double z = computeSEP(ecvcgPayments, blockingCoalition);
		double totalPayment = computeTotalPayment(ecvcgPayments);
		double totalCost = 0;

		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		IloNumVar[] pi = new IloNumVar[_allocation.getBiddersInvolved(0).size() ];
		IloNumExpr objectiveLP = _cplexSolver.constant(1e-12);
		IloNumExpr objectiveQP = _cplexSolver.constant(1e-12);
		
		//Create optimization variables and formulate the objective function
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			int itsId =  _allocation.getBiddersInvolved(0).get(i);
			int itsAllocatedBundleIdx = _allocation.getAllocatedBundlesOfTrade(0).get(i);
			AtomicBid itsAllocatedBundle = _bids.get( itsId - 1).getAtom( itsAllocatedBundleIdx );
			double expectedValue = itsAllocatedBundle.getValue() * computeExpectedMarginalAvailability(itsAllocatedBundle);
			
			totalCost += itsAllocatedBundle.computeCost(_costs)  * _allocation.getRealizedRV(0, i/*itsId - 1*/);
			
			if( ecvcgPayments.get(i) > expectedValue )
			{
				_logger.debug("Empty Core. p_i^{EC-VCG}=" + ecvcgPayments.get(i)+" > " + expectedValue + "= E[v_i]. VCG:" + ecvcgPayments.toString());
				_logger.debug("Bids: " + _bids.toString());
				_logger.debug("Costs" + _costs.toString());
				for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					_logger.debug("Realization for bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " is " + _allocation.getRealizedRV(0, j));
				_logger.debug("EC-VCG: " + ecvcgPayments.toString());
				_logger.debug("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
				_logger.debug("Total payment: " + totalPayment);
				throw new PaymentException("Empty Core",0);
			}
			IloNumVar x = _cplexSolver.numVar(ecvcgPayments.get(i), expectedValue, IloNumVarType.Float, "pi_" + itsId);
			pi[i] = x;
			objectiveLP = _cplexSolver.sum(objectiveLP, pi[i]);
			
			IloNumExpr term = _cplexSolver.sum(-1*ecvcgPayments.get(i), pi[i]);
			term = _cplexSolver.prod(term, term);
			objectiveQP = _cplexSolver.sum(objectiveQP, term);
		}
		_cplexSolver.add(_cplexSolver.minimize(objectiveLP));
				
		int constraintIdBPO = 0;
		
		while( z > totalPayment /*- totalCost*/ + TOL )
		{
			_logger.debug("z="+z+" totalPayment="+totalPayment+" titalCost=" + totalCost );
			
			//Create optimization constraints
			double cnst = 0.;
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				for(int j  = 0; j < blockingCoalition.size(); ++j)
				{
					int allocatedAgent = _allocation.getBiddersInvolved(0).get(i);
					if( allocatedAgent == blockingCoalition.get(j) )
					{
						cnst += _payments.get(i);
						break;
					}
				}
			
			IloNumExpr constraint = _cplexSolver.constant(0);

			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
			{
				boolean isBlocking = false;
				for(int j = 0; j < blockingCoalition.size(); ++j)
				{
					int allocatedAgent = _allocation.getBiddersInvolved(0).get(i);
					if( allocatedAgent == blockingCoalition.get(j) )
					{
						isBlocking = true;
						break;
					}
				}
				if( ! isBlocking)
					constraint = _cplexSolver.sum(constraint, pi[i]);
			}
			
			IloRange range = _cplexSolver.range(z - cnst + totalCost, constraint, Double.MAX_VALUE, "c"+constraintIdBPO);
			lp.addRow(range);
			constraintIdBPO += 1;
			
			try 
			{
				_cplexSolver.solve();
			} 
			catch (IloException e1) 
			{
				e1.printStackTrace();
			}
			
			double mu = -1.;
			try 
			{
				mu = _cplexSolver.getObjValue();
			} 
			catch (IloException e1) 
			{
				if(e1.getMessage().contains("CPLEX Error  1217: No solution exists."))
					throw new PaymentException("Empty Core",1);
				else
				{
					_logger.debug("Bids: " + _bids.toString());
					_logger.debug("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						_logger.debug("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
						_logger.debug("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					_logger.debug("EC-VCG: " + ecvcgPayments.toString());
					_logger.debug("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.debug("Total payment: " + totalPayment);
					e1.printStackTrace();
				}
			}
			
			//---------------------------------------------
			//Now solve the QP:
			//---------------------------------------------
			IloNumExpr equalityConstraint = _cplexSolver.constant(0);
			equalityConstraint = objectiveLP;
			IloRange equalityConstraintLeft = _cplexSolver.range(mu-1e-4, equalityConstraint, mu+1e-4, "mu");
			lp.addRow(equalityConstraintLeft);
			_cplexSolver.remove( _cplexSolver.getObjective());
			_cplexSolver.add( _cplexSolver.minimize(objectiveQP));
			
			long timeBefore1 = System.currentTimeMillis();
			_cplexSolver.solve();
			long timeAfter1 = System.currentTimeMillis();
			
			_payments = new LinkedList<Double>();
			
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				try 
				{
					_payments.add( _cplexSolver.getValue(pi[i]) );			//Linear payments
				} 
				catch (IloException e) 
				{
					_logger.debug("Bids: " + _bids.toString());
					_logger.debug("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
						_logger.debug("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					_logger.debug("EC-VCG: " + ecvcgPayments.toString());
					_logger.debug("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.debug("Total payment: " + totalPayment);
					e.printStackTrace();
				}
			_logger.debug("New payments: " + _payments.toString());
			
			blockingCoalition = new LinkedList<Integer>();
			z = computeSEP( _payments, blockingCoalition);
			totalPayment = computeTotalPayment(_payments);
			
			_cplexSolver.add(lp);
			lp.removeRow( lp.getNrows() - 1);					//Remove the equality constraint for the QP problem
			_cplexSolver.remove( _cplexSolver.getObjective());
			_cplexSolver.add( _cplexSolver.minimize(objectiveLP));
		}
		lp.clear();
		_cplexSolver.clearModel();
		if( !_isExternalSolver)
		{
			_cplexSolver.endModel();
			_cplexSolver.end();
		}

		if(constraintIdBPO == 0)
			throw new PaymentException("VCG is in the Core",0);
		
		return _payments;
	}

	/*
	 * @param paymentsT - winners payments
	 * @param blockingCoalition - a reference for the blocking coalition to be stored (IDs of agents within the coalition)
	 */
	public double computeSEP(List<Double> paymentsT, List<Integer> blockingCoalition) throws IloException
	{
		_cplexSolver.clearModel();
		
		List<List<IloNumVar> > variables = new LinkedList<List<IloNumVar> >();// i-th element of the list contains the list of variables 
																			  // corresponding to the i-th agent
		List<IloNumVar> gammaVariables = new LinkedList<IloNumVar>();		//Variables for winners of WDP willing to join a coalition 
		
		//Create optimization variables and formulate an objective function
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _numberOfBuyers; ++i)							//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new LinkedList<IloNumVar>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				double expectedMarginalAvailability = computeExpectedMarginalAvailability( bid.getAtom(j) );
				
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				IloNumExpr term = _cplexSolver.prod((value - cost) * expectedMarginalAvailability, x);
				objective = _cplexSolver.sum(objective, term);
			}
			variables.add(varI);
		}
						
		for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
		{
			int agentId = _allocation.getBiddersInvolved(0).get(j);
			int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
			AtomicBid allocatedBundle = _bids.get( agentId-1 ).getAtom( itsAllocatedAtom );
					
			double value = allocatedBundle.getValue();
			double cost  = allocatedBundle.computeCost(_costs);
			double expectedMarginalAvailability = computeExpectedMarginalAvailability( allocatedBundle );
			double realizedMarginalAvailability = _allocation.getRealizedRV(0, j);
			
			IloNumVar gamma = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_" + j);
			gammaVariables.add(gamma);
			
			IloNumExpr term1 = _cplexSolver.prod(-1*( (value-cost)*expectedMarginalAvailability ), gamma);
			objective = _cplexSolver.sum(objective, term1);
			
			IloNumExpr term2 = _cplexSolver.prod(-1., gamma);
			term2 = _cplexSolver.sum(1, term2);
			term2 = _cplexSolver.prod(cost * realizedMarginalAvailability, term2);
			
			objective = _cplexSolver.sum(objective, term2);
		}
		
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			
			for(int j = 0; j < _numberOfBuyers; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);
				List<IloNumVar> varI = (List<IloNumVar>)(variables.get(j));
				for( int q = 0; q < varI.size(); ++q )
				{
					if( binaryBid[q][i] > 0 )
					{
						IloNumExpr term = _cplexSolver.prod(binaryBid[q][i], varI.get(q));
						constraint = _cplexSolver.sum(constraint, term);
					}
				}
			}
			IloRange range = _cplexSolver.range(0, constraint, 1.0, "Item_"+i);
			lp.addRow(range);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfBuyers; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			double upperBound = 0.;
			
			boolean isWinner = false;
			int itsIdx = 0;
			if( _allocation.getBiddersInvolved(0).contains( _bids.get(i).getAgentId() ) )
			{
				isWinner = true;
				itsIdx = _allocation.getBiddersInvolved(0).indexOf( _bids.get(i).getAgentId() );
			}
			if(isWinner)
			{				
				upperBound = 0.;
				IloNumExpr term = _cplexSolver.prod(-1, gammaVariables.get(itsIdx));
				constraint = _cplexSolver.sum(constraint, term);
			}
			else
				upperBound = 1.;
						
			List<IloNumVar> varI = (List<IloNumVar>)(variables.get(i));
			for(int q = 0; q < varI.size(); ++q)
				constraint = _cplexSolver.sum(constraint, varI.get(q));
			
			IloRange range1 = _cplexSolver.ge(upperBound, constraint, "Bidder"+i+"_1");//(0, constraint, upperBound, "Bidder"+i);
			
			try 
			{
				lp.addRow(range1);
			} 
			catch (IloException e) 
			{
				_logger.error("Cannot add the following constraint: ");
				_logger.error("" + range1.toString());
				_logger.error("LP matrix: ");
				_logger.error(lp.toString());
				_logger.error("Bids: " + _bids.toString());
				throw e;
			}
		}
		
		_logger.debug("SEP: " + _cplexSolver.toString());
		
		//Launch CPLEX to solve the problem:
		try 
		{
			_cplexSolver.solve();
		} 
		catch (IloException e) 
		{
			System.out.println(_bids.toString());
			e.printStackTrace();
		}

		for(int i = 0; i < _numberOfBuyers; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if(Math.abs(_cplexSolver.getValue(variables.get(i).get(j)) - 1.0) < 1e-6)
					blockingCoalition.add( _bids.get(i).getAgentId() );
		
		_logger.debug("Blocking coalition: " + blockingCoalition.toString() + ". Coalitional value is " + _cplexSolver.getObjValue() );
		double obj = _cplexSolver.getObjValue();
		_cplexSolver.clearModel();
		return obj;
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
	
	/*
	 * The method computes the expected consumption of a bundle by a buyer given the reported probability distribution.
	 * @param atom - an atomic bid containing the bundle
	 * @return the expected consumption of the bundle
	 */
	public double computeExpectedMarginalAvailability(AtomicBid atom)
	{
		//double expectedConsumption = 0.;
		
		//if( (double)atom.getTypeComponent("Distribution") == (double)Distribution.UNIFORM.ordinal() ) 
		//	expectedConsumption = ((double)atom.getTypeComponent("UpperBound") - (double)atom.getTypeComponent("LowerBound") ) / 2.;
		
		return _jpmf.getMarginalProbability( atom.getInterestingSet(), null, null );
	}

	/*
	 * The method returns an index of the specified agent Id in the list of all submitted bids.
	 * @param agentId - an ID of an agent for which an index is required.
	 * @throws an exception if there're no bids of the specified agent
	 */
	private int getIndexOfAllocatedAgent(int agentId)
	{
		for(int j  = 0; j < _numberOfBuyers; ++j)
			if( _bids.get(j).getAgentId() == agentId )
				return j;
		
		throw new RuntimeException("No agent with id " + agentId + " found in the list of submitted bids: " + _bids.toString());
	}
	
	private double computeTotalPayment(List<Double> payments)
	{
		double total = 0.;
		for(Double p : payments)
			total += p;
		return total;
	}
	
	private int _numberOfBuyers;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;						//Resulting allocation of the auction 
	private List<int[][]> _binaryBids;					//Bids converted into a binary matrix format
	private List<Double> _payments;
	private JointProbabilityMass _jpmf;					//Joint probability mass function for availabilities of goods
	
	private IloCplex _cplexSolver;
	private boolean _isExternalSolver;
	private final double TOL = 1e-4;
}
