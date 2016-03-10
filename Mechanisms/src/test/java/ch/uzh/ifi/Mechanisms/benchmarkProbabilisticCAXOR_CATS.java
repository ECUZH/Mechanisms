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
import ch.uzh.ifi.DomainGenerators.DomainGeneratorCATSUncertain;
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
	 */
	public static void main(String[] args) throws IloException 
	{
			int numberOfGoods = 9;
			int numberOfAgents = 5;
			int numberOfSampleGames = 100;
			int numberOfRuns = 1;
			boolean isLowVariance = false;
			
			double shadingFactor = 1.;
			String paymentRule = "ECC-CORE"; 
			
			double costsMax = 10.;
			double primaryReductionCoef = 0.3;
			double secondaryReductionCoef = 0.2;
			
			List<Type> types = new ArrayList<Type>();
			IntStream.range(0, numberOfAgents).boxed().forEach( i -> types.add( new CombinatorialType( new AtomicBid(i+1, Arrays.asList(0), 0.) ) ) );

			IloCplex solver = new IloCplex();
			
			int[] irViolation  =  new int[numberOfRuns];
			double[] efficiency = new double[numberOfRuns];
			int[] emptyCoreCounter = new int[numberOfRuns];
			int[] vcgInCoreCounter = new int[numberOfRuns];
			
			for(int j = 0; j < numberOfRuns; ++j)
			{
				DomainGeneratorCATSUncertain domainGenerator = new DomainGeneratorCATSUncertain(numberOfGoods);
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
						costs.add( costsMax * generator.nextDouble());
					
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
