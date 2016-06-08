package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class AllocationNonDiscriminatingBiddersLLG implements IAllocationRule
{

	private static final Logger _logger = LogManager.getLogger(AllocationNonDiscriminatingBiddersLLG.class);
	
	/**
	 * Constructor.
	 * @param bids submitted bids of bidders
	 * @param costs per-good costs
	 * @param jpmf joint probability mass function
	 */
	AllocationNonDiscriminatingBiddersLLG(List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{
		_bids = bids;
		_costs = costs;
		_jpmf = jpmf;
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
	 * The method computes the expected availability of a bundle by a buyer given the exogenous joint probability density function.
	 * @param atom - an atomic bid for the bundle
	 * @param allocatedGoods
	 * @param realizedAvailabilities
	 * @return the expected availability of the bundle
	 */
	private double computeExpectedMarginalAvailability(AtomicBid atom, List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
	{
		_logger.debug("-> computeExpectedMarginalAvailability(atom: "+atom.toString()+ ", " + (allocatedGoods != null ? allocatedGoods.toString(): "")+ ", " + (realizedAvailabilities != null ? realizedAvailabilities.toString():"")+ ")");
		double res =  _jpmf.getMarginalProbability( atom.getInterestingSet(), allocatedGoods, realizedAvailabilities);
		_logger.debug("<- computeExpectedMarginalAvailability() = " + res);
		return res;
	}
	
	/**
	 * The method computes availability of the specified bundle given realizations of random variables. This availability
	 * is computed as minimal availability among all individual goods within the bundle. 
	 * @param bundle - the bundle for which availability should be computed
	 * @param sample - one random sample of realizations of all random variables.
	 * @return realized availability of the bundle
	 */
	private double getRealizedAvailability(List<Integer> bundle, double[] sample)
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
	
	/**
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
	
	private List<Type> _bids;						//Types of bidders
	private List<Double> _costs;					//Per-good costs
	private JointProbabilityMass _jpmf;				//Joint probability mass function
	private AllocationEC _allocation;				//An allocation object
}
