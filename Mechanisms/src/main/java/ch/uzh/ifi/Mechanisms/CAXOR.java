package ch.uzh.ifi.Mechanisms;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;
import edu.harvard.econcs.jopt.solver.SolveParam;

/**
 * The class implements a combinatorial auction with XOR bidding language.
 * @author Dmitry Moor
 */
public class CAXOR implements Auction
{

	private static final Logger _logger = LogManager.getLogger(CAXOR.class);
	
	/**
	 * The constructor builds the initial data structures required by the solver.
	 * @param numberOfAgents - the total number of bidders in the CA
	 * @param numberOfItems - the number of different items to be sold
	 * @param bids - a vector of XOR bids of all agents
	 */
	public CAXOR(int numberOfAgents, int numberOfItems, List<Type> bids)
	{
		if(numberOfAgents != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of agents");
		
		_numberOfAgents = numberOfAgents;
		_numberOfItems = numberOfItems;
		_allocation = new Allocation();
		_paymentRule = "VCG";
		
		resetTypes(bids);
	}
	
	public CAXOR(int numberOfAgents, int numberOfItems, List<Type> bids, List<Double> costs)
	{
		if(numberOfAgents != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of agents");
		
		_numberOfAgents = numberOfAgents;
		_numberOfItems = numberOfItems;
		_allocation = new Allocation();
		_paymentRule = "VCG";
		_costs = costs;
		
		resetTypes(bids);
	}
	/*
	public CAXOR(int numberOfAgents, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{
		if(numberOfAgents != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of agents");
		if(costs == null)					throw new RuntimeException("Costs were not specified");
		if(costs.size() != numberOfItems)	throw new RuntimeException("Costs dimensionality mismatch");
		
		_numberOfAgents = numberOfAgents;
		_numberOfItems = numberOfItems;
		_allocation = new AllocationEC();
		_paymentRule = "VCG";
		_costs = costs;
		_jpmf = jpmf;
		
		resetTypes(bids);
	}*/
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String str = "#Agents="+_numberOfAgents + ", #Items=" + _numberOfItems + "\n";
		for(int i = 0; i < _numberOfAgents; ++i)
			str += _bids.get(i).toString() + "\n";
		return str;
	}
	
	/**
	 * The method sets up reserve prices for individual goods and adds reserve prices on bundles of goods. These prices are computed
	 * as additive prices of individual goods within the bundle. The reserve prices are computed not for all possible bundles but only
	 * for those which are present in bids submitted by all agents.
	 * @param reservePrices - a list of reserve prices for individual goods
	 */
	public void setupReservePrices( List<Double> reservePrices )
	{
		//Vector sellersBid = new Vector();
		for(Type bid : _bids)
		{
			for(int i = 0; i < bid.getNumberOfAtoms(); ++i)							//For every atomic bid
			{
				List<Integer> itemsSet = bid.getAtom(i).getInterestingSet();
				List<Integer> items = new LinkedList<Integer>();
				for(Integer item : itemsSet)
					items.add(item);
				
				double reservePrice = 0.;
				for(int j = 0; j < items.size(); ++j)								//Compute the reserve price for this bundle
					reservePrice += reservePrices.get( (int)(items.get(j)) - 1);
				
				if( ((AtomicBid)(bid.getAtom(i))).getValue() < reservePrice )		//Remove the bid if an agent's value for it is smaller than 
					bid.removeAtom(i);												//the reserved price
				//AtomicBid sellersAtom = new AtomicBid(items, reservePrice);
				//sellersBid.add(sellersAtom);
			}
		}
		//_bids.add(sellersBid);
		//_numberOfAgents += 1;
		convertAllBidsToBinaryFormat();
	}
	
	/**
	 * The method sets the payment rule to be used by the auction.
	 * @param paymentRule - the payment rule to be used by the auction
	 */
	public void setPaymentRule(String paymentRule)
	{
		_paymentRule = paymentRule;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) 
	{	
		_mip = new MIP();
		_mip.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		_bids = agentsTypes;
		_payments = new LinkedList<Double>();
		//for(int i = 0; i < _numberOfAgents; ++i)
		//	_payments.add(0.);
		
		convertAllBidsToBinaryFormat();		
		
		_solverClient = new SolverClient();
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#resetPlanner(ch.uzh.ifi.Mechanisms.Planner)
	 */
	@Override
	public void resetPlanner(Planner planner)
	{
		throw new RuntimeException("No planner is available for the CAXOR m-m");
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getPaymentRule()
	 */
	@Override
	public String getPaymentRule()
	{
		return _paymentRule;
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt()
	{
		if( _paymentRule.equals("VCG_LLG") || _paymentRule.equals("CORE_LLG") )
			computeWinnerDeterminationLLG();
		else
			computeWinnerDetermination();
		
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0 && _allocation.getBiddersInvolved(0).size() > 0)
			switch( _paymentRule )
			{
				case "VCG_LLG"	  :  	computeVCG();
										break;
				case "VCG"        : 	computeVCG();
										break;
				case "CORE"       :		computeCorePayments();
										break;
				case "CORE_LLG":		computeNearestVCG2();
										break;
				default			  :		throw new RuntimeException("No such payment rule exists: " + _paymentRule);
			}
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getPayments()
	 */
	@Override
	public double[] getPayments() throws Exception 
	{
		if( (_payments == null) || (_payments.size() == 0) ) throw new Exception("Payments were not computed.");
		
		double[] paymentsArray = new double[_payments.size()];
		for(int i = 0 ; i < _payments.size(); ++i)
			paymentsArray[i] = _payments.get(i);
		
		return paymentsArray;
	}
	
	/*
	 * The method solves WDP for LLG domain without using CPLEX.
	 */
	public void computeWinnerDeterminationLLG()
	{
		_allocation = new Allocation();
		List<Integer> allocatedBidders  = new LinkedList<Integer>();
		List<Integer> allocatedBundles  = new LinkedList<Integer>();
		List<Double> buyersValues  		= new LinkedList<Double>();

		double sellerCost = 0.;
		
		if( _bids.size() == 3 )
		{
			AtomicBid localBundle1 = _bids.get(0).getAtom(0);
			AtomicBid localBundle2 = _bids.get(1).getAtom(0);
			AtomicBid globalBundle = _bids.get(2).getAtom(0);

			double values[] = {localBundle1.getValue(), localBundle2.getValue(), globalBundle.getValue()};
			double costs[]  = {localBundle1.computeCost(_costs), localBundle2.computeCost(_costs), globalBundle.computeCost(_costs)};

			double swLocal1 = values[0] - costs[0];
			double swLocal2 = values[1] - costs[1];
			double swGlobal = values[2] - costs[2];
			_logger.debug("3 bidders. " + "sw1 = " + swLocal1 + " sw2 = " + swLocal2 + " sw3 = " + swGlobal);

			if( swLocal1 >= 0 && swLocal2 >= 0 && swLocal1 + swLocal2 >= swGlobal )	//Allocate to local bidders
			{
				_logger.debug("Allocate to local bidders.");
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, localBundle1, values[0], costs[0],  0);
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, localBundle2, values[1], costs[1],  0);
			}
			else if( swLocal1 >= 0 && swLocal2 < 0 && swLocal1 >= swGlobal )
			{
				_logger.debug("Allocate to a single local bidder.");
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, localBundle1, values[0], costs[0], 0);
			}
			else if( swLocal2 >= 0 && swLocal1 < 0 && swLocal2 >= swGlobal )
			{
				_logger.debug("Allocate to a single local bidder.");
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, localBundle2, values[1], costs[1], 0);
			}
			else if( swGlobal >= 0 )
			{
				_logger.debug("Allocate to a global bidder.");
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, globalBundle, values[2], costs[2], 0);
			}
		}
		else if (_bids.size() == 2)									//Reduced LLG is used when computing, for example, VCG
		{
			_logger.debug("2 bidders.");
			if( _bids.get(1).getAgentId() == 3)						//If one of bidders is a global bidder
			{
				AtomicBid globalBundle = _bids.get(1).getAtom(0);
				AtomicBid localBundle  = _bids.get(0).getAtom(0);
				
				double values[] = {localBundle.getValue(), globalBundle.getValue()};
				double costs[] = {localBundle.computeCost(_costs), globalBundle.computeCost(_costs)};
				
				double sw1 = values[0] - costs[0];
				double sw2 = values[1] - costs[1];
				_logger.debug("2 bidders (local+global). " + "sw1 = " + sw1 + " sw2 = " + sw2);

				AtomicBid allocatedBundle = null;
				int allocatedIdx = 0;
				if( sw1 >= 0 && sw1 >= sw2 )
				{
					allocatedBundle = localBundle;
					allocatedIdx = 0;
				}
				else if( sw2 >= 0 && sw2 >= sw1 )
				{
					allocatedBundle = globalBundle;
					allocatedIdx = 1;
				}
				
				if( allocatedBundle != null )
					sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, allocatedBundle, values[allocatedIdx], costs[allocatedIdx], 0);
			}
			else											//Only local bidders
			{
				AtomicBid[] bundles = { _bids.get(0).getAtom(0), _bids.get(1).getAtom(0) };
				double values[] = {bundles[0].getValue(), bundles[1].getValue()};
				double costs[]  = {bundles[0].computeCost(_costs), bundles[1].computeCost(_costs)};
				double[] sw = {values[0] - costs[0], values[1] - costs[1]};
				
				for(int i = 0; i < 2; ++i)
					if( sw[i] >= 0 )
						sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, bundles[i], values[i], costs[i], 0);
			}
		}
		else
		{
			AtomicBid bundle = _bids.get(0).getAtom(0);
			double value = bundle.getValue();
			double cost = bundle.computeCost(_costs);
			double sw = value - cost;
			
			if(sw >= 0)
				sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, bundle, value, cost, 0);
		}
		
		if(allocatedBundles.size() > 0)
			try
			{
				_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, sellerCost, buyersValues);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
	}
	
	/*
	 * The method builds a MIP problem and feeds it to the CPLEX solver.
	 */
	public void computeWinnerDetermination() //throws Exception
	{
		List<Integer> allocatedBidders = new LinkedList<Integer>();
		List<Integer> allocatedBundles = new LinkedList<Integer>();
		List<Double> buyersValues      = new LinkedList<Double>();
		double sellerCost = 0.;
		
		List<List<Variable> > variables = new LinkedList<List<Variable> >();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		//Create the optimization variables and formulate the objective function:
		for(int i = 0; i < _bids.size(); ++i)								//For every bidder ...
		{
			Type bid = _bids.get(i);
			List<Variable> varI = new LinkedList<Variable>();				//Create a new variable per atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )				//For every atomic bid ...
			{
				AtomicBid bundle = bid.getAtom(j);
				double value = bundle.getValue();
				double cost = bundle.computeCost(_costs);
				Variable x = new Variable("x" + i + "_" + j, VarType.INT, 0, 1);
				varI.add(x);
				_mip.add(x);
				_mip.addObjectiveTerm( value - cost, x);
			}
			variables.add(varI);
		}
		_mip.setObjectiveMax(true);
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			Constraint c = new Constraint(CompareType.LEQ, 1);
			for(int j = 0; j < _numberOfAgents; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);
				List<Variable> varI = (List<Variable>)(variables.get(j));
				for( int q = 0; q < varI.size(); ++q )
					c.addTerm( binaryBid[q][i], varI.get(q));
			}
			_mip.add(c);
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfAgents; ++i)
		{
			Constraint c = new Constraint(CompareType.LEQ, 1);
			List<Variable> varI = (List<Variable>)(variables.get(i));
			for(int q = 0; q < varI.size(); ++q)
				c.addTerm( 1.0, varI.get(q));

			_mip.add(c);
		}
		
		//Launch CPLEX to solve the problem:
		_result = _solverClient.solve(_mip);
				
		Map<String, Double> m = _result.getValues();
		_allocation = new Allocation();
		
		for(int i = 0; i < _numberOfAgents; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if( Math.abs( (double) m.get("x"+i+"_"+j) - 1.0 ) < 1e-6 )
				{
					try 
					{						
						sellerCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersValues, _bids.get(i).getAtom(j), 
																_bids.get(i).getAtom(j).getValue(), _bids.get(i).getAtom(j).computeCost( _costs  ),  j);

					} catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
		
		if(allocatedBundles.size() > 0)
			try 
			{
				_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, sellerCost, buyersValues);
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
		List<Integer> units = new LinkedList<Integer>();
		for(int i = 0; i < _numberOfItems; ++i)
			units.add(1);
				
		PaymentRule paymentRule = new CorePayments( _allocation, _bids, units, _numberOfItems, _binaryBids, _costs);
		try 
		{
			_payments = paymentRule.computePayments();
		} 
		catch (Exception e) 
		{
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
		List<Integer> units = new LinkedList<Integer>();
		for(int i = 0; i < _numberOfItems; ++i)
			units.add(1);
		
		PaymentRule paymentRule = new VCGPayments(_allocation, _bids, units, _numberOfItems, _costs);
		try 
		{
			_payments = paymentRule.computePayments();
		} 
		catch (Exception e) 
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
		List<Integer> units = new LinkedList<Integer>();
		for(int i = 0; i < _numberOfItems; ++i)
			units.add(1);
		
		PaymentRule paymentRule = new CoreNearestVCG2(_allocation, _bids, units, _numberOfItems, _costs);
		try 
		{
			_payments = paymentRule.computePayments();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return _payments;
	}
	
	/*
	 * The method prints out the results of the winner determination problem.
	 */
	public void printResults()
	{
		double obj = _result.getObjectiveValue();
		System.out.println("Objective: " + obj + ". Wellfare: " + _allocation.getAllocatedWelfare() );
		
		Map m = _result.getValues();
		for(int i = 0; i < _numberOfAgents; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j )
				System.out.println("x"+i+"_"+j+" is " + m.get("x"+i+"_"+j));
		
		double revenue = 0.;
		for(Double p : _payments)
			revenue += p;
		if( _payments.size() != 0 )
			System.out.println("Total revenue: " + revenue);
		else
			System.out.println("Payments were not computed.");
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation()
	{
		return _allocation;
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
	
	/*
	 * The method used by WDP for LLG domain to fill some data structures required by the Allocation object.
	 * @param allocatedBidders - a list of allocated bidders (is filled by this method)
	 * @param allocatedBundles - a list of indexes of allocated bundles for every allocated bidder
	 * @param buyersExpectedValues - an empty list for expected values of allocated buyers
	 * @param atom  - allocated bundle
	 * @param value - marginal value of the buyer for the bundle
	 * @param cost -  marginal cost of the seller for the bundle
	 * @param allocatedBundleIdx - an index of the bundle allocated for the bidder (within his combinatorial bid)
	 * @return an expected cost of the seller for the bundle
	 */
	private double addAllocatedAgent(List<Integer> allocatedBidders,  List<Integer> allocatedBundles, List<Double> buyersExpectedValues,  
			                         AtomicBid atom, double value, double cost, int allocatedBundleIdx)
	{
		allocatedBundles.add(allocatedBundleIdx);
		allocatedBidders.add( atom.getAgentId() );
		double sellerExpectedCost = cost;
		buyersExpectedValues.add( value );		
		return sellerExpectedCost;
	}
	
	/*
	 * The method computes availability of the specified bundle given realizations of random variables. This availability
	 * is computed as minimal availability among all individual goods within the bundle. 
	 * @param bundle - the bundle for which availability should be computed
	 * @param sample - one random sample of realizations of all random variables.
	 * @return realized availability of the bundle
	 */
	public double getRealizedAvailability(List<Integer> bundle, double[] sample)
	{
		double realizedRV = 1.;
		for(Integer itemId : bundle)
		{
			int itemIdx = itemId - 1;
			if( sample[ itemIdx ] < realizedRV )
				realizedRV = sample[ itemIdx ];
		}
		return realizedRV;
	}
	
	private String _paymentRule;
	private PaymentRule _paymentRuleObj;
	
	private int _numberOfAgents;									//The number of bidders in the CA
	private int _numberOfItems;										//The number of different items to be sold
	private List<Double> _costs;									//Seller's costs of items
	private List<Type> _bids;										//Bids submitted by agents
	private List<int[][]> _binaryBids;								//Bids converted into a binary matrix format

	private List<Double> _payments;
	private Allocation _allocation;
	private JointProbabilityMass _jpmf;

	SolverClient _solverClient;										//A CPLEX solver client
	private IMIPResult _result;										//A data structure for the solution
	private IMIP _mip = new MIP();									//A data structure for the mixed integer program
	
	static int constraintID = 0;									//Constraints counter
}
