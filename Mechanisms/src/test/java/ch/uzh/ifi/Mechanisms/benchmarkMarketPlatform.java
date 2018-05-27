package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

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
		int numberOfArguments = 10;
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
		String competition = args[2 + offset];							if( !(competition.toUpperCase().equals("UNIFORM") || competition.toUpperCase().equals("LINEAR") || competition.toUpperCase().equals("MONOPOLISTS")) ) throw new RuntimeException("Wrong competition structure: " + competition.toUpperCase());
		double costMin = Double.parseDouble( args[3 + offset] );		if( costMin < 0) 			throw new RuntimeException("Negative min cost.");				// Min of the costs distribution support
		double costMax = Double.parseDouble( args[4 + offset] );		if( costMax <= costMin ) 	throw new RuntimeException("Max cost smaller than min cost.");	// Max of the costs distribution support
		String costDistribution = args[5 + offset];						if( !(costDistribution.toUpperCase().equals("UNIFORM") || costDistribution.toUpperCase().equals("NORMAL"))) throw new RuntimeException("Wrong costs distribution specified.");
		int numberOfBuyers = Integer.parseInt( args[6 + offset] );		if( numberOfBuyers <= 0 )	throw new RuntimeException("The number of sellers should be positive.");
		double endowment = Double.parseDouble( args[7 + offset] );		if( endowment < 0 )			throw new RuntimeException("Negative endowments.");
		int nSamples = Integer.parseInt( args[8 + offset] );			if( nSamples <= 0 )			throw new RuntimeException("Negative number of samples.");		
		int nThreads = Integer.parseInt( args[9 + offset] );  
		
		System.out.println("BENCHMARK START: " + numberOfDBs + ", " + numberOfSellers + ", " + competition + ", [" + costMin + ", " +
							costMax + "], " + costDistribution + ", " + numberOfBuyers + ", " + endowment + ", " + nSamples + ", " + nThreads);
		
		//0. Define DBs
		int[] dbIDs = new int[numberOfDBs];
		for(int i = 0; i < numberOfDBs; ++i)
			dbIDs[i] = i;
		
		//0.1. Define gradient descent parameters
		double TOL = 1e-6;
		double step = 1e-2;
		if( numberOfDBs == 2 )
		{
			TOL = 1e-6;
			step = 1e-2;
		}
		else if ( numberOfDBs == 3 || numberOfDBs == 4 || numberOfDBs == 5  || numberOfDBs == 6  || numberOfDBs == 7)
		{
			TOL = 0.01;
			step = numberOfBuyers >= 512 ? 1e-4 : 1e-3;
		}
		else if (numberOfDBs == 8 || numberOfDBs == 9 || numberOfDBs == 10)
		{
			TOL = 0.01;
			step = numberOfBuyers >= 512 ? 1e-4 : 10*1e-3;
		}
		else throw new RuntimeException("Unspecified TOL.");
		System.out.println("step="+step);

		//1. Create sellers
		double costMean = (costMax + costMin) / 2;
		double costVar = 0.;
		double[] costs = new double[numberOfSellers];
		
		if( costDistribution.toUpperCase().equals("UNIFORM") )  
			costVar = Math.pow(costMax-costMin, 2) / 12. ;
		else throw new RuntimeException("Not implemented");
				
		List<Double> p = new LinkedList<Double>();
		List<Double> welfare = new ArrayList<Double>();
		List<Double> surplus = new ArrayList<Double>();
		double[][] profits = new double[nSamples][numberOfSellers];
		List<List<Double> > allocations = new ArrayList< List<Double> >();
				
		//3. Compute equilibrium prices and allocation for the sample				
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
					int producedDB = dbIDs[ i % numberOfDBs ];
					AtomicBid sellerBid = new AtomicBid(i+1, Arrays.asList( producedDB ), costs[i]);
					SellerType seller = new SellerType(sellerBid, Distribution.UNIFORM, costMean, costVar);
					_logger.debug("Create seller id=" + (i+1) + ". DB produced: " + producedDB);
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
					_logger.debug("Create seller id=" + (i+1) + ". DB produced: " + producedDB);
					sellers.add(seller);
				}
				else if( competition.toUpperCase().equals("MONOPOLISTS"))
				{
					// Number of sellers equals to the number of monopolists. For other DBs competition is equal to 2.
					int numberOfMonopolists = 2 * numberOfDBs - numberOfSellers;
					int producedDB = 0;
					if( i < numberOfMonopolists )
						producedDB = dbIDs[i];
					else
						producedDB = dbIDs[numberOfMonopolists + (i - numberOfMonopolists)%(numberOfDBs-numberOfMonopolists) ];
					
					AtomicBid sellerBid = new AtomicBid(i+1, Arrays.asList( producedDB ), costs[i]);
					SellerType seller = new SellerType(sellerBid, Distribution.UNIFORM, costMean, costVar);
					_logger.debug("Create seller id=" + (i+1) + ". DB produced: " + producedDB);
					sellers.add(seller);
				}
			}
			
			//3.2. Generate buyers
			BuyersGenerator buyersGenerator = new BuyersGenerator(numberOfDBs, endowment, s);
			List<ParametrizedQuasiLinearAgent> buyers = new CopyOnWriteArrayList<ParametrizedQuasiLinearAgent>();
			
			for(int i = 0; i < numberOfBuyers; ++i)
				buyers.add(buyersGenerator.generateBuyer(i+1));
			
			//3.2. Create market platform and evaluate the market demand
			MarketPlatform mp = new MarketPlatform(buyers, sellers);
			mp.setToleranceLvl(TOL);
			mp.setStep(step);
			mp.setNumberOfThreads(nThreads);
					
			//3.3. Compute the equilibrium price
			double price  = mp.tatonementPriceSearch();
			p.add(price);
			_logger.debug("Price = " + price);
			
			//---------------------------------------------------
			//
			//3.4. Measure the efficiency, surplus, etc.
			//
			//---------------------------------------------------
			
			//3.4.1.
			List<Double> allocationProbabilities = mp.getAllocationProbabilities();
			for(int i = 0; i < allocationProbabilities.size(); ++i)
				if( allocationProbabilities.get(i) < 0.5 )
					allocationProbabilities.set(i, 0.);
				else
					allocationProbabilities.set(i, 1.);
			allocations.add(allocationProbabilities);
			System.out.println("Allocation probabilities: " + Arrays.toString( allocationProbabilities.toArray() ));
			
			List<Integer> bidders = new LinkedList<Integer>();
			List<Integer> bundles = new LinkedList<Integer>();
			
			for(int j = 0; j < sellers.size(); ++j)
			{
				bidders.add(sellers.get(j).getAgentId());
				bundles.add(sellers.get(j).getAtom(0).getInterestingSet().get(0));
			}
			ProbabilisticAllocation allocationOfSellers = new ProbabilisticAllocation();
			allocationOfSellers.addAllocatedAgent(0, bidders, bundles, allocationProbabilities);
			
			//3.4.2.
			double totalCost = 0.;
			for(int k = 0; k < numberOfDBs; ++k)
			{
				System.out.println("Consider DB " + dbIDs[k] + ". Sellers involved: ");
				
				List<Type> sellersInvolved = new ArrayList<Type>();
				for( SellerType seller : sellers )
					if( seller.getInterestingSet(0).get(0) == k )
					{
						sellersInvolved.add(seller);
						System.out.println( seller.getAgentId() + " cost: " + seller.getAtom(0).getValue());
					}
				
				double dbValue = mp.computeValueOfDB(dbIDs[k], price, allocationOfSellers);
				System.out.println( "DB's value " + dbValue );
				
				// Run BORA for the DB
				SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(sellersInvolved, dbValue);
				auction.solveIt();
				
				//
				int isAllocated = auction.getAllocation().getNumberOfAllocatedAuctioneers();
				
				if( isAllocated > 0 && price > 1e-3 )
				{
					double payment = auction.getPayments()[0];
					System.out.println("Payment = " + payment);
					for( SellerType seller : sellers)
						if( auction.getAllocation().isAllocated(seller.getAgentId()) )
						{
							totalCost += seller.getAtom(0).getValue();
							profits[s][seller.getAgentId()-1] = payment-seller.getAtom(0).getValue();
							System.out.println("Profit of the winner ("+seller.getAgentId()+") is " + (payment-seller.getAtom(0).getValue()) );
						}
				}
				else
					System.out.println("Nobody allocated: zero profits!");
				
			}
			
			buyers.get(0).updateAllocProbabilityDistribution(allocationOfSellers);
			for(int i = 1; i < numberOfBuyers; ++i)
				buyers.get(i).setAllocProbabilityDistribution( buyers.get(0).getAllocProbabilityDistribution());
			
			double totalUtility = 0.;
			double totalValue = 0.;
			for(int i = 0; i < numberOfBuyers; ++i)
			{
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(price);
				totalUtility += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(allocationOfSellers, consumptionBundle) - consumptionBundle.get(0);
			}
			System.out.println("Total surplus of buyers: " + totalUtility );
			System.out.println("Total welfare: " + (totalValue - totalCost) );
			surplus.add( totalUtility );
			welfare.add( totalValue - totalCost );
		}
		
		System.out.println("Equilibrium Prices: " + p.toString());
		System.out.println("Welfare: " + surplus.toString());
		System.out.println("Welfare: " + welfare.toString());
		for(int i = 0; i < numberOfSellers; ++i)
		{
			double profit = 0.;
			for(int j = 0; j < nSamples; ++j)
				profit += profits[j][i];
			System.out.println("Av. profit of seller i="+i+ " is " + profit/nSamples);
		}
		System.out.println("Allocations: ");
		for(List<Double> allocation : allocations)
			System.out.println(allocation.toString());
		
		System.out.println("mean(price) = " + p.stream().reduce(0., (i, j) -> i+j) / nSamples );
		System.out.println("mean(Surplus) = " + surplus.stream().reduce(0., (i, j) -> i+j) / nSamples);
		System.out.println("mean(SW) = " + welfare.stream().reduce(0., (i, j) -> i+j) / nSamples);
	}

}
