package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.FocusedBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.IBombingStrategy;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.DomainGenerators.GridGenerator;
import ch.uzh.ifi.GraphAlgorithms.Graph;

public class testCAXOR {

	/**
	 * The test creates a setup with two agents and four items.
	 * Check allocation
	 * Check VCG payments
	 * @throws Exception
	 */
	@Test
	public void testCAXOR_Simple() throws Exception {
		
		int numberOfAgents = 2;
		int numberOfItems = 4;
		List<Double> costs = new ArrayList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			costs.add(0.);
				
		//Create bid 1:
		List<Integer> items1 = Arrays.asList(1);
		List<Integer> items2 = Arrays.asList(2);
		List<Integer> items3 = Arrays.asList(3, 4);
		
		AtomicBid atom1 = new AtomicBid(1, items1, 20);
		AtomicBid atom2 = new AtomicBid(1, items2, 20);
		AtomicBid atom3 = new AtomicBid(1, items3, 10);
		Type bid1 = new CombinatorialType(atom1, atom2, atom3);
		
		//Create bid 2:
		List<Integer> items21 = Arrays.asList(1, 2);
		List<Integer> items22 = Arrays.asList(3);
		
		AtomicBid atom21 = new AtomicBid(2, items21, 35);
		AtomicBid atom22 = new AtomicBid(2, items22, 10);
		Type bid2 = new CombinatorialType(atom21, atom22);
		
		List<Type> bids = Arrays.asList(bid1, bid2);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids, costs);
		ca.computeWinnerDetermination();
		
