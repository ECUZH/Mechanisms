package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.MultiUnitAtom;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

public class MultiUnitCAXOR implements Auction
{

	/*
	 * Constructor
	 * @param numberOfAgents - the number of agents in the auction
	 * @param numberOfItems - the number of items to be auctioned
	 * @param quantitiesOfItems - numbers of units of items to be auctioned
	 * @param bids - bids of agents
	 */
	public MultiUnitCAXOR(int numberOfAgents, int numberOfItems, List<Integer> quantitiesOfItems, List<Type> bids)
	{
		if(numberOfAgents != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of agents");
		if(numberOfItems != quantitiesOfItems.size() ) throw new RuntimeException("The size of the quantities list should match to the number of items");
		
		_numberOfAgents = numberOfAgents;
		_numberOfItems = numberOfItems;
		_unitsOfItems = quantitiesOfItems;
		_paymentRule = "VCG";
		resetTypes(bids);
	}

	/*
	 * Constructor
	 * @param numberOfAgents - the number of agents in the auction
	 * @param numberOfItems - the number of items to be auctioned
	 * @param quantitiesOfItems - numbers of units of items to be auctioned
	 * @param bids - bids of agents
	 * @param paymentRule - payment rule
	 */
	public MultiUnitCAXOR(int numberOfAgents, int numberOfItems, List<Integer> quantitiesOfItems, List<Type> bids, String paymentRule)
	{
		if(numberOfAgents != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of agents");
		if(numberOfItems != quantitiesOfItems.size() ) throw new RuntimeException("The size of the quantities list should match to the number of items");
		
		_numberOfAgents = numberOfAgents;
		_numberOfItems = numberOfItems;
		_unitsOfItems = quantitiesOfItems;
		_paymentRule = paymentRule;
		resetTypes(bids);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String str = "#Agents="+_numberOfAgents + ", #Items=" + _numberOfItems + "\n";
		for(int i = 0; i < _numberOfAgents; ++i)
			str += _bids.get(i).toString() + "\n";
		return str;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices( List<Double> reservePrices )
	{
		for(Type bid : _bids)
			for(int i = 0; i < bid.getNumberOfAtoms(); ++i)							//For every atomic bid
			{
				List<Integer> itemsSet = bid.getAtom(i).getInterestingSet();
				
				double reservePrice = 0.;
				for(Integer item : itemsSet)										//Compute the reserve price for this bundle
					reservePrice += reservePrices.get( item - 1 ) * ((double)bid.getTypeComponent(i, "Cardinality"));
				
				if( bid.getAtom(i).getValue() < reservePrice )						//Remove the bid if an agent's value for it is smaller than 
					bid.removeAtom(i);												//the reserved price
			}
		
		List<Integer> agentsToRemove = new LinkedList<Integer>();
		for(int i = 0; i < _bids.size(); ++i)
			if( _bids.get(i).getNumberOfAtoms() == 0 )
				agentsToRemove.add(i);
		for(int i = agentsToRemove.size() -1; i >= 0; --i)
			_bids.remove(i);
		
		_numberOfAgents -= agentsToRemove.size();
		convertAllBidsToBinaryFormat();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception 
	{
		computeWinnerDetermination();
		//System.out.println("MOOR: " + this.toString());
		//for(int i = 0; i < _allocation.getNumberOfAllocatedAgents(); ++i)
		//	System.out.println("Allocation. Agent = " + _allocation.getAgentId(i) + ". Val = " + _allocation.getAgentValue(i) );

		switch( _paymentRule )
		{
			case "VCG"        : 	computeVCG();
									break;
			case "Core"       :		computeCorePayments();
									break;
			case "VCGNearest2":		computeNearestVCG2();
									break;
			default			  :		throw new Exception("No such payment rule exists: " + _paymentRule);
		}
	}

	@Override
	public double[] getPayments() throws Exception 
	{
		if( (_payments == null) || (_payments.size() == 0) ) throw new Exception("Payments were not computed.");
		
		double[] paymentsArray = new double[_payments.size()];
		for(int i = 0 ; i < _payments.size(); ++i)
			paymentsArray[i] = _payments.get(i);
		
		return paymentsArray;
	}

	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	@Override
	public void resetPlanner(Planner planner)
	{
		//TODO throw an exception
	}
	
	@Override
	public String getPaymentRule()
	{
		return _paymentRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) 
	{
		_mip = new MIP();
		_mip.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		_bids = agentsTypes;
		_payments = new LinkedList<Double>();
		
		convertAllBidsToBinaryFormat();
		_solverClient = new SolverClient();
	}

	/*
	 * The method builds a MIP problem and feeds it to the CPLEX solver.
	 */
	public void computeWinnerDetermination()
	{
		List<List<Variable> > variables = new LinkedList<List<Variable> >();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		//Create the optimization variables and formulate the objective function:
		for(int i = 0; i < _bids.size(); ++i)								//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<Variable> varI = new LinkedList<Variable>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				Variable x = new Variable("x" + i + "_" + j, VarType.INT, 0, 1);
				varI.add(x);
				_mip.add(x);
				_mip.addObjectiveTerm( bid.getAtom(j).getValue(), x);
			}
			variables.add(varI);
		}
		_mip.setObjectiveMax(true);
				
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			Constraint c = new Constraint(CompareType.LEQ, _unitsOfItems.get(i));
			for(int j = 0; j < _numberOfAgents; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);
				for( int q = 0; q < variables.get(j).size(); ++q )			//Find an atom which contains the i-th item ( itemId = i+1)
					if( binaryBid[q][i] > 0)
					{
						int slotsUsed = _bids.get(j).getAtom(q).getNumberOfUnitsByItemId(i+1);
						c.addTerm( slotsUsed * binaryBid[q][i], variables.get(j).get(q));
					}
			}
			_mip.add(c);
		}

		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfAgents; ++i)
		{
			Constraint c = new Constraint(CompareType.LEQ, 1);
			List<Variable> varI = variables.get(i);
			for(int q = 0; q < varI.size(); ++q)
				c.addTerm( 1.0, varI.get(q) );
				
			_mip.add(c);
		}
				
