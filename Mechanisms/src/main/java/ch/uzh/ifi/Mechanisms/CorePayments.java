package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.Term;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/*
 * The class implements computation of core bidder optimal payments according to the constraint generation technique proposed by Day et al.
 * The bids should be specified using the XOR bidding language.
 */
public class CorePayments implements PaymentRule
{
	/*
	 * @param allocation - allocation produced by WDP
	 * @param allBids - list of all bids of the CA
	 * @param numberOfItems - the number of items in the CA
	 * @param binaryBids - (see CAXOR.java for explanation of the data structure)
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
		
		_solverClient = new SolverClient();
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception
	{
		if( _allocation.getBiddersInvolved(0).size() <= 0 ) 
		{
			_payments = new LinkedList<Double>();
			throw new Exception("No agents were allocated, return an empty list.");
		}
		
		PaymentRule vcgRule = new VCGPayments(_allocation, _bids, _unitsOfItems, _numberOfItems, _costs);
		List<Double> vcg = vcgRule.computePayments();
		_payments = vcg;
		
		List<Integer> blockingCoalition = new LinkedList<Integer>();
		double z = computeSEP(vcg, blockingCoalition);
		double total = computeTotalPayments(vcg);
				
		Variable[] pi = new Variable[ _allocation.getBiddersInvolved(0).size() ];
		IMIP mipBPO = new MIP();
		mipBPO.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		mipBPO.setSolveParam(SolveParam.DISPLAY_OUTPUT, false);
		
		//Create optimization variables and formulate the objective function
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			int allocatedBidderId = _allocation.getBiddersInvolved(0).get(i);
			int itsIdx = allocatedBidderId - 1;
			int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(i);
			double itsValue = _bids.get(itsIdx).getAtom(itsAllocatedAtom ).getValue();
					
			if( vcg.get(i) > itsValue +	TOL )
				throw new RuntimeException("MOOR: THE LOWER BOUND " + vcg.get(i)+" SHOULD NOT BE HIGHER THAN THE UPPER BOUND: " + _bids.get(itsIdx).getAtom( itsAllocatedAtom ).getValue() + ". VCG:" + vcg.toString());

			Variable x = new Variable("pi_"+_allocation.getBiddersInvolved(0).get(i), VarType.DOUBLE, vcg.get(i), itsValue );
			pi[i] = x;
			mipBPO.add(x);
			mipBPO.addObjectiveTerm( 1.0, x  );
		}
		mipBPO.setObjectiveMax(false);
		int constraintIdBPO = 0;
		
		while( z > total + TOL )
		{		
			//Create optimization constraints
			double cnst = 0.;
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				for(int j  = 0; j < blockingCoalition.size(); ++j)
					if( _allocation.getBiddersInvolved(0).get(i) == blockingCoalition.get(j) )
					{
						cnst += _payments.get(i);
						break;
					}
			Constraint c = new Constraint(CompareType.GEQ, z - cnst);

			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
			{
				boolean isBlocking = false;
				for(int j = 0; j < blockingCoalition.size(); ++j)
					if( _allocation.getBiddersInvolved(0).get(i) == blockingCoalition.get(j) )
					{
						isBlocking = true;
						break;
					}
				
				if( ! isBlocking)
					c.addTerm(1.0, pi[i]);
			}
			mipBPO.add(c);
			constraintIdBPO += 1;
			
			IMIPResult result = _solverClient.solve(mipBPO);
			
			_payments = new LinkedList<Double>();
			Map m = result.getValues();
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				_payments.add( (Double)(m.get("pi_"+_allocation.getBiddersInvolved(0).get(i)   )) );

			blockingCoalition = new LinkedList<Integer>();
			z = computeSEP( _payments, blockingCoalition);
			total = computeTotalPayments(_payments);
			
			if( constraintIdBPO > 10)
			{
				//System.out.println("New payments: " + _payments.toString());
				//System.out.println("z: " + z + " iter = " + constraintIdBPO);
			}
		}
		return _payments;
	}
	
	
	/*
	 * @param paymentsT - winners payments
	 * @param blockingCoalition - a reference for the blocking coalition to be stored (IDs of agents within the coalition)
	 */
	public double computeSEP(List<Double> paymentsT, List<Integer> blockingCoalition)
	{
		int constraintIdSEP = 0;											//constraints counter
		IMIP mipSEP = new MIP();
		mipSEP.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		
		List<List<Variable> > variables = new LinkedList<List<Variable> >();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		List<Variable> gammaVariables = new LinkedList<Variable>();			//variables taking into account winners of the previous iteration
		
		//Create the optimization variables and formulate the objective function:
		for(int i = 0; i < _bids.size(); ++i)							//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<Variable> varI = new LinkedList<Variable>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				Variable x = new Variable("x" + i + "_" + j, VarType.INT, 0, 1);
				AtomicBid bundle = bid.getAtom(j);
				double value = bundle.getValue();
				double cost = computeCost(bundle);
				varI.add(x);
				mipSEP.add(x);
				mipSEP.addObjectiveTerm( value - cost, x);
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

			Variable gamma = new Variable("Gamma_" + j, VarType.INT, 0, 1);
			gammaVariables.add(gamma);
			mipSEP.add(gamma);
			mipSEP.addObjectiveTerm( -1*( value - paymentsT.get(j)  ), gamma );
		}
		
