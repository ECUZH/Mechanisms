package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.IParametrizedValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.LinearThresholdValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class testMarketPlatform {

	/**
	 * There are 2 sellers and 2 buyers in this scenario (see AAMAS'18 paper).
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
		
		AtomicBid sellerBid1 = new AtomicBid(1, bundle1, cost1);
		AtomicBid sellerBid2 = new AtomicBid(2, bundle2, cost2);
		
		SellerType seller1 = new SellerType(sellerBid1, Distribution.UNIFORM, 1., 1./3.);
		SellerType seller2 = new SellerType(sellerBid2, Distribution.UNIFORM, 1., 1./3.);
		
		List<SellerType> sellers = Arrays.asList(seller1, seller2);
		
		
		//2. Create 2 buyers
		double endowment = 10;
		int allocations[] = {0b00, 0b01, 0b10, 0b11};									// 4 possible deterministic allocations
		
		List<Double> alloc1 = Arrays.asList(0., 0.);
		LinearThresholdValueFunction v11 = new LinearThresholdValueFunction(0, 0, alloc1);
		LinearThresholdValueFunction v21 = new LinearThresholdValueFunction(0, 0, alloc1);
		
		List<Double> alloc2 = Arrays.asList(0., 1.);
		LinearThresholdValueFunction v12 = new LinearThresholdValueFunction(4, 1, alloc2);
		LinearThresholdValueFunction v22 = new LinearThresholdValueFunction(1, 2, alloc2);
		
		List<Double> alloc3 = Arrays.asList(1., 0.);
		LinearThresholdValueFunction v13 = new LinearThresholdValueFunction(4, 1, alloc3);
		LinearThresholdValueFunction v23 = new LinearThresholdValueFunction(1, 2, alloc3);
		
		List<Double> alloc4 = Arrays.asList(1., 1.);
		LinearThresholdValueFunction v14 = new LinearThresholdValueFunction(6, 1, alloc4);
		LinearThresholdValueFunction v24 = new LinearThresholdValueFunction(1, 4, alloc4);
		
		Map<Integer, LinearThresholdValueFunction> valueFunctions1 = new HashMap<Integer, LinearThresholdValueFunction>();
		Map<Integer, LinearThresholdValueFunction> valueFunctions2 = new HashMap<Integer, LinearThresholdValueFunction>();
		valueFunctions1.put(0, v11);
		valueFunctions1.put(1, v12);
		valueFunctions1.put(2, v13);
		valueFunctions1.put(3, v14);
		
		valueFunctions2.put(0, v21);
		valueFunctions2.put(1, v22);
		valueFunctions2.put(2, v23);
		valueFunctions2.put(3, v24);
		//IParametrizedValueFunction[] valueFunctions1 = {v11, v12, v13, v14};			//Parameterized value functions for both buyers 
		//IParametrizedValueFunction[] valueFunctions2 = {v21, v22, v23, v24};
		ParametrizedQuasiLinearAgent buyer1 = new ParametrizedQuasiLinearAgent(1, endowment, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer2 = new ParametrizedQuasiLinearAgent(2, endowment, valueFunctions2);
		
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
		biddersValues.add(seller1.getAtom(0).getValue());
		biddersValues.add(seller2.getAtom(0).getValue());
		
		ProbabilisticAllocation allocation = new ProbabilisticAllocation();				// Probabilistic allocation of sellers
		List<Double> allocationProbabilities = new LinkedList<Double>();
		allocationProbabilities.add(0.);												// Allocate only the 2nd DB with p=1
		allocationProbabilities.add(1.0);
		
		allocation.addAllocatedAgent(auctioneerId, bidders, bundles, allocationProbabilities);
		
		List<Double> marketDemand = mp.computeMarketDemand(0., allocation, true);
		
		assertTrue(Math.abs( marketDemand.get(1) - 3. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 20. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(1. - 1e-8, allocation, true);
		assertTrue(Math.abs( marketDemand.get(1) - 3. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 17. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(1. + 1e-8, allocation, true);
		assertTrue(Math.abs( marketDemand.get(1) - 1. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 19. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(4. - 1e-8, allocation, true);
		assertTrue(Math.abs( marketDemand.get(1) - 1. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 16. ) < 1e-6);
		
		marketDemand = mp.computeMarketDemand(4. + 1e-8, allocation, true);
		assertTrue(Math.abs( marketDemand.get(1) - 0. ) < 1e-6);
		assertTrue(Math.abs( marketDemand.get(0) - 20. ) < 1e-6);
		
		//4. Test aggregate value function
		
		/*assertTrue(Math.abs( mp.computeAggregateValue(123, allocation)-6 ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(3+ 1e-8, allocation)-6 ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(3- 0.1, allocation)-(5.9) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(1+ 0.1, allocation)-(4.1) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(1- 0.1, allocation)-(3.6) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(0.5, allocation)-(2) ) < 1e-6);
		assertTrue(Math.abs( mp.computeAggregateValue(0, allocation)-0 ) < 1e-6);
		*/
		//5. Test values of DBs
		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 0) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 6) < 1e-6);
		
		allocation.deallocateBundle(dbID1);
		allocation.deallocateBundle(dbID2);

		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 0) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 0) < 1e-6);
		
		allocationProbabilities = Arrays.asList(1., 1.);
		allocation.resetAllocationProbabilities(allocationProbabilities);

		//System.out.println(">>> " + mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation));
		assertTrue(Math.abs( mp.computeValueOfDB(dbID1, 1 - 1e-8, allocation)  - 5) < 1e-6);
		assertTrue(Math.abs( mp.computeValueOfDB(dbID2, 1 - 1e-8, allocation)  - 5) < 1e-6);
	}

	
	/**
	 * There are 2 sellers and 2 buyers in this scenario (see AAMAS'17 paper).
	 * Different sellers produce different DBs.
	 * @throws Exception 
	 */
	/*@Test
	public void testPriceFindingWithLargeEndowments() throws Exception 
	{
		//0. Define DBs
		int dbID1 = 0;
		int dbID2 = 1;
		
		//1. Create 2 sellers
		List<Integer> bundle1 = Arrays.asList(dbID1);
		List<Integer> bundle2 = Arrays.asList(dbID2);
		
		double costMin = 0.;
		double costMax = 2.;
		double costMean = (costMax + costMin) / 2;
		double costVar = Math.pow(costMax-costMin, 2) / 12.;
		double cost1 = 1.5 * costMean;
		double cost2 = 2. * costMean;
		double cost3 = 1.5 * costMean;
		
		AtomicBid sellerBid1 = new AtomicBid(1, bundle1, cost1);
		AtomicBid sellerBid2 = new AtomicBid(2, bundle2, cost2);
		AtomicBid sellerBid3 = new AtomicBid(3, bundle2, cost3);
		
		SellerType seller1 = new SellerType(sellerBid1, Distribution.UNIFORM, costMean, costVar);
		SellerType seller2 = new SellerType(sellerBid2, Distribution.UNIFORM, costMean, costVar);
		SellerType seller3 = new SellerType(sellerBid3, Distribution.UNIFORM, costMean, costVar);
		
		List<SellerType> sellers = Arrays.asList(seller1, seller2, seller3);
		
		//2. Create 2 buyers
		double endowment = 10;
		int allocations[] = {0b00, 0b01, 0b10, 0b11};									// 4 possible deterministic allocations of DBs
		
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
		mp.setToleranceLvl(1e-7);
		
		double price  = mp.tatonementPriceSearch();
		System.out.println("Price=" + price);
		assertTrue( Math.abs(price - 0.8) < 1e-3);
	}*/
	
	/**
	 * There are 2 sellers and many buyers in this scenario.
	 * @throws Exception 
	 */
	/*@Test
	public void testPriceFindingLarge() throws Exception 
	{
		//0. Define DBs
		int dbID1 = 0;
		int dbID2 = 1;
		
		//1. Create 2 sellers
		List<Integer> bundle1 = Arrays.asList(dbID1);
		List<Integer> bundle2 = Arrays.asList(dbID2);
		
		double costMin = 0.;
		double costMax = 20.;
		double costMean = (costMax + costMin) / 2;
		double costVar = Math.pow(costMax-costMin, 2) / 12.;
		double cost1 = 1.5 * costMean;
		double cost2 = 2. * costMean;
		double cost3 = 1.5 * costMean;
		
		AtomicBid sellerBid1 = new AtomicBid(1, bundle1, cost1);
		AtomicBid sellerBid2 = new AtomicBid(2, bundle2, cost2);
		AtomicBid sellerBid3 = new AtomicBid(3, bundle2, cost3);
		
		SellerType seller1 = new SellerType(sellerBid1, Distribution.UNIFORM, costMean, costVar);
		SellerType seller2 = new SellerType(sellerBid2, Distribution.UNIFORM, costMean, costVar);
		SellerType seller3 = new SellerType(sellerBid3, Distribution.UNIFORM, costMean, costVar);
		
		List<SellerType> sellers = Arrays.asList(seller1, seller2, seller3);
		
		//2. Create 2 buyers
		double endowment = 10;
		int allocations[] = {0b00, 0b01, 0b10, 0b11};									// 4 possible deterministic allocations of DBs
		
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
		
		IParametrizedValueFunction[] valueFunctions1 = {v11, v12, v13, v14};			// Parameterized value functions for both buyers 
		IParametrizedValueFunction[] valueFunctions2 = {v21, v22, v23, v24};
		ParametrizedQuasiLinearAgent buyer1 = new ParametrizedQuasiLinearAgent(1, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer2 = new ParametrizedQuasiLinearAgent(2, endowment, allocations, valueFunctions2);		
		ParametrizedQuasiLinearAgent buyer3 = new ParametrizedQuasiLinearAgent(3, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer4 = new ParametrizedQuasiLinearAgent(4, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer5 = new ParametrizedQuasiLinearAgent(5, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer6 = new ParametrizedQuasiLinearAgent(6, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer7 = new ParametrizedQuasiLinearAgent(7, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer8 = new ParametrizedQuasiLinearAgent(8, endowment, allocations, valueFunctions2);
		ParametrizedQuasiLinearAgent buyer9 = new ParametrizedQuasiLinearAgent(9, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer10 = new ParametrizedQuasiLinearAgent(10, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer11 = new ParametrizedQuasiLinearAgent(11, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer12 = new ParametrizedQuasiLinearAgent(12, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer13 = new ParametrizedQuasiLinearAgent(13, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer14 = new ParametrizedQuasiLinearAgent(14, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer15 = new ParametrizedQuasiLinearAgent(15, endowment, allocations, valueFunctions1);
		ParametrizedQuasiLinearAgent buyer16 = new ParametrizedQuasiLinearAgent(16, endowment, allocations, valueFunctions1);
		
		List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
		buyers.add(buyer1);
		buyers.add(buyer2);
		buyers.add(buyer3);
		buyers.add(buyer4);
		buyers.add(buyer5);
		buyers.add(buyer6);
		buyers.add(buyer7);
		buyers.add(buyer8);
		buyers.add(buyer9);
		buyers.add(buyer10);
		buyers.add(buyer11);
		buyers.add(buyer12);
		buyers.add(buyer13);
		buyers.add(buyer14);
		buyers.add(buyer15);
		buyers.add(buyer16);
		
		//3. Create market platform and evaluate the market demand
		MarketPlatform mp = new MarketPlatform(buyers, sellers);
		
		double price  = mp.tatonementPriceSearch();
		//assertTrue( Math.abs(price - 0.8) < 1e-3);
	}*/
	
	
}
