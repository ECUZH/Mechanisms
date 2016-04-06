package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

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
	
	/**
	 * Constructor
	 * @param numberOfAgents number of bidders
	 * @param numberOfItems number of goods
	 * @param bids bids of bidders
	 * @param costs costs for goods
	 */
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
	 * The method sets up a CPLEX solver for WDP.
	 * @param solver CPLEX solver
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) 
	{	
		_bids = agentsTypes;
		_payments = new ArrayList<Double>();
		convertAllBidsToBinaryFormat();	
		_cplexSolver = null;
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
	public void solveIt() throws Exception
	{
		if( _paymentRule.equals("VCG_LLG") || _paymentRule.equals("CORE_LLG") || _paymentRule.equals("CORE_SH_LLG") )
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
				case "CORE_SH_LLG":		computeNearestShapleyProj();
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
	
	/**
	 * The method builds a MIP problem and feeds it to the CPLEX solver.
	 */
	public void computeWinnerDetermination() throws IloException //throws Exception
	{
		if( _cplexSolver == null)
			_cplexSolver = new IloCplex();
		else
			_cplexSolver.clearModel();
		_cplexSolver.setOut(null);
		
		List<Integer> allocatedBidders = new LinkedList<Integer>();
		List<Integer> allocatedBundles = new LinkedList<Integer>();
		List<Double> buyersValues      = new LinkedList<Double>();
		double sellerCost = 0.;
		
		List<List<IloNumVar> > variables = new ArrayList<List<IloNumVar>>();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
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
				
				IloNumExpr term = _cplexSolver.prod((value - cost), x);
				objective = _cplexSolver.sum(objective, term);
			}
			variables.add(varI);
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
					if( binaryBid[q][i] > 0)
					{
						IloNumExpr term = _cplexSolver.prod(binaryBid[q][i], varI.get(q));
						constraint = _cplexSolver.sum(constraint, term);
					}
			}
			lp.addRow( _cplexSolver.ge(1.0, constraint, "Item_"+i) );
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfAgents; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			double upperBound = 1.;
			
			List<IloNumVar> varI = (List<IloNumVar>)(variables.get(i));
			for(int q = 0; q < varI.size(); ++q)
				constraint = _cplexSolver.sum(constraint, varI.get(q));

			lp.addRow( _cplexSolver.ge(upperBound, constraint, "Bidder"+i));
		}
		
		//Launch CPLEX to solve the problem:
		_cplexSolver.setParam(IloCplex.Param.RootAlgorithm, 2);		
		
		_cplexSolver.solve();
		
		//Map<String, Double> m = _result.getValues();
		_allocation = new Allocation();
		
		for(int i = 0; i < _numberOfAgents; ++i)
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
				if( Math.abs( _cplexSolver.getValue(variables.get(i).get(j)) - 1.0 ) < 1e-6 )
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
				_logger.debug("Allocated bidders: " + allocatedBidders);
				_logger.debug("Allocated bundles: " + allocatedBundles);
				_allocation.addAllocatedAgent(0, allocatedBidders, allocatedBundles, sellerCost, buyersValues);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
	}
	
	/**
	 * The method computes bidder optimal core payments.
	 * @return a list of payments of allocated agents.
	 * @throws PaymentException if VCG is in the core
	 */
	public List<Double> computeCorePayments() throws PaymentException
	{
		List<Integer> units = new LinkedList<Integer>();
		for(int i = 0; i < _numberOfItems; ++i)
			units.add(1);
				
		CorePayments paymentRule = new CorePayments( _allocation, _bids, units, _numberOfItems, _binaryBids, _costs);
		paymentRule.setSolver(_cplexSolver);
		try 
		{
			_payments = paymentRule.computePayments();
			_vcgToValueRatio = paymentRule.getVCGtoValueRatio();
			_revenueRatio = paymentRule.getRevenueRatio();
		} 
		catch (PaymentException e) 
		{
			if(e.getMessage().equals("VCG is in the Core"))
			{
				switch(_paymentRule)
				{
					case "CORE" :	_payments = e.getPayments(); break;
					default     :	
				}
				throw e;
			}
			e.printStackTrace();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return _payments;
	}
	
	/**
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
	
	/**
	 * 
	 * @return
	 */
	public List<Double> computeNearestShapleyProj()
	{
		List<Integer> units = new LinkedList<Integer>();
		for(int i = 0; i < _numberOfItems; ++i)
			units.add(1);
		
		PaymentRule paymentRule = new CoreNearestShapleyProj(_allocation, _bids, units, _numberOfItems, _costs);
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
	/*public void printResults()
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
	}*/
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation()
	{
		return _allocation;
	}
	
	/**
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
	
	/**
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
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return false;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isExPostIR()
	 */
	@Override
	public boolean isExPostIR() 
	{
		return true;
	}
	
	/**
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
	
	/**
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
	
	/**
	 * The method returns the ratio of the distance of  VCG to core to the distance between VCG and reported values
	 * @return the ratio of the distance of  VCG to core to the distance between VCG and reported values
	 */
	public double getVCGtoValueRatio()
	{
		return _vcgToValueRatio;
	}
	
	/**
	 * The method returns the ratio of the VCG revenue to core revenue.
	 * @return the ratio of VCG to core revenue
	 */
	public double getRevenueRatio()
	{
		return _revenueRatio;
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
	
	private IloCplex _cplexSolver;
	
	private double _vcgToValueRatio;								//Used for benchmarking
	private double _revenueRatio;									//Used for benchmarking
}
