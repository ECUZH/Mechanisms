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
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

public class ECCCorePayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(ECCCorePayments.class);
	
	/**
	 * Constructor
	 * @param allocation
	 * @param numberOfBuyers
	 * @param numberOfItems
	 * @param bids
	 * @param costs
	 * @param binaryBids
	 * @param jpmf
	 */
	public ECCCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf)
	{
		this.init(allocation, numberOfBuyers, numberOfItems, bids, costs, binaryBids, jpmf);		
	}
	
	/**
	 * Constructor
	 * @param allocation
	 * @param numberOfBuyers
	 * @param numberOfItems
	 * @param bids
	 * @param costs
	 * @param binaryBids
	 * @param jpmf
	 * @param solver
	 */
	public ECCCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf, IloCplex solver)
	{
		this.init(allocation, numberOfBuyers, numberOfItems, bids, costs, binaryBids, jpmf);
		setSolver(solver);
	}
	
	/**
	 * Initialization
	 * @param allocation
	 * @param numberOfBuyers
	 * @param numberOfItems
	 * @param bids
	 * @param costs
	 * @param binaryBids
	 * @param jpmf
	 */
	private void init(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
            List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf)
	{		
		_allocation = allocation;
		_numberOfBidders = numberOfBuyers;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_binaryBids = binaryBids;
		_jpmf = jpmf;
		_isExternalSolver = false;
		_cplexSolver = null;
	}
	
	/**
	 * The method sets up the CPLEX solver.
	 * @param solver CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
		_isExternalSolver = true;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		_logger.debug("-> computePayments()");
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
		_cplexSolver.setParam(IloCplex.Param.RootAlgorithm.AuxRootThreads, -1);
		
		if( _allocation.getNumberOfAllocatedAuctioneers() <= 0 ) 
		{
			_payments = new LinkedList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		_logger.debug("Compute ECC-VCG payments: " + _bids.toString());
		PaymentRule eccvcgRule = new ECCVCGPayments(_allocation, _numberOfBidders, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver);
		List<Double> eccvcgPayments = eccvcgRule.computePayments();
		_payments = eccvcgPayments;
		_logger.debug("ECC-VCG payments: " + _payments.toString());

		List<Double> realizedValues = new LinkedList<Double>();
		
		List<Integer> blockingCoalition = new LinkedList<Integer>();
		double z = computeSEP(eccvcgPayments, blockingCoalition);
		double totalPayment = computeTotalPayment(eccvcgPayments);
		_logger.debug("z="+z + ". Blocking coalition:" + blockingCoalition.toString() + ". Total payment=" + totalPayment);
		
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		IloNumVar[] pi = new IloNumVar[_allocation.getBiddersInvolved(0).size() ];
		IloNumExpr objectiveLP = _cplexSolver.constant(1e-12);
		IloNumExpr objectiveQP = _cplexSolver.constant(1e-12);
		
		//Create optimization variables and formulate the objective function
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			int itsId =  _allocation.getBiddersInvolved(0).get(i);
			int itsAllocatedBundleIdx = _allocation.getAllocatedBundlesByIndex(0).get(i);
			AtomicBid itsAllocatedBundle = _bids.get( itsId - 1).getAtom( itsAllocatedBundleIdx );
			double realizedValue = itsAllocatedBundle.getValue() * _allocation.getRealizedRV(0, i);
			realizedValues.add(realizedValue);
			
			if( eccvcgPayments.get(i) > realizedValue )
			{
				_logger.warn("Empty Core. p_i^{EC-VCG}=" + eccvcgPayments.get(i)+" > " + realizedValue + "= E[v_i]. VCG:" + eccvcgPayments.toString());
				_logger.warn("Bids: " + _bids.toString());
				_logger.warn("Costs" + _costs.toString());
				for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					_logger.warn("Realization for bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " is " + _allocation.getRealizedRV(0, j));
				_logger.warn("EC-VCG: " + eccvcgPayments.toString());
				_logger.warn("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
				_logger.warn("Total payment: " + totalPayment);
				throw new PaymentException("Empty Core",0);
			}
			IloNumVar x = _cplexSolver.numVar(eccvcgPayments.get(i), realizedValue, IloNumVarType.Float, "pi_" + itsId);
			pi[i] = x;
			objectiveLP = _cplexSolver.sum(objectiveLP, pi[i]);
			
			IloNumExpr term = _cplexSolver.sum(-1*eccvcgPayments.get(i), pi[i]);
			term = _cplexSolver.prod(term, term);
			objectiveQP = _cplexSolver.sum(objectiveQP, term);
		}
		_cplexSolver.add(_cplexSolver.minimize(objectiveLP));

		int constraintIdBPO = 0;
		
		while( z > TOL )
		{
			_logger.debug("z="+z+" totalPayment="+totalPayment );
			
			//Create optimization constraints			
			IloNumExpr constraint = _cplexSolver.constant(0);

			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
			{
				int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
				if( ! blockingCoalition.contains( allocatedAgentId ) )
					constraint = _cplexSolver.sum(constraint, pi[i]);
			}
			
			lp.addRow( _cplexSolver.le(z+totalPayment, constraint, "c"+constraintIdBPO));
			constraintIdBPO += 1;
			
			//Solve LP
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
				{
					_logger.error("z="+z+" totalPayment="+totalPayment + "; blocking coalition: " + blockingCoalition.toString());
					_logger.error("LP: " + _cplexSolver.toString());
					_logger.error("Bids: " + _bids.toString());
					_logger.error("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						_logger.error("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesByIndex(0).get(j));
						_logger.error("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					_logger.error("EC-VCG: " + eccvcgPayments.toString());
					_logger.error("Realized values: " + realizedValues.toString());
					_logger.error("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.error("Total payment: " + totalPayment);
					throw new PaymentException("Empty Core", 1);
				}
				else
				{
					_logger.warn("Bids: " + _bids.toString());
					_logger.warn("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						_logger.warn("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesByIndex(0).get(j));
						_logger.warn("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					_logger.warn("EC-VCG: " + eccvcgPayments.toString());
					_logger.warn("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.warn("Total payment: " + totalPayment);
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
			
			_cplexSolver.solve();
			_payments = new LinkedList<Double>();
			
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				try 
				{
					_payments.add( _cplexSolver.getValue(pi[i]) );			//Linear payments
				} 
				catch (IloException e) 
				{
					_logger.warn("Bids: " + _bids.toString());
					_logger.warn("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
						_logger.warn("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					_logger.warn("EC-VCG: " + eccvcgPayments.toString());
					_logger.warn("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.warn("Total payment: " + totalPayment);
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
		
		if(constraintIdBPO == 0)	throw new PaymentException("VCG is in the Core",0);
		
		_logger.debug("<- computePayments()");
		return _payments;
	}

	/**
	 * The method solves the separation problem (ILP)
	 * @param paymentsT - winners payments
	 * @param blockingCoalition - a reference for the blocking coalition to be stored (IDs of agents within the coalition)
	 * @return the coalitional value
	 */
	public double computeSEP(List<Double> paymentsT, List<Integer> blockingCoalition) throws IloException
	{
		_logger.debug("-> computeSEP(paymentsT="+paymentsT.toString()+", blockingCoalition="+ blockingCoalition.toString()+")");
		_cplexSolver.clearModel();
		
		List<List<IloNumVar> > variables = new LinkedList<List<IloNumVar> >();// i-th element of the list contains the list of variables 
																			  // corresponding to the i-th agent
		List<IloNumVar> gammaVariables = new LinkedList<IloNumVar>();		  //Variables for winners of WDP willing to join a coalition 
		
		//Create optimization variables and formulate an objective function
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _numberOfBidders; ++i)							//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new LinkedList<IloNumVar>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				List<Integer> bundle = bid.getAtom(j).getInterestingSet();
				
				List<Integer> allocatedAvailabilitiesPerGood = _allocation.getGoodIdsWithKnownAvailabilities(_bids, true);
				List<Double> realizedRVsPerGood = _allocation.getRealizationsOfAvailabilitiesPerGood(_bids, true);
				double expectedMarginalAvailability = _jpmf.getMarginalProbability(bundle, allocatedAvailabilitiesPerGood, realizedRVsPerGood);
				
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				IloNumExpr term = _cplexSolver.prod((value - cost) * expectedMarginalAvailability, x);
				objective = _cplexSolver.sum(objective, term);
				_logger.debug("SEP: Adding term " + term.toString() + " to the objective.");
			}
			variables.add(varI);
		}
						
		for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
		{
			int agentId = _allocation.getBiddersInvolved(0).get(j);
			int itsAllocatedAtom = _allocation.getAllocatedBundlesByIndex(0).get(j);
			AtomicBid allocatedBundle = _bids.get( agentId-1 ).getAtom( itsAllocatedAtom );
					
			double value = allocatedBundle.getValue();
			double cost  = allocatedBundle.computeCost(_costs);
			double realizedMarginalAvailability = _allocation.getRealizedRV(0, j);
			
			IloNumVar gamma = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_" + j);
			gammaVariables.add(gamma);
			
			IloNumExpr term1 = _cplexSolver.prod(-1*( (value-cost)*realizedMarginalAvailability ), gamma);
			objective = _cplexSolver.sum(objective, term1);
			
			IloNumExpr term2 = _cplexSolver.prod(-1., gamma);
			term2 = _cplexSolver.sum(1, term2);
			term2 = _cplexSolver.prod(cost * realizedMarginalAvailability - paymentsT.get(j), term2);
			objective = _cplexSolver.sum(objective, term2);
			_logger.debug("SEP: Adding terms " + term1.toString() + " and " + term2.toString() + " to the objective");
		}
		
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			
			for(int j = 0; j < _numberOfBidders; ++j)
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
		for(int i = 0; i < _numberOfBidders; ++i)
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
			
			IloRange range1 = _cplexSolver.ge(upperBound, constraint, "Bidder"+i+"_1");
			
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
			_logger.error(_bids.toString());
			e.printStackTrace();
		}

		for(int i = 0; i < _numberOfBidders; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if(Math.abs(_cplexSolver.getValue(variables.get(i).get(j)) - 1.0) < 1e-6)
					blockingCoalition.add( _bids.get(i).getAgentId() );
		
		_logger.debug("Blocking coalition: " + blockingCoalition.toString() + ". Coalitional value is " + _cplexSolver.getObjValue() );
		double obj = _cplexSolver.getObjValue();
		_cplexSolver.clearModel();
		
		_logger.debug("<- computeSEP(...)");
		return obj;
	}
	
	private double computeTotalPayment(List<Double> payments)
	{
		return payments.stream().reduce( (x1, x2) -> x1 + x2).get();
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

	private int _numberOfBidders;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;					//Resulting allocation of the auction 
	private List<int[][]> _binaryBids;					//Bids converted into a binary matrix format
	private List<Double> _payments;
	private JointProbabilityMass _jpmf;					//Joint probability mass function for availabilities of goods
	
	private IloCplex _cplexSolver;
	private boolean _isExternalSolver;
	private final double TOL = 1e-4;
}
