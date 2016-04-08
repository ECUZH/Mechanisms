package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

public class ECCCorePayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(ECCCorePayments.class);
	
	/**
	 * Constructor
	 * @param allocation allocation of the auction
	 * @param numberOfBidders number of bidders in the auction
	 * @param numberOfItems number of goods in the auction
	 * @param bids bids of bidders
	 * @param costs (additive) costs per good
	 * @param binaryBids binary form of bids of agents
	 * @param jpmf joint probability mass function
	 */
	public ECCCorePayments(AllocationEC allocation, int numberOfBidders, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf)
	{
		this.init(allocation, numberOfBidders, numberOfItems, bids, costs, binaryBids, jpmf);		
	}
	
	/**
	 * Constructor
	 * @param allocation allocation of the auction
	 * @param numberOfBidders number of bidders in the auction
	 * @param numberOfItems number of goods in the auction
	 * @param bids bids of bidders
	 * @param costs (additive) costs per good
	 * @param binaryBids binary form of bids of agents
	 * @param jpmf joint probability mass function
	 * @param solver CPLEX solver
	 */
	public ECCCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
			              List<Double> costs, List<int[][]> binaryBids, JointProbabilityMass jpmf, IloCplex solver)
	{
		this.init(allocation, numberOfBuyers, numberOfItems, bids, costs, binaryBids, jpmf);
		setSolver(solver);
	}
	
	/**
	 * Initialization
	 * @param allocation allocation of the auction
	 * @param numberOfBidders number of bidders in the auction
	 * @param numberOfItems number of goods in the auction
	 * @param bids bids of bidders
	 * @param costs (additive) costs per good
	 * @param binaryBids binary form of bids of agents
	 * @param jpmf joint probability mass function
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
		long t1 = System.currentTimeMillis();
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
			_payments = new ArrayList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		_logger.debug("Compute ECC-VCG payments: " + _bids.toString());
		PaymentRule eccvcgRule = new ECCVCGPayments(_allocation, _numberOfBidders, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver);
		List<Double> eccvcgPayments = eccvcgRule.computePayments();
		_payments = eccvcgPayments;
		_logger.debug("ECC-VCG payments: " + _payments.toString());

		List<Double> realizedValues = new ArrayList<Double>();
		
		List<Integer> blockingCoalition = new ArrayList<Integer>();
		long tSEP1 = System.currentTimeMillis();
		double z = computeSEP(eccvcgPayments, blockingCoalition);
		double totalPayment = computeTotalPayment(eccvcgPayments);
		long tSEP2 = System.currentTimeMillis();
		//System.out.println(tSEP2-tSEP1);
		_logger.debug("z="+z + ". Blocking coalition:" + blockingCoalition.toString() + ". Total payment=" + totalPayment);
		
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
			double realizedValue = itsAllocatedBundle.getValue() * _allocation.getRealizedRV(0, i);
			realizedValues.add(realizedValue);
			
			if( eccvcgPayments.get(i) > realizedValue )
			{
				_logger.warn("Empty Core. p_i^{EC-VCG}=" + eccvcgPayments.get(i)+" > " + realizedValue + "= E[v_i]. VCG:" + eccvcgPayments.toString());
				_logger.warn("Bids: " + _bids.toString());
				_logger.warn("Costs" + _costs.toString());
				for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					_logger.warn("Realization for bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " is " + _allocation.getRealizedRV(0, j));
				_logger.warn("ECC-VCG: " + eccvcgPayments.toString());
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
		_logger.debug("Create LP objective: " + objectiveLP.toString());
		_logger.debug("Create QP objective: " + objectiveQP.toString());
		
		_cplexSolver.add(_cplexSolver.minimize(objectiveLP));

		int constraintIdBPO = 0;
		
		while( z > _TOL )
		{
			_logger.debug("z="+z+" totalPayment="+totalPayment );
			
			//Create optimization constraints			
			IloNumExpr constraint = _cplexSolver.constant(0);
			double subTotalPayment = 0.;
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
			{
				int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
				if( ! blockingCoalition.contains( allocatedAgentId ) )
				{
					constraint = _cplexSolver.sum(constraint, pi[i]);
					subTotalPayment += _payments.get(i);
				}
			}
			
			lp.addRow( _cplexSolver.le(z+subTotalPayment, constraint, "c"+constraintIdBPO));
			constraintIdBPO += 1;
			
			//---------------------------------------------
			//Linear Programming Problem:
			//---------------------------------------------
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
				if(e1.getMessage().contains("CPLEX Error  1217: No solution exists.") )
				{
					if(!blockingCoalition.containsAll(_allocation.getBiddersInvolved(0)) && !_allocation.getBiddersInvolved(0).containsAll(blockingCoalition) )
					{
						_logger.error("z="+z+" totalPayment="+totalPayment + "; blocking coalition: " + blockingCoalition.toString());
						_logger.error("LP: " + _cplexSolver.toString());
						_logger.error("Bids: " + _bids.toString());
						_logger.error("Costs" + _costs.toString());
						for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
						{
							_logger.error("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
							_logger.error("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
						}
						_logger.error("ECC-VCG: " + eccvcgPayments.toString());
						_logger.error("Realized values: " + realizedValues.toString());
						_logger.error("ALL RVs="+ _allocation.getRealizedRVsPerGood(0).toString());
						_logger.error("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
						_logger.error("Total payment: " + totalPayment);
					}
					throw new PaymentException("Empty Core", 1);
				}
				else
				{
					_logger.warn("Bids: " + _bids.toString());
					_logger.warn("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						_logger.warn("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
						_logger.warn("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					_logger.warn("EC-VCG: " + eccvcgPayments.toString());
					_logger.warn("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.warn("Total payment: " + totalPayment);
					e1.printStackTrace();
				}
			}

			//---------------------------------------------
			//Quadratic Programming Problem:
			//---------------------------------------------
			IloNumExpr equalityConstraint = _cplexSolver.constant(0);
			equalityConstraint = objectiveLP;
			IloRange equalityConstraintLeft = _cplexSolver.range(mu-1e-4, equalityConstraint, mu+1e-4, "mu");
			lp.addRow(equalityConstraintLeft);
			_cplexSolver.remove( _cplexSolver.getObjective());
			_cplexSolver.add( _cplexSolver.minimize(objectiveQP));
			
			_cplexSolver.solve();
			_payments = new ArrayList<Double>();
			
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
			
			blockingCoalition = new ArrayList<Integer>();
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
		{
			_logger.debug("VCG is in the core => throwing an exception");
			throw new PaymentException("VCG is in the Core",0, _payments);
		}
		
		long t2 = System.currentTimeMillis();
		//System.out.println(t2-t1);
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
		
		long tBuild1 = System.currentTimeMillis();
		List<List<IloNumVar> > variables = new ArrayList<List<IloNumVar> >();// i-th element of the list contains the list of variables 
																			  // corresponding to the i-th agent
		List<IloNumVar> gammaVariables = new ArrayList<IloNumVar>();		  //Variables for winners of WDP willing to join a coalition 
		
		//Create optimization variables and formulate an objective function
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _numberOfBidders; ++i)							//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new ArrayList<IloNumVar>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				List<Integer> bundle = bid.getAtom(j).getInterestingSet();
				
				List<Integer> goodsWithKnownAvailabilities = _allocation.getGoodIdsWithKnownAvailabilities(_bids, true);
				List<Double> realizedRVsPerGood = _allocation.getRealizationsOfAvailabilitiesPerGood(_bids, true);
				double expectedMarginalAvailability = _jpmf.getMarginalProbability(bundle, goodsWithKnownAvailabilities, realizedRVsPerGood);
				
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				IloNumExpr term = _cplexSolver.prod((value - cost) * expectedMarginalAvailability, x);
				objective = _cplexSolver.sum(objective, term);
				_logger.debug("SEP: Adding term " + term.toString() + " to the objective.");
			}
			variables.add(varI);
		}
		
		double totalCost = 0.;
		double totalPayment = computeTotalPayment(paymentsT);
		for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
		{
			int agentId = _allocation.getBiddersInvolved(0).get(j);
			int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
			AtomicBid allocatedBundle = _bids.get( agentId-1 ).getAtom( itsAllocatedAtom );
					
			double value = allocatedBundle.getValue();
			double cost  = allocatedBundle.computeCost(_costs);
			double realizedAvailability = _allocation.getRealizedRV(0, j);
			
			IloNumVar gamma = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_" + j);
			gammaVariables.add(gamma);
			
			IloNumExpr term1 = _cplexSolver.prod(-1*( value*realizedAvailability - paymentsT.get(j) ), gamma);
			objective = _cplexSolver.sum(objective, term1);
			
			totalCost += cost * realizedAvailability;
			_logger.debug("SEP: Adding terms " + term1.toString() + " to the objective");
		}
		IloNumVar gammaS = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_S");
		gammaVariables.add(gammaS);
		IloNumExpr termS = _cplexSolver.prod(-1 * ( totalPayment - totalCost ), gammaS);
		objective = _cplexSolver.sum(objective, termS);
		_logger.debug("SEP: Adding terms " + termS.toString() + " to the objective");
		
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
			IloNumVar y = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "y_"+i);
			IloNumExpr termY = _cplexSolver.prod( -1., y );
			constraint = _cplexSolver.sum(constraint, termY);
			IloRange range = _cplexSolver.eq(0, constraint,  "Item_"+i);
			//IloRange range = _cplexSolver.range(0, constraint, 1.0, "Item_"+i);
			lp.addRow(range);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfBidders; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			IloNumExpr constraintGamma = _cplexSolver.constant(0);
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
				
				//Constraint for Gamma_S:   GammaI <= GammaS
				IloNumExpr termGammaI = _cplexSolver.prod(1, gammaVariables.get(itsIdx));
				IloNumExpr termGammaS = _cplexSolver.prod(-1, gammaS);
				constraintGamma = _cplexSolver.sum(constraintGamma, termGammaI);
				constraintGamma = _cplexSolver.sum(constraintGamma, termGammaS);
				IloNumVar y = _cplexSolver.numVar(0, Double.MAX_VALUE, IloNumVarType.Int, "y_GammaS"+i);
				IloNumExpr termY = _cplexSolver.prod( 1, y );
				constraintGamma = _cplexSolver.sum(constraintGamma, termY);
				IloRange range = _cplexSolver.eq(0., constraintGamma, "GammaS_"+i);
				
				//IloRange range = _cplexSolver.ge(0., constraintGamma, "GammaS_"+i);
				lp.addRow(range);
			}
			else
				upperBound = 1.;
						
			List<IloNumVar> varI = (List<IloNumVar>)(variables.get(i));
			for(int q = 0; q < varI.size(); ++q)
				constraint = _cplexSolver.sum(constraint, varI.get(q));
			
			IloRange range1 = _cplexSolver.ge(upperBound, constraint, "Bidder"+i+"_1");
			
			if( ! isWinner )
			{
				//Constraint for Gamma_S:   x_{i1} + x_{i2} + ... <= GammaS
				IloNumExpr termGammaS = _cplexSolver.prod(-1, gammaS);
				constraintGamma = _cplexSolver.sum(constraintGamma, constraint);
				constraintGamma = _cplexSolver.sum(constraintGamma, termGammaS);
				IloRange range = _cplexSolver.ge(0., constraintGamma, "GammaS_"+i);
				lp.addRow(range);
			}
			
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
		long tBuild2 = System.currentTimeMillis();
		//System.out.println(tBuild2-tBuild1);
		
		_logger.debug("SEP: " + _cplexSolver.toString());
		
		//Launch CPLEX to solve the problem:
		try 
		{
			long tSolve1 = System.currentTimeMillis();
			_cplexSolver.solve();
			long tSolve2 = System.currentTimeMillis();
			//System.out.println(tSolve2-tSolve1);
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
	
	/**
	 * The method computes total payments.
	 * @param payments sum of payments in the specified list
	 * @return
	 */
	private double computeTotalPayment(List<Double> payments)
	{
		double totalPayment = 0.;
		for(Double p : payments)
			totalPayment += p;
		return totalPayment;
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

	private int _numberOfBidders;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;					//Resulting allocation of the auction 
	private List<int[][]> _binaryBids;					//Bids converted into a binary matrix format
	private List<Double> _payments;						//A list of payments to be computed
	private JointProbabilityMass _jpmf;					//Joint probability mass function for availabilities of goods
	
	private IloCplex _cplexSolver;						//CPLEX solver
	private boolean _isExternalSolver;					//True if the instantiation of the class uses an external CPLEX solver and false otherwise
	private final double _TOL = 1e-4;					//Tolerance level
}
