package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

/**
 * The class implements functionality to compute execution contingent core-selecting payments based on an assumption
 * that availabilities of all goods are known. 
 * @author Dmitry Moor
 */
public class ECRCorePayments implements PaymentRule
{

	private static final Logger _logger = LogManager.getLogger(ECRCorePayments.class);
	
	/**
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers number of bidders
	 * @param numberOfItems number of goods
	 * @param bids bids of bidders
	 * @param costs a list of costs per good
	 * @param binaryBids bids of bidders in a binary format
	 * @param jpmf join probability mass function
	 */
	public ECRCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
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
	
	/**
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers number of bidders
	 * @param numberOfItems number of goods
	 * @param bids bids of bidders
	 * @param costs a list of costs per good
	 * @param binaryBids bids of bidders in a binary format
	 * @param jpmf join probability mass function
	 * @param solver CPLEX solver
	 */
	public ECRCorePayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, 
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
	
	/**
	 * The method sets up the CPLEX solver.
	 * @param solver - a CPLEX solver
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
		
		if( _allocation.getNumberOfAllocatedAuctioneers() <= 0 ) 
		{
			_payments = new ArrayList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		_logger.debug("Compute ECR-VCG payments: " + _bids.toString());
		PaymentRule ecrvcgRule = new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver);
		List<Double> ecrvcgPayments = ecrvcgRule.computePayments();
		_payments = ecrvcgPayments;
		_logger.debug("ECR-VCG payments: " + ecrvcgPayments.toString());
		
		List<Integer> blockingCoalition = new ArrayList<Integer>();
		double z = computeSEP(ecrvcgPayments, blockingCoalition);
		double totalPayment = computeTotalPayment(ecrvcgPayments);

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
			
			if( ecrvcgPayments.get(i) > realizedValue )
			{
				_logger.debug("Empty Core. p_i^{EC-VCG}=" + ecrvcgPayments.get(i)+" > " + realizedValue + "= \tilde{v}_i. VCG:" + ecrvcgPayments.toString());
				_logger.debug("Bids: " + _bids.toString());
				_logger.debug("Costs" + _costs.toString());
				for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					_logger.debug("Realization for bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " is " + _allocation.getRealizedRV(0, j));
				_logger.debug("EC-VCG: " + ecrvcgPayments.toString());
				_logger.debug("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
				_logger.debug("Total payment: " + totalPayment);
				throw new PaymentException("Empty Core",0);
			}
			IloNumVar x = _cplexSolver.numVar(ecrvcgPayments.get(i), realizedValue, IloNumVarType.Float, "pi_" + itsId);
			pi[i] = x;
			objectiveLP = _cplexSolver.sum(objectiveLP, pi[i]);
			
			IloNumExpr term = _cplexSolver.sum(-1*ecrvcgPayments.get(i), pi[i]);
			term = _cplexSolver.prod(term, term);
			objectiveQP = _cplexSolver.sum(objectiveQP, term);
		}
		_cplexSolver.add(_cplexSolver.minimize(objectiveLP));
				
		int constraintIdBPO = 0;
		
		while( z >  TOL )
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
				if(e1.getMessage().contains("CPLEX Error  1217: No solution exists."))
				{
					System.out.println("LP: " + _cplexSolver.toString());
					System.out.println("Bids: " + _bids.toString());
					System.out.println("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						System.out.println("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
						System.out.println("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					System.out.println("EC-VCG: " + ecrvcgPayments.toString());
					System.out.println("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					System.out.println("Total payment: " + totalPayment);
					throw new PaymentException("Empty Core", 1);
				}
				else
				{
					_logger.debug("Bids: " + _bids.toString());
					_logger.debug("Costs" + _costs.toString());
					for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
					{
						//_logger.debug("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesByIndex(0).get(j));
						_logger.debug("Realization " + j + ": " + _allocation.getRealizedRV(0, j));
					}
					_logger.debug("EC-VCG: " + ecrvcgPayments.toString());
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
			//_cplexSolver.setParam(IloCplex.Param.RootAlgorithm, 3);
			
			long timeBefore1 = System.currentTimeMillis();
			_cplexSolver.solve();
			long timeAfter1 = System.currentTimeMillis();
			//System.out.println(timeAfter1-timeBefore1);
			//System.out.println("QP before: " + _cplexSolverLP.toString());
			
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
					_logger.debug("EC-VCG: " + ecrvcgPayments.toString());
					_logger.debug("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.debug("Total payment: " + totalPayment);
					e.printStackTrace();
				}
			//_logger.debug("New payments: " + _payments.toString());
			
			blockingCoalition = new LinkedList<Integer>();
			z = computeSEP( _payments, blockingCoalition);
			totalPayment = computeTotalPayment(_payments);
			
			//_cplexSolver.clearModel();
			_cplexSolver.add(lp);
			lp.removeRow( lp.getNrows() - 1);					//Remove the equality constraint for the QP problem
			_cplexSolver.remove( _cplexSolver.getObjective());
			_cplexSolver.add( _cplexSolver.minimize(objectiveLP));
			//System.out.println("QP after : " + _cplexSolverLP.toString());
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

	/**
	 * The method solves separation problem
	 * @param paymentsT - winners payments
	 * @param blockingCoalition - a reference for the blocking coalition to be stored (IDs of agents within the coalition)
	 * @return opportunity cost of the coalition with the highest coalitional value
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
		
		for(int i = 0; i < _numberOfBuyers; ++i)							//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new LinkedList<IloNumVar>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				List<Integer> bundle = bid.getAtom(j).getInterestingSet();
				List<Integer> allocatedAvailabilitiesPerGood = _allocation.getGoodIdsWithKnownAvailabilities(_bids, false);
				List<Double> realizedRVsPerGood = _allocation.getRealizationsOfAvailabilitiesPerGood(_bids, false);
				double expectedMarginalAvailability = _jpmf.getMarginalProbability(bundle, allocatedAvailabilitiesPerGood, realizedRVsPerGood);
				
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
			double realizedMarginalAvailability = _allocation.getRealizedRV(0, j);
			
			IloNumVar gamma = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_" + j);
			gammaVariables.add(gamma);
			
			IloNumExpr term1 = _cplexSolver.prod(-1*( value*realizedMarginalAvailability - paymentsT.get(j) ), gamma);
			objective = _cplexSolver.sum(objective, term1);
			
			totalCost += cost * realizedMarginalAvailability;
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
			IloNumVar y = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "y_"+i);
			IloNumExpr termY = _cplexSolver.prod( -1., y );
			constraint = _cplexSolver.sum(constraint, termY);
			IloRange range = _cplexSolver.eq(0, constraint,  "Item_"+i);
			//IloRange range = _cplexSolver.range(0, constraint, 1.0, "Item_"+i);
			lp.addRow(range);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfBuyers; ++i)
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
		
		//_logger.debug("SEP: " + _cplexSolver.toString());
		
		//Launch CPLEX to solve the problem:
		try 
		{
			//System.out.println("SEP: " + _cplexSolver.toString());
			_cplexSolver.solve();
		} 
		catch (IloException e) 
		{
			_logger.error(_bids.toString());
			e.printStackTrace();
		}

		for(int i = 0; i < _numberOfBuyers; ++i)
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
		double total = 0.;
		for(Double p : payments)
			total += p;
		return total;
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

	private int _numberOfBuyers;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;					//Resulting allocation of the auction 
	private List<int[][]> _binaryBids;					//Bids converted into a binary matrix format
	private List<Double> _payments;						//Payments to be computed
	private JointProbabilityMass _jpmf;					//Joint probability mass function for availabilities of goods
	
	private IloCplex _cplexSolver;						//CPLEX solver
	private boolean _isExternalSolver;					//True if an external solver should be used; false otherwise
	private final double TOL = 1e-4;					//Tolerance level
}
