package ch.uzh.ifi.Mechanisms;

import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ch.uzh.ifi.DomainGenerators.DomainGeneratorCATS;
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

	/*
	 * Entry point
	 */
	public static void main(String[] args) throws IloException 
	{
			int numberOfGoods = 9;
			int numberOfAgents = 5;
			List<Type> types = new LinkedList<Type>();
			for(int i = 0; i < numberOfAgents; ++i)
			{
				List<Integer> bundle = new LinkedList<Integer>();
				bundle.add( 0 );
				double marginalValue1 = 0.;
				AtomicBid atom11  =  new AtomicBid(i+1, bundle, Math.max(marginalValue1 ,0));		
				CombinatorialType t1 = new CombinatorialType();
				t1.addAtomicBid(atom11);
				types.add(t1);
			}
			IloCplex solver = new IloCplex();
			
			boolean isLowVariance = false;
			String paymentRule = "ECC-CORE";

			int numberOfSampleGames = 1000;
			double shadingFactor = 0.96;
			
			double costsMax = 10.;
			double primaryReductionCoef = 0.3;
			double secondaryReductionCoef = 0.2;
			
			List<Integer> items = new LinkedList<Integer>();
			items.add(1);
			items.add(2);
			
			int numberOfRuns = 10;
			int[] irViolation = new int[numberOfRuns];
			double[] efficiency = new double[numberOfRuns];
			int[] emptyCoreCounter = new int[numberOfRuns];
			
			for(int j = 0; j < numberOfRuns; ++j)
			{	
				GridGenerator gridGenerator = new GridGenerator(3, 3);
				gridGenerator.setSeed(0);
				gridGenerator.buildProximityGraph();
				Graph grid = gridGenerator.getGrid();
			
				JointProbabilityMass jpmf = new JointProbabilityMass( grid);
				jpmf.setNumberOfSamples(10000);
				jpmf.setNumberOfBombsToThrow(1);
				
				IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
				List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
				bombs.add(b1);
				
				List<Double> pd = new LinkedList<Double>();
				pd.add(1.);
				
				jpmf.setBombs(bombs, pd);
				jpmf.update();
						
				for(int i = 0; i < numberOfSampleGames; ++i)
				{
					Random generator = new Random(j*10000 + i);
					generator.setSeed(System.nanoTime());
					
					DomainGeneratorCATS domainGenerator = new DomainGeneratorCATS(numberOfGoods, 1, grid);
					List<Type> bids = new LinkedList<Type>();
					
					for(int q = 0; q < numberOfAgents; ++q)
					{
						Type ct = domainGenerator.generateBid(j*10000 + i*10 + q, types.get(q));
						bids.add(ct);
					}
					
					//System.out.println("Bids: " + bids.toString());
					
					
					List<Double> costs = new LinkedList<Double>();
					for(int q = 0; q < numberOfGoods; ++q)
						costs.add( costsMax * generator.nextDouble());
					
					ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), numberOfGoods, bids, costs, jpmf);
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
						}
						AllocationEC allocation = (AllocationEC)auction.getAllocation();
						double[] payments = auction.getPayments();
						
						int numberOfAllocatedAgents = 0;
						if(allocation.getNumberOfAllocatedAuctioneers() > 0)
							numberOfAllocatedAgents = allocation.getBiddersInvolved(0).size();
						
						for(int q = 0; q < numberOfAllocatedAgents; ++q)
						{
							int allocatedBidderId = allocation.getBiddersInvolved(0).get(q);
							int allocatedBundleIdx = allocation.getAllocatedBundlesByIndex(0).get(q);
							double value = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).getValue() / shadingFactor;
							double realizedAvailability = allocation.getRealizedRV(0, q);
							if( value*realizedAvailability - payments[q] < 0 )
								irViolation[j]+=1;
							
							double realizedValue = value*realizedAvailability; 
							double realizedCost = computeCost(bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx), costs)*realizedAvailability;
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
				//System.out.println("EmptyCore=" + emptyCoreCounter[j]);
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
	
	public static double computeCost(AtomicBid atom, List<Double> costs)
	{
		double cost = 0.;
		
		for(int item : atom.getInterestingSet())
			cost += costs.get( item - 1 );
		
		return cost;
	}
}
