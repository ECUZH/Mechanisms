package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ch.uzh.ifi.DomainGenerators.DomainGeneratorSpatial;
import ch.uzh.ifi.DomainGenerators.DomainGeneratorSpatialUncertain;
import ch.uzh.ifi.DomainGenerators.SpacialDomainGenerationException;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.FocusedBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.IBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.DomainGenerators.GridGenerator;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.GraphAlgorithms.Graph;

public class benchmarkProbabilisticCAXOR_CATS 
{

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
		double[] shadeECRLS= {																			   };  //ECR with low variance and small problem size (9 goods, 5 bidders)
		double[] shadeECRHS= {																			   };  //ECR with high variance and small problem size (9 goods, 5 bidders)
		double[] shadeECRLB= {																			   };  //ECR with low variance and big problem size (16 goods, 8 bidders)
		double[] shadeECRHB= {																			   };  //ECR with high variance and big problem size (16 goods, 8 bidders)
		
		//double[] shadeECRL = {0.185,   0.132,  0.104,  0.098,  0.077,  0.054,  0.046,  0.026,  0.028,  0.011, 0.008};
		//double[] shadeExpL = {0.165,   0.132,  0.108,  0.093,  0.075,  0.053,  0.045,  0.032,  0.021,  0.012, 0.004};
		//double[] shadeSML  = {0.107,   0.098,  0.066,  0.068,  0.028,  0.020,  0.005,  0.006,  0.000,  0.000, 0.000};
		
		//double[] shadeECCH = {0.125,   0.085,  0.085,  0.066,  0.047,  0.038,  0.028,  0.030,  0.020,  0.014, 0.003};
		//double[] shadeECRH = {0.144,   0.142,  0.073,  0.072,  0.067,  0.064,  0.032,  0.030,  0.011,  0.016, 0.012};
		//double[] shadeExpH = {0.164,   0.131,  0.105,  0.096,  0.068,  0.058,  0.048,  0.032,  0.024,  0.019, 0.010};
		//double[] shadeSMH  = {0.058,   0.046,  0.042,  0.033,  0.033,  0.009,  0.004,  0.002,  0.005,  0.004, 0.000};

		int numberOfGoods = problemSize.equals("small") ? 9 : 16;
		int numberOfAgents= problemSize.equals("small") ? 5 : 8;
		int numberOfSampleGames = 1000;
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
				case "Exp-CORE":	break;
				default: 			throw new RuntimeException("Incorrect payment rule specified.");
			}
			
			if( isTruthful == 1 )
				shadingFactor = 1.; 
			
			List<Type> types = new ArrayList<Type>();
			IntStream.range(0, numberOfAgents).boxed().forEach( i -> types.add( new CombinatorialType( new AtomicBid(i+1, Arrays.asList(0), 0.) ) ) ); 	//Add dummy types

			IloCplex solver = new IloCplex();
			
			int[] irViolation  =  new int[numberOfRuns];
			double[] efficiency = new double[numberOfRuns];
			int[] emptyCoreCounter = new int[numberOfRuns];
			int[] vcgInCoreCounter = new int[numberOfRuns];
			
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
						System.out.println("i="+i);
						Random generator = new Random(j*10000 + i);
						generator.setSeed(System.nanoTime());
					
						List<Type> bids = new LinkedList<Type>();
					
						for(int q = 0; q < numberOfAgents; ++q)
						{
							Type ct = domainGenerator.generateBid(j*10000 + i*10 + q, types.get(q).getAgentId());
							bids.add(ct);
						}
					
						List<Double> costs = new LinkedList<Double>();
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
									emptyCoreCounter[j] += 1;
								else if(e.getMessage().equals("VCG is in the Core"))
									vcgInCoreCounter[j] += 1;
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
									irViolation[j]+=1;
							
								double realizedValue = value*realizedAvailability; 
								double realizedCost = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).computeCost(costs)*realizedAvailability;
								efficiency[j] += realizedValue - realizedCost;
							}
						/*
						if(numberOfAllocatedAgents == 2)
						{
							double realizedValue1 = marginalValue1 * allocation.getRealizedRV(0, 0);
							double realizedValue2 = marginalValue2 * allocation.getRealizedRV(0, 1);
							double[] payments = auction.getPayments();
							
							efficiency[j] += realizedValue1 + realizedValue2
										  - costs.get(0) * allocation.getRealizedRV(0, 0) - costs.get(1) * allocation.getRealizedRV(0, 1);
							
							if(realizedValue1 - payments[0] < 0)
								irViolation[j]+=1;
							if(realizedValue2 - payments[1] < 0)
								irViolation[j]+=1;
						}*/
						
						}
						catch (Exception e) 
						{
						e.printStackTrace();
						}
					}
					//System.out.println("[IRV]="+irViolation[j]);
					System.out.println("Eff=" + efficiency[j]);
					System.out.println("EmptyCore=" + emptyCoreCounter[j]);
					System.out.println("VCG in the Core=" + vcgInCoreCounter[j]);
				} catch (SpacialDomainGenerationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
			double irMean = 0.;
			double effMean = 0.;
			double emptyCoreMean = 0.;
			for(int j = 0; j < numberOfRuns; ++j)
			{
				irMean += irViolation[j];
				effMean+= efficiency[j];
				emptyCoreMean += emptyCoreCounter[j];
			}
			irMean /= numberOfRuns;
			irMean /= numberOfSampleGames;
			irMean *= 100;
			effMean/= numberOfRuns;
			emptyCoreMean /= numberOfRuns;
			emptyCoreMean /=numberOfSampleGames;
			//System.out.println(irMean);
			System.out.println(effMean);
			//System.out.println(emptyCoreMean );
		}
	}	
}