		//Launch CPLEX to solve the problem:
		_result = _solverClient.solve(_mip);
				
		Map m = _result.getValues();
		_allocation = new Allocation();
		List<Integer> allocatedBidders = new LinkedList<Integer>();
		List<Integer> allocatedBundles = new LinkedList<Integer>();
		List<Double> biddersValues = new LinkedList<Double>();
		for(int i = 0; i < _numberOfAgents; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if( Math.abs( (double) m.get("x"+i+"_"+j) - 1.0 ) < 1e-6 )
				{
					allocatedBundles.add(j);
					try 
					{
						allocatedBidders.add(_bids.get(i).getAgentId());
						allocatedBundles.add(j);
						biddersValues.add(_bids.get(i).getAtom(j).getValue());
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
		try 
		{
			_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, 0., biddersValues);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * The method computes bidder optimal core payments.
	 * @return a list of payments of allocated agents.
	 */
	public List<Double> computeCorePayments()
	{
		IPaymentRule paymentRule = new CorePayments( _allocation, _bids, _unitsOfItems, _numberOfItems, _binaryBids, null);
		try 
		{
			_payments = paymentRule.computePayments();
		} 
		catch (Exception e) 
		{
			System.out.println("ERROR: " + this.toString());
			e.printStackTrace();
		}
		return _payments;
	}
	
	/*
	 * The method computes and returns VCG prices for the CA.
	 * @return a vector of VCG prices for bidders
	 */
	public List<Double> computeVCG()
	{
		IPaymentRule paymentRule = new VCGPayments(_allocation, _bids, _unitsOfItems, _numberOfItems, null);
		try 
		{
			_payments = paymentRule.computePayments();
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		return _payments;
	}
	
	/*
	 * The methods implements bidder-optimal core-selecting mechanism for computation of payments in a 2 winning bidders case.
	 * @return a list of VCG nearest (in L2) core prices.
	 */
	public List<Double> computeNearestVCG2()
	{
		IPaymentRule paymentRule = new CoreNearestVCG2(_allocation, _bids, _unitsOfItems, _numberOfItems, null);
		try {
			_payments = paymentRule.computePayments();
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		return _payments;
	}
	
	/*
	 * The method converts bids of all agents into a binary matrix form.
	 */
	private void convertAllBidsToBinaryFormat()
	{
		_binaryBids = new LinkedList<int[][]>();						//The list contains bids of all agents in a binary format
		for(int i = 0; i < _bids.size(); ++i)
		{
			Type bid = _bids.get(i);									//The bid of the i-th bidder
			int[][] binaryBid = new int[bid.getNumberOfAtoms()][_numberOfItems];	//The binary representation of a bid is used to build constraints
	
			convertBidToBinaryForm(binaryBid, bid.getNumberOfAtoms(), _numberOfItems, bid);
			_binaryBids.add(binaryBid);
		}
	}
	
	/*
	 * The method converts an input bid into a binary format. In the binary format the bid is represented as a matrix A [M x K],
	 * where M is the number of atomic bids in the XOR bid and k is the number of items in the CA. A[i][j] = 1 if a package i 
	 * contains an item j and 0 otherwise. 
	 * @param binary bid - the result will be stored in this matrix
	 * @param numberOfRows - the number of rows of the binaryBid (is equal to the number of atomic bids in the bid)
	 * @param bid - an XOR bid
	 */
	private void convertBidToBinaryForm(int binaryBid[][], int numberOfRows, int numberOfColumns, Type bid )
	{
		for(int i = 0; i < numberOfRows; ++i)
		{
			List<Integer> itemsSet = bid.getAtom(i).getInterestingSet();
			List<Integer> items = new LinkedList<Integer>();
			for(Integer item : itemsSet)
				items.add(item);
			for(int j = 0; j < items.size(); ++j)
				binaryBid[i][(int)(items.get(j))-1 ]  = 1;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isExPostIR()
	 */
	@Override
	public boolean isExPostIR() 
	{
		return true;
	}
	
	private String _paymentRule;
	private IPaymentRule _paymentRuleObj;
	
	private int _numberOfAgents;									//The number of bidders in the CA
	private int _numberOfItems;										//The number of different items to be sold
	private List<Type> _bids;										//Bids submitted by agents
	List<Integer> _unitsOfItems;									//A list of numbers of units of each kind of item
	private List<int[][]> _binaryBids;								//Bids converted into a binary matrix format
	
	private Allocation _allocation;
	private List<Double> _payments;

	SolverClient _solverClient;										//A CPLEX solver client
	private IMIPResult _result;										//A data structure for the solution
	private IMIP _mip = new MIP();									//A data structure for the mixed integer program
	
	static int constraintID = 0;									//Constraints counter
}
