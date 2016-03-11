package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

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
 * The class implements a test for the LLG setting with uncertainty and dependent
 * availabilities of goods.
 * @author Dmitry
 *
 */
public class benchmarkProbabilisticCAXOR_LLG 
{

	/*
	 * Entry point
	 */
	public static void main(String[] args) 
	{
		int varianceLevel = Integer.parseInt(args[0]);			if( !(varianceLevel == 0 || varianceLevel == 1) )	throw new RuntimeException("Wrong variance level");
		int isTruthful    = Integer.parseInt(args[1]);			if( !(isTruthful == 0 || isTruthful == 1) )		throw new RuntimeException("Wrong paraemtner value: " + isTruthful);
		String testName   = args[2];							if( !(testName.equals("Efficiency") || 
																		testName.equals("IRV") || testName.equals("EmptyCore")) )	throw new RuntimeException("Wrong test name specified: "+testName );
		String paymentRule= args[3];
		
		System.out.println("Test: " + testName + " varianceLvl = " + varianceLevel + ". isTruthful = " + isTruthful + " . " +paymentRule);
		
		boolean isLowVariance = varianceLevel == 0 ? true : false;
		
		double[] shadeECCL = {0.166,   0.132,  0.103,  0.098,  0.079,  0.055,  0.045,  0.033,  0.022,  0.018, 0.009};
		double[] shadeECRL = {0.185,   0.132,  0.104,  0.098,  0.077,  0.054,  0.046,  0.026,  0.028,  0.011, 0.008};
		double[] shadeExpL = {0.165,   0.132,  0.108,  0.093,  0.075,  0.053,  0.045,  0.032,  0.021,  0.012, 0.004};
		double[] shadeSML  = {0.107,   0.098,  0.066,  0.068,  0.028,  0.020,  0.005,  0.006,  0.000,  0.000, 0.000};
		
		double[] shadeECCH = {0.125,   0.085,  0.085,  0.066,  0.047,  0.038,  0.028,  0.030,  0.020,  0.014, 0.003};
		double[] shadeECRH = {0.144,   0.142,  0.073,  0.072,  0.067,  0.064,  0.032,  0.030,  0.011,  0.016, 0.012};
		double[] shadeExpH = {0.164,   0.131,  0.105,  0.096,  0.068,  0.058,  0.048,  0.032,  0.024,  0.019, 0.010};
		double[] shadeSMH  = {0.058,   0.046,  0.042,  0.033,  0.033,  0.009,  0.004,  0.002,  0.005,  0.004, 0.000};
		
		for(int k = 0; k < shadeECCL.length; ++k )
		{
			int numberOfSampleGames = 1;
			double shadingFactor = 0.;
			switch(paymentRule) 
			{
				case "ECC-CORE_LLG" : 	shadingFactor = isLowVariance ? shadeECCL[k] : shadeECCH[k];
										break;
				case "ECR-CORE_LLG" : 	shadingFactor = isLowVariance ? shadeECRL[k] : shadeECRH[k];
										break;
				case "Exp-CORE_LLG" : 	shadingFactor = isLowVariance ? shadeExpL[k] : shadeExpH[k];
										break;
				case "expostIR_ECR" : 	shadingFactor = isLowVariance ? shadeSML[k] : shadeSMH[k];
										break;
				default				:	throw new RuntimeException("Wrong payment rule specified");
			}
			
			if( isTruthful == 1 )
				shadingFactor = 0.;
			
			double costsMax = 0.1 * k;
			double primaryReductionCoef = isLowVariance ? 0.3 : 0.6;
			double secondaryReductionCoef = isLowVariance ? 0.2 : 0.1;
			double primaryReductionCoef1 = 0.4;
			double secondaryReductionCoef1 = 0.3;
			
			List<Integer> items = new LinkedList<Integer>();
			items.add(1);
			items.add(2);
			
			int numberOfRuns = 100;
			int[] irViolation = new int[numberOfRuns];
			double[] efficiency = new double[numberOfRuns];
			int[] emptyCoreCounter = new int[numberOfRuns];
			
			
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
		
			
			for(int j = 0; j < numberOfRuns; ++j)
			{
				//System.out.println("Run: " + j);
				double maxRegret = 0.;
				List<Double> regret = new LinkedList<Double>();
			
				for(int i = 0; i < numberOfSampleGames; ++i)
				{
					Random generator = new Random(10 * i + j*numberOfRuns);
					generator.setSeed(System.nanoTime());
				
					//Local bidder
					List<Integer> bundle = new LinkedList<Integer>();
					bundle.add( items.get(0) );
					double marginalValue1 = generator.nextDouble();
					AtomicBid atom11  =  new AtomicBid(1, bundle, Math.max(marginalValue1 - shadingFactor,0));		
					CombinatorialType t1 = new CombinatorialType();
					t1.addAtomicBid(atom11);
					
					//Local bidder
					bundle = new LinkedList<Integer>();
					bundle.add( items.get(1) );
					double marginalValue2 = generator.nextDouble();
					AtomicBid atom21 = new AtomicBid(2, bundle, Math.max(marginalValue2 - shadingFactor,0));
					CombinatorialType t2 = new CombinatorialType();
					t2.addAtomicBid(atom21);
					
					//Global bidder
					bundle = new LinkedList<Integer>();
					bundle.add( items.get(0) );
					bundle.add( items.get(1) );
					double marginalValue3 = generator.nextDouble() * 2.;
					AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue3);
					
					CombinatorialType t3 = new CombinatorialType();
					t3.addAtomicBid(atom31);
					
					List<Type> bids = new LinkedList<Type>();
					bids.add(t1);
					bids.add(t2);
					bids.add(t3);
					
					List<Double> costs = new LinkedList<Double>();
					costs.add( costsMax * generator.nextDouble());
					costs.add( costsMax * generator.nextDouble());
					
					ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
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
							{	emptyCoreCounter[j] += 1;
								continue;
							}
						}
						AllocationEC allocation = (AllocationEC)auction.getAllocation();
						
						int numberOfAllocatedAgents = 0;
						if(allocation.getNumberOfAllocatedAuctioneers() > 0)
							numberOfAllocatedAgents = allocation.getBiddersInvolved(0).size();
						
						if(numberOfAllocatedAgents == 2)
						{
							double realizedValue1 = marginalValue1 * allocation.getRealizedRV(0, 0);
							double realizedValue2 = marginalValue2 * allocation.getRealizedRV(0, 1);
							double[] payments = auction.getPayments();
							
							efficiency[j] += realizedValue1 + realizedValue2
										  - costs.get(0) * allocation.getRealizedRV(0, 0) - costs.get(1) * allocation.getRealizedRV(0, 1);
							
							if(realizedValue1 - payments[0] < 0)
							{
								System.out.println(">> " + bids.toString() );
								System.out.println(">> marginalValue1= " + marginalValue1 );
								System.out.println(">> RVs: " + allocation.getRealizedRV(0).toString());
								System.out.println(">> costs: " + costs.toString());
								System.out.println(">> p[0]="+payments[0] + " p[1]="+payments[1]   );
								irViolation[j]+=1;
							}
							if(realizedValue2 - payments[1] < 0)
								irViolation[j]+=1;
						}
						else if (numberOfAllocatedAgents == 1)
						{
							double realizedValue = marginalValue3 * allocation.getRealizedRV(0, 0);
							double realizedCosts = (costs.get(0) + costs.get(1)) * allocation.getRealizedRV(0, 0);
							double[] payments = auction.getPayments();
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
			effMean/= numberOfRuns;
			emptyCoreMean /= numberOfRuns;
			emptyCoreMean /=numberOfSampleGames;
			
			
			double effStdErr = 0.;
			for(int j = 0; j < numberOfRuns; ++j)
				effStdErr += Math.pow(efficiency[j] - effMean, 2);

			effStdErr /= numberOfRuns;
			effStdErr = Math.sqrt(effStdErr);
			effStdErr = effStdErr / Math.sqrt(numberOfRuns);

			
			double irStdErr = 0.;
			for(int j = 0; j < numberOfRuns; ++j)
				irStdErr += Math.pow(irViolation[j] - irMean, 2);

			irStdErr /= numberOfRuns;
			irStdErr = Math.sqrt(irStdErr);
			irStdErr = irStdErr / Math.sqrt(numberOfRuns);

			
			double emptyCoreStdErr = 0.;
			for(int j = 0; j < numberOfRuns; ++j)
				emptyCoreStdErr += Math.pow(emptyCoreCounter[j] - emptyCoreMean, 2);

			emptyCoreStdErr /= numberOfRuns;
			emptyCoreStdErr = Math.sqrt(emptyCoreStdErr);
			emptyCoreStdErr = emptyCoreStdErr / Math.sqrt(numberOfRuns);

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
