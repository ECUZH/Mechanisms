package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
	 * There are 2 sellers and 2 buyers in this scenario (see AAMAS'17 paper).
	 * Different sellers produce different DBs.
	 * @throws Exception 
	 */
/*	@Test
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
		biddersValues.add(seller1.getAtom(0).getValue());
		biddersValues.add(seller2.getAtom(0).getValue());
		
		ProbabilisticAllocation allocation = new ProbabilisticAllocation();				//Probabilistic allocation of sellers
		List<Double> allocationProbabilities = new LinkedList<Double>();
		allocationProbabilities.add(0.);
		allocationProbabilities.add(1.0);
		
		allocation.addAllocatedAgent(auctioneerId, bidders, bundles, allocationProbabilities);
		
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
	}*/

	
	/**
	 * There are 2 sellers and 2 buyers in this scenario (see AAMAS'17 paper).
	 * Different sellers produce different DBs.
	 * @throws Exception 
	 */
/*	@Test
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
		
		double price  = mp.tatonementPriceSearch();
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
	
	@Test
	public void testBuyersGenerator() throws Exception 
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
		
		
		List<Double> p = new LinkedList<Double>();
		int nSamples = 10;
		int numberOfBuyers = 65;
		System.out.println("Number of buyers: " + numberOfBuyers);
		for(int s = 0; s < nSamples; ++s)
		{
			BuyersGenerator gen = new BuyersGenerator(2, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
			
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(gen.generateBuyer(i+1));
			
			//3. Create market platform and evaluate the market demand
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
			
			double price  = mp.tatonementPriceSearch();
			p.add(price);
			//double price = 0.682;
			System.out.println("Price = " + price);
		}
		
		System.out.println("Prices: " + p.toString());
		
		ProbabilisticAllocation probAllocation = new ProbabilisticAllocation();
		List<Integer> bidders = new LinkedList<Integer>();
		List<Double> allocationProbabilities = new LinkedList<Double>();
		List<Integer> bundles = new LinkedList<Integer>();
		for(int j = 0; j < sellers.size(); ++j)
		{
			bidders.add(sellers.get(j).getAgentId());
			bundles.add(sellers.get(j).getAtom(0).getInterestingSet().get(0));
		}
		allocationProbabilities.add(1.0);
		allocationProbabilities.add(0.0);
		allocationProbabilities.add(1.0);
		probAllocation.addAllocatedAgent(0, bidders, bundles, allocationProbabilities);
		
		List<Double> profitsS1 = new LinkedList<Double>();
		List<Double> profitsS3 = new LinkedList<Double>();
		List<Double> surplusP = new LinkedList<Double>();
		List<Double> surplus0 = new LinkedList<Double>();
		List<Double> welfareP = new LinkedList<Double>();
		List<Double> welfare0 = new LinkedList<Double>();
		
		for(int s= 0; s < nSamples; ++s)
		{
			BuyersGenerator gen = new BuyersGenerator(2, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(gen.generateBuyer(i+1));
			
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
			
			
			double dbValue1 = mp.computeValueOfDB(dbID1, p.get(s), probAllocation);
			List<Type> sellersInvolved1 = new LinkedList<Type>();
			sellersInvolved1.add(seller1);
			
			double dbValue2 = mp.computeValueOfDB(dbID2, p.get(s), probAllocation);
			List<Type> sellersInvolved2 = new LinkedList<Type>();
			sellersInvolved2.add(seller2);
			sellersInvolved2.add(seller3);
			
			SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(sellersInvolved1, dbValue1);
			// Solve the auction
			auction.solveIt();
			
			
			// Buyers' surplus
			double totalUtility = 0.;
			double totalValue = 0.;
			List<Double> prices = new LinkedList<Double>();
			prices.add(1.);
			prices.add(p.get(s));
			
			int isAllocated1 = auction.getAllocation().getNumberOfAllocatedAuctioneers(); 
			if( isAllocated1 > 0 && p.get(s)>0.01)
			{
				double payment = auction.getPayments()[0];
				profitsS1.add(payment - cost1);
			}
			else
				profitsS1.add(0.);
			
			auction = new SurplusOptimalReverseAuction(sellersInvolved2, dbValue2);
			// Solve the auction
			auction.solveIt();
			int isAllocated2 = auction.getAllocation().getNumberOfAllocatedAuctioneers();
			if( isAllocated2 > 0 && p.get(s) > 0.01 )
			{
				double payment = auction.getPayments()[0];
				profitsS3.add(payment - cost3);
			}
			else
				profitsS3.add(0.);
			
			if( isAllocated1 > 0 && isAllocated2 == 0 && p.get(s) > 0.01)
			{
				List<Double> newAlloc = Arrays.asList(1., 0., 0.);
				probAllocation.resetAllocationProbabilities( newAlloc );
			}
			else if ( isAllocated1 == 0 && isAllocated2 > 0 && p.get(s) > 0.01)
			{
				List<Double> newAlloc = Arrays.asList(0., 0., 1.);
				probAllocation.resetAllocationProbabilities( newAlloc );
			}
			else if ( (isAllocated1 == 0 && isAllocated2 == 0) || p.get(s) < 0.01)
			{
				List<Double> newAlloc = Arrays.asList(0., 0., 0.);
				probAllocation.resetAllocationProbabilities( newAlloc );
			}
			

			for(int i = 0; i < numberOfBuyers; ++i)
			{
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(prices, probAllocation);
				totalUtility += buyers.get(i).computeUtility(probAllocation, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(probAllocation, consumptionBundle) - consumptionBundle.get(0);
			}
			surplusP.add(totalUtility);
			welfareP.add( (totalValue-cost1 - cost3 < 0) ? 0. : (totalValue-cost1 - cost3));
				
			// Buyers' surplus if p==0
			totalUtility = 0.;
			totalValue = 0.;
			prices = new LinkedList<Double>();
			prices.add(1.);
			prices.add(0.);
							
			for(int i = 0; i < numberOfBuyers; ++i)
			{
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(prices, probAllocation);
				totalUtility += buyers.get(i).computeUtility(probAllocation, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(probAllocation, consumptionBundle) - consumptionBundle.get(0);
			}
			surplus0.add(totalUtility);
			welfare0.add( (totalValue-cost1 - cost3) < 0 ? 0 : (totalValue-cost1 - cost3));
			
			List<Double> newAlloc = Arrays.asList(1., 0., 1.);
			probAllocation.resetAllocationProbabilities( newAlloc );
		}
		System.out.println("pi(s1): " + profitsS1.toString());
		System.out.println("pi(s3): " + profitsS3.toString());
		System.out.println("surp(p): " + surplusP.toString());
		System.out.println("surp(0): " + surplus0.toString());
		System.out.println("sw(p): " + welfareP.toString());
		System.out.println("sw(0): " + welfare0.toString());
		System.out.println("mean(price) = " + p.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((profitsS1) ) = " + profitsS1.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((profitsS3) ) = " + profitsS3.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((surp(p)) ) = " + surplusP.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((surp(0)) ) = " + surplus0.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((sw(p)) ) = " + welfareP.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean((sw(0)) ) = " + welfare0.stream().reduce(0., (i, j) -> i+j) / nSamples );
	}
}
