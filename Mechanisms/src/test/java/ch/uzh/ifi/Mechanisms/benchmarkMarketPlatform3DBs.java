package ch.uzh.ifi.Mechanisms;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class benchmarkMarketPlatform3DBs {

	public static void main(String[] args) throws Exception 
	{
		System.out.println("BENCHMARK");
		//0. Define DBs
		int nDBs  = 3;
		int dbID1 = 0;
		int dbID2 = 1;
		int dbID3 = 2;
				
		List<Integer> bundle1 = Arrays.asList(dbID1);
		List<Integer> bundle2 = Arrays.asList(dbID2);
		List<Integer> bundle3 = Arrays.asList(dbID3);
		
		//1. Create 2 sellers
		double costMin = 0.;										// Min of the costs distribution support
		double costMax = 20.;										// Max of the costs distribution support
		double costMean = (costMax + costMin) / 2;
		double costVar = Math.pow(costMax-costMin, 2) / 12.;
		double cost1 = 1.5 * costMean;								// Actual cost of seller 1
		double cost2 = 2. * costMean;								// Actual cost of seller 2
		double cost3 = 1.5 * costMean;								// Actual cost of seller 3
		double cost4 = 0.9 * costMean;
		double cost5 = 1.1 * costMean;
				
		AtomicBid sellerBid1 = new AtomicBid(1, bundle1, cost1);
		AtomicBid sellerBid2 = new AtomicBid(2, bundle2, cost2);
		AtomicBid sellerBid3 = new AtomicBid(3, bundle2, cost3);
		AtomicBid sellerBid4 = new AtomicBid(4, bundle3, cost4);
		AtomicBid sellerBid5 = new AtomicBid(5, bundle3, cost5);
				
		SellerType seller1 = new SellerType(sellerBid1, Distribution.UNIFORM, costMean, costVar);
		SellerType seller2 = new SellerType(sellerBid2, Distribution.UNIFORM, costMean, costVar);
		SellerType seller3 = new SellerType(sellerBid3, Distribution.UNIFORM, costMean, costVar);
		SellerType seller4 = new SellerType(sellerBid4, Distribution.UNIFORM, costMean, costVar);
		SellerType seller5 = new SellerType(sellerBid5, Distribution.UNIFORM, costMean, costVar);
				
		List<SellerType> sellers = Arrays.asList(seller1, seller2, seller3, seller4, seller5);
				
		//2. Create 2 buyers
		double endowment = 10;
				
		List<Double> p = new LinkedList<Double>();
		int numberOfBuyers = 20;
		System.out.println("Number of buyers: " + numberOfBuyers);
				
		//3. Compute equilibrium prices for a sample of 10 
		int nSamples = 10;
				
		for(int s = 0; s < nSamples; ++s)
		{
			BuyersGenerator gen = new BuyersGenerator(nDBs, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
				
			//3.1. Generate buyers
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(gen.generateBuyer(i+1));
					
			//3.2. Create market platform and evaluate the market demand
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
			mp.setToleranceLvl(1e-7);
					
			//3.3. Compute the equilibrium price
			double price  = mp.tatonementPriceSearch();
			p.add(price);
			System.out.println("Price = " + price);
		}
		
		System.out.println("Equilibrium Prices: " + p.toString());
				
		List<Integer> bidders = new LinkedList<Integer>();
		List<Integer> bundles = new LinkedList<Integer>();
		for(int j = 0; j < sellers.size(); ++j)
		{
			bidders.add(sellers.get(j).getAgentId());
			bundles.add(sellers.get(j).getAtom(0).getInterestingSet().get(0));
		}
				
		ProbabilisticAllocation allocationOfSellers = new ProbabilisticAllocation();
		List<Double> allocationProbabilities = new LinkedList<Double>();
		allocationProbabilities.add(1.0);
		allocationProbabilities.add(0.0);
		allocationProbabilities.add(1.0);
		allocationOfSellers.addAllocatedAgent(0, bidders, bundles, allocationProbabilities);
				
		List<Double> profitsS1 = new LinkedList<Double>();
		List<Double> profitsS3 = new LinkedList<Double>();
		List<Double> surplusP = new LinkedList<Double>();
		List<Double> surplus0 = new LinkedList<Double>();
		List<Double> welfareP = new LinkedList<Double>();
		List<Double> welfare0 = new LinkedList<Double>();
				
		//4. Benchmark profits, welfare, etc. in equilibrium
		for(int s = 0; s < nSamples; ++s)
		{
			//4.1. Instantiate same buyers (same random seed is used)
			BuyersGenerator gen = new BuyersGenerator(nDBs, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(gen.generateBuyer(i+1));
					
			//4.2. Instantiate the market platform
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
					
			//4.3. Compute values of DBs under equilibrium allocation and prices
			double dbValue1 = mp.computeValueOfDB(dbID1, p.get(s), allocationOfSellers);
			List<Type> sellersInvolved1 = new LinkedList<Type>();
			sellersInvolved1.add(seller1);
					
			double dbValue2 = mp.computeValueOfDB(dbID2, p.get(s), allocationOfSellers);
			List<Type> sellersInvolved2 = new LinkedList<Type>();
			sellersInvolved2.add(seller2);
			sellersInvolved2.add(seller3);
					
			//4.4. Run BORA for the first DB
			SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(sellersInvolved1, dbValue1);
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
					
			//4.5. Run BORA for the 2nd DB
			auction = new SurplusOptimalReverseAuction(sellersInvolved2, dbValue2);
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
				allocationOfSellers.resetAllocationProbabilities( newAlloc );
			}
			else if ( isAllocated1 == 0 && isAllocated2 > 0 && p.get(s) > 0.01)
			{
				List<Double> newAlloc = Arrays.asList(0., 0., 1.);
				allocationOfSellers.resetAllocationProbabilities( newAlloc );
			}
			else if ( (isAllocated1 == 0 && isAllocated2 == 0) || p.get(s) < 0.01)
			{
				List<Double> newAlloc = Arrays.asList(0., 0., 0.);
				allocationOfSellers.resetAllocationProbabilities( newAlloc );
			}
					
			for(int i = 0; i < numberOfBuyers; ++i)
			{
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(prices, allocationOfSellers);
				totalUtility += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - consumptionBundle.get(0);
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
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(prices, allocationOfSellers);
				totalUtility += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - consumptionBundle.get(0);
			}
			surplus0.add(totalUtility);
			welfare0.add( (totalValue-cost1 - cost3) < 0 ? 0 : (totalValue-cost1 - cost3));
			
			List<Double> newAlloc = Arrays.asList(1., 0., 1.);
			allocationOfSellers.resetAllocationProbabilities( newAlloc );
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
