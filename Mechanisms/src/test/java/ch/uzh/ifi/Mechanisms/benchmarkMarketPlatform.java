package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
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
		int numberOfArguments = 11;
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
		double stepSize = Double.parseDouble( args[8 + offset] );
		int nSamples = Integer.parseInt( args[9 + offset] );			if( nSamples <= 0 )			throw new RuntimeException("Negative number of samples.");		
		int nThreads = Integer.parseInt( args[10 + offset] );  
		
		System.out.println("BENCHMARK START: " + numberOfDBs + ", " + numberOfSellers + ", " + competition + ", [" + costMin + ", " +
							costMax + "], " + costDistribution + ", " + numberOfBuyers + ", " + endowment + ", " + stepSize + ", "+ nSamples + ", " + nThreads);
		
		//0. Define DBs
		int[] dbIDs = new int[numberOfDBs];
		for(int i = 0; i < numberOfDBs; ++i)
			dbIDs[i] = i + 1;
		
		double startPrice = 0.;
		
		//0.1. Define gradient descent parameters
		double TOL = 1e-6;
		double step = 1e-2;
		if( numberOfDBs == 2 )
		{
			TOL = 1e-6;
			step = 1e-3;
		}
		else if ( numberOfDBs == 3 || numberOfDBs == 4 || numberOfDBs == 5  || numberOfDBs == 6  || numberOfDBs == 7)
		{
			TOL = 0.1;
			step = numberOfBuyers >= 512 ? 1e-4 : 1e-4;
		}
		else if (numberOfDBs == 8 || numberOfDBs == 9 || numberOfDBs == 10)
		{
			TOL = 0.1;
			step = (numberOfBuyers >= 512) ? 1e-2 : 1e-2;
		}
		else throw new RuntimeException("Unspecified TOL.");
		
		if(numberOfDBs == 10 && numberOfSellers == 10)
			startPrice = 5.;
		else if(numberOfDBs == 10 && numberOfSellers <= 14)
			startPrice = 3.;
		else if(numberOfDBs == 10 && numberOfSellers <= 18)
			startPrice = 1.;
		else if(numberOfDBs == 10 && numberOfSellers <= 19)
			startPrice = 0.5;
		else if(numberOfDBs == 10 && numberOfSellers <= 20)
		{
			step = 1e-3;
			startPrice = 0.;//0.004;
		}
		else if(numberOfDBs == 10 && numberOfSellers > 20)
		{
			step = 1e-2;
			startPrice = 0.;//0.004;
		}
		step = stepSize;
		
		System.out.println("step="+step);

		//1. Create sellers
		double costMean = (costMax + costMin) / 2;
		double costVar = 0.;
		double[] costs = new double[numberOfSellers];
		
		if( costDistribution.toUpperCase().equals("UNIFORM") )  
			costVar = Math.pow(costMax-costMin, 2) / 12.;
		else throw new RuntimeException("Not implemented");
				
		List<Double> p = new LinkedList<Double>();
		List<Double> welfare = new ArrayList<Double>();
		List<Double> surplus = new ArrayList<Double>();
		double[][] profits = new double[nSamples][numberOfSellers];
		List<Allocation> allocations = new ArrayList<Allocation>();
				
		//3. Compute equilibrium prices and allocation for the sample. Do the measurements in equilibrium for each sample.				
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
			_logger.debug("Generate  buyers...");
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
			double price  =  mp.tatonementPriceSearch(startPrice);
			//mp.resetCache();
			
			p.add(price);
			_logger.debug("Price = " + price);
			
			//---------------------------------------------------
			//
			//3.4. Measure the efficiency, surplus, etc.
			//
			//---------------------------------------------------
			_logger.debug("Start benchmarking with the equilibrium price p="+price);
			
			//3.4.1.
			List< List<Double> > inducedValues = mp.computeValuesOfDBs(price);
			System.out.println(">>" + inducedValues.get(0).toString());
			
			//Now, solve the BORA auction with the induced values of DBs and compute the total payment to be accrued to sellers
			_logger.debug("Instantiate BORA...");
			SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(sellers, inducedValues);
			auction.setSolver(mp.getSolver());
			auction.solveIt();
			
			Allocation allocation = auction.getAllocation();
			double[] payments = auction.getPayments();
			allocations.add(allocation);
			
			int alloc = 0;									// Binary representation of the deterministic allocation of DBs
			double totalCost = 0.;
			for(int i = 0; i < allocation.getBiddersInvolved(0).size(); ++i )
			{
				int sellerId = allocation.getBiddersInvolved(0).get(i);
				totalCost += sellers.get(sellerId-1).getAtom(0).getValue();
				profits[s][sellerId - 1] = payments[i] - sellers.get(sellerId-1).getAtom(0).getValue();
				int bit = 1 << (sellers.get(sellerId-1).getInterestingSet(0).get(0) - 1);
				alloc = alloc | bit;
				System.out.println("Seller with id="+ sellerId + " is allocated. Payment="+payments[i]);
				System.out.println("Profit of this seller ("+sellers.get(sellerId-1).getAgentId()+") is " + payments[i] + "-" + sellers.get(sellerId-1).getAtom(0).getValue() + " = " + profits[s][sellerId - 1] );
			}
			System.out.println("Allocation of BORA: " + allocation.getBiddersInvolved(0).toString() + "(" + alloc + ")");
			
			if(alloc == 0) 					//trivial equilibrium
				p.remove(p.size()-1);		//remove the last element
			
			//3.4.2.			
			for(int k = 0; k < numberOfDBs; ++k)
			{
				int dbId = dbIDs[k];
				double dbValue = inducedValues.get(dbId-1).get(alloc);
				System.out.println( "DB's value " + dbValue );
			}
			
			// Compute buyers' utility
			double totalUtility = 0.;
			double totalValue = 0.;
			for(int i = 0; i < numberOfBuyers; ++i)
			{
				List<Double> consumptionBundle = buyers.get(i).solveConsumptionProblem(price);
				totalUtility += buyers.get(i).computeUtility(alloc, numberOfDBs, consumptionBundle) - buyers.get(i).getEndowment();
				totalValue += buyers.get(i).computeUtility(alloc, numberOfDBs, consumptionBundle) - consumptionBundle.get(0);
			}
			System.out.println("Total surplus of buyers: " + totalUtility );
			System.out.println("Total welfare: " + (totalValue - totalCost) );
			surplus.add( totalUtility );
			welfare.add( totalValue - totalCost );
