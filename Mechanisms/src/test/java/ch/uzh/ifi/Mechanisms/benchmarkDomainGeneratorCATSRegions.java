package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ch.uzh.ifi.DomainGenerators.DomainGeneratorCATS;
import ch.uzh.ifi.DomainGenerators.DomainGeneratorSpatial;
import ch.uzh.ifi.DomainGenerators.SpacialDomainGenerationException;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.IDomainGenerator;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

/**
 * Benchmark for different implementations of CATS region domain generator. 
 * @author Dmitry Moor
 *
 */
public class benchmarkDomainGeneratorCATSRegions {

	/**
	 * Entry point
	 */
	public static void main(String[] args) throws IloException 
	{
		boolean isCATS = false;						//True if CATS regions should be tested, false if my impl. of regions to be tested
		//    Test number:             0                   1                     2              3                     4                     5             
		String[] benchmarks = { "VCG in the Core", "VCG to Value Ratio", "Revenue Ratio", "Number of Winners", "Av Size of Bundle", "Value of Bundle"};
		String benchmarkName = benchmarks[0];
		
		String problemSize = "big";// "small"
		
		int numberOfGoods = problemSize.equals("small") ? 9 : 16;
		int numberOfAgents = problemSize.equals("small") ? 5 : 8;
		
		int numberOfSamples = 100;
		int numberOfGamesPerSample = 100;
		String paymentRule = "CORE"; 
		double costsMax = 1e-6;
		IloCplex cplexSolver = new IloCplex();
		
		//Create dummy types for all agents.
		List<Type> types = new ArrayList<Type>();
		IntStream.range(0, numberOfAgents).boxed().forEach( i -> types.add( new CombinatorialType( new AtomicBid(i+1, Arrays.asList(0), 0.) ) ) );
					
		IDomainGenerator domainGenerator;
		
		for(int k = 0; k < numberOfSamples; ++k)						//For every sample
		{
			int vcgInCoreCounter = 0;
			List<Double> vcgToValueRatio = new ArrayList<Double>();
			List<Double> revenueRatio = new ArrayList<Double>();
			List<Integer> numberOfWinners = new ArrayList<Integer>();
			List<Double> avSizeOfWinningBundle = new ArrayList<Double>();
			List<Double> avValueOfWinningBundle = new ArrayList<Double>();
			try 
			{
				for(int i = 0; i < numberOfGamesPerSample; ++i)			
				{
					if( isCATS )
						domainGenerator = new DomainGeneratorCATS(numberOfGoods, numberOfAgents, "C:\\Users\\Dmitry\\Downloads\\files\\files\\files"+(k+1));
					else
						domainGenerator = new DomainGeneratorSpatial(numberOfGoods, problemSize.equals("small") ? 0.78 : 0.86);
					
					Random generator = new Random(i);
					generator.setSeed(System.nanoTime());
							
					List<Type> bids = new LinkedList<Type>();
					for(int j = 0; j < numberOfAgents; ++j)
					{
						Type ct;
						if( isCATS )
							ct = domainGenerator.generateBid(i, types.get(j).getAgentId());
						else
							ct = domainGenerator.generateBid((k+1)*(i*10 + j), types.get(j).getAgentId());
						bids.add(ct);
					}
							
					List<Double> costs = new LinkedList<Double>();
					for(int j = 0; j < numberOfGoods; ++j)
						costs.add( costsMax * generator.nextDouble());
							
					CAXOR auction = new CAXOR( bids.size(), numberOfGoods, bids, costs);
					auction.setPaymentRule(paymentRule);
					auction.setSolver(cplexSolver);
							
					try
					{
						try
						{
							auction.solveIt();
							vcgToValueRatio.add(auction.getVCGtoValueRatio());
							revenueRatio.add(auction.getRevenueRatio());
							if(auction.getAllocation().getNumberOfAllocatedAuctioneers() > 0)
							{
								Allocation allocation = auction.getAllocation();
								double totalSizeOfWinningBundles = 0.;
								double totalValueOfWinningBundles = 0.;
								for(int s = 0; s < allocation.getBiddersInvolved(0).size(); ++s)
								{
									int bidderId = allocation.getBiddersInvolved(0).get(s);
									int allocatedBundleIdx = allocation.getAllocatedBundlesOfTrade(0).get(s);
									int bundleSize = bids.get(bidderId-1).getAtom(allocatedBundleIdx).getInterestingSet().size();
									double bundleValue = bids.get(bidderId-1).getAtom(allocatedBundleIdx).getValue();
									totalSizeOfWinningBundles += bundleSize;
									totalValueOfWinningBundles += bundleValue;
								}
								numberOfWinners.add( allocation.getBiddersInvolved(0).size() );
								avSizeOfWinningBundle.add( totalSizeOfWinningBundles / allocation.getBiddersInvolved(0).size());
								avValueOfWinningBundle.add( totalValueOfWinningBundles / allocation.getBiddersInvolved(0).size());
							}
							else
							{
								numberOfWinners.add(0);
								avSizeOfWinningBundle.add(0.);
							}
						}
						catch(PaymentException e)
						{
							if(e.getMessage().equals("VCG is in the Core"))
								vcgInCoreCounter += 1;
						}
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
				
				switch(benchmarkName)
				{
					case "VCG in the Core" 	:	System.out.println(vcgInCoreCounter); 
												break;
					case "VCG to Value Ratio":	double meanVCGtoValueRatio = vcgToValueRatio.stream().reduce( (x1, x2) -> x1+x2).get() / vcgToValueRatio.size();
												System.out.println(meanVCGtoValueRatio);
												break;
					case "Revenue Ratio"	:	double meanRevenueRatio = revenueRatio.stream().reduce( (x1, x2) -> x1+x2).get() / revenueRatio.size();
												System.out.println(meanRevenueRatio);
												break;
					case "Number of Winners":	double meanNumberOfWinners = (double)numberOfWinners.stream().reduce( (x1, x2) -> x1+x2).get() / numberOfWinners.size();
												System.out.println(meanNumberOfWinners);
												break;
					case "Av Size of Bundle":	double meanAvSizeOfWinningBundle = avSizeOfWinningBundle.stream().reduce( (x1, x2) -> x1+x2).get() / avSizeOfWinningBundle.size();
												System.out.println(meanAvSizeOfWinningBundle);
												break;
					case "Value of Bundle"	:	double meanAvValueOfWinningBundle = avValueOfWinningBundle.stream().reduce( (x1, x2) -> x1+x2).get() / avValueOfWinningBundle.size();
												System.out.println(meanAvValueOfWinningBundle);
												break;
					default 				:	throw new RuntimeException("No such benchmark exists: " + benchmarkName);
				}
			} 
			catch (SpacialDomainGenerationException e1) 
			{
				e1.printStackTrace();
			}
		}
	}
}
