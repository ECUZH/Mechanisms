package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public abstract class AllocationRuleProbabilistic implements IAllocationRule
{

	private static final Logger _logger = LogManager.getLogger(AllocationRuleProbabilistic.class);

	/**
	 * Constructor.
	 * @param bids submitted bids of bidders
	 * @param costs per-good costs
	 * @param jpmf joint probability mass function
	 */
	AllocationRuleProbabilistic(List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{
		_bids = bids;
		_costs = costs;
		_jpmf = jpmf;
	}
	
	@Override
	public abstract void computeAllocation(List<Integer> allocatedGoods,
			List<Double> realizedAvailabilities) throws IloException;

	@Override
	abstract public Allocation getAllocation();

	@Override
	abstract public void setSolver(IloCplex solver);

	/**
	 * The method computes the expected availability of a bundle by a buyer given the exogenous joint probability density function.
	 * @param atom - an atomic bid for the bundle
	 * @param allocatedGoods
	 * @param realizedAvailabilities
	 * @return the expected availability of the bundle
	 */
	protected double computeExpectedMarginalAvailability(AtomicBid atom, List<Integer> allocatedGoods, List<Double> realizedAvailabilities)
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
	protected double getRealizedAvailability(List<Integer> bundle, double[] sample)
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
	protected double addAllocatedAgent(List<Integer> allocatedBidders,  List<Integer> allocatedBundles, List<Double> buyersExpectedValues, 
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
	
	protected List<Type> _bids;						//Types of bidders
	protected List<Double> _costs;					//Per-good costs
	protected JointProbabilityMass _jpmf;			//Joint probability mass function
}
