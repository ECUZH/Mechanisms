package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/**
 * The class implements computation of core bidder optimal payments according to the constraint generation technique proposed by Day et al.
 * The bids should be specified using the XOR bidding language.
 * @author Dmitry Moor
 */
public class CorePayments implements PaymentRule
{
	
	private static final Logger _logger = LogManager.getLogger(CorePayments.class);
	
	/**
	 * Constructor
	 * @param allocation allocation produced by WDP
	 * @param allBids list of all bids of the CA
	 * @param quantitiesOfItems a list of units of items (for multiunit auction)
	 * @param numberOfItems the number of items in the CA
	 * @param binaryBids (see CAXOR.java for explanation of the data structure)
	 * @param costs costs per item
	 */
	public CorePayments(Allocation allocation, List<Type> allBids, List<Integer> quantitiesOfItems, int numberOfItems, List<int[][]> binaryBids, List<Double> costs)
	{
		if( numberOfItems != quantitiesOfItems.size() ) throw new RuntimeException("The number of items should correspond to the size of the list.");
		
		_allocation = allocation;
		_bids = allBids;
		_unitsOfItems = quantitiesOfItems;
		_numberOfItems = numberOfItems;
		_numberOfAgents = _bids.size();
		_binaryBids = binaryBids;
		_costs = costs;
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws PaymentException, Exception
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
		
		if( _allocation.getBiddersInvolved(0).size() <= 0 ) 
		{
			_payments = new LinkedList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		_logger.debug("Compute VCG payments: " + _bids.toString());
		PaymentRule vcgRule = new VCGPayments(_allocation, _bids, _unitsOfItems, _numberOfItems, _costs);
		List<Double> vcg = vcgRule.computePayments();
		_payments = vcg;
		_logger.debug("VCG payments: " + _payments.toString());
		
		List<Integer> blockingCoalition = new ArrayList<Integer>();
		double z = computeSEP(vcg, blockingCoalition);
		double totalPayment = computeTotalPayments(vcg);
		_logger.debug("z="+z + ". Blocking coalition:" + blockingCoalition.toString() + ". Total payment=" + totalPayment);
		
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		IloNumVar[] pi = new IloNumVar[_allocation.getBiddersInvolved(0).size() ];
		IloNumExpr objectiveLP = _cplexSolver.constant(1e-12);
		IloNumExpr objectiveQP = _cplexSolver.constant(1e-12);
		
		//Create optimization variables and formulate the objective function
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			int allocatedBidderId = _allocation.getBiddersInvolved(0).get(i);
			int allocatedBidderIdx = allocatedBidderId - 1;
			int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(i);
			double itsValue = _bids.get(allocatedBidderIdx).getAtom(itsAllocatedAtom ).getValue();
					
			if( vcg.get(i) > itsValue +	TOL )
			{
				_logger.error("Empty Core");
				throw new RuntimeException("THE LOWER BOUND " + vcg.get(i)+" SHOULD NOT BE HIGHER THAN THE UPPER BOUND: " + _bids.get(allocatedBidderIdx).getAtom( itsAllocatedAtom ).getValue() + ". VCG:" + vcg.toString());
			}
			
			IloNumVar x = _cplexSolver.numVar(vcg.get(i), itsValue, IloNumVarType.Float, "pi_" + _allocation.getBiddersInvolved(0).get(i));
			pi[i] = x;
			objectiveLP = _cplexSolver.sum(objectiveLP, pi[i]);
			
			IloNumExpr term = _cplexSolver.sum(-1*vcg.get(i), pi[i]);
			term = _cplexSolver.prod(term, term);
			objectiveQP = _cplexSolver.sum(objectiveQP, term);
		}
		_logger.debug("Create LP objective: " + objectiveLP.toString());
		_logger.debug("Create QP objective: " + objectiveQP.toString());
		
		_cplexSolver.add(_cplexSolver.minimize(objectiveLP));
		
		int constraintIdBPO = 0;
		
		while( z > /*totalPayment + */ TOL )
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
					if(!(blockingCoalition.containsAll(_allocation.getBiddersInvolved(0)) && _allocation.getBiddersInvolved(0).containsAll(blockingCoalition)) )
					{
						_logger.error("z="+z+" totalPayment="+totalPayment + "; blocking coalition: " + blockingCoalition.toString());
						_logger.error("LP: " + _cplexSolver.toString());
						_logger.error("Bids: " + _bids.toString());
						_logger.error("Costs" + _costs.toString());
						for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
							_logger.error("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
						_logger.error("VCG: " + vcg.toString());
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
						_logger.warn("Bidder id=" + _allocation.getBiddersInvolved(0).get(j) + " got its " + _allocation.getAllocatedBundlesOfTrade(0).get(j));
					_logger.warn("VCG: " + vcg.toString());
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
					_logger.warn("VCG: " + vcg.toString());
					_logger.warn("Blocking coalition: " + blockingCoalition.toString() + " with z="+z);
					_logger.warn("Total payment: " + totalPayment);
					e.printStackTrace();
				}
			_logger.debug("New payments: " + _payments.toString());

			blockingCoalition = new ArrayList<Integer>();
			z = computeSEP( _payments, blockingCoalition);
			totalPayment = computeTotalPayments(_payments);
			
			_cplexSolver.add(lp);
			lp.removeRow( lp.getNrows() - 1);					//Remove the equality constraint for the QP problem
			_cplexSolver.remove( _cplexSolver.getObjective());
			_cplexSolver.add( _cplexSolver.minimize(objectiveLP));
			
			if( constraintIdBPO > 10)
			{
				_logger.debug("New payments: " + _payments.toString());
				_logger.debug("z: " + z + " iter = " + constraintIdBPO);
			}
		}
		
		lp.clear();
		_cplexSolver.clearModel();
		_cplexSolver.endModel();
		_cplexSolver.end();
		
		if(constraintIdBPO == 0)
		{
			_logger.info("VCG is in the core => throwing an exception");
			throw new PaymentException("VCG is in the Core", 0, _payments);
		}
		_logger.debug("<- computePayments()");
		return _payments;
	}
	
	
	/**
	 * The method solves the separation problem
	 * @param paymentsT - winners payments
	 * @param blockingCoalition - a reference for the blocking coalition to be stored (IDs of agents within the coalition)
	 * @throws IloException 
	 */
	public double computeSEP(List<Double> paymentsT, List<Integer> blockingCoalition) throws IloException
	{
		_logger.debug("-> computeSEP(paymentsT="+paymentsT.toString()+", blockingCoalition="+ blockingCoalition.toString()+")");
		_cplexSolver.clearModel();
		
		//IMIP mipSEP = new MIP();
		List<List<IloNumVar> > variables = new ArrayList<List<IloNumVar>>();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		List<IloNumVar> gammaVariables = new ArrayList<IloNumVar>();		//variables taking into account winners of the previous iteration
		
		//Create the optimization variables and formulate the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
		
		for(int i = 0; i < _bids.size(); ++i)								//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new ArrayList<IloNumVar>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				AtomicBid bundle = bid.getAtom(j);
				double value = bundle.getValue();
				double cost = bundle.computeCost(_costs);
				
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				IloNumExpr term = _cplexSolver.prod( value - cost, x);
				objective = _cplexSolver.sum(objective, term);
				_logger.debug("SEP: Adding term " + term.toString() + " to the objective.");
			}
			variables.add(varI);
		}
		
		for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
		{
			int bidderId = _allocation.getBiddersInvolved(0).get(j);
			Type t = _bids.get(bidderId-1); 
			int allocatedBundleIdx = _allocation.getAllocatedBundlesOfTrade(0).get(j);
			AtomicBid bundle = t.getAtom(allocatedBundleIdx);
			double value = bundle.getValue();

			IloNumVar gamma = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "Gamma_" + j);
			gammaVariables.add(gamma);
			
			IloNumExpr term1 = _cplexSolver.prod(-1*(value-paymentsT.get(j)), gamma);
			objective = _cplexSolver.sum(objective, term1);
			_logger.debug("SEP: Adding term " + term1.toString() +  " to the objective");
		}
		
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);

			for(int j = 0; j < _numberOfAgents; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);
				List<IloNumVar> varI = (List<IloNumVar>)(variables.get(j));
				for( int q = 0; q < varI.size(); ++q )
					if( binaryBid[q][i] > 0 )
					{
						try 
						{
							int slotsUsed = 1;//(/*(MultiUnitAtom)*/_bids.get(j).getAtom(q)).getNumberOfUnitsByItemId(itemId);
							IloNumExpr term = _cplexSolver.prod( slotsUsed*binaryBid[q][i], varI.get(q));
							constraint = _cplexSolver.sum(constraint, term);
						} 
						catch (Exception e) 
						{
							System.out.println("ITEMS constr. Item idx=" + i + ", ItemId=" + (i+1) + 
							           ", for agent " + j + "'s atom " + q + ": " + _bids.get(j).getAtom(q).toString());
							e.printStackTrace();
						}
					}
			}
			IloRange range = _cplexSolver.range(0, constraint, 1.0, "Item_"+i);
			lp.addRow(range);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfAgents; ++i)
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
		
		for(int i = 0; i < _numberOfAgents; ++i)
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
	 * The method computes total payment made by all bidders
	 * @param payments of bidders
	 * @return a total payment
	 */
	private double computeTotalPayments(List<Double> payments)
	{
		double total = 0.;
		for(Double p : payments)
			total += p;
		return total;
	}
	
	private Allocation _allocation;								//Allocation of the auction
	private List<Double> _payments;								//A list of payments to be computed
	private List<Type> _bids;									//Bids of bidders
	private List<Integer> _unitsOfItems;						//A list of units of items per item
	private List<Double> _costs;								//A list of costs of items

	private int _numberOfItems;									//Number of goods in the auction
	private int _numberOfAgents;								//number of bidders
	private List<int[][]> _binaryBids;							//Bids converted into a binary matrix format
	
	private final double TOL = 1e-4;							//Tolerance level
	private IloCplex _cplexSolver;								//CPLEX solver
}
