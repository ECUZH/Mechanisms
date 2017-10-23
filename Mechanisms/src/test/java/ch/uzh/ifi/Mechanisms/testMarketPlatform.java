package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.IParametrizedValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.LinearThresholdValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;

public class testMarketPlatform {

	/**
	 * There are 2 sellers and 2 buyers in this scenario (see AAMAS'17 paper).
	 * Different sellers produce different DBs.
	 * @throws Exception 
	 */
	@Test
	public void testMarketDemandWithLargeEndowments() throws Exception 
	{
		//0. Define DBs
		int dbID1 = 0;
		int dbID2 = 1;
		
		//1. Create 2 sellers
		List<Integer> bundle1 = Arrays.asList(dbID1);
		List<Integer> bundle2 = Arrays.asList(dbID2);
		
		double cost1 = 10;
		double cost2 = 20;
		
		AtomicBid seller1 = new AtomicBid(1, bundle1, cost1);
		AtomicBid seller2 = new AtomicBid(2, bundle2, cost2);
		
		List<AtomicBid> sellers = Arrays.asList(seller1, seller2);
		
		
		//2. Create 2 buyers
		double endowment = 10;
		int allocations[] = {0b00, 0b01, 0b10, 0b11};									// 4 possible deterministic allocations
		
		double[] alloc1 = {0,0};
		IParametrizedValueFunction v11 = new LinearThresholdValueFunction(0, 0, alloc1);
		IParametrizedValueFunction v21 = new LinearThresholdValueFunction(0, 0, alloc1);
		
		double[] alloc2 = {0,1};
		IParametrizedValueFunction v12 = new LinearThresholdValueFunction(4, 1, alloc2);
		IParametrizedValueFunction v22 = new LinearThresholdValueFunction(1, 2, alloc2);
		
		double[] alloc3 = {1,0};
		IParametrizedValueFunction v13 = new LinearThresholdValueFunction(4, 1, alloc3);
		IParametrizedValueFunction v23 = new LinearThresholdValueFunction(1, 2, alloc3);
		
		double[] alloc4 = {1,1};
		IParametrizedValueFunction v14 = new LinearThresholdValueFunction(6, 1, alloc4);
		IParametrizedValueFunction v24 = new LinearThresholdValueFunction(1, 4, alloc4);
		
		IParametrizedValueFunction[] valueFunctions1 = {v11, v12, v13, v14};			//Parameterized value functions for both buyers 
		IParametrizedValueFunction[] valueFunctions2 = {v21, v22, v23, v24};
		ParametrizedQuasiLinearAgent buyer1 = new ParametrizedQuasiLinearAgent(1, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer2 = new ParametrizedQuasiLinearAgent(2, endowment, allocations, valueFunctions2);
		
		List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
		buyers.add(buyer1);
		buyers.add(buyer2);
		
		//3. Create market platform and evaluate the market demand
		MarketPlatform mp = new MarketPlatform(buyers, sellers);
		
		int auctioneerId = 0; 															//The market platform, M
		
		List<Integer> bidders = new LinkedList<Integer>();
		bidders.add(seller1.getAgentId());
		bidders.add(seller2.getAgentId());
		
		List<Integer> bundles = new LinkedList<Integer>();
		bundles.add(dbID1);																//Id of the bundle allocated to the 1st bidder
		bundles.add(dbID2);																//Id of the bundle allocated to the 2nd bidder
		
		double auctioneerValue = 0;
		List<Double> biddersValues = new LinkedList<Double>();
		biddersValues.add(seller1.getValue());
		biddersValues.add(seller2.getValue());
		
		ProbabilisticAllocation allocation = new ProbabilisticAllocation();				//Probabilistic allocation of sellers
		List<Double> allocationProbabilities = new LinkedList<Double>();
		allocationProbabilities.add(0.);
		allocationProbabilities.add(1.0);
		
		allocation.addAllocatedAgent(auctioneerId, bidders, bundles, auctioneerValue, biddersValues, allocationProbabilities);
		
		List<Double> marketDemand = mp.computeMarketDemand(0., allocation);
		
		assertTrue(Math.abs( marketDemand.get(1) - 3. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 20. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(1. - 1e-8, allocation);
		assertTrue(Math.abs( marketDemand.get(1) - 3. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 17. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(1. + 1e-8, allocation);
		assertTrue(Math.abs( marketDemand.get(1) - 1. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 19. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(4. - 1e-8, allocation);
		assertTrue(Math.abs( marketDemand.get(1) - 1. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 16. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(4. + 1e-8, allocation);
		assertTrue(Math.abs( marketDemand.get(1) - 0. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 20. ) < 1e-6);
		
		//4. Test aggregate value function
		
		assertTrue(Math.abs( mp.computeAggregateValue(123, allocation)-6 ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(3+ 1e-8, allocation)-6 ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(3- 0.1, allocation)-(5.9) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(1+ 0.1, allocation)-(4.1) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(1- 0.1, allocation)-(3.6) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(0.5, allocation)-(2) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(0, allocation)-0 ) < 1e-6);
		
		//5. Test values of DBs
		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 0) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 6) < 1e-6);
		
		allocation.deallocateBundle(dbID1);
		allocation.deallocateBundle(dbID2);
		
		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 0) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 0) < 1e-6);
		
		allocationProbabilities = Arrays.asList(1., 1.);
		allocation.resetAllocationProbabilities(allocationProbabilities);
		
		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 4) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 4) < 1e-6);
	}

}
