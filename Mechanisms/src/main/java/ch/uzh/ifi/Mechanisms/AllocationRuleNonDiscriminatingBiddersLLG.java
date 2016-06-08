package ch.uzh.ifi.Mechanisms;

import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class AllocationRuleNonDiscriminatingBiddersLLG extends AllocationRuleProbabilistic
{

	private static final Logger _logger = LogManager.getLogger(AllocationRuleNonDiscriminatingBiddersLLG.class);
	
	/**
	 * Constructor.
	 * @param bids submitted bids of bidders
	 * @param costs per-good costs
	 * @param jpmf joint probability mass function
	 */
	AllocationRuleNonDiscriminatingBiddersLLG(List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{
		super(bids, costs, jpmf);
	}
	
	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.IAllocationRule#computeAllocation(java.util.List, java.util.List)
	 */
	@Override
	public void computeAllocation(List<Integer> allocatedGoods, List<Double> realizedAvailabilities) 
	{
		_logger.debug("-> computeAllocation(allocatedGoods="+(allocatedGoods != null ? allocatedGoods.toString():"")+", " +( realizedAvailabilities!= null ? realizedAvailabilities.toString():"") +")");
		_allocation = new AllocationEC();
		List<Integer> allocatedBidders     = new ArrayList<Integer>();
		List<Integer> allocatedBundles     = new ArrayList<Integer>();
		List<Double> buyersExpectedValues  = new ArrayList<Double>();
		List<Double> allocatedBiddersValues= new ArrayList<Double>();
		List<Double> realizedRandomVars    = new ArrayList<Double>();
		List<Double> realizedRVsPerGood    = new ArrayList<Double >();
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
		_logger.debug("<- computeAllocation(...)");
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.IAllocationRule#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	/**
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.IAllocationRule#setSolver(ilog.cplex.IloCplex)
	 */
	@Override
	public void setSolver(IloCplex solver) 
	{
		_logger.error("No CPLEX solver required for solving LLG domain.");	
	}
	
	private AllocationEC _allocation;				//An allocation object
}
