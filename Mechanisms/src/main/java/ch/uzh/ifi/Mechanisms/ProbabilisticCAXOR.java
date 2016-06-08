package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

/**
 * The class implements a probabilistic Combinatorial Auction with XOR bidding language.
 * @author Dmitry Moor
 *
 */
public class ProbabilisticCAXOR implements Auction
{
	
	private static final Logger _logger = LogManager.getLogger(ProbabilisticCAXOR.class);
	
	/**
	 * Constructor
	 * @param numberOfBuyers the number of buyers in the auction
	 * @param numberOfItems the number of items to be auctioned
	 * @param bids bids of agents. A bid consists of atomic bids. An Atomic Bid must include a bundle, i.e., a list
	 *               of items an agent is willing to buy. Each item must be encoded with an integer starting from 1, 2, ... m.
	 * @param costs a list of costs per good
	 * @param jpmf joint probability mass function for availabilities of individual goods
	 */
	public ProbabilisticCAXOR(int numberOfBuyers, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{
		if(numberOfBuyers != bids.size())	throw new RuntimeException("The number of bids should be equla to the number of buyers");
				
		_numberOfBuyers = numberOfBuyers;
		_numberOfItems = numberOfItems;
		_paymentRule = "EC-VCG";
		_costs = costs;
		_jpmf = jpmf;
		_cplexSolver = null;
		resetTypes(bids);
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
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String str = "< #Buyers=" + _numberOfBuyers + ", #Items=" + _numberOfItems + ", Bids: " + _bids.toString() + 
				     ", costs: " + _costs.toString() + ">";
		
		if( _allocation != null )
		{
			str += " E[SW] = " + _allocation.getExpectedWelfare();
			for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
				str += ". Realizations["+ _allocation.getBiddersInvolved(0).get(i) +"]: " + _allocation.getRealizedRV(0, i);
		}
		
		str += "\n";
		return str;
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception 
	{
		computeWinnerDetermination();
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
			switch( _paymentRule )
			{
				case "ECR-VCG_LLG"  :	computePayments(new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;
				case "ECC-VCG_LLG"  :	computePayments(new ECCVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;
				case "EC-VCG_LLG"  	:	computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;					
				case "EC-VCG"     	:	computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;
				case "ECR-CORE_LLG"	:	computePayments(new ECRCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
										break;
				case "ECC-CORE_LLG"	:	computePayments(new ECCCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
										break;
				case "EC-CORE_LLG"	:	computePayments(new ECCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
										break;
				case "EC-CORE"	  	: 	computePayments(new ECCorePayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _binaryBids, _jpmf, _cplexSolver));
										break;
				case "ECC-CORE"	  	: 	computePayments(new ECCCorePayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _binaryBids, _jpmf, _cplexSolver));
										break;
				case "ECR-CORE"	  	: 	computePayments(new ECRCorePayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _binaryBids, _jpmf, _cplexSolver));
										break;
				case "Exp-VCG"	  	: 	computePayments(new ExpVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;
				case "Exp-VCG_LLG"	: 	computePayments(new ExpVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
										break;
				case "Exp-CORE_LLG"	:	computePayments(new ExpCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
										break;
				case "Exp-CORE"		:	computePayments(new ExpCorePayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _binaryBids, _jpmf, _cplexSolver));
										break;
				case "expostIR_ECR"	:	computePayments(new ECRCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
										break;
				default			  	:	throw new Exception("No such payment rule exists: " + _paymentRule);
			}
	}

	/*
	 * The method builds a MIP problem and feeds it to the CPLEX solver.
	 */
	public void computeWinnerDetermination()
	{
		computeWinnerDetermination(null, null);
	}
	
	/*
	 * The method builds a MIP problem and feeds it to the CPLEX solver.
	 */
	public void computeWinnerDetermination(List<Integer> allocatedAvailabilities, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeWinnerDetermination(allocatedAvailabilities, realizedAvailabilities)");
		
		if( _paymentRule.equals("expostIR_ECR") )
		{
			throw new RuntimeException("Currently unsupported");
			//computeWinnerDeterminationStrawMan(allocatedAvailabilities, realizedAvailabilities);
		}
		else if( _paymentRule.equals("EC-VCG_LLG")  || _paymentRule.equals("EC-CORE_LLG") || 
			     _paymentRule.equals("Exp-VCG_LLG") || _paymentRule.equals("Exp-CORE_LLG")||
			     _paymentRule.equals("ECC-VCG_LLG") || _paymentRule.equals("ECC-CORE_LLG")|| 
			     _paymentRule.equals("ECR-VCG_LLG") || _paymentRule.equals("ECR-CORE_LLG") )		//TODO: Should also work for EC-VCG for LLG
		{
			computeWinnerDeterminationLLG(allocatedAvailabilities, realizedAvailabilities);
		}
		else
		{
			try 
			{
				computeWinnerDeterminationGeneral(allocatedAvailabilities, realizedAvailabilities);
			} 
			catch (IloException e) 
			{
				e.printStackTrace();
			}
		}
		
		_logger.debug("WDP finished: " + _allocation.toString());
		_logger.debug("<- computeWinnerDetermination(...)");
	}

	/**
	 * The method solved the WDP for the LLG domain.
	 * @param allocatedGoods
	 * @param realizedAvailabilities
	 */
	public void computeWinnerDeterminationLLG(List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeWinnerDeterminationLLG(allocatedGoods="+(allocatedGoods != null ? allocatedGoods.toString():"")+", " +( realizedAvailabilities!= null ? realizedAvailabilities.toString():"") +")");
		IAllocationRule allocationRule = new AllocationRuleNonDiscriminatingBiddersLLG(_bids, _costs, _jpmf);
		try 
		{
			allocationRule.computeAllocation(allocatedGoods, realizedAvailabilities);
		} 
		catch (IloException e) 
		{
			_logger.error("The WDP for the LLG domain should not use CPLEX.");
			e.printStackTrace();
		}
		_allocation = (AllocationEC)allocationRule.getAllocation();
		_logger.debug("<- computeWinnerDeterminationLLG(...)");
	}
	
	/**
	 * The method solves WDP for a general (non-LLG) setting
	 * @param allocatedGoods a list of previously allocated goods if known (null if no allocation happened so far) 
	 * @param realizedAvailabilities realizations of availabilities of allocated goods (null if no allocation happened so far)
	 */
	public void computeWinnerDeterminationGeneral(List<Integer> allocatedGoods, List<Double> realizedAvailabilities) throws IloException 
	{
		_logger.debug("-> computeWinnerDeterminationGeneral(allocatedGoods="+ (allocatedGoods!=null?allocatedGoods.toString():"") + ", realizedAvailabilities="+ (realizedAvailabilities!=null?realizedAvailabilities.toString():"") + ")");
		IAllocationRule allocationRule = new AllocationRuleNonDiscriminatingBidders(_bids, _costs, _jpmf, _numberOfItems, _binaryBids);
		allocationRule.setSolver(_cplexSolver);
		allocationRule.computeAllocation(allocatedGoods, realizedAvailabilities);
		_allocation = (AllocationEC)allocationRule.getAllocation();
		_logger.debug("<- computeWinnerDeterminationGeneral(...)");
	}
	
	/**
	 * The method computes and returns payments for the market.
	 * @param pr - payment rule to be used
	 * @return a vector of prices for bidders
	 */
	public List<Double> computePayments(IPaymentRule pr) throws Exception
	{
		IPaymentRule paymentRule = pr;
		try
		{
			if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
				_payments = paymentRule.computePayments();
			else
				_payments = new ArrayList<Double>();
		}
		catch(PaymentException e)
		{
			if( e.getMessage().equals("Empty Core")  )
			{
				switch(_paymentRule )
				{
					case "EC-CORE"		:	_payments = computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "EC-CORE_LLG"	:	_payments = computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECC-CORE"		:	_payments = computePayments(new ECCVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECC-CORE_LLG"	:	_payments = computePayments(new ECCVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECR-CORE"		:	_payments = computePayments(new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECR-CORE_LLG"	:	_payments = computePayments(new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											throw e;
											//break;
					case "Exp-CORE"		:	if( e.getMessage().equals("VCG is in the Core") )
												_payments = computePayments(new ExpVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											else throw new RuntimeException("The Exp-CORE cannot be empty. " + e.toString());
											break;
					case "expostIR_ECR" :	throw e;
					default				:	throw new Exception("Empty Core exception. No such payment rule exists: " + _paymentRule);
				}
				throw e;					//Required by the BNE algorithm to estimate the number of empty core cases (see UtilityEstimator.java)
			}
			else if(e.getMessage().equals("VCG is in the Core"))
			{
				switch(_paymentRule )
				{
					case "EC-CORE"		:	_payments = computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "EC-CORE_LLG"	:	_payments = computePayments(new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECC-CORE"		:	_payments = e.getPayments(); 
											break;
					case "ECC-CORE_LLG"	:	_payments = computePayments(new ECCVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECR-CORE"		:	_payments = computePayments(new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											break;
					case "ECR-CORE_LLG"	:	_payments = computePayments(new ECRVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											throw e;
											//break;
					case "Exp-CORE"		:	if( e.getMessage().equals("VCG is in the Core") )
												_payments = computePayments(new ExpVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, _cplexSolver));
											else throw new RuntimeException("The Exp-CORE cannot be empty. " + e.toString());
											break;
					case "expostIR_ECR" :	throw e;
					default				:	throw new Exception("Empty Core exception. No such payment rule exists: " + _paymentRule);
				}
				throw e;					//Required by the BNE algorithm to estimate the number of empty core cases (see UtilityEstimator.java)
			}
			else							_payments = new ArrayList<Double>();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return _payments;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices(List<Double> reservePrices) 
	{
		throw new RuntimeException("No reserve prices supported.");
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPayments()
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
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) 
	{
		//_mip = new MIP();
		//_mip.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		
		_bids = agentsTypes;
		_payments = new ArrayList<Double>();
		
		convertAllBidsToBinaryFormat();
		
		//_solverClient = new SolverClient();
		_randomSeed = System.nanoTime();
		_generator = new Random();
		_generator.setSeed(_randomSeed);
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetPlanner(Mechanisms.Planner)
	 */
	@Override
	public void resetPlanner(Planner planner) 
	{
		throw new RuntimeException("No planer is used for this type of auction.");
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
		return false;
	}
	
	/**
	 * The method sets the random seed.
	 * @param seed - a random seed to be used
	 */
	public void setSeed(long seed)
	{
		_randomSeed = seed;
		_generator.setSeed(_randomSeed);
	}
	
	/**
	 * The method sets the payment rule to be used by the auction.
	 * Possible payment rules:
	 * - EC-VCG
	 * - EC-CORE_LLG
	 * - EC-CORE
	 * @param paymentRule - the payment rule to be used by the auction
	 */
	public void setPaymentRule(String paymentRule)
	{
		_paymentRule = paymentRule;
	}
	
	/**
	 * The method converts bids of all agents into a binary matrix form.
	 */
	private void convertAllBidsToBinaryFormat()
	{
		_binaryBids = new ArrayList<int[][]>();						//The list contains bids of all agents in a binary format
		for(int i = 0; i < _bids.size(); ++i)
		{
			Type bid = _bids.get(i);									//The bid of the i-th bidder
			int[][] binaryBid = new int[bid.getNumberOfAtoms()][_numberOfItems];
	
			convertBidToBinaryForm(binaryBid, bid.getNumberOfAtoms(), _numberOfItems, bid);
			_binaryBids.add(binaryBid);
		}
	}
	
	/**
	 * The method converts an input bid into a binary format. In the binary format the bid is represented as a matrix A [M x K],
	 * where M is the number of atomic bids in the XOR bid and k is the number of items in the CA. A[i][j] = 1 if a package i 
	 * contains an item j and 0 otherwise. 
	 * @param binaryBid - the result will be stored in this matrix
	 * @param numberOfRows - the number of rows of the binaryBid (is equal to the number of atomic bids in the bid)
	 * @param numberOfColumns - the number of columns of the binary bid
	 * @param bid - an XOR bid
	 */
	private void convertBidToBinaryForm(int binaryBid[][], int numberOfRows, int numberOfColumns, Type bid )
	{
		for(int i = 0; i < numberOfRows; ++i)
		{
			List<Integer> itemsSet = bid.getAtom(i).getInterestingSet();
			List<Integer> items = new ArrayList<Integer>();
			
			for(Integer item : itemsSet)
				items.add(item);
			
			for(int j = 0; j < items.size(); ++j)
				binaryBid[ i ][ (int)(items.get(j))-1 ]  = 1;
		}
	}
	
	/**
	 * The method computes the expected availability of a bundle by a buyer given the exogenous joint probability density function.
	 * @param atom - an atomic bid for the bundle
	 * @param allocatedGoods
	 * @param realizedAvailabilities
	 * @return the expected availability of the bundle
	 */
	public double computeExpectedMarginalAvailability(AtomicBid atom, List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeExpectedMarginalAvailability(atom: "+atom.toString()+ ", " + (allocatedGoods != null ? allocatedGoods.toString(): "")+ ", " + (realizedAvailabilities != null ? realizedAvailabilities.toString():"")+ ")");
		double res =  _jpmf.getMarginalProbability( atom.getInterestingSet(), allocatedGoods, realizedAvailabilities);
		_logger.debug("<- computeExpectedMarginalAvailability() = " + res);
		return res;
	}

	private JointProbabilityMass _jpmf;				//Joint probability mass function
	private int _numberOfBuyers;					//Number of buyers participating in the auction
	private int _numberOfItems;						//Number of items to be sold
	private String _paymentRule;					//Payment rule to be used
	private List<Double> _costs;					//Sellers' costs
	private List<Type> _bids;						//Bids submitted by buyers
	private List<int[][]> _binaryBids;				//Bids converted into a binary matrix format
	private List<Double> _payments;					//A list of payments of allocated bidders
	private AllocationEC _allocation;				//The data structure contains the resulting allocation of the auction
	private Random _generator;						//A random number generator used to resolve the uncertainty
	private long _randomSeed;						//A seed used to setup the random numbers generator

	private IloCplex _cplexSolver;
	
	static int constraintID = 0;					//Constraints counter
}
