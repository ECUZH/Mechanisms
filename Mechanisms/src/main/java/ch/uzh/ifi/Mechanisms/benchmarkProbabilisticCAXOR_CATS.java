package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.DomainGenerators.DomainGeneratorSpatialUncertain;
import ch.uzh.ifi.DomainGenerators.SpacialDomainGenerationException;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class benchmarkProbabilisticCAXOR_CATS 
{

	private static final Logger _logger = LogManager.getLogger(benchmarkProbabilisticCAXOR_CATS.class);
	
	/**
	 * Entry point
	 * @throws SpacialDomainGenerationException 
	 */
	public static void main(String[] args) throws IloException 
	{
		String problemSize= args[0];							if( !(problemSize.equals("small") || problemSize.equals("big")) ) throw new RuntimeException("Wrong problemsize specified");
		int varianceLevel = Integer.parseInt(args[1]);			if( !(varianceLevel == 0 || varianceLevel == 1) )	throw new RuntimeException("Wrong variance level");
		int isTruthful    = Integer.parseInt(args[2]);			if( !(isTruthful == 0 || isTruthful == 1) )			throw new RuntimeException("Wrong paraemtner value: " + isTruthful);
		String testName   = args[3];							if( !(testName.equals("Efficiency") || 
																	  testName.equals("IRV") || testName.equals("EmptyCore")) )	throw new RuntimeException("Wrong test name specified: "+testName );
		String paymentRule= args[4];

		System.out.println("Test: " + testName + " varianceLvl = " + varianceLevel + ". isTruthful = " + isTruthful + " . " +paymentRule);

		boolean isLowVariance = varianceLevel == 0 ? true : false;
		
		double[] costsLimit= {  5.,     10.,    20.,    30.,    40.,    50.,    60.,    70.,    80.,    90.};
		//Shading factors for ECC-CORE:
		double[] shadeECCLS= {0.95,    0.96,  0.959,  0.968,   0.99,  0.976,  0.973,  0.979,  0.984,  0.989};  //ECC with low variance and small problem size (9 goods, 5 bidders)
		double[] shadeECCHS= {																			   };  //ECC with high variance and small problem size (9 goods, 5 bidders)
		double[] shadeECCLB= {0.94,      0.,   0.96,  0.958,  0.963,  0.975,  0.978,  0.991,  0.987,  0.994};  //ECC with low variance and big problem size (16 goods, 8 bidders)
		double[] shadeECCHB= {																			   };  //ECC with high variance and big problem size (16 goods, 8 bidders)

		//Shading factors for ECR-CORE:
		double[] shadeECRLS= {0.99,    0.99,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99};  //ECR with low variance and small problem size (9 goods, 5 bidders)
		double[] shadeECRHS= {																			   };  //ECR with high variance and small problem size (9 goods, 5 bidders)
		double[] shadeECRLB= {	 0,	   0.93,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99,   0.99};  //ECR with low variance and big problem size (16 goods, 8 bidders)
		double[] shadeECRHB= {																			   };  //ECR with high variance and big problem size (16 goods, 8 bidders)
		
		//Shading factors for ECR-CORE:
		double[] shadeExpLS= {0.96,    0.96,   0.97,  0.975,  0.967,   0.99,   0.98,   0.99,   0.99,   0.99};  //Exp with low variance and small problem size (9 goods, 5 bidders)
		double[] shadeExpHS= {																			   };  //Exp with high variance and small problem size (9 goods, 5 bidders)
		double[] shadeExpLB= {   0,    0.89,   0.96,   0.98,  0.979,   0.98,   0.99,   0.99,   0.99,   0.99};  //Exp with low variance and big problem size (16 goods, 8 bidders)
		double[] shadeExpHB= {																			   };  //Exp with high variance and big problem size (16 goods, 8 bidders)

		int numberOfGoods = problemSize.equals("small") ? 9 : 16;
		int numberOfAgents= problemSize.equals("small") ? 5 : 8;
		int numberOfSampleGames = 100;
		int numberOfRuns = 1;
		double primaryReductionCoef = isLowVariance ? 0.3 : 0.6;
		double secondaryReductionCoef = isLowVariance ? 0.2 : 0.1;
		
		for(int k = 0; k < costsLimit.length; ++k)
		{
			double shadingFactor = 1.;
			
			switch(paymentRule)
			{
				case "ECC-CORE":	if(problemSize.equals("small") && isLowVariance)  
										shadingFactor = shadeECCLS[k];
									else if(problemSize.equals("small") && !isLowVariance)
										shadingFactor = shadeECCHS[k];
									else if(problemSize.equals("big") && isLowVariance)
										shadingFactor = shadeECCLB[k];
									else if(problemSize.equals("big") && !isLowVariance)
										shadingFactor = shadeECCHB[k];
									break;
				case "ECR-CORE":	if(problemSize.equals("small") && isLowVariance)  
										shadingFactor = shadeECRLS[k];
									else if(problemSize.equals("small") && !isLowVariance)
										shadingFactor = shadeECRHS[k];
									else if(problemSize.equals("big") && isLowVariance)
										shadingFactor = shadeECRLB[k];
									else if(problemSize.equals("big") && !isLowVariance)
										shadingFactor = shadeECRHB[k];
									break;
				case "Exp-CORE":	if(problemSize.equals("small") && isLowVariance)  
										shadingFactor = shadeExpLS[k];
									else if(problemSize.equals("small") && !isLowVariance)
										shadingFactor = shadeExpHS[k];
									else if(problemSize.equals("big") && isLowVariance)
										shadingFactor = shadeExpLB[k];
									else if(problemSize.equals("big") && !isLowVariance)
										shadingFactor = shadeExpHB[k];
									break;
				default: 			throw new RuntimeException("Incorrect payment rule specified.");
			}
			
			if( isTruthful == 1 )
				shadingFactor = 1.; 
			//System.out.println("Shading factor: " + shadingFactor);
			
			List<Type> types = new ArrayList<Type>();
			IntStream.range(0, numberOfAgents).boxed().forEach( i -> types.add( new CombinatorialType( new AtomicBid(i+1, Arrays.asList(0), 0.) ) ) ); 	//Add dummy types

			IloCplex solver = new IloCplex();
			
			int[] irViolation  =  new int[numberOfSampleGames];
			double[] efficiency = new double[numberOfSampleGames];
			int[] emptyCoreCounter = new int[numberOfSampleGames];
			int[] vcgInCoreCounter = new int[numberOfSampleGames];
			
			for(int j = 0; j < numberOfRuns; ++j)
			{
				DomainGeneratorSpatialUncertain domainGenerator;
				try {
					domainGenerator = new DomainGeneratorSpatialUncertain(numberOfGoods);
				
					domainGenerator.setNumberOfJPMFSamples(10000);
					domainGenerator.setNumberOfBombsToThrow(1);
					domainGenerator.setBombsParameters(Arrays.asList(primaryReductionCoef), Arrays.asList(secondaryReductionCoef), Arrays.asList(1.), Arrays.asList(1.));
					domainGenerator.generateJPMF();
				
					for(int i = 0; i < numberOfSampleGames; ++i)
					{
						//System.out.println("i="+i);
						Random generator = new Random(j*10000 + i);
						//generator.setSeed(System.nanoTime());
					
						List<Type> bids = new ArrayList<Type>();
						for(int q = 0; q < numberOfAgents; ++q)
						{
							Type ct = domainGenerator.generateBid(j*10000 + i*100 + q*10, types.get(q).getAgentId());
							for(int s = 0; s < ct.getNumberOfAtoms(); ++s)
								ct.getAtom(s).setValue( ct.getAtom(s).getValue() * shadingFactor );
							bids.add(ct);
						}
					
						List<Double> costs = new ArrayList<Double>();
						for(int q = 0; q < numberOfGoods; ++q)
							costs.add( costsLimit[k] * generator.nextDouble());
					
						ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), numberOfGoods, bids, costs, domainGenerator.getJPMF());
						auction.setSolver(solver);
						auction.setPaymentRule(paymentRule);
						auction.setSeed(System.nanoTime());
					
						try
						{
							try
							{
								auction.solveIt();
							}
							catch(PaymentException e)
							{
								if(e.getMessage().equals("Empty Core"))
									emptyCoreCounter[i] += 1;
								else if(e.getMessage().equals("VCG is in the Core"))
									vcgInCoreCounter[i] += 1;
							}
							AllocationEC allocation = (AllocationEC)auction.getAllocation();
							double[] payments = auction.getPayments();
								
							int numberOfAllocatedAgents = 0;
							if(allocation.getNumberOfAllocatedAuctioneers() > 0)
								numberOfAllocatedAgents = allocation.getBiddersInvolved(0).size();
						
							for(int q = 0; q < numberOfAllocatedAgents; ++q)
							{
								int allocatedBidderId = allocation.getBiddersInvolved(0).get(q);
								int allocatedBundleIdx = allocation.getAllocatedBundlesOfTrade(0).get(q);
								double value = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).getValue() / shadingFactor;
								double realizedAvailability = allocation.getRealizedRV(0, q);
								if( value*realizedAvailability - payments[q] < 0 )
									irViolation[i]+=1;
							
								double realizedValue = value*realizedAvailability; 
								double realizedCost = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).computeCost(costs)*realizedAvailability;
								efficiency[i] += realizedValue - realizedCost;
							}
						}
						catch (Exception e) 
						{
						e.printStackTrace();
						}
					}
					//System.out.println("[IRV]="+irViolation[j]);
					//System.out.println("Eff=" + efficiency[j]);
					//System.out.println("EmptyCore=" + emptyCoreCounter[j]);
					//System.out.println("VCG in the Core=" + vcgInCoreCounter[j]);
				} 
				catch (SpacialDomainGenerationException e1) 
				{
					e1.printStackTrace();
				}
			}
			
			double irMean = 0.;
			double effMean = 0.;
			double emptyCoreMean = 0.;
			for(int j = 0; j < numberOfSampleGames; ++j)
			{
				irMean += irViolation[j];
				effMean+= efficiency[j];
				emptyCoreMean += emptyCoreCounter[j];
			}
			irMean /= numberOfSampleGames;
			irMean *= 100;
			effMean/= numberOfSampleGames;
			emptyCoreMean /=numberOfSampleGames;
			
			double effStdErr = 0.;
			for(int j = 0; j < numberOfSampleGames; ++j)
				effStdErr += Math.pow(efficiency[j] - effMean, 2);

			effStdErr /= numberOfSampleGames;
			effStdErr = Math.sqrt(effStdErr);
			effStdErr = effStdErr / Math.sqrt(numberOfSampleGames);

			
			double irStdErr = 0.;
			for(int j = 0; j < numberOfSampleGames; ++j)
				irStdErr += Math.pow(irViolation[j] - irMean, 2);

			irStdErr /= numberOfSampleGames;
			irStdErr = Math.sqrt(irStdErr);
			irStdErr = irStdErr / Math.sqrt(numberOfSampleGames);

			
			double emptyCoreStdErr = 0.;
			for(int j = 0; j < numberOfSampleGames; ++j)
				emptyCoreStdErr += Math.pow(emptyCoreCounter[j] - emptyCoreMean, 2);

			emptyCoreStdErr /= numberOfSampleGames;
			emptyCoreStdErr = Math.sqrt(emptyCoreStdErr);
			emptyCoreStdErr = emptyCoreStdErr / Math.sqrt(numberOfSampleGames);

			switch(testName)
			{
			case "Efficiency"		:	System.out.println("E[eff]=" + effMean+ " S="+effStdErr);
										break;
			case "IRV"				:	System.out.println("E[IRV]=" + irMean + " S="+irStdErr);
										break;
			case "EmptyCore"		:   System.out.println("E[emptyCore]=" + emptyCoreMean + " S="+ emptyCoreStdErr);
										break;
			default					: 	throw new RuntimeException("Wrong test name : " + testName);
			}
		}
	}	
}