		Allocation allocation = ca.getAllocation();
		assertTrue( allocation.getNumberOfAllocatedAuctioneers() == 1 );
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 2 );
		
		assertTrue( allocation.getAllocatedWelfare() == 45);
		
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 2);
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 2);
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 0);
		
		List<Double> vcg = ca.computeVCG();		
		assertTrue( vcg.size() == 2 );
		assertTrue( vcg.get(0) == 0);
		assertTrue( vcg.get(1) == 10 );
	}

	@Test
	public void testCAXOR3() throws Exception {
		
		int numberOfAgents = 3;
		int numberOfItems = 4;
		List<Double> costs = new ArrayList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			costs.add(0.);
				 
		//Create bid 1:
		List<Integer> items1 = Arrays.asList(1);
		List<Integer> items2 = Arrays.asList(2);
		List<Integer> items3 = Arrays.asList(3, 4);
		
		AtomicBid atom1 = new AtomicBid(1, items1, 20);
		AtomicBid atom2 = new AtomicBid(1, items2, 20);
		AtomicBid atom3 = new AtomicBid(1, items3, 10);
		Type bid1 = new CombinatorialType(atom1, atom2, atom3);
		
		//Create bid 2:
		List<Integer> items21 = Arrays.asList(1, 2);
		List<Integer> items22 = Arrays.asList(3);
		
		AtomicBid atom21 = new AtomicBid(2, items21, 35);
		AtomicBid atom22 = new AtomicBid(2, items22, 10);
		Type bid2 = new CombinatorialType(atom21, atom22);
		
		//Create bid 3:
		List<Integer> items31 = Arrays.asList(1);
		List<Integer> items32 = Arrays.asList(2);
		List<Integer> items33 = Arrays.asList(3, 4);
		List<Integer> items34 = Arrays.asList(1, 2);
		List<Integer> items35 = Arrays.asList(3);
		
		AtomicBid atom31 = new AtomicBid(3, items31, 20);
		AtomicBid atom32 = new AtomicBid(3, items32, 20);
		AtomicBid atom33 = new AtomicBid(3, items33, 20);
		AtomicBid atom34 = new AtomicBid(3, items34, 40);
		AtomicBid atom35 = new AtomicBid(3, items35, 10);
		Type bid3 = new CombinatorialType(atom31, atom32, atom33, atom34, atom35);
		
		List<Type> bids = Arrays.asList(bid1, bid2, bid3);
		
		List<Double> reservePrice = Arrays.asList(20., 20., 10., 10.);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids, costs);
		//ca.setupReservePrices(reservePrice);
		ca.computeWinnerDetermination();
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getNumberOfAllocatedAuctioneers() == 1 );
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );

		assertTrue( allocation.getAllocatedWelfare() == 55 );
		
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 0 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(1) == 2 );
		
		List<Double> vcg = ca.computeVCG();
		assertTrue( vcg.size() == 2 );
		assertTrue( vcg.get(0) == 30);
		assertTrue( vcg.get(1) == 10 );
	}
	
	@Test
	public void testReservedPrice() throws Exception {
		
		int numberOfAgents = 2;
		int numberOfItems = 4;
		List<Double> costs = new LinkedList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			costs.add(0.);
		
		
		//Create bid 1:
		List<Integer> items1 = new LinkedList<Integer>();
		items1.add(1);
		List<Integer> items2  = new LinkedList<Integer>();
		items2.add(2);
		List<Integer> items3  = new LinkedList<Integer>();
		items3.add(3);
		items3.add(4);
		
		AtomicBid atom1 = new AtomicBid(1, items1, 20);
		AtomicBid atom2 = new AtomicBid(1, items2, 20);
		AtomicBid atom3 = new AtomicBid(1, items3, 10);
		
		Type bid1 = new CombinatorialType();
		bid1.addAtomicBid(atom1);
		bid1.addAtomicBid(atom2);
		bid1.addAtomicBid(atom3);
		
		//Create bid 2:
		List<Integer> items21 = new LinkedList<Integer>();
		items21.add(1);
		items21.add(2);
		List<Integer> items22  = new LinkedList<Integer>();
		items22.add(3);
		
		AtomicBid atom21 = new AtomicBid(2, items21, 35);
		AtomicBid atom22 = new AtomicBid(2, items22, 10);
		
		Type bid2 = new CombinatorialType();
		bid2.addAtomicBid(atom21);
		bid2.addAtomicBid(atom22);
		
		//Aggregate two bids:
		List<Type> bids = new LinkedList<Type>();
		bids.add(bid1);
		bids.add(bid2);
		
		List<Double> reservePrice = new LinkedList<Double>();
		reservePrice.add( 20. );									//Reserve price for the 1st item
		reservePrice.add( 20. );									//Reserve price for the 2nd item
		reservePrice.add( 10. );									//Reserve price for the 3rd item
		reservePrice.add( 10. );									//Reserve price for the 4th item
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids, costs);
		ca.setupReservePrices(reservePrice);
		ca.computeWinnerDetermination();
		
		List<Double> vcg = ca.computeVCG();
		//ca.printResults();
		assertTrue( vcg.size() == 2 );
		assertTrue( vcg.get(0) == 0);
		assertTrue( vcg.get(1) == 0);
		
		//List<Double> vcgNearest = ca.computeNearestVCG2();
		//double revenue = 0.;
		//for(Double p : vcgNearest)
		//	revenue += p;
		
		//System.out.println("VCG nearest revenue: " + revenue + ". Wellfare: " + ca.getWellfare());
	}
	/*
	@Test
	public void testReservedPriceStudy() {
		
		int numberOfAgents = 2;
		int numberOfItems = 8;
		int nAtoms = 20;
		
		List<Vector> bids = new LinkedList<Vector>();
		
		for(int i = 0; i < numberOfAgents; ++i)
		{
			File file = new File("C:\\Users\\Dmitry\\Downloads\\CATS-windows\\moor000" + i + ".txt");
			Vector bid = new Vector();
			Scanner input;
			
			try {
				input = new Scanner(file);
				
				boolean isBid = false;
				while( input.hasNext() )
				{
					String nextLine = input.nextLine();
					
					if( nextLine.startsWith("0") )
						isBid = true;
					
					if( ! isBid)
						continue;
					
					System.out.println( nextLine );
					String[] terms = nextLine.split("\t");
					
					int counter = 0;
					double val = 0.;
					Vector items = new Vector();
					
					for(String str : terms)
					{
						if( counter == 0)
							;//Just skip it (it's a number of atom)
						else if (counter == 1)
							val = Double.parseDouble(str);
						else if ( (!str.equals("#")) && (Integer.parseInt(str) < numberOfItems) )
							items.add( Integer.parseInt(str) + 1);
						
						counter++;
					}		
					AtomicBid atom = new AtomicBid(items, val);
					bid.add(atom);
				}
				
				input.close();
			} catch (FileNotFoundException e) {
				System.out.println("testReservedPriceStudy() exception");
				e.printStackTrace();
			}
			bids.add(bid);
			System.out.println("MOOR " + bid.toString());
		}
	
		boolean allowReservePrices = true;

		List<Double> reservePrice = new LinkedList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			reservePrice.add(200.);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids);

		if( allowReservePrices)
			ca.setupReservePrices(reservePrice);

		ca.computeWinnerDetermination();
		
		List<Double> vcg = ca.computeVCG();
		ca.printResults();
		
		List<Double> vcgNearest = ca.computeNearestVCG2();
		double revenue = 0.;
		for(Double p : vcgNearest)
			revenue += p;
		
		System.out.println("VCG nearest revenue: " + revenue);
		
		//assertTrue( vcg.size() == 3 );
		//assertTrue( vcg.get(0) == 0);
		//assertTrue( vcg.get(1) == 30 );
		//assertTrue( Math.abs(vcg.get(2) - 10) < 1e-6 );
	}*/
	