//welfare.add(totalUtility);//!!!!!!!!!!!!!!!!!!!
			if(alloc == 0) 								//trivial equilibrium
			{
				surplus.remove(surplus.size()-1);		//remove the last element
				welfare.remove(welfare.size()-1);
			}
		}
		
		System.out.println("(Non-trivial) Equilibrium Prices: " + p.toString());
		System.out.println("Surplus: " + surplus.toString());
		System.out.println("Welfare: " + welfare.toString());
		
		List<Double> profitsMean = new ArrayList<Double>();
		for(int i = 0; i < numberOfSellers; ++i)
		{
			double profitMean = 0.;
			for(int j = 0; j < nSamples; ++j)
				profitMean += profits[j][i];

			profitMean = profitMean/p.size();
			
			double profitStderr = 0.;
			for(int j = 0; j < nSamples; ++j)
				profitStderr += (profits[j][i] - profitMean) * (profits[j][i] - profitMean);
			
			profitStderr = Math.sqrt( profitStderr/ (p.size() - 1) / p.size() );
			System.out.println("Av. profit of seller i=" + i + " in a non-trivial equilibrium is " + profitMean + " (" + profitStderr +")");
			profitsMean.add(profitMean);
		}
		
		//for t-test for the surplus of competing sellers (useful for the small scenario)
		double[] diff = new double[nSamples];
		for(int j = 0; j < nSamples; ++j)
			diff[j] = profits[j][0] - profits[j][1];
		
		double diffMean = 0.;
		for(int j = 0; j < nSamples; ++j)
			diffMean += diff[j];

		diffMean = diffMean/p.size();
		
		double diffStderr = 0.;
		for(int j = 0; j < nSamples; ++j)
			diffStderr += (diff[j] - diffMean) * (diff[j] - diffMean);
		
		diffStderr = Math.sqrt( diffStderr/ (p.size() - 1) / p.size() );
		System.out.println("Av. diff in a non-trivial equilibrium is " + diffMean + " (" + diffStderr +")");


		System.out.println("Allocations: ");
		for(Allocation allocation : allocations)
			System.out.println(allocation.getBiddersInvolved(0).toString());
		
		double pMean = p.stream().reduce(0., (i, j) -> i+j) / p.size();
		double pStd = Math.sqrt( p.stream().map(x -> (x - pMean)*(x - pMean)).reduce(0., (i, j) -> i+j) / (p.size() - 1) );
		double surplusMean = surplus.stream().reduce(0., (i, j) -> i+j) / surplus.size();
		double surplusStd = Math.sqrt( surplus.stream().map(x -> (x - surplusMean)*(x - surplusMean)).reduce(0., (i, j) -> i+j) / (surplus.size() - 1) );
		double welfareMean = welfare.stream().reduce(0., (i, j) -> i+j) / welfare.size();
		//double welfareMean = welfare.stream().reduce(0., (i, j) -> i+j) / welfare.size() + profitsMean.stream().reduce(0., (i, j) -> i+j);//!!!!
		double welfareStd = Math.sqrt( welfare.stream().map(x -> (x - welfareMean)*(x - welfareMean)).reduce(0., (i, j) -> i+j) / (welfare.size() - 1) );
		
		System.out.println("mean(price) = " + pMean + " (" + pStd/Math.sqrt(p.size()) + ")");
		System.out.println("mean(Surplus) = " + surplusMean + " (" + surplusStd/Math.sqrt(surplus.size()) + ")");
		System.out.println("mean(SW) = " + welfareMean + " (" + welfareStd/Math.sqrt(welfare.size()) + ")");
	}

}