		mipSEP.setObjectiveMax(true);
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			Constraint c = new Constraint(CompareType.LEQ, _unitsOfItems.get(i) );
			for(int j = 0; j < _numberOfAgents; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);
				List<Variable> varI = (List<Variable>)(variables.get(j));
				for( int q = 0; q < varI.size(); ++q )
					if( binaryBid[q][i] > 0 )
					{
						int itemId = i+1;
						try 
						{
							int slotsUsed = 1;//(/*(MultiUnitAtom)*/_bids.get(j).getAtom(q)).getNumberOfUnitsByItemId(itemId);
							c.addTerm(slotsUsed * binaryBid[q][i], varI.get(q));
						} 
						catch (Exception e) 
						{
							System.out.println("ITEMS constr. Item idx=" + i + ", ItemId=" + itemId + 
							           ", for agent " + j + "'s atom " + q + ": " + _bids.get(j).getAtom(q).toString());
							e.printStackTrace();
						}
					}
			}
			mipSEP.add(c);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfAgents; ++i)
		{
			Constraint c;
			boolean isWinner = false;
			int itsIdx = 0;
			for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
			{
				if( _allocation.getBiddersInvolved(0).get(j) == _bids.get(i).getAgentId())
				{
					isWinner = true;
					break;
				}
				itsIdx += 1;
			}
			
			if(isWinner)
			{
				c = new Constraint(CompareType.LEQ, 0);
				c.addTerm( -1., gammaVariables.get(itsIdx));
			}
			else
				c = new Constraint(CompareType.LEQ, 1);
			
			List<Variable> varI = (List<Variable>)(variables.get(i));
			for(int q = 0; q < varI.size(); ++q)
				c.addTerm(1.0, varI.get(q) );

			mipSEP.add(c);
		}
		
		//Launch CPLEX to solve the problem:
		IMIPResult result = _solverClient.solve(mipSEP);
		//System.out.println("Res " + result.getObjectiveValue());
		
		
		Map m = result.getValues();
		for(int i = 0; i < _numberOfAgents; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if( Math.abs( (double) m.get("x"+i+"_"+j) - 1.0 ) < 1e-6 )
					blockingCoalition.add( _bids.get(i).getAgentId() );
		
		//System.out.println("Blocking coalition: " + blockingCoalition.toString() + ". Coalitional value is " + result.getObjectiveValue() );
		return result.getObjectiveValue();
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#isBudgetBalanced()
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
	
	/*
	 * The method returns an index of the specified agent Id in the list of all submitted bids.
	 * @param agentId - an ID of an agent for which an index is required.
	 * @throws an exception if there're no bids of the specified agent
	 */
	private int getIndexOfAllocatedAgent(int agentId)
	{
		for(int j  = 0; j < _numberOfAgents; ++j)
			if( _bids.get(j).getAgentId() == agentId )
				return j;
		
		throw new RuntimeException("No agent with id " + agentId + " found in the list of submitted bids: " + _bids.toString());
	}
	
	private double computeTotalPayments(List<Double> payments)
	{
		double total = 0.;
		for(Double p : payments)
			total += p;
		return total;
	}
	
	/*
	 * The method computes the additive cost of a given bundle.
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
	
	private Allocation _allocation;
	private List<Double> _payments;
	private List<Type> _bids;
	private List<Integer> _unitsOfItems;
	private List<Double> _costs;

	private int _numberOfItems;
	private int _numberOfAgents;
	private List<int[][]> _binaryBids;								//Bids converted into a binary matrix format
	
	SolverClient _solverClient;										//A CPLEX solver client

	private final double TOL = 1e-4;
}
