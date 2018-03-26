package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class benchmarkMarketPlatform {

	private static final Logger _logger = LogManager.getLogger(benchmarkMarketPlatform.class);
	
	public static void main(String[] args) throws Exception 
	{
		System.out.println("BENCHMARK START");
		
		int numberOfArguments = 9;
		int offset = 0;
		if( args.length == numberOfArguments)
			offset = 0;
		else if( args.length == numberOfArguments + 3 )	//offset caused by MPI parameters
			offset = 3;
		else
			throw new RuntimeException("Wrong number of input parameters: " + args.length);
			
		//1. Parse command line arguments
		int numberOfDBs = Integer.parseInt(args[0 + offset]);			if( numberOfDBs <= 0)		throw new RuntimeException("The number of DBs should be posititve.");
		int numberOfSellers = Integer.parseInt( args[1 + offset]);		if( numberOfSellers <= 0 )	throw new RuntimeException("The number of sellers should be positive.");
		String competition = args[2 + offset];							if( !(competition.toUpperCase().equals("UNIFORM") || competition.toUpperCase().equals("LINEAR")) ) throw new RuntimeException("Wrong competition structure: " + competition.toUpperCase());
		double costMin = Double.parseDouble( args[3 + offset] );		if( costMin < 0) 			throw new RuntimeException("Negative min cost.");				// Min of the costs distribution support
		double costMax = Double.parseDouble( args[4 + offset] );		if( costMax <= costMin ) 	throw new RuntimeException("Max cost smaller than min cost.");	// Max of the costs distribution support
		String costDistribution = args[5 + offset];						if( !(costDistribution.toUpperCase().equals("UNIFORM") || costDistribution.toUpperCase().equals("NORMAL"))) throw new RuntimeException("Wrong costs distribution specified.");
		int numberOfBuyers = Integer.parseInt( args[6 + offset] );		if( numberOfBuyers <= 0 )	throw new RuntimeException("The number of sellers should be positive.");
		double endowment = Double.parseDouble( args[7 + offset] );		if( endowment < 0 )			throw new RuntimeException("Negative endowments.");
		int nSamples = Integer.parseInt( args[8 + offset] );			if( nSamples <= 0 )			throw new RuntimeException("Negative number of samples.");		
		
		//0. Define DBs
		int[] dbIDs = new int[numberOfDBs];
		for(int i = 0; i < numberOfDBs; ++i)
			dbIDs[i] = i;
		
		//0.1. Define gradient descent parameters
		double TOL = 1e-6;
		double step = 1e-2;
		if( numberOfDBs == 2 )
			TOL = 1e-7;
		else if ( numberOfDBs == 3)
			TOL = 6 * 1e-6;
		else if (numberOfDBs == 4 || numberOfDBs == 5)
		{
			TOL = 5*1e-6;
			step = 2*1e-2;
		}
		else if (numberOfDBs == 10)
		{
			TOL = 1e-4;
			step = 5*1e-2;
		}
		else throw new RuntimeException("Unspecified TOL.");

		//1. Create sellers
		double costMean = (costMax + costMin) / 2;
		double costVar = 0.;
		double[] costs = new double[numberOfSellers];
		
		if( costDistribution.toUpperCase().equals("UNIFORM") )  
			costVar = Math.pow(costMax-costMin, 2) / 12. ;
		else throw new RuntimeException("Not implemented");
				
		//2. Create 2 buyers				
		List<Double> p = new LinkedList<Double>();
				
		//3. Compute equilibrium prices for the sample				
		for(int s = 0; s < nSamples; ++s)
		{
			Random gen = new Random( s * 1000 );
			List<SellerType> sellers = new ArrayList<SellerType>();
			
			// 3.1 Generate sellers
			for(int i = 0; i < numberOfSellers; ++i)
			{
				// 3.1.1. Generate cost
				if( costDistribution.toUpperCase().equals("UNIFORM") )
					costs[i] = costMin + gen.nextDouble() * (costMax - costMin);
				else throw new RuntimeException("Not implemented.");
				
				// 3.1.2. Choose the bundle (DB produced by the seller)
				if( competition.toUpperCase().equals("UNIFORM"))
				{
					AtomicBid sellerBid = new AtomicBid(i+1, Arrays.asList( dbIDs[ i % numberOfDBs ] ), costs[i]);
					SellerType seller = new SellerType(sellerBid, Distribution.UNIFORM, costMean, costVar);
					sellers.add(seller);
				}
				else if( competition.toUpperCase().equals("LINEAR"))						// 1 + d + 2d + ... + (#DBs-1)*d = #sellers     (arithmetic progression)
				{
					int d = (int)Math.floor(2. * (numberOfSellers - 1) / numberOfDBs / (numberOfDBs - 1));	// Increment of the number of sellers per DB 
					
					int producedDB = 0;
					if( i+1 == 1 )
						producedDB = dbIDs[0];
					else
						for(int j = 1; j <= numberOfDBs; ++j)
							if( 1 + d*j*(j-1)/2 <= i+1 && i+1 < 1 + d*j*(j+1)/2)
								producedDB = dbIDs[j-1];
							
					AtomicBid sellerBid = new AtomicBid(i+1, Arrays.asList( producedDB ), costs[i]);
					SellerType seller = new SellerType(sellerBid, Distribution.UNIFORM, costMean, costVar);
					//_logger.debug("Create seller id=" + (i+1) + ". DB produced: " + producedDB);
					sellers.add(seller);
				}	
			}
				
			//3.2. Generate buyers
			BuyersGenerator buyersGenerator = new BuyersGenerator(numberOfDBs, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new LinkedList<ParametrizedQuasiLinearAgent>();
			
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(buyersGenerator.generateBuyer(i+1));
					
			//3.2. Create market platform and evaluate the market demand
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
			mp.setToleranceLvl(TOL);
			mp.setStep(step);
					
			//3.3. Compute the equilibrium price
			double price  = mp.tatonementPriceSearch();
			p.add(price);
			_logger.debug("Price = " + price);
		}
		
		System.out.println("Equilibrium Prices: " + p.toString());
				
		/*List<Integer> bidders = new LinkedList<Integer>();
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
			BuyersGenerator gen = new BuyersGenerator(numberOfDBs, endowment, s);
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
		System.out.println("mean((sw(0)) ) = " + welfare0.stream().reduce(0., (i, j) -> i+j) / nSamples );*/
	}

}
