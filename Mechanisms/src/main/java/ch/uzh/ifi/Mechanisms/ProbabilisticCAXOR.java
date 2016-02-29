package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
	
	/*
	 * Constructor
	 * @param numberOfBuyers - the number of buyers in the auction
	 * @param numberOfItems - the number of items to be auctioned
	 * @param bids - bids of agents. A bid consists of atomic bids. An Atomic Bid must include a bundle, i.e., a list
	 *               of items an agent is willing to buy. Each item must be encoded with an integer starting from 1, 2, ... m.
	 * @param jpmf - joint probability mass function for availabilities of individual goods
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
	
	/*
	 * 
	 */
	public void setSolver(IloCplex solver)
	{
		_cplexSolver = solver;
	}
	
	/*
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
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#solveIt()
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

	
	/*
	 * The method solved the WDP for the LLG domain.
	 */
	/*public void computeWinnerDeterminationStrawMan(List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		double[] realizedSample = new double[_numberOfItems];
		for(int i = 0; i < _jpmf.getBombs().size(); ++i)
		{
			for(int j = 0; j < _numberOfItems; ++j)
			{
				if(j == 0)
				{
					realizedSample[0] = 1. - _jpmf.getBombs().get(i).getPrimaryReductionCoefficient();
					realizedSample[1] = 1. - _jpmf.getBombs().get(i).getSecondaryReductionCoefficient();
				}
				else
				{
					realizedSample[0] = 1. - _jpmf.getBombs().get(i).getSecondaryReductionCoefficient();
					realizedSample[1] = 1. - _jpmf.getBombs().get(i).getPrimaryReductionCoefficient();
				}
				
				_allocation = new Allocation();
				List<Integer> allocatedBidders     = new LinkedList<Integer>();
				List<Integer> allocatedBundles     = new LinkedList<Integer>();
				List<Double> buyersExpectedValues  = new LinkedList<Double>();
				List<Double> allocatedBiddersValues= new LinkedList<Double>();
				List<Double> realizedRandomVars    = new LinkedList<Double>();
				List<Double> realizedRVsPerGood    = new LinkedList<Double >();
				double sellerExpectedCost = 0.;
				
				if( _bids.size() == 3 )
				{
					AtomicBid localBundle1 = _bids.get(0).getAtom(0);
					AtomicBid localBundle2 = _bids.get(1).getAtom(0);
					AtomicBid globalBundle = _bids.get(2).getAtom(0);
		
					double values[] = {localBundle1.getValue(), localBundle2.getValue(), globalBundle.getValue()};
					double costs[]  = {computeCost(localBundle1), computeCost(localBundle2), computeCost(globalBundle)};
					double expectedMarginalAvailabilities[] = {computeExpectedMarginalAvailability( localBundle1, allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( localBundle2, allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( globalBundle, allocatedGoods, realizedAvailabilities )};
		
					double swLocal1 = (values[0] - costs[0]) * expectedMarginalAvailabilities[0];
					double swLocal2 = (values[1] - costs[1]) * expectedMarginalAvailabilities[1];
					double swGlobal = (values[2] - costs[2]) * expectedMarginalAvailabilities[2];
					//_logger.debug("3 bidders. " + "sw1 = " + swLocal1 + " sw2 = " + swLocal2 + " sw3 = " + swGlobal);
		
					if( swLocal1 >= 0 && swLocal2 >= 0 && swLocal1 + swLocal2 >= swGlobal )	//Allocate to local bidders
					{
						//_logger.debug("Allocate to local bidders.");
						//double[] realizedSample = _jpmf.getSample();
						for(Double rRV : realizedSample)
							realizedRVsPerGood.add(rRV);
		
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars, 
								 allocatedBiddersValues, realizedSample, localBundle1, values[0], costs[0], 
								expectedMarginalAvailabilities[0], 0);
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
								allocatedBiddersValues, realizedSample, localBundle2, values[1], costs[1], expectedMarginalAvailabilities[1], 0);
					}
					else if( swLocal1 >= 0 && swLocal2 < 0 && swLocal1 >= swGlobal )
					{
						//_logger.debug("Allocate to a single local bidder.");
						//double[] realizedSample = _jpmf.getSample();
						for(Double rRV : realizedSample)
							realizedRVsPerGood.add(rRV);
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars, 
															    allocatedBiddersValues, realizedSample, localBundle1, values[0], 
																costs[0], expectedMarginalAvailabilities[0], 0);
					}
					else if( swLocal2 >= 0 && swLocal1 < 0 && swLocal2 >= swGlobal )
					{
						//_logger.debug("Allocate to a single local bidder.");
						//double[] realizedSample = _jpmf.getSample();
						for(Double rRV : realizedSample)
							realizedRVsPerGood.add(rRV);
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
																allocatedBiddersValues, realizedSample, localBundle2, values[1], 
																costs[1], expectedMarginalAvailabilities[1], 0);
					}
					else if( swGlobal >= 0 )
					{
						//_logger.debug("Allocate to a global bidder.");
						//double[] realizedSample = _jpmf.getSample();
						for(Double rRV : realizedSample)
							realizedRVsPerGood.add(rRV);
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
																allocatedBiddersValues, realizedSample, globalBundle, values[2], 
																costs[2], expectedMarginalAvailabilities[2], 0);
					}
				}
						
				if(allocatedBundles.size() > 0)
					try 
					{
						_allocation.addAllocatedAgents( 0, allocatedBidders, allocatedBundles, sellerExpectedCost, buyersExpectedValues, false);
						_allocation.addRealizedValues(realizedRandomVars);
						_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
						_allocation.setAllocatedBiddersValues(allocatedBiddersValues);
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				
				if(allocatedBundles.size() > 0)
				try 
				{
					List<Double> payments = computePayments(new ECRCoreLLGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf));
					
					for(int k = 0; k < _allocation.getBiddersInvolved(0).size(); ++k)
					{
						int allocatedBidderId = _allocation.getBiddersInvolved(0).get(k);
						int allocatedAtomIdx = _allocation.getAllocatedBundlesByIndex(0).get(k);
						AtomicBid atom = _bids.get(allocatedBidderId-1).getAtom(allocatedAtomIdx);
						double value = atom.getValue();
						double realizedAvailability = _allocation.getRealizedRV(0, k);
						double realizedValue = value * realizedAvailability;
						
						if( realizedValue < payments.get(k))
						{
							_allocation = new Allocation();
							return;
						}
					}
				} 
				catch (PaymentException e) 
				{
					if(e.getMessage().equals("Empty Core"))
					{
						_allocation = new Allocation();
						return;
					}
				}
				catch (Exception e)
				{
					System.err.println(e.toString());
				}
			}
		}
		
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0)
		{
			realizedSample = _jpmf.getSample();
			List<Double> realizedRandomVars    = new LinkedList<Double>();
			List<Double> realizedRVsPerGood    = new LinkedList<Double >();
			for(Double rRV : realizedSample)
				realizedRVsPerGood.add(rRV);
			
			for(int k = 0; k < _allocation.getBiddersInvolved(0).size(); ++k)
			{
				int allocatedBidderId = _allocation.getBiddersInvolved(0).get(k);
				int allocatedAtomIdx = _allocation.getAllocatedBundlesByIndex(0).get(k);
				AtomicBid atom = _bids.get(allocatedBidderId-1).getAtom(allocatedAtomIdx);
				double value = atom.getValue();
				
				realizedRandomVars.add(getRealizedAvailability( atom.getInterestingSet(), realizedSample));
			}
			
			List<Integer> allocatedBidders  = _allocation.getBiddersInvolved(0);
			List<Integer> allocatedBundles     = _allocation.getAllocatedBundlesByIndex(0);
			List<Double> buyersExpectedValues  = _allocation.getBiddersExpectedValues(0);
			List<Double> allocatedBiddersValues= _allocation.getBiddersAllocatedValues(0);
	
			double sellerExpectedCost =_allocation.getAuctioneerExpectedValue(0);
			
			_allocation.addRealizedValues(realizedRandomVars);
			_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
			
			_allocation = new Allocation();
			try {
				_allocation.addAllocatedAgents( 0, allocatedBidders, allocatedBundles, sellerExpectedCost, buyersExpectedValues, false);
				_allocation.addRealizedValues(realizedRandomVars);
				_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
				_allocation.setAllocatedBiddersValues(allocatedBiddersValues);
	
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
	}*/

	
	
	/*
	 * The method solved the WDP for the LLG domain.
	 */
	public void computeWinnerDeterminationLLG(List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeWinnerDeterminationLLG(allocatedGoods="+(allocatedGoods != null ? allocatedGoods.toString():"")+", " +( realizedAvailabilities!= null ? realizedAvailabilities.toString():"") +")");
		_allocation = new AllocationEC();
		List<Integer> allocatedBidders     = new LinkedList<Integer>();
		List<Integer> allocatedBundles     = new LinkedList<Integer>();
		List<Double> buyersExpectedValues  = new LinkedList<Double>();
		List<Double> allocatedBiddersValues= new LinkedList<Double>();
		List<Double> realizedRandomVars    = new LinkedList<Double>();
		List<Double> realizedRVsPerGood    = new LinkedList<Double >();
		double sellerExpectedCost = 0.;
		
		if( _bids.size() == 3 )
		{
			AtomicBid localBundle1 = _bids.get(0).getAtom(0);
			AtomicBid localBundle2 = _bids.get(1).getAtom(0);
			AtomicBid globalBundle = _bids.get(2).getAtom(0);

			double values[] = {localBundle1.getValue(), localBundle2.getValue(), globalBundle.getValue()};
			double costs[]  = {localBundle1.computeCost(_costs), localBundle2.computeCost(_costs), globalBundle.computeCost(_costs)};
			double expectedMarginalAvailabilities[] = {computeExpectedMarginalAvailability( localBundle1, allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( localBundle2, allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( globalBundle, allocatedGoods, realizedAvailabilities )};

			double swLocal1 = (values[0] - costs[0]) * expectedMarginalAvailabilities[0];
			double swLocal2 = (values[1] - costs[1]) * expectedMarginalAvailabilities[1];
			double swGlobal = (values[2] - costs[2]) * expectedMarginalAvailabilities[2];
			_logger.debug("3 bidders. " + "sw(L1) = " + swLocal1 + " sw(L2) = " + swLocal2 + " sw(G) = " + swGlobal);

			if( swLocal1 >= 0 && swLocal2 >= 0 && swLocal1 + swLocal2 >= swGlobal )	//Allocate to local bidders
			{
				_logger.debug("Allocate to local bidders.");
				double[] realizedSample = _jpmf.getSample();
				for(Double rRV : realizedSample)
					realizedRVsPerGood.add(rRV);

				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars, 
						 allocatedBiddersValues, realizedSample, localBundle1, values[0], costs[0], 
						expectedMarginalAvailabilities[0], 0);
				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
						allocatedBiddersValues, realizedSample, localBundle2, values[1], costs[1], expectedMarginalAvailabilities[1], 0);
			}
			else if( swLocal1 >= 0 && swLocal2 < 0 && swLocal1 >= swGlobal )
			{
				_logger.debug("Allocate to a single local bidder.");
				double[] realizedSample = _jpmf.getSample();
				for(Double rRV : realizedSample)
					realizedRVsPerGood.add(rRV);
				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars, 
													    allocatedBiddersValues, realizedSample, localBundle1, values[0], 
														costs[0], expectedMarginalAvailabilities[0], 0);
			}
			else if( swLocal2 >= 0 && swLocal1 < 0 && swLocal2 >= swGlobal )
			{
				_logger.debug("Allocate to a single local bidder.");
				double[] realizedSample = _jpmf.getSample();
				for(Double rRV : realizedSample)
					realizedRVsPerGood.add(rRV);
				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
														allocatedBiddersValues, realizedSample, localBundle2, values[1], 
														costs[1], expectedMarginalAvailabilities[1], 0);
			}
			else if( swGlobal >= 0 )
			{
				_logger.debug("Allocate to a global bidder.");
				double[] realizedSample = _jpmf.getSample();
				for(Double rRV : realizedSample)
					realizedRVsPerGood.add(rRV);
				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
														allocatedBiddersValues, realizedSample, globalBundle, values[2], 
														costs[2], expectedMarginalAvailabilities[2], 0);
			}
		}
		else if (_bids.size() == 2)									//Reduced LLG when computing, for example, VCG
		{
			_logger.debug("2 bidders.");
			if( _bids.get(1).getAgentId() == 3)						//If one of bidders is a global bidder
			{
				AtomicBid globalBundle = _bids.get(1).getAtom(0);
				AtomicBid localBundle  = _bids.get(0).getAtom(0);
				
				double values[] = {localBundle.getValue(), globalBundle.getValue()};
				double costs[] = {localBundle.computeCost(_costs), globalBundle.computeCost(_costs)};
				double expectedMarginalAvailabilities[] = {computeExpectedMarginalAvailability( localBundle, allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( globalBundle, allocatedGoods, realizedAvailabilities )};
				
				double sw1 = (values[0] - costs[0]) * expectedMarginalAvailabilities[0];
				double sw2 = (values[1] - costs[1]) * expectedMarginalAvailabilities[1];
				_logger.debug("2 bidders (local+global). " + "sw(L1) = " + sw1 + " sw(G) = " + sw2);

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
				{
					double[] realizedSample = _jpmf.getSample();
					for(Double rRV : realizedSample)
						realizedRVsPerGood.add(rRV);
					sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
							allocatedBiddersValues, realizedSample, allocatedBundle, values[allocatedIdx], 
							costs[allocatedIdx], expectedMarginalAvailabilities[allocatedIdx], 0);
				}
			}
			else											//Only local bidders
			{
				AtomicBid[] bundles = { _bids.get(0).getAtom(0), _bids.get(1).getAtom(0) };
				double values[] = {bundles[0].getValue(), bundles[1].getValue()};
				double costs[]  = {bundles[0].computeCost(_costs), bundles[1].computeCost(_costs)};
				double expectedMarginalAvailabilities[] = {computeExpectedMarginalAvailability( bundles[0], allocatedGoods, realizedAvailabilities ), computeExpectedMarginalAvailability( bundles[1], allocatedGoods, realizedAvailabilities )};
				double[] sw = {(values[0] - costs[0]) * expectedMarginalAvailabilities[0], 
				               (values[1] - costs[1]) * expectedMarginalAvailabilities[1]};
				
				for(int i = 0; i < 2; ++i)
					if( sw[i] >= 0 )
					{
						double[] realizedSample = _jpmf.getSample();
						for(Double rRV : realizedSample)
							realizedRVsPerGood.add(rRV);
						sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
								allocatedBiddersValues, realizedSample, bundles[i], values[i], costs[i], 
								expectedMarginalAvailabilities[i], 0);
					}
			}
		}
		else
		{
			AtomicBid bundle = _bids.get(0).getAtom(0);
			double value = bundle.getValue();
			double cost = bundle.computeCost(_costs);
			double expectedMarginalAvailability = computeExpectedMarginalAvailability( bundle, allocatedGoods, realizedAvailabilities );
			double sw = (value - cost) * expectedMarginalAvailability;
			
			if(sw >= 0)
			{
				double[] realizedSample = _jpmf.getSample();
				for(Double rRV : realizedSample)
					realizedRVsPerGood.add(rRV);
				sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
										allocatedBiddersValues, realizedSample, bundle, value, cost, expectedMarginalAvailability, 0);
			}
		}
		
		if(allocatedBundles.size() > 0)
			try 
			{
				_allocation.addAllocatedAgents( 0, allocatedBidders, allocatedBundles, sellerExpectedCost, buyersExpectedValues, false);
				_allocation.addRealizedRVs(realizedRandomVars);
				_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
				_allocation.setAllocatedBiddersValues(allocatedBiddersValues);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		_logger.debug("<- computeWinnerDeterminationLLG(...)");
	}
	
	/*
	 * The method solves WDP for a general (non-LLG) setting
	 */
	public void computeWinnerDeterminationGeneral(List<Integer> allocatedGoods, List<Double> realizedAvailabilities) throws IloException 
	{
		_logger.debug("-> computeWinnerDeterminationGeneral(allocatedGoods="+ (allocatedGoods!=null?allocatedGoods.toString():"") + ", realizedAvailabilities="+ (realizedAvailabilities!=null?realizedAvailabilities.toString():"") + ")");
		_cplexSolver.clearModel();
		_cplexSolver.setOut(null);
		List<List<IloNumVar> > variables = new LinkedList<List<IloNumVar> >();// i-th element of the list contains the list of variables 
																			// corresponding to the i-th agent
		//Create the optimization variables and set the objective function:
		IloNumExpr objective = _cplexSolver.constant(0.);
		IloLPMatrix lp = _cplexSolver.addLPMatrix();
				
		for(int i = 0; i < _numberOfBuyers; ++i)
		{
			Type bid = _bids.get(i);
			List<IloNumVar> varI = new LinkedList<IloNumVar>();				//Create a new variable for every atomic bid
			for(int j = 0; j < bid.getNumberOfAtoms(); ++j )
			{
				IloNumVar x = _cplexSolver.numVar(0, 1, IloNumVarType.Int, "x" + i + "_" + j);
				varI.add(x);
				
				double value = bid.getAtom(j).getValue();
				double cost  = bid.getAtom(j).computeCost(_costs);
				double expectedMarginalAvailability = computeExpectedMarginalAvailability( bid.getAtom(j), allocatedGoods, realizedAvailabilities );
				
				IloNumExpr term = _cplexSolver.prod((value - cost) * expectedMarginalAvailability, x);
				objective = _cplexSolver.sum(objective, term);				
			}
			variables.add(varI);
		}
		
		_cplexSolver.add(_cplexSolver.maximize(objective));
		
		//Create optimization constraints for ITEMS:
		for(int i = 0; i < _numberOfItems; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			
			for(int j = 0; j < _numberOfBuyers; ++j)
			{
				int[][] binaryBid = _binaryBids.get(j);						//Binary bid of agent j
				List<IloNumVar> varI = (List<IloNumVar>)(variables.get(j));

				for( int q = 0; q < varI.size(); ++q )			//Find an atom which contains the i-th item ( itemId = i+1)
					if( binaryBid[q][i] > 0)
					{
						IloNumExpr term = _cplexSolver.prod(binaryBid[q][i], varI.get(q));
						constraint = _cplexSolver.sum(constraint, term);
					}
			}
			lp.addRow( _cplexSolver.ge(1.0, constraint, "Item_"+i) );
		}
		
		//Create optimization constraints for XOR:
		for(int i = 0; i < _numberOfBuyers; ++i)
		{
			IloNumExpr constraint = _cplexSolver.constant(0);
			double upperBound = 1.;
			
			List<IloNumVar> varI = variables.get(i);
			for(int q = 0; q < varI.size(); ++q)
				constraint = _cplexSolver.sum(constraint, varI.get(q));
			
			lp.addRow( _cplexSolver.ge(upperBound, constraint, "Bidder"+i));
		}
		
		//Launch CPLEX to solve the problem:
		_cplexSolver.setParam(IloCplex.Param.RootAlgorithm, 2);
		try 
		{
			long t1 = System.currentTimeMillis();
			_cplexSolver.solve();
			long t2 = System.currentTimeMillis();
			//System.out.println(t2-t1);
		} 
		catch (IloException e1) 
		{
			_logger.error("MIP: " + _cplexSolver.toString());
			_logger.error("Bids: " + _bids.toString());
			e1.printStackTrace();
		}

		_allocation = new AllocationEC();
		
		List<Integer> allocatedBidders    = new LinkedList<Integer>();
		List<Integer> allocatedBundles    = new LinkedList<Integer>();
		List<Double> buyersExpectedValues = new LinkedList<Double>();
		List<Double> realizedRandomVars   = new LinkedList<Double>();
		List<Double> allocatedBiddersValues = new LinkedList<Double>();
		List<Double> realizedRVsPerGood = new LinkedList<Double>();
		
		double sellerExpectedCost = 0.;
		double[] realizedSample = _jpmf.getSample();
		for(Double rRV : realizedSample)
			realizedRVsPerGood.add(rRV);
		
		for(int i = 0; i < _numberOfBuyers; ++i)
		{
			for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j)
			{
				if( Math.abs( _cplexSolver.getValue(variables.get(i).get(j)) - 1.0 ) < 1e-6 )			//if allocated
				{
					AtomicBid allocatedAtom = _bids.get(i).getAtom(j);
					double value = allocatedAtom.getValue();
					double cost = allocatedAtom.computeCost(_costs);
					double expectedMarginalAvailability = computeExpectedMarginalAvailability( allocatedAtom, allocatedGoods, realizedAvailabilities );
					sellerExpectedCost += addAllocatedAgent(allocatedBidders, allocatedBundles, buyersExpectedValues, realizedRandomVars,
										allocatedBiddersValues, realizedSample, allocatedAtom, value, cost, expectedMarginalAvailability, j);
					break;
				}
			}
		}
		
		if(allocatedBidders.size() > 0)
			try
			{
				_allocation.addAllocatedAgents( 0, allocatedBidders, allocatedBundles, sellerExpectedCost, buyersExpectedValues, false);
				_allocation.addRealizedRVs(realizedRandomVars);
				_allocation.addRealizedValuesPerGood(realizedRVsPerGood);
				_allocation.setAllocatedBiddersValues(allocatedBiddersValues);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		_logger.debug("<- computeWinnerDeterminationGeneral(...)");
	}
	
	/*
	 * The method computes and returns payments for the market.
	 * @param pr - payment rule to be used
	 * @return a vector of prices for bidders
	 */
	public List<Double> computePayments(PaymentRule pr) throws Exception
	{
		PaymentRule paymentRule = pr;
		try
		{
			if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
				_payments = paymentRule.computePayments();
			else
				_payments = new LinkedList<Double>();
		}
		catch(PaymentException e)
		{
			if( e.getMessage().equals("Empty Core") || e.getMessage().equals("VCG is in the Core") )
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
			else							_payments = new LinkedList<Double>();
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
		_payments = new LinkedList<Double>();
		
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

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPaymentRule()
	 */
	@Override
	public String getPaymentRule() 
	{
		return _paymentRule;
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
		return false;
	}
	
	/*
	 * The method sets the random seed.
	 * @param seed - a random seed to be used
	 */
	public void setSeed(long seed)
	{
		_randomSeed = seed;
		_generator.setSeed(_randomSeed);
	}
	
	/*
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
	
	/*
	 * The method converts bids of all agents into a binary matrix form.
	 */
	private void convertAllBidsToBinaryFormat()
	{
		_binaryBids = new LinkedList<int[][]>();						//The list contains bids of all agents in a binary format
		for(int i = 0; i < _bids.size(); ++i)
		{
			Type bid = _bids.get(i);									//The bid of the i-th bidder
			int[][] binaryBid = new int[bid.getNumberOfAtoms()][_numberOfItems];
	
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
	 * @param numberOfColumns - the number of columns of the binary bid
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
				binaryBid[ i ][ (int)(items.get(j))-1 ]  = 1;
		}
	}
	
	/*
	 * The method computes the expected availability of a bundle by a buyer given the exogenous joint probability density function.
	 * @param atom - an atomic bid for the bundle
	 * @return the expected availability of the bundle
	 */
	public double computeExpectedMarginalAvailability(AtomicBid atom, List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeExpectedMarginalAvailability(atom: "+atom.toString()+ ", " + (allocatedGoods != null ? allocatedGoods.toString(): "")+ ", " + (realizedAvailabilities != null ? realizedAvailabilities.toString():"")+ ")");
		double res =  _jpmf.getMarginalProbability( atom.getInterestingSet(), allocatedGoods, realizedAvailabilities);
		_logger.debug("<- computeExpectedMarginalAvailability() = " + res);
		return res;
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
				realizedRV = sample[ itemIdx ] ;/// 2.; //TODO: !!! divided by 2
		}
		return realizedRV;
	}
	
	/*
	 * The method used by WDP for LLG domain to fill some data structures required by the Allocation object.
	 * @param allocatedBidders - a list of allocated bidders (is filled by this method)
	 * @param allocatedBundles - a list of indexes of allocated bundles for every allocated bidder
	 * @param buyersExpectedValues - an empty list for expected values of allocated buyers
	 * @param realizedRandomVars - an empty list for realizations of random variables (availabilities)
	 * @param allocatedBiddersValues - an empty list for realized values of bidders for their allocated bundles
	 * @param realizedSample - a sample drawn from the joint probability mass function
	 * @param atom  - allocated bundle
	 * @param value - marginal value of the buyer for the bundle
	 * @param cost -  marginal cost of the seller for the bundle
	 * @param expectedMarginalAvailability - expected marginal availability of the bundle
	 * @param allocatedBundleIdx - an index of the bundle allocated for the bidder (within his combinatorial bid)
	 * @return an expected cost of the seller for the bundle
	 */
	private double addAllocatedAgent(List<Integer> allocatedBidders,  List<Integer> allocatedBundles, List<Double> buyersExpectedValues, 
            List<Double> realizedRandomVars, List<Double> allocatedBiddersValues, double[] realizedSample, 
            AtomicBid atom, double value, double cost, double expectedMarginalAvailability, int allocatedBundleIdx)
	{
		allocatedBundles.add(allocatedBundleIdx);
		allocatedBidders.add( atom.getAgentId() );
		double sellerExpectedCost = cost * expectedMarginalAvailability;
		buyersExpectedValues.add( value * expectedMarginalAvailability );

		//Resolve uncertainty
		double realizationRV1 = getRealizedAvailability( atom.getInterestingSet(), realizedSample);
		realizedRandomVars.add(realizationRV1);
		allocatedBiddersValues.add( realizationRV1 * value );
		return sellerExpectedCost;
	}

	private JointProbabilityMass _jpmf;				//Joint probability mass function
	private int _numberOfBuyers;					//Number of buyers participating in the auction
	private int _numberOfItems;						//Number of items to be sold
	private String _paymentRule;					//Payment rule to be used
	private List<Double> _costs;					//Sellers' costs
	private List<Type> _bids;						//Bids submitted by buyers
	private List<int[][]> _binaryBids;				//Bids converted into a binary matrix format
	private List<Double> _payments;					//A list of payments of allocated bidders
	private AllocationEC _allocation;					//The data structure contains the resulting allocation of the auction
	private Random _generator;						//A random number generator used to resolve the uncertainty
	private long _randomSeed;						//A seed used to setup the random numbers generator

	private IloCplex _cplexSolver;
	
	static int constraintID = 0;					//Constraints counter
}