/*	@Test
	public void testReservedPriceStudy() {
		
		int numberOfAgents = 2;
		int numberOfItems = 8;
		int nAtoms = 20;
		
		List<Vector> bids = new LinkedList<Vector>();
		
		for(int i = 0; i < numberOfAgents; ++i)
		{
			File file = new File("C:\\Users\\Dmitry\\Downloads\\CATS-windows\\hard000" + i + ".txt");
			Vector bid = new Vector();
			Scanner input;
			
			try {
				input = new Scanner(file);
				
				boolean isBid = false;
				while( input.hasNext() )
				{
					String nextLine = input.nextLine();
					
					if( nextLine.startsWith("0") )
						isBid = true;
					
					if( ! isBid)
						continue;
					
					System.out.println( nextLine );
					String[] terms = nextLine.split("\t");
					
					int counter = 0;
					double val = 0.;
					Vector items = new Vector();
					
					for(String str : terms)
					{
						if( counter == 0)
							;//Just skip it (it's a number of atom)
						else if (counter == 1)
							val = Double.parseDouble(str);
						else if ( (!str.equals("#")) && (Integer.parseInt(str) < numberOfItems) )
							items.add( Integer.parseInt(str) + 1);
						
						counter++;
					}		
					AtomicBid atom = new AtomicBid(items, val);
					bid.add(atom);
				}
				
				input.close();
			} catch (FileNotFoundException e) {
				System.out.println("testReservedPriceStudy() exception");
				e.printStackTrace();
			}
			bids.add(bid);
			System.out.println("MOOR " + bid.toString());
		}
	
		boolean allowReservePrices = true;

		List<Double> reservePrice = new LinkedList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			reservePrice.add(200.);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids);

		if( allowReservePrices)
			ca.setupReservePrices(reservePrice);

		ca.computeWinnerDetermination();
		
		List<Double> vcg = ca.computeVCG();
		ca.printResults();
		
		List<Double> vcgNearest = ca.computeNearestVCG2();
		double revenue = 0.;
		for(Double p : vcgNearest)
			revenue += p;
		
		System.out.println("VCG nearest revenue: " + revenue);
		
		//assertTrue( vcg.size() == 3 );
		//assertTrue( vcg.get(0) == 0);
		//assertTrue( vcg.get(1) == 30 );
		//assertTrue( Math.abs(vcg.get(2) - 10) < 1e-6 );
	}*/
	
	@Test
	public void testLLG() throws Exception {
		
		int numberOfAgents = 3;
		int numberOfItems = 2;
		List<Double> costs = new LinkedList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			costs.add(0.);
		
		//Create bid 1:
		List<Integer> items1 = new LinkedList<Integer>();
		items1.add(1);		
		AtomicBid atom1 = new AtomicBid(1, items1, 0.9);
		
		CombinatorialType bid1 = new CombinatorialType();
		bid1.addAtomicBid(atom1);
		
		//Create bid 2:
		List<Integer> items2 = new LinkedList<Integer>();
		items2.add(2);
		AtomicBid atom2 = new AtomicBid(2, items2, 0.8);
		
		CombinatorialType bid2 = new CombinatorialType();
		bid2.addAtomicBid(atom2);
		
		//Create bid 3:
		List<Integer> items3 = new LinkedList<Integer>();
		items3.add(1);
		items3.add(2);
		AtomicBid atom3 = new AtomicBid(3, items3, 2);
		
		CombinatorialType bid3 = new CombinatorialType();
		bid3.addAtomicBid(atom3);
		
		//Aggregate two bids:
		List<Type> bids = new LinkedList<Type>();
		bids.add(bid1);
		bids.add(bid2);
		bids.add(bid3);
				
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids, costs);
		ca.computeWinnerDetermination();
		
		List<Double> vcg = ca.computeVCG();
		assertTrue( vcg.size() == 1 );
		assertTrue( Math.abs( vcg.get(0) - 1.7) < 1e-6);
	}
	
	/**
	 * VCG-Nearest Core-selecting auction with 4 agents and two items.
	 */
	@Test
	public void testQuadraticPayment() {
		
		int numberOfAgents = 4;
		int numberOfItems = 2;
		List<Double> costs = new ArrayList<Double>();
		for(int i = 0; i < numberOfItems; ++i)
			costs.add(0.);

		//Create bid 1:
		List<Integer> items11 = Arrays.asList(1);
		AtomicBid atom11 = new AtomicBid(1, items11, 0.05);
		
		List<Integer> items12 = Arrays.asList(1, 2);
		AtomicBid atom12 = new AtomicBid(1, items12, 0.05);
		CombinatorialType bid1 = new CombinatorialType(atom11, atom12);
		
		//Create bid 2:
		List<Integer> items21 = Arrays.asList(2);
		AtomicBid atom21 = new AtomicBid(2, items21, 0.150);
				
		List<Integer> items22 = Arrays.asList(1, 2);
		AtomicBid atom22 = new AtomicBid(2, items22, 0.150);
				
		CombinatorialType bid2 = new CombinatorialType(atom21, atom22);
		
		//Create bid 3:
		List<Integer> items31 = Arrays.asList(1);
		AtomicBid atom31 = new AtomicBid(3, items31, 0.01);
				
		List<Integer> items32 = Arrays.asList(1, 2);
		AtomicBid atom32 = new AtomicBid(3, items32, 0.01);
				
		CombinatorialType bid3 = new CombinatorialType(atom31, atom32);

		//Create bid 4:
		List<Integer> items41 = Arrays.asList(1, 2);
		AtomicBid atom41 = new AtomicBid(4, items41, 0.1);
		CombinatorialType bid4 = new CombinatorialType(atom41);
		
		List<Type> bids = Arrays.asList(bid1, bid2, bid3, bid4);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, bids, costs);
		try 
		{
			ca.solveIt();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		Allocation allocation = ca.getAllocation();

		assertTrue( allocation.getNumberOfAllocatedAuctioneers() == 1);
		assertTrue( allocation.getBiddersInvolved(0).size() == 2);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 2);
		
		List<Double> vcg = ca.computeVCG();
		assertTrue( vcg.size() == 2 );
		assertTrue( Math.abs( vcg.get(0) - 0.01 ) < 1e-6 );
		assertTrue( Math.abs( vcg.get(1) - 0.05 ) < 1e-6 );
		
		List<Double> payments;
		try 
		{
			payments = ca.computeCorePayments();
			assertTrue( payments.size() == 2);
			assertTrue( Math.abs(payments.get(0) + payments.get(1) - 0.1) < 1e-6 );
			assertTrue( Math.abs( payments.get(0) - 0.03 ) < 1e-6 ); 
			assertTrue( Math.abs( payments.get(1) - 0.07 ) < 1e-6 );
		} 
		catch (PaymentException e) 
		{
			e.printStackTrace();
		}
	}

	/**
	 * VCG for LLG domain
	 */
	@Test
	public void testCAXOR_VCG() 
	{	
		List<Integer> items = Arrays.asList(1, 2);
		
		//Local bidder
		double marginalValue = 0.1;
		List<Integer> bundle = Arrays.asList(items.get(0));
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		CombinatorialType t1 = new CombinatorialType(atom11);
		
		//Local bidder
		marginalValue = 0.2;
		bundle = Arrays.asList(items.get(1));
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		CombinatorialType t2 = new CombinatorialType(atom21);
		
		//Global bidder
		marginalValue = 0.15;
		bundle = Arrays.asList(items.get(0), items.get(1));
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		CombinatorialType t3 = new CombinatorialType(atom31);
		
		List<Type> bids = Arrays.asList(t1, t2, t3);
		
		List<Double> costs = Arrays.asList(0.05, 0.1);
		
		CAXOR ca = new CAXOR(bids.size(), items.size(), bids, costs);
		ca.setPaymentRule("VCG_LLG");
		
		try 
		{
			ca.solveIt();
			Allocation allocation = ca.getAllocation();
			assertTrue( Math.abs(allocation.getAllocatedWelfare() - 0.15) < 1e-6);
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(allocation.getBiddersInvolved(0).size() == 2);
			
			double[] payments = ca.getPayments();
			assertTrue( payments[0] == 0.05);
			assertTrue( payments[1] == 0.1);	
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	@Test
	public void testCAXOR_CORE() 
	{	
		List<Integer> items = Arrays.asList(1, 2);
		
		//Local bidder
		double marginalValue = 0.1;
		List<Integer> bundle = Arrays.asList( items.get(0) );
		AtomicBid atom11 = new AtomicBid(1, bundle, marginalValue);
		CombinatorialType t1 = new CombinatorialType(atom11);
		
		//Local bidder
		marginalValue = 0.2;
		bundle = Arrays.asList( items.get(1) );
		AtomicBid atom21 = new AtomicBid(2, bundle, marginalValue);
		CombinatorialType t2 = new CombinatorialType(atom21);
		
		//Global bidder
		marginalValue = 0.21;
		bundle = Arrays.asList( items.get(0), items.get(1));
		AtomicBid atom31 = new AtomicBid(3, bundle, marginalValue);
		CombinatorialType t3 = new CombinatorialType(atom31);
		
		List<Type> bids = Arrays.asList(t1, t2, t3);
		List<Double> costs = Arrays.asList(0.05, 0.1);
		
		GridGenerator generator = new GridGenerator(1, 2);
		generator.setSeed(0);
		generator.buildProximityGraph();
		Graph grid = generator.getGrid();
		
		double primaryReductionCoef = 1.0;
		double secondaryReductionCoef = 0.5;
		JointProbabilityMass jpmf = new JointProbabilityMass( grid);
		jpmf.setNumberOfSamples(1000000);
		jpmf.setNumberOfBombsToThrow(1);
		
		IBombingStrategy b = new FocusedBombingStrategy(grid, 1., primaryReductionCoef, secondaryReductionCoef);
		List<IBombingStrategy> bombs = new LinkedList<IBombingStrategy>();
		bombs.add(b);
		
		List<Double> pd = new LinkedList<Double>();
		pd.add(1.);
		
		jpmf.setBombs(bombs, pd);
		jpmf.update();
		
		CAXOR ca = new CAXOR(bids.size(), items.size(), bids, costs);
		ca.setPaymentRule("CORE_LLG");
		
		try 
		{
			ca.solveIt();
			Allocation allocation = ca.getAllocation();
			assertTrue(allocation.getAllocatedWelfare() == 0.15);
			assertTrue(allocation.getNumberOfAllocatedAuctioneers() == 1);
			assertTrue(allocation.getBiddersInvolved(0).size() == 2);
			
			double[] payments = ca.getPayments();
			assertTrue( Math.abs(payments[0] - 0.075) < 1e-4);
			assertTrue( Math.abs(payments[1] - 0.135) < 1e-4);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}*/
}
