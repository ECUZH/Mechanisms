package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.FocusedBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.IBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.DomainGenerators.GridGenerator;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.GraphAlgorithms.Graph;

/**
 * Benchmark for LLG domain.
 * @author Dmitry Moor
 */
public class benchmarkUncertaintyCAXOR_LLG 
{
	
	/*
	 * Entry point
	 */
	public static void main(String[] args) 
	{
		boolean isLowVariance = false;
		String paymentRule = "CORE_LLG";
		
		double[] shadeCAL   = { 0.23,   0.220,  0.190,  0.180,  0.180,  0.180,  0.200,  0.210,  0.220,  0.220, 0.210 };
		double[] shadeCAL_G = { 0.29,   0.32,  0.38,  0.38,  0.41,  0.43,  0.49,  0.52,  0.57,  0.56, 0.56 };
		
		double[] shadeCAH  = { 0.2 ,   0.200,  0.180,  0.170,  0.200,  0.180,  0.180,  0.220,  0.230,  0.220, 0.220 };
		double[] shadeCAH_G= { 0.61,   0.61,  0.66,  0.65,  0.70,  0.79,  0.84,  0.86,  0.85,  0.83, 0.83 };
		
		for(int k = 0; k < shadeCAL.length; ++k)
		{
			int numberOfSampleGames = 100000;
			double shadingFactorL = isLowVariance ? shadeCAL[k] : shadeCAH[k];
			double shadingFactorG = isLowVariance ? shadeCAL_G[k] : shadeCAH_G[k];
			
			double costsMax = 0.1 * k;
			double primaryReductionCoef = isLowVariance ? 0.3 : 0.6;
			double secondaryReductionCoef = isLowVariance ? 0.2 : 0.1;
			double primaryReductionCoef1 = 0.4;
			double secondaryReductionCoef1 = 0.3;
			
			List<Integer> items = new LinkedList<Integer>();
			items.add(1);
			items.add(2);
			
			int numberOfRuns = 10;
			int[] irViolation = new int[numberOfRuns];
			double[] efficiency = new double[numberOfRuns];
			
			for(int j = 0; j < numberOfRuns; ++j)
			{
				GridGenerator gridGenerator = new GridGenerator(1, 2);
				gridGenerator.setSeed(0);
				gridGenerator.buildProximityGraph();
				Graph grid = gridGenerator.getGrid();
			
				JointProbabilityMass jpmf = new JointProbabilityMass( grid);
				jpmf.setNumberOfSamples(10000);
				jpmf.setNumberOfBombsToThrow(1);
				
				IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
				IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef1, secondaryReductionCoef1);
				List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
				bombs.add(b1);
				bombs.add(b2);
				
				List<Double> pd = new LinkedList<Double>();
				pd.add(0.5);
				pd.add(0.5);
				
				jpmf.setBombs(bombs, pd);
				jpmf.update();
			
				double maxRegret = 0.;
				double avRegret  = 0.;
				List<Double> regret = new LinkedList<Double>();
			
				for(int i = 0; i < numberOfSampleGames; ++i)
				{
					Random generator = new Random( i*10);
					generator.setSeed(System.nanoTime());
				
					//System.out.println("i="+i);
					//Local bidder
					List<Integer> bundle = new LinkedList<Integer>();
					bundle.add( items.get(0) );
					double marginalValue1 = generator.nextDouble();
					AtomicBid atom11  =  new AtomicBid(1, bundle, Math.max(marginalValue1 - shadingFactorL,0));		
					CombinatorialType t1 = new CombinatorialType();
					t1.addAtomicBid(atom11);
					
					//Local bidder
					bundle = new LinkedList<Integer>();
					bundle.add( items.get(1) );
					double marginalValue2 = generator.nextDouble();
					AtomicBid atom21 = new AtomicBid(2, bundle, Math.max(marginalValue2 - shadingFactorL,0));
					CombinatorialType t2 = new CombinatorialType();
					t2.addAtomicBid(atom21);
					
					//Global bidder
					bundle = new LinkedList<Integer>();
					bundle.add( items.get(0) );
					bundle.add( items.get(1) );
					double marginalValue3 = generator.nextDouble() * 2.;
					AtomicBid atom31 = new AtomicBid(3, bundle, Math.max(marginalValue3 - shadingFactorG, 0));
					
					CombinatorialType t3 = new CombinatorialType();
					t3.addAtomicBid(atom31);
					
					List<Type> bids = new LinkedList<Type>();
					bids.add(t1);
					bids.add(t2);
					bids.add(t3);
					
					List<Double> costs = new LinkedList<Double>();
					costs.add( costsMax * generator.nextDouble());
					costs.add( costsMax * generator.nextDouble());
					
					CAXOR auction = new CAXOR( bids.size(), items.size(), bids, costs);
					//auction.setPaymentRule("Exp-CORE_LLG");
					auction.setPaymentRule(paymentRule);
					//auction.setSeed(System.nanoTime());
					
					try
					{
						auction.solveIt();
						AllocationEC allocation = (AllocationEC)auction.getAllocation();
						
						int numberOfAllocatedAgents = 0;
						if(allocation.getNumberOfAllocatedAuctioneers() > 0)
							numberOfAllocatedAgents = allocation.getBiddersInvolved(0).size();
						
						if(numberOfAllocatedAgents == 2)
						{
							//System.out.println(">> " + allocation.getRealizedRV(0, 0));
							//System.out.println(">> " + allocation.getRealizedRV(0, 1));
							double realizedValue1 = marginalValue1 * allocation.getRealizedRV(0, 0);
							double realizedValue2 = marginalValue2 * allocation.getRealizedRV(0, 1);
							double[] payments = auction.getPayments();
							
							efficiency[j] += realizedValue1 + realizedValue2
										  - costs.get(0) * allocation.getRealizedRV(0, 0) - costs.get(1) * allocation.getRealizedRV(0, 1);
							
							if(realizedValue1 - payments[0] < 0)
								irViolation[j]+=1;
							if(realizedValue2 - payments[1] < 0)
								irViolation[j]+=1;
						}
						else if (numberOfAllocatedAgents == 1)
						{
							double realizedValue = marginalValue3 * allocation.getRealizedRV(0, 0);
							double realizedCosts = (costs.get(0) + costs.get(1)) * allocation.getRealizedRV(0, 0);
							double[] payments = auction.getPayments();
							avRegret += realizedValue - payments[0];
							regret.add(realizedValue - payments[0]);
							
							if( realizedValue - payments[0] < 0)
								irViolation[j]+=1;
							if( realizedValue - payments[0] < maxRegret )
								maxRegret = realizedValue - payments[0];
							
							efficiency[j] += realizedValue - realizedCosts;
						}
						
					}
					catch (Exception e) 
					{
						e.printStackTrace();
					}
				}
				//System.out.println("[IRV]="+irViolation[j]);
			}
			
			double irMean = 0.;
			double effMean = 0.;
			for(int j = 0; j < numberOfRuns; ++j)
			{
				irMean += irViolation[j];
				effMean+= efficiency[j];
			}
			irMean /= numberOfRuns;
			effMean/= numberOfRuns;
	
			System.out.println(irMean / 1000);
			//System.out.println(effMean);
		}
	}
}
