package ch.uzh.ifi.Mechanisms;

import java.util.List;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;

/**
 * An interface for different auction allocation rules. 
 * @author Dmitry Moor
 */
public interface IAllocationRule 
{
	
	/**
	 * The method computes an allocation of an auction.
	 * @param allocatedGoods goods that were already allocated (used to compute an alternative allocation given the current one)
	 * @param realizedAvailabilities realized availabilities of the goods which were already allocated
	 */
	public void computeAllocation(List<Integer> allocatedGoods, List<Double> realizedAvailabilities);
	
	/**
	 * The method returns an allocation of an auction.
	 * @return an allocation object
	 */
	public Allocation getAllocation();
}
