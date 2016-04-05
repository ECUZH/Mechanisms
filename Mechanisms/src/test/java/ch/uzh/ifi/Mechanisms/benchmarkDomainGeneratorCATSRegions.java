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
		int numberOfGoods = 9;
		int numberOfAgents = 5;
		int numberOfSampleGames = 100;
		int numberOfRuns = 1;
		String paymentRule = "CORE"; 
		double costsMax = 1e-6;

		//Create dummy types for all agents.
		List<Type> types = new ArrayList<Type>();
		IntStream.range(0, numberOfAgents).boxed().forEach( i -> types.add( new CombinatorialType( new AtomicBid(i+1, Arrays.asList(0), 0.) ) ) );
			
		double efficiency = 0.;
		int vcgInCoreCounter = 0;
		
		//DomainGeneratorSpatial domainGenerator;
		IDomainGenerator domainGenerator;
		try 
		{
			//domainGenerator = new DomainGeneratorSpatial(numberOfGoods);
			domainGenerator = new DomainGeneratorCATS(numberOfGoods, numberOfAgents);
				
			for(int i = 0; i < numberOfSampleGames; ++i)
			{
				System.out.println("i="+i);
				Random generator = new Random(i);
				generator.setSeed(System.nanoTime());
						
				List<Type> bids = new LinkedList<Type>();
				for(int j = 0; j < numberOfAgents; ++j)
				{
					//Type ct = domainGenerator.generateBid(i*10 + j, types.get(j).getAgentId());
					Type ct = domainGenerator.generateBid(i, types.get(j).getAgentId());
					//System.out.println(">> ct="+ct.toString());
					bids.add(ct);
				}
						
				List<Double> costs = new LinkedList<Double>();
				for(int j = 0; j < numberOfGoods; ++j)
					costs.add( costsMax * generator.nextDouble());
						
				CAXOR auction = new CAXOR( bids.size(), numberOfGoods, bids, costs);
				auction.setPaymentRule(paymentRule);
						
				try
				{
					try
					{
						auction.solveIt();
					}
					catch(PaymentException e)
					{
						if(e.getMessage().equals("VCG is in the Core"))
							vcgInCoreCounter += 1;
					}
	
					Allocation allocation = auction.getAllocation();
					double[] payments = auction.getPayments();
							
					int numberOfAllocatedAgents = 0;
					if(allocation.getNumberOfAllocatedAuctioneers() > 0)
						numberOfAllocatedAgents = allocation.getBiddersInvolved(0).size();
							
					for(int q = 0; q < numberOfAllocatedAgents; ++q)
					{
						int allocatedBidderId = allocation.getBiddersInvolved(0).get(q);
						int allocatedBundleIdx = allocation.getAllocatedBundlesOfTrade(0).get(q);
						double value = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).getValue();
	 
						double realizedCost = bids.get(allocatedBidderId-1).getAtom(allocatedBundleIdx).computeCost(costs);
						efficiency += value - realizedCost;
					}
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
			System.out.println("Eff=" + efficiency);
			System.out.println("VCG in the Core=" + vcgInCoreCounter);
				
			double effMean = efficiency / numberOfRuns;
			System.out.println(effMean);
			
		} 
		catch (SpacialDomainGenerationException e1) 
		{
			e1.printStackTrace();
		}
	}
}
