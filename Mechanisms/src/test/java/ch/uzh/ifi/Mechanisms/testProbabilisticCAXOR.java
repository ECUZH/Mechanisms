package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

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


public class testProbabilisticCAXOR {

	/*
	 * EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECVCG_LLG() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = Arrays.asList(1, 2);
		
		//Local bidder 
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.15;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 1.0;
		double secondaryReductionCoef = 0.5;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombingStrategies = new LinkedList<IBombingStrategy>();
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		bombingStrategies.add(b);
		jpmf.setBombs(bombingStrategies, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0375 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			if((Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6))
			{
				assertTrue( allocation.getRealizedRVsPerGood(0).size() == 2 );
				assertTrue( allocation.getRealizedRVsPerGood(0).get(0) == 1.-primaryReductionCoef );
			}
			else if ((Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6))
			{
				assertTrue( allocation.getRealizedRVsPerGood(0).size() == 2 );
				assertTrue( allocation.getRealizedRVsPerGood(0).get(0) == 1.-secondaryReductionCoef );
			}
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			if( allocation.getRealizedRV(0, 0) == 0.5 )
				assertTrue( Math.abs( payments[0] - 0.025 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 0) == 0. )
				assertTrue( Math.abs( payments[0] - 0. ) < 1e-4);
			
			if( allocation.getRealizedRV(0, 1) == 0.5 )
				assertTrue( Math.abs( payments[1] - 0.05 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 1) == 0. )
				assertTrue( Math.abs( payments[1] - 0. ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	/*
	 * EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECCVCG_LLG() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.15;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 1.0;
		double secondaryReductionCoef = 0.5;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("ECC-VCG_LLG");
		auction.setSolver(cplexSolver);
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0375 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			if( allocation.getRealizedRV(0, 0) == 0.5 )
				assertTrue( Math.abs( payments[0] - 0.025 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 0) == 0. )
				assertTrue( Math.abs( payments[0] - 0. ) < 1e-4);
			
			if( allocation.getRealizedRV(0, 1) == 0.5 )
				assertTrue( Math.abs( payments[1] - 0.05 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 1) == 0. )
				assertTrue( Math.abs( payments[1] - 0. ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECRVCG_LLG() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.15;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 1.0;
		double secondaryReductionCoef = 0.5;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("ECR-VCG_LLG");
		auction.setSolver(cplexSolver);
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0375 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			if( allocation.getRealizedRV(0, 0) == 0.5 )
				assertTrue( Math.abs( payments[0] - 0.025 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 0) == 0. )
				assertTrue( Math.abs( payments[0] - 0. ) < 1e-4);
			
			if( allocation.getRealizedRV(0, 1) == 0.5 )
				assertTrue( Math.abs( payments[1] - 0.05 ) < 1e-4);
			else if( allocation.getRealizedRV(0, 1) == 0. )
				assertTrue( Math.abs( payments[1] - 0. ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECVCG_LLG1() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.35;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.14 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			assertTrue( allocation.getRealizedRV(0, 0) == 0.7);
			assertTrue( allocation.getRealizedRVsPerGood(0).size() == 2 );
			assertTrue( (allocation.getRealizedRVsPerGood(0).get(0) == (1.-primaryReductionCoef)) || (allocation.getRealizedRVsPerGood(0).get(0) == (1.-secondaryReductionCoef)));
			if( allocation.getRealizedRVsPerGood(0).get(0) == (1.-primaryReductionCoef) )
				assertTrue(allocation.getRealizedRVsPerGood(0).get(1) == (1.-secondaryReductionCoef));
			else if( allocation.getRealizedRVsPerGood(0).get(0) == (1.-secondaryReductionCoef) )
				assertTrue(allocation.getRealizedRVsPerGood(0).get(1) == (1.-primaryReductionCoef));
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			//System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[0]);
			if( allocation.getRealizedRV(0, 0) == 0.7 )						//Min{0.7, 0.8}
				assertTrue( Math.abs( payments[0] - 0.2175 ) < 1e-4);
			//else if( allocation.getRealizedRV(0, 0) == 0.8 )
			//	assertTrue( Math.abs( payments[0] - 0.2225 ) < 1e-4);
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue(!allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECCVCG_LLG1() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.35;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.14 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[0]);
			assertTrue(allocation.getRealizedRV(0, 0) == 0.7);
			
			if( allocation.getRealizedRVsPerGood(0).get(0) == 0.7 )						//Min{0.7, 0.8}
				assertTrue( Math.abs( payments[0] - 0.22 ) < 1e-4);
			else if( allocation.getRealizedRVsPerGood(0).get(0) == 0.8 )
				assertTrue( Math.abs( payments[0] - 0.215 ) < 1e-4);
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue(!allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
	}
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECRVCG_LLG1() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.35;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy( grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECR-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.14 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[0]);
			assertTrue(allocation.getRealizedRV(0, 0) == 0.7);
			
			if( allocation.getRealizedRVsPerGood(0).get(0) == 0.7 )						//Min{0.7, 0.8}
				assertTrue( Math.abs( payments[0] - 0.22 ) < 1e-4);
			else if( allocation.getRealizedRVsPerGood(0).get(0) == 0.8 )
				assertTrue( Math.abs( payments[0] - 0.215 ) < 1e-4);
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue(!allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace(); 
		}
	}
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECVCG_LLG2() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[1]);
			if( allocation.getRealizedRV(0, 0) == 0.7 )
			{
				assertTrue( Math.abs( payments[0] - 0.065 ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.1475) < 1e-4);
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{	
				assertTrue( Math.abs( payments[0] - 0.07  ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.1375) < 1e-4);
			}
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
		}
	}
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECCVCG_LLG2() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
	
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			if( allocation.getRealizedRV(0, 0) == 0.7 )
			{
				assertTrue( Math.abs( payments[0] - 0.06 ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.15) < 1e-4);
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{	
				assertTrue( Math.abs( payments[0] - 0.075 ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.135) < 1e-4);
			}
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
		}
	}
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECRVCG_LLG2() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECR-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			if( allocation.getRealizedRV(0, 0) == 0.7 )
			{
				assertTrue( Math.abs( payments[0] - 0.06 ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.15) < 1e-4);
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{	
				assertTrue( Math.abs( payments[0] - 0.075 ) < 1e-4);
				assertTrue( Math.abs( payments[1] - 0.135) < 1e-4);
			}
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
		}
	}
	
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECCVCG_LLG3() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.02;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.21);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., 0.4, 0.3);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b1);
		bombs.add(b2);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(0.5);
		pd.add(0.5);
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.035 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			System.out.println(">> " + allocation.getRealizedRV(0, 0) + " payments[0]="+payments[0]);
			if( allocation.getRealizedRV(0, 0) == 0.6 )
			{
				assertTrue( Math.abs( payments[0] - 0.054 ) < 1e-4);
			}
			else if( allocation.getRealizedRV(0, 0) == 0.7 )
			{	
				assertTrue( Math.abs( payments[0] - 0.061 ) < 1e-4);
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{	
				assertTrue( Math.abs( payments[0] - 0.068 ) < 1e-4);
			} 
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
		}
	}
	
	/*
	 * Another EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testECRVCG_LLG3() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.02;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.21);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., 0.4, 0.3);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b1);
		bombs.add(b2);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(0.5);
		pd.add(0.5);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECR-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.035 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			System.out.println(">> " + allocation.getRealizedRVsPerGood(0).get(0) + " " + allocation.getRealizedRVsPerGood(0).get(1) + " payments[0]="+payments[0]);
			if( allocation.getRealizedRVsPerGood(0).get(0) == 0.6 && allocation.getRealizedRVsPerGood(0).get(1) == 0.7 )
			{
				assertTrue( Math.abs( payments[0] - 0.054 ) < 1e-4);
			}
			else if( allocation.getRealizedRVsPerGood(0).get(0) == 0.7 && allocation.getRealizedRVsPerGood(0).get(1) == 0.6 )
			{	
				assertTrue( Math.abs( payments[0] - 0.059 ) < 1e-4);
			}
			else if( allocation.getRealizedRVsPerGood(0).get(0) == 0.7 && allocation.getRealizedRVsPerGood(0).get(1) == 0.8 )
			{	
				assertTrue( Math.abs( payments[0] - 0.063 ) < 1e-4);
			} 
			else if( allocation.getRealizedRVsPerGood(0).get(0) == 0.8 && allocation.getRealizedRVsPerGood(0).get(1) == 0.7 )
			{
				assertTrue( Math.abs( payments[0] - 0.068 ) < 1e-4);
			}
			else throw new RuntimeException("Wrong realized value");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{ 
			e.printStackTrace();
		}
	}
	
	/*
	 * EC-VCG for LLG domain with uncertainty.
	 * Assuming jointly distributed availabilities of individual goods.
	 */
	@Test
	public void testExpVCG_LLG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.15;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 1.0;
		double secondaryReductionCoef = 0.5;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution );
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("Exp-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0375 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			System.out.println(">> " + payments[0]);
			assertTrue( Math.abs( payments[0] - 0.0125 ) < 1e-4);
			assertTrue( Math.abs( payments[1] - 0.025 ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	@Test
	public void testExpVCG_LLG1() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.35;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("Exp-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.14 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 1);
			
			//System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[0]);
			assertTrue( Math.abs( payments[0] - 0.2175 ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue(!allocation.isAllocated(1) );
			assertTrue(!allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	
	@Test
	public void testExpVCG_LLG2() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.3;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("Exp-VCG_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println("Realized value = "  + allocation.getRealizedRV(0, 0) + " p="+payments[0]);
			assertTrue( Math.abs( payments[0] - 0.0675 ) < 1e-4);
			assertTrue( Math.abs( payments[1] - 0.1425) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 */
	@Test
	public void testECCORE_LLG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.3;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("EC-CORE_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println("payments[0]="+payments[0] + " payments[1]=" + payments[1]);
			//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0] + " p[1]="+payments[1]);
			if( allocation.getRealizedRV(0, 0) == 0.7 )
			{
				double vcgPayments[] = {0.065, 0.1475};
				assertTrue( payments[0] >= vcgPayments[0] );
				assertTrue( payments[1] >= vcgPayments[1] );
				assertTrue( Math.abs( payments[0] + payments[1] - 0.22 ) < 1e-4);

				assertTrue( Math.abs(payments[0] - 0.07) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.15) < 1e-4 );
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{
				double vcgPayments[] = {0.07, 0.1375};
				assertTrue( payments[0] >= vcgPayments[0] );
				assertTrue( payments[1] >= vcgPayments[1] );
				assertTrue( Math.abs( payments[0] + payments[1] - 0.215 ) < 1e-4);
				
				assertTrue( Math.abs(payments[0] - 0.07375) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.14124) < 1e-4 );
			}
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace();
		}
	}

	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 */
	@Test
	public void testExpCORE_LLG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.3;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("Exp-CORE_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			//System.out.println("Test sw = " + allocation.getExpectedWelfare() );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.1125 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
			assertTrue( (Math.abs(allocation.getRealizedRV(0, 1) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 1) - (1.-secondaryReductionCoef)) < 1e-6) );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println("payments[0]="+payments[0] + " payments[1]=" + payments[1]);
			//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0] + " p[1]="+payments[1]);
			assertTrue( Math.abs(payments[0] - 0.07125) < 1e-4 );
			assertTrue( Math.abs(payments[1] - 0.14625) < 1e-4 );
			
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace(); 
		}
	}

	
	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 */
	@Test
	public void testECCCORE_LLG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.3;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);

		IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., 0.4, 0.3);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b1);
		bombs.add(b2);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(0.5);
		pd.add(0.5);
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("ECC-CORE_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.105 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " " + allocation.getRealizedRV(0, 1) + " p[0]="+payments[0] + " p[1]="+payments[1]);
			if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.8) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.065) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.155) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.8) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0775) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1375) < 1e-4 );
			} 
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.6) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.055) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.135) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.6) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0675) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1175) < 1e-4 );
			}
			else throw new RuntimeException("Wrong realized a vailabilities");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace(); 
		}
	}
	
	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 */
	@Test
	public void testECCCORE_LLG_ConstraintGeneration() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.3;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., 0.4, 0.3);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b1);
		bombs.add(b2);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(0.5);
		pd.add(0.5);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.105 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			System.out.println(">> " + allocation.getRealizedRV(0, 0) + " " + allocation.getRealizedRV(0, 1) + " p[0]="+payments[0] + " p[1]="+payments[1]);
			if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.8) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.065) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.155) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.8) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0775) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1375) < 1e-4 );
			} 
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.6) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.055) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.135) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.6) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0675) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1175) < 1e-4 );
			}
			else throw new RuntimeException("Wrong realized a vailabilities");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace(); 
		}
	}
	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 */
	@Test
	public void testECRCORE_LLG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.3;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b1 = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		IBombingStrategy b2 = new FocusedBombingStrategy(grid, 1., 0.4, 0.3);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b1);
		bombs.add(b2);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(0.5);
		probDistribution.add(0.5);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setPaymentRule("ECR-CORE_LLG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.105 ) < 1e-4);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " " + allocation.getRealizedRV(0, 1) + " p[0]="+payments[0] + " p[1]="+payments[1]);
			if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.8) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.065) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.155) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.8) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0775) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1375) < 1e-4 );
			} 
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.6) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.7) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.055) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.135) < 1e-4 );
			}
			else if( Math.abs(allocation.getRealizedRV(0, 0) - 0.7) < 1e-4 && Math.abs(allocation.getRealizedRV(0, 1) - 0.6) < 1e-4  )
			{
				assertTrue( Math.abs(payments[0] - 0.0675) < 1e-4 );
				assertTrue( Math.abs(payments[1] - 0.1175) < 1e-4 );
			}
			else throw new RuntimeException("Wrong realized a vailabilities");
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue(!allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace(); 
		}
	}
		
	/*
	 * EC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 * Test the Constraints Generation algorithm. A single blocking coalition.
	 */
	@Test
	public void testECCORE_LLG1_ConstraintGeneration() throws Exception
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.1;
		double marginalValueL2 = 0.2;
		double marginalValueG  = 0.4;
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("EC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
		}
		catch (Exception e)
		{
			if( e.getMessage().equals("VCG is in the Core") )
			{
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				//System.out.println("Test sw = " + allocation.getExpectedWelfare() );
				assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.175 ) < 1e-4);
				
				//Get allocated buyers involved in the trade 
				assertTrue( allocation.getBiddersInvolved(0).size() == 1);
				assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
				
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );
				
				assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
				
				double[] payments = auction.getPayments();
				assertTrue( payments.length == 1);
				
				//System.out.println("payments[0]="+payments[0] + " payments[1]=" + payments[1]);
				//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0] + " p[1]="+payments[1]);
				if( allocation.getRealizedRV(0, 0) == 0.7 )
				{
					assertTrue( Math.abs(payments[0] - 0.2175) < 1e-4 );				
				}
				
				assertTrue( allocation.isAllocated(0) );
				assertTrue( !allocation.isAllocated(1) );
				assertTrue( !allocation.isAllocated(2) );
				assertTrue( allocation.isAllocated(3) );
				return;
			}	
		}
		assertTrue(false); 	//PaymentException must be thrown
	}
	
	/**
	 * ECC-CORE for LLG domain with uncertainty and dependent availabilities of goods.
	 * Test the Constraints Generation algorithm. A single blocking coalition.
	 * A single local winning bidder.
	 */
	@Test
	public void testECCCORE_LLG2_ConstraintGeneration() throws Exception 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		double marginalValueL1 = 0.3867;
		double marginalValueL2 = 0.7139;
		double marginalValueG  = 0.9067;
		
		//Local bidder
		List<Integer> bundle = Arrays.asList( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//Local bidder
		bundle = Arrays.asList( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = Arrays.asList( items.get(0), items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = Arrays.asList(t1, t2, t3);
		
		List<Double> costs = Arrays.asList(0.7275, 0.6832);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 2);
		assertTrue(grid.getAdjacencyLists().size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 1);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = Arrays.asList(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			throw new RuntimeException("VCG should be in the core for this setting");
		}
		catch (PaymentException e)
		{
			if( e.getMessage().equals("VCG is in the Core"))
			{
				System.out.println("VCG is in the Core");
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				assertTrue(allocation.getBiddersInvolved(0).size() == 1);
				assertTrue(allocation.getBiddersInvolved(0).get(0) == 2);
				
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0230 ) < 1e-4);
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );
				
				double[] payments = auction.getPayments();
				assertTrue(payments.length == 1);
				
				if(allocation.getRealizedRV(0, 0) == 0.7)
					assertTrue( Math.abs( payments[0] - 0.6832*0.7) < 1e-4 );
				else if(allocation.getRealizedRV(0, 0) == 0.8)
					assertTrue( Math.abs( payments[0] - 0.6832*0.8) < 1e-4 );
				else throw new RuntimeException("Incorrect realization of RV");
				
				assertTrue( allocation.isAllocated(0) );
				assertTrue( !allocation.isAllocated(1) );
				assertTrue( allocation.isAllocated(2) );
				assertTrue( !allocation.isAllocated(3) );
			}
			else
				e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
	@Test
	public void testECCORE_CATS1_ConstraintGeneration() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		items.add(3);
		items.add(4);
		
		double marginalValueL1 = 7.5314;
		double marginalValueL2 = 32.6815;
		double marginalValueG  = 74.3865;
		
		//1st  bidder
		List<Integer> bundle = Arrays.asList( items.get(3) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValueL1);
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);

		//2nd bidder
		bundle = Arrays.asList( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValueL2);
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//3rd bidder
		bundle = Arrays.asList( items.get(0) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValueG);
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(37.3411);
		costs.add(11.5589);
		costs.add(34.7386);
		costs.add( 4.3435);
		
		GridGenerator generator = new GridGenerator(2, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		assertTrue(grid.getVertices().size() == 4);
		assertTrue(grid.getAdjacencyLists().size() == 4);
		assertTrue(grid.getAdjacencyLists().get(0).size() == 2);
		assertTrue(grid.getAdjacencyLists().get(0).get(0)._v.getID()==2);
		assertTrue(grid.getAdjacencyLists().get(0).get(1)._v.getID()==3);
		assertTrue(grid.getAdjacencyLists().get(1).size() == 2);
		assertTrue(grid.getAdjacencyLists().get(1).get(0)._v.getID()==1);
		assertTrue(grid.getAdjacencyLists().get(1).get(1)._v.getID()==4);
		//etc.
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = Arrays.asList(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		List<Integer> bundle1 = new LinkedList<Integer>();
		
		for(int i = 1; i <= 4; ++i)
		{
			bundle1.add(i);
			assertTrue( Math.abs(jpmf.getMarginalProbability(bundle1, null, null) - 0.825) < 1e-3);
			bundle1.clear();
		}
		
		bundle1.add(1);
		bundle1.add(2);
		assertTrue( Math.abs(jpmf.getMarginalProbability(bundle1, null, null) - 0.75) < 1e-3);

		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
		}
		catch (PaymentException e)
		{
			if(e.getMessage().equals("VCG is in the Core"))
			{
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				
				System.out.println("Test sw = " + allocation.getExpectedWelfare() );
				assertTrue( allocation.getBiddersInvolved(0).size() == 3 );
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 50.61738 ) < 1e-1);
				
				//Get allocated buyers involved in the trade 
				assertTrue( allocation.getBiddersInvolved(0).size() == 3);
				assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
				assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
				assertTrue( allocation.getBiddersInvolved(0).get(2) == 3 );
				
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 3 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(2) == 0 );
				
				//assertTrue( (Math.abs(allocation.getRealizedRV(0, 0) - (1.-primaryReductionCoef)) < 1e-6) || (Math.abs(allocation.getRealizedRV(0, 0) - (1.-secondaryReductionCoef)) < 1e-6) );
				
				double[] payments;
				try 
				{
					payments = auction.getPayments();
					assertTrue( payments.length == 3);
					//System.out.println("payments[0]="+payments[0]);
					//System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0]);
					if( allocation.getRealizedRV(0, 0) == 0.7 )
					{
						assertTrue( Math.abs(payments[0] - 3.04045) < 1e-4 );				
					}
					else if( allocation.getRealizedRV(0, 0) == 0.8 )
					{
						assertTrue( Math.abs(payments[0] - 3.4748) < 1e-4 );
					}
					else if( allocation.getRealizedRV(0, 0) == 1.0 )
					{
						assertTrue( Math.abs(payments[0] - 4.3435) < 1e-4 );
					}
					
					assertTrue( allocation.isAllocated(0) );
					assertTrue( allocation.isAllocated(1) );
					assertTrue( allocation.isAllocated(2) );
					assertTrue( allocation.isAllocated(3) );
				} 
				catch (Exception e1) 
				{
					e1.printStackTrace();
				}
			}
			else
				e.printStackTrace();
		}
		catch( Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
	@Test
	public void testECVCG_CATS2_ConstraintGeneration() throws IloException 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		items.add(3);
		items.add(4);
		
		double marginalValue11 = 141.524;
		double marginalValue12 = 53.1275;
		double marginalValue21 = 178.591;
		double marginalValue22 = 51.2827;
		double marginalValue31 = 34.8635;
		double marginalValue32 = 108.519;
		
		//1st  bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		bundle.add( items.get(2) );
		bundle.add( items.get(3) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue11);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue12);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);

		//2nd bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		bundle.add( items.get(2) );
		bundle.add( items.get(3) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue21);

		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue22);

		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//3rd bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(3) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue31);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(3) );
		bundle.add( items.get(0) );
		AtomicBid atom32 = new AtomicBid(3, bundle, marginalValue32);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		t3.addAtomicBid(atom32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(36.25);
		costs.add(21.52);
		costs.add(13.85);
		costs.add( 9.04);
		
		GridGenerator generator = new GridGenerator(2, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);

		List<Double> pd = new LinkedList<Double>();
		pd.add(1.0);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("EC-VCG");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			System.out.println("Test sw = " + allocation.getExpectedWelfare() );
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 73.497 ) < 1e-1);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).size() == 2);
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 1 );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			System.out.println("payments[0]="+payments[0]);
			System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0]);
			if( allocation.getRealizedRV(0, 0) == 0.7 )
			{
				assertTrue( Math.abs(payments[0] - 36.19395) < 1e-1 );				
			}
			else if( allocation.getRealizedRV(0, 0) == 0.8 )
			{
				assertTrue( Math.abs(payments[0] - 38.34595) < 1e-1 );
			}
			else if( allocation.getRealizedRV(0, 0) == 1.0 )
			{
				assertTrue( Math.abs(payments[0] - 42.6499) < 1e-1 );
			}
			
			System.out.println(">> " + allocation.getRealizedRV(0, 1) + " p[1]="+payments[1]);
			if( allocation.getRealizedRV(0, 1) == 0.7 )
			{
				assertTrue( Math.abs(payments[1] - 74.1785) < 1e-1 );				
			}
			else if( allocation.getRealizedRV(0, 1) == 0.8 )
			{
				assertTrue( Math.abs(payments[1] - 78.7075) < 1e-1 );
			}
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( !allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (Exception e)
		{ 
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 */
	@Test
	public void testECCCORE_CATS2_ConstraintGeneration() throws Exception 
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		items.add(3);
		items.add(4);
		
		double marginalValue11 = 141.524;
		double marginalValue12 = 53.1275;
		double marginalValue21 = 178.591;
		double marginalValue22 = 51.2827;
		double marginalValue31 = 34.8635;
		double marginalValue32 = 108.519;
		
		//1st  bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		bundle.add( items.get(2) );
		bundle.add( items.get(3) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue11);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue12);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);

		//2nd bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		bundle.add( items.get(2) );
		bundle.add( items.get(3) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue21);

		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue22);

		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//3rd bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(3) );
		bundle.add( items.get(1) );
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue31);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(3) );
		bundle.add( items.get(0) );
		AtomicBid atom32 = new AtomicBid(3, bundle, marginalValue32);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		t3.addAtomicBid(atom32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(36.25);
		costs.add(21.52);
		costs.add(13.85);
		costs.add( 9.04);
		
		GridGenerator generator = new GridGenerator(2, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(10000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1, primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> probDistribution = new LinkedList<Double>();
		probDistribution.add(1.);
		
		jpmf.setBombs(bombs, probDistribution);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			System.out.println("Test sw = " + allocation.getExpectedWelfare() );
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 73.497 ) < 1e-1);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).size() == 2);
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );
			
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
			assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 1 );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			
			System.out.println("payments[0]="+payments[0]);
			System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0]);
			System.out.println(">> " + allocation.getRealizedRV(0, 1) + " p[1]="+payments[1]);
			if( allocation.getRealizedRV(0, 0) == 0.8 && allocation.getRealizedRV(0, 1) == 0.7 )
			{
				assertTrue( Math.abs(payments[0] - 42.005) < 1e-2 );
				assertTrue( Math.abs(payments[1] - 75.465) < 1e-2 );
			}
			else if( allocation.getRealizedRV(0, 0) == 0.7 && allocation.getRealizedRV(0, 1) == 0.8 )
			{
				assertTrue( Math.abs(payments[0] - 35.11) < 1e-2 );
				assertTrue( Math.abs(payments[1] - 84.73) < 1e-2 );
			}
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( !allocation.isAllocated(2) );
			assertTrue( allocation.isAllocated(3) );
		}
		catch (PaymentException e)
		{
			System.out.println("Payment Exception.");
			if(e.getMessage().equals("Empty Core"))
			{
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				
				System.out.println("Test sw = " + allocation.getExpectedWelfare() );
				assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 73.497 ) < 1e-1);
				
				//Get allocated buyers involved in the trade 
				assertTrue( allocation.getBiddersInvolved(0).size() == 2);
				assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
				assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );
				
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 1 );
				
				double[] payments = auction.getPayments();
				assertTrue( payments.length == 2);
				
				System.out.println("payments[0]="+payments[0]);
				System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0]);
				System.out.println(">> " + allocation.getRealizedRV(0, 1) + " p[1]="+payments[1]);
				assertTrue(allocation.getRealizedRV(0, 0) == 1.0 && allocation.getRealizedRV(0, 1) == 0.8);
				
				//VCG
				assertTrue( Math.abs(payments[0] - 42.6499) < 1e-1 );
				assertTrue( Math.abs(payments[1] - 78.70) < 1e-1 );
				
				assertTrue( allocation.isAllocated(0) );
				assertTrue( allocation.isAllocated(1) );
				assertTrue( !allocation.isAllocated(2) );
				assertTrue( allocation.isAllocated(3) );
			}
			else if(e.getMessage().equals("VCG is in the Core"))
			{
				System.out.println("VCG in the core");
				
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				System.out.println("Test sw = " + allocation.getExpectedWelfare() );
				assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 73.497 ) < 1e-1);
				
				//Get allocated buyers involved in the trade 
				assertTrue( allocation.getBiddersInvolved(0).size() == 2);
				assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
				assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );
				
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 1 );
				
				double[] payments = auction.getPayments();
				assertTrue( payments.length == 2);
				
				System.out.println("payments[0]="+payments[0]);
				System.out.println(">> " + allocation.getRealizedRV(0, 0) + " p[0]="+payments[0]);
				System.out.println(">> " + allocation.getRealizedRV(0, 1) + " p[1]="+payments[1]);
				if( allocation.getRealizedRV(0, 0) == 0.8 && allocation.getRealizedRV(0, 1) == 0.7 )
				{
					assertTrue( Math.abs(payments[0] - 40.82) < 1e-1 );
					assertTrue( Math.abs(payments[1] - 76.65) < 1e-1 );
				}
				else if( allocation.getRealizedRV(0, 0) == 0.7 && allocation.getRealizedRV(0, 1) == 0.8 )
				{
					assertTrue( Math.abs(payments[0] - 38.66695) < 1e-1 );
					assertTrue( Math.abs(payments[1] - 81.17305) < 1e-1 );
				}
				
				assertTrue( allocation.isAllocated(0) );
				assertTrue( allocation.isAllocated(1) );
				assertTrue( !allocation.isAllocated(2) );
				assertTrue( allocation.isAllocated(3) );
			}
		}
		catch (Exception e)
		{ 
			System.out.println("Exception.");
			e.printStackTrace();
		}
	}


	/**
	 * ECC-CORE for a large domain with uncertainty and dependent availabilities of goods.
	 * Test the Constraints Generation algorithm. A single local winning bidder.
	 */
	@Test
	public void testECCCORE_Large_ConstraintGeneration() throws Exception
	{
		IloCplex cplexSolver = new IloCplex();
		List<Integer> items = Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9 );
		
		//Bidder 1
		AtomicBid atom11 = new AtomicBid(1, Arrays.asList( 5, 8 ), 174.1030);
		AtomicBid atom12 = new AtomicBid(1, Arrays.asList( 5, 6 ), 148.8033);
		AtomicBid atom13 = new AtomicBid(1, Arrays.asList( 8, 9 ), 207.3529);
		CombinatorialType t1 = new CombinatorialType(atom11, atom12, atom13);

		//Bidder 2
		AtomicBid atom21 = new AtomicBid(2, Arrays.asList( 6, 9), 156.144);
		AtomicBid atom22 = new AtomicBid(2, Arrays.asList( 6, 8), 107.793);
		CombinatorialType t2 = new CombinatorialType(atom21, atom22);
		
		//Bidder 3
		AtomicBid atom31 = new AtomicBid(3, Arrays.asList( 1 ), 97.014);
		CombinatorialType t3 = new CombinatorialType(atom31);
		
		//Bidder 4
		AtomicBid atom41 = new AtomicBid(4, Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8, 9 ), 581.890);
		CombinatorialType t4 = new CombinatorialType(atom41);
		
		//Bidder 5
		AtomicBid atom51 = new AtomicBid(5, Arrays.asList( 1, 2, 3, 4, 5 ), 314.316);
		AtomicBid atom52 = new AtomicBid(5, Arrays.asList( 2, 5, 6, 7, 8 ), 208.312);
		AtomicBid atom53 = new AtomicBid(5, Arrays.asList( 1, 2, 3, 5, 6 ), 273.986);
		AtomicBid atom54 = new AtomicBid(5, Arrays.asList( 1, 2, 4, 5, 7 ), 278.663);
		CombinatorialType t5 = new CombinatorialType(atom51, atom52, atom53, atom54);
		
		List<Type> bids = Arrays.asList(t1, t2, t3, t4, t5);
		
		List<Double> costs = Arrays.asList(6.346, 3.552, 2.723, 0.262, 9.937, 4.205, 5.280, 7.560, 7.739);
		
		GridGenerator generator = new GridGenerator(3, 3);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		double primaryReductionCoef = 0.3;
		double secondaryReductionCoef = 0.2;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid );
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = Arrays.asList(b);
		
		List<Double> pd = Arrays.asList(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs, jpmf);
		auction.setSolver(cplexSolver);
		auction.setPaymentRule("ECC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			System.out.println("Solved.");

			AllocationEC allocation = (AllocationEC)auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(allocation.getBiddersInvolved(0).size() == 2);
			assertTrue(allocation.getBiddersInvolved(0).get(0) == 1);
			assertTrue(allocation.getBiddersInvolved(0).get(1) == 5);
			
			assertTrue(allocation.getAllocatedBundlesOfTrade(0).size() == 2);
			assertTrue(allocation.getAllocatedBundlesOfTrade(0).get(0) == 2);
			assertTrue(allocation.getAllocatedBundlesOfTrade(0).get(1) == 0);
			
			System.out.println("> " + allocation.getRealizedRV(0).toString());
		}
		catch (PaymentException e)
		{
			if( e.getMessage().equals("VCG is in the Core"))
			{
				System.out.println("VCG is in the Core");
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
				assertTrue(allocation.getBiddersInvolved(0).size() == 1);
				assertTrue(allocation.getBiddersInvolved(0).get(0) == 2);
				
				assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.0230 ) < 1e-4);
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
				assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );
				
				double[] payments = auction.getPayments();
				assertTrue(payments.length == 1);
				
				if(allocation.getRealizedRV(0, 0) == 0.7)
					assertTrue( Math.abs( payments[0] - 0.6832*0.7) < 1e-4 );
				else if(allocation.getRealizedRV(0, 0) == 0.8)
					assertTrue( Math.abs( payments[0] - 0.6832*0.8) < 1e-4 );
				else throw new RuntimeException("Incorrect realization of RV");
				
				assertTrue( allocation.isAllocated(0) );
				assertTrue( !allocation.isAllocated(1) );
				assertTrue( allocation.isAllocated(2) );
				assertTrue( !allocation.isAllocated(3) );
			}
			else if(e.getMessage().equals("Empty Core"))
			{
				System.out.println("Empty Core");
				AllocationEC allocation = (AllocationEC)auction.getAllocation();
				assertTrue(allocation.getRealizedRV(0, 0)==0.8);
				assertTrue(allocation.getRealizedRV(0, 1)==0.7);
			}
			else
				e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * EC-CORE for LLG domain (empty core) 
	 */
	/*@Test
	public void testECCORE_LLG1() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom11.setTypeComponent("LowerBound", 0.0);
		atom11.setTypeComponent("UpperBound", 1.0);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.1;
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue);
		atom12.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom12.setTypeComponent("LowerBound", 0.0);
		atom12.setTypeComponent("UpperBound", 1.0);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent("LowerBound", 0.0);
		atom21.setTypeComponent("UpperBound", 1.0);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue);
		atom22.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom22.setTypeComponent("LowerBound", 0.0);
		atom22.setTypeComponent("UpperBound", 1.0);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.22;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent("LowerBound", 0.0);
		atom31.setTypeComponent("UpperBound", 2.0);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-CORE_LLG");
		auction.setSeed(514161);
		
		try
		{
			auction.solveIt();
			Allocation allocation = auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.075 ) < 1e-6);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesById(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesById(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesById(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( Math.abs(allocation.getRealizedRV(0, 0) - 0.00019537592) < 1e-6 );
			assertTrue( Math.abs(allocation.getRealizedRV(0, 1) - 0.000582061786) < 1e-6 );
			
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0)) < 1e-3);
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1)) < 1e-3);
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			assertTrue( Math.abs( payments[0] + payments[1] - 0.07 ) < 1e-3);
			assertTrue( payments[0] >= 0.020);
			assertTrue( payments[1] >= 0.045);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue( !allocation.isAllocated(3));
		}
		catch (Exception e)  
		{
			assertTrue(e.getMessage().equals("Empty Core"));
				
			//e.printStackTrace();
		}
	}*/
	
	
	/*
	 * EC-CORE for LLG domain (empty core)
	 */
	/*@Test
	public void testDomainGeneration() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom11.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom11.setTypeComponent( AtomicBid.UpperBound, 1.0);
		atom11.setTypeComponent( AtomicBid.MinValue, 0.0);
		atom11.setTypeComponent( AtomicBid.MaxValue, 1.0);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom21.setTypeComponent( AtomicBid.UpperBound, 1.0);
		atom21.setTypeComponent( AtomicBid.MinValue, 0.0);
		atom21.setTypeComponent( AtomicBid.MaxValue, 1.0);
		
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.22;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom31.setTypeComponent( AtomicBid.UpperBound, 1.0);
		atom31.setTypeComponent( AtomicBid.MinValue, 0.0);
		atom31.setTypeComponent( AtomicBid.MaxValue, 2.0);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-CORE_LLG");
		auction.setSeed(514161);
		
		
		DomainGeneratorLLG domainGenerator = new DomainGeneratorLLG();
		domainGenerator.setTypes( t1.getAtoms() );
		List<AtomicBid> newAtoms1 = domainGenerator.resetType(0);
		assertTrue( newAtoms1.size() == 1 );
		
		domainGenerator.setTypes( t2.getAtoms() );
		List<AtomicBid> newAtoms2 = domainGenerator.resetType(1000000);
		
		domainGenerator.setTypes( t3.getAtoms() );
		List<AtomicBid> newAtoms3 = domainGenerator.resetType(0);
		assertTrue( newAtoms3.size() == 1 );		
	}
	*/
	/*
	 * EC-VCG for LLG domain 
	 */
	/*@Test
	public void testVCG() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		double lowerDistributionBound = 1.0;
		double upperDistributionBound = 1.0;
		
		AtomicBid atom11  =  new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal() );
		atom11.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom11.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.1;
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue);
		atom12.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom12.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom12.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom21.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue);
		atom22.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom22.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom22.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.22;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom31.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.//0.05);
		costs.add(0.//0.1);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-VCG");
		//auction.setSeed(514161);
		
		try
		{
			auction.solveIt();
			Allocation allocation = auction.getAllocation();
			assertTrue(allocation.getRealizedRV(0, 0) == 1.0);
			assertTrue(allocation.getRealizedRV(0, 1) == 1.0);
			
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.3 ) < 1e-6);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesById(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesById(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesById(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0) - 0.1) < 1e-3);
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1) - 0.2) < 1e-3);
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			//System.out.println("payments[0] = " + payments[0]);
			assertTrue( Math.abs( payments[0] - 0.0200 ) < 1e-3);
			assertTrue( Math.abs( payments[1] - 0.12 ) < 1e-3);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue( !allocation.isAllocated(3));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	*/
	
	/*
	 * EC-VCG for LLG domain without uncertainty but with costs.
	 */
	/*@Test
	public void testVCG_Costs() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		double lowerDistributionBound = 1.0;
		double upperDistributionBound = 1.0;
		
		AtomicBid atom11  =  new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal() );
		atom11.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom11.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.1;
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue);
		atom12.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom12.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom12.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom21.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue);
		atom22.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom22.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom22.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.22;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent( AtomicBid.LowerBound, lowerDistributionBound );
		atom31.setTypeComponent( AtomicBid.UpperBound, upperDistributionBound );
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-VCG");
		//auction.setSeed(514161);
		
		try
		{
			auction.solveIt();
			Allocation allocation = auction.getAllocation();
			assertTrue(allocation.getRealizedRV(0, 0) == 1.0);
			assertTrue(allocation.getRealizedRV(0, 1) == 1.0);
			
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.15 ) < 1e-6);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesById(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesById(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesById(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0) - 0.1) < 1e-3);
			assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1) - 0.2) < 1e-3);
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			//System.out.println("payments[0] = " + payments[0]);
			assertTrue( Math.abs( payments[0] - 0.05 ) < 1e-3);
			assertTrue( Math.abs( payments[1] - 0.12 ) < 1e-3);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue( !allocation.isAllocated(3));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}*/
	/*
	 * EC-CORE for LLG domain (using EC-CORE payment rule)
	 */
	/*@Test
	public void testECCORE_LLG_General() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.1;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom11.setTypeComponent("LowerBound", 0.0);
		atom11.setTypeComponent("UpperBound", 1.0);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.1;
		AtomicBid atom12 = new AtomicBid(1, bundle, marginalValue);
		atom12.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom12.setTypeComponent("LowerBound", 0.0);
		atom12.setTypeComponent("UpperBound", 1.0);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		t1.addAtomicBid(atom12);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent("LowerBound", 0.0);
		atom21.setTypeComponent("UpperBound", 1.0);
		
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.2;
		AtomicBid atom22 = new AtomicBid(2, bundle, marginalValue);
		atom22.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom22.setTypeComponent("LowerBound", 0.0);
		atom22.setTypeComponent("UpperBound", 1.0);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		t2.addAtomicBid(atom22);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.16;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent("Distribution", Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent("LowerBound", 0.0);
		atom31.setTypeComponent("UpperBound", 2.0);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.05);
		costs.add(0.1);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-CORE");
		auction.setSeed(0);
		try
		{
			auction.solveIt();
			Allocation allocation = auction.getAllocation();
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(Math.abs( allocation.getExpectedWelfare() - 0.075 ) < 1e-6);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesById(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesById(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesById(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			assertTrue( Math.abs(allocation.getRealizedRV(0, 0) - 0.73096778) < 1e-6 );
			assertTrue( Math.abs(allocation.getRealizedRV(0, 1) - 0.24053641) < 1e-6 );
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			assertTrue( Math.abs( payments[0] + payments[1] - 0.0706 ) < 1e-4);
			assertTrue( Math.abs( payments[0] - 0.0415483 ) < 1e-4);
			assertTrue( Math.abs( payments[1] - 0.0290536 ) < 1e-4);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue( !allocation.isAllocated(3));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}*/
	
	/*
	 * EC-VCG for LLG domain 
	 */
	/*@Test
	public void testECVCG_LLG2() 
	{
		List<Integer> items = new LinkedList<Integer>();
		items.add(1);
		items.add(2);
		
		//Local bidder
		List<Integer> bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		double marginalValue = 0.7711603357772624;
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		atom11.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom11.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom11.setTypeComponent( AtomicBid.UpperBound, 1.0);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(atom11);
		
		//Local bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(1) );
		marginalValue = 0.15844990327505515;
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		atom21.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom21.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom21.setTypeComponent( AtomicBid.UpperBound, 1.0);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(atom21);
		
		//Global bidder
		bundle = new LinkedList<Integer>();
		bundle.add( items.get(0) );
		bundle.add( items.get(1) );
		marginalValue = 0.12789031998315137;
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		atom31.setTypeComponent( AtomicBid.Distribution, Distribution.UNIFORM.ordinal());
		atom31.setTypeComponent( AtomicBid.LowerBound, 0.0);
		atom31.setTypeComponent( AtomicBid.UpperBound, 1.0);
		
		CombinatorialType t3 = new CombinatorialType();
		t3.addAtomicBid(atom31);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		bids.add(t3);
		
		List<Double> costs = new LinkedList<Double>();
		costs.add(0.);
		costs.add(0.);
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( bids.size(), items.size(), bids, costs);
		auction.setPaymentRule("EC-VCG");
		auction.setSeed(5161);
		
		try
		{
			auction.solveIt();
			Allocation allocation = auction.getAllocation();
			//assertTrue(allocation.getRealizedRV(0, 0) < 1e-3);
			//assertTrue(allocation.getRealizedRV(0, 1) < 1e-3);
			
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
			System.out.println("SW=" + allocation.getExpectedWelfare());
			assertTrue(Math.abs( allocation.getExpectedWelfare() - (0.15844990327505515 + 0.7711603357772624)*0.5 ) < 1e-6);
			
			//Get allocated buyers involved in the trade 
			assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
			assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
			
			assertTrue( allocation.getAllocatedBundlesById(0).size() == 2 );
			assertTrue( allocation.getAllocatedBundlesById(0).get(0) == 0 );		//First bidder gets its first bid
			assertTrue( allocation.getAllocatedBundlesById(0).get(1) == 0 );		//Second bidder gets its first bid		
			
			//assertTrue( Math.abs(allocation.getRealizedRV(0, 0) - 0.00019537592) < 1e-6 );
			//assertTrue( Math.abs(allocation.getRealizedRV(0, 1) - 0.000582061786) < 1e-6 );
			//assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0)) < 1e-3);
			//assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1)) < 1e-3);
			
			double[] payments = auction.getPayments();
			assertTrue( payments.length == 2);
			System.out.println("payments[0] = " + payments[0]);
			assertTrue( Math.abs( payments[0] - 0.0 ) < 1e-3);
			assertTrue( Math.abs( payments[1] - 0.0 ) < 1e-3);
			
			assertTrue( allocation.isAllocated(0) );
			assertTrue( allocation.isAllocated(1) );
			assertTrue( allocation.isAllocated(2) );
			assertTrue( !allocation.isAllocated(3));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}*/
	
}
