package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.*;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;
import ch.uzh.ifi.MechanismDesignPrimitives.SimpleType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class testMechanisms {

	@Test
	public void test() 
	{
		SimpleType t1 = new SimpleType(1, 0.1);
		SimpleType t2 = new SimpleType(2, 0.2);
		SimpleType t3 = new SimpleType(3, 0.3);
		List<Type> lst = new LinkedList<Type>();
		lst.add(t1);
		lst.add(t2);
		lst.add(t3);
		
		FirstPriceAuction fp = new FirstPriceAuction(lst);
		fp.solveIt();
		
		assertTrue( fp.getAllocation().getAuctioneerId(0) == 3);
		assertTrue( fp.getPayments()[0] == 0.3);
		//System.out.println(fp.toString());
	}
	
	@Test
	public void testSecondPriceAuction() 
	{
		SimpleType t1 = new SimpleType(1, 0.1);
		SimpleType t2 = new SimpleType(2, 0.2);
		SimpleType t3 = new SimpleType(3, 0.3);
		List<Type> lst = new LinkedList<Type>();
		lst.add(t1);
		lst.add(t2);
		lst.add(t3);
		
		SecondPriceAuction sp = new SecondPriceAuction(lst);
		sp.solveIt();
		
		assertTrue( sp.getAllocation().getAuctioneerId(0) == 3);
		assertTrue( sp.getPayments()[0] == 0.2);
	}

	@Test
	public void testCombinatorialAuction() 
	{
		int numberOfAgents = 3;
		int numberOfItems = 2;
		
		List<Integer> items1 = new LinkedList<Integer>();
		items1.add(1);
		AtomicBid t1 = new AtomicBid(1, items1, 0.9);
		CombinatorialType ct1 = new CombinatorialType();
		ct1.addAtomicBid(t1);
		
		List<Integer> items2 = new LinkedList<Integer>();
		items2.add(2);		
		AtomicBid t2 = new AtomicBid(2, items2, 0.8);
		CombinatorialType ct2 = new CombinatorialType();
		ct2.addAtomicBid(t2);
		
		List<Integer> items3 = new LinkedList<Integer>();
		items3.add(1);
		items3.add(2);		
		AtomicBid t3 = new AtomicBid(3, items3, 1.9);
		CombinatorialType ct3 = new CombinatorialType();
		ct3.addAtomicBid(t3);
		
		List<Type> lst = new LinkedList<Type>();
		lst.add(ct1);
		lst.add(ct2);
		lst.add(ct3);
		
		Auction ca = new CAXOR(numberOfAgents, numberOfItems, lst);
		try 
		{
			ca.solveIt();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		assertTrue( ca.getAllocation().getAuctioneerId(0) == 3);
		//assertTrue( ca.getPayments()[0] == 0.2);
	}
	
	@Test
	public void testCombinatorialAuction1() 
	{
		int numberOfAgents = 4;
		int numberOfItems = 2;
		
		List<Integer> items11 = new LinkedList<Integer>();
		items11.add(1);
		AtomicBid t1 = new AtomicBid(1, items11, 0.050);
		List<Integer> items12 = new LinkedList<Integer>();
		items12.add(1);
		items12.add(2);
		AtomicBid t2 = new AtomicBid(1, items12, 0.050);
		CombinatorialType ct1 = new CombinatorialType();
		ct1.addAtomicBid(t1);
		ct1.addAtomicBid(t2);
		
		List<Integer> items21 = new LinkedList<Integer>();
		items21.add(2);		
		AtomicBid t21 = new AtomicBid(2, items21, 0.150);
		List<Integer> items22 = new LinkedList<Integer>();
		items22.add(1);
		items22.add(2);
		AtomicBid t22 = new AtomicBid(2, items22, 0.150);
		CombinatorialType ct2 = new CombinatorialType();
		ct2.addAtomicBid(t21);
		ct2.addAtomicBid(t22);
		
		
		List<Integer> items31 = new LinkedList<Integer>();
		items31.add(1);
		AtomicBid t31 = new AtomicBid(3, items31, 0.010);
		List<Integer> items32 = new LinkedList<Integer>();
		items32.add(1);
		items32.add(2);
		AtomicBid t32 = new AtomicBid(3, items32, 0.010);
		CombinatorialType ct3 = new CombinatorialType();
		ct3.addAtomicBid(t31);
		ct3.addAtomicBid(t32);
		
		List<Integer> items41 = new LinkedList<Integer>();
		items41.add(1);
		items41.add(2);
		AtomicBid t41 = new AtomicBid(4, items41, 0.100);
		CombinatorialType ct4 = new CombinatorialType();
		ct4.addAtomicBid(t41);
		
		List<Type> lst = new LinkedList<Type>();
		lst.add(ct1);
		lst.add(ct2);
		lst.add(ct3);
		lst.add(ct4);
		
		CAXOR ca = new CAXOR(numberOfAgents, numberOfItems, lst);
		ca.setPaymentRule("VCGNearest2");
		try {
			ca.solveIt();
		} catch (Exception e1) 
		{
			e1.printStackTrace();
		}
		
		assertTrue( ca.getAllocation().getNumberOfAllocatedAuctioneers() == 2);
		assertTrue( ca.getAllocation().getAuctioneerId(0) == 1);
		assertTrue( ca.getAllocation().getAuctioneerId(1) == 2);
		
		try {
			assertTrue(ca.getPayments().length == 2);
			assertTrue( Math.abs(ca.getPayments()[0] - 0.03) < 1e-6);
			assertTrue(ca.getPayments()[1] == 0.07);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCombinatorialAuctionLLG() 
	{
		int numberOfAgents = 3;
		int numberOfItems = 2;
		
		List<Integer> items1 = new LinkedList<Integer>();
		items1.add(1);
		AtomicBid t1 = new AtomicBid(1, items1, 0.9);
		CombinatorialType ct1 = new CombinatorialType();
		ct1.addAtomicBid(t1);
		
		List<Integer> items2 = new LinkedList<Integer>();
		items2.add(2);
		AtomicBid t2 = new AtomicBid(2, items2, 0.8);
		CombinatorialType ct2 = new CombinatorialType();
		ct2.addAtomicBid(t2);
		
		List<Integer> items3 = new LinkedList<Integer>();
		items3.add(1);
		items3.add(2);
		AtomicBid t3 = new AtomicBid(3, items3, 1.71);
		CombinatorialType ct3 = new CombinatorialType();
		ct3.addAtomicBid(t3);
		
		List<Type> lst = new LinkedList<Type>();
		lst.add(ct1);
		lst.add(ct2);
		lst.add(ct3);
		
		Auction ca = new CAXOR(numberOfAgents, numberOfItems, lst);
		try {
			ca.solveIt();
		} catch (Exception e1) 
		{
			e1.printStackTrace();
		}
		
		assertTrue( ca.getAllocation().getNumberOfAllocatedAuctioneers() == 1);
		assertTrue( ca.getAllocation().getAuctioneerId(0) == 3);
		//assertTrue( ca.getWinnerIds()[1] == 2);
		
		try {
			assertTrue(ca.getPayments().length == 1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			System.out.println("Payment " + ca.getPayments()[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//assertTrue( Math.abs(ca.getPayments()[0] - 0.03) < 1e-6);
		//assertTrue(ca.getPayments()[1] == 0.07);
	}
	
	@Test
	public void testSemanticWebType() 
	{
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType t = new CombinatorialType();
		t.addAtomicBid(plan1);
		t.addAtomicBid(plan2);
		
		assertTrue( t.getNumberOfAtoms() == 2 );
		assertTrue( t.getAgentId() == 1 );
		assertTrue( t.getAtom(0).getInterestingSet().size() == 2 );
		assertTrue( t.getAtom(1).getInterestingSet().size() == 2 );
		assertTrue( t.getAtom(0).getInterestingSet().get(0) == 1 );
		assertTrue( t.getAtom(0).getInterestingSet().get(1) == 2 );
		assertTrue( t.getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( t.getAtom(1).getInterestingSet().get(1) == 3 );
		
		assertTrue( Math.abs( (double)t.getTypeComponent(0, "Cost1") - 0.1) < 1e-6 );
		assertTrue( Math.abs( (double)t.getTypeComponent(0, "Cost2") - 0.2) < 1e-6 );
		assertTrue( Math.abs( (double)t.getTypeComponent(1, "Cost1") - 0.1) < 1e-6 );
		assertTrue( Math.abs( (double)t.getTypeComponent(1, "Cost2") - 0.4) < 1e-6 );
		
		assertTrue( t.getAtom(0).getValue() == 0.3 );
		assertTrue( t.getAtom(1).getValue() == 0.6 );
		
		assertTrue( Math.abs( (double)t.getAtom(0).getTypeComponent("Cost") - 0.3) < 1e-6 );
		assertTrue( Math.abs( (double)t.getAtom(1).getTypeComponent("Cost") - 0.5) < 1e-6 );
		
		System.out.println( t.toString() );
	}
	/*
	@Test
	public void testDoubleSidedMarket() throws Exception 
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();							//Sellers
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(plan1);
		t1.addAtomicBid(plan2);
		
		
		List<Integer> serversUsed3 = new LinkedList<Integer>();							//Sellers
		serversUsed3.add(1);
		serversUsed3.add(2);
		List<Double> costsAssociated3 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated3.add(0.1);
		costsAssociated3.add(0.2);
		AtomicBid plan3 = new SemanticWebType(2, serversUsed1, 0.6, costsAssociated3);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(plan3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		
		Auction market = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids);
		market.solveIt();
		
		Allocation  allocation = market.getAllocation();
		assertTrue( allocation.getNumberOfAllocatedAgents() == 2);						//The number of allocated buyers (or couples buyer--seller )
		assertTrue( allocation.getAgentId(0) == 1 );
		assertTrue( allocation.getAgentId(1) == 2 );
		assertTrue( Math.abs( allocation.getAgentUtility(0) - 0.1) < 1e-6 );
		assertTrue( Math.abs( allocation.getAgentUtility(1) - 0.3) < 1e-6 );
		assertTrue( Math.abs( allocation.getWelfare() - 0.4) < 1e-6 );
		
		assertTrue( allocation.getAllocatedBundlesById( 1 ).get(0) == 1);
		assertTrue( allocation.getAllocatedBundlesById( 2 ).get(0) == 0);
	}
	
	@Test
	public void testDoubleSidedMarketPaymentsVCG() throws Exception 
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();							//Sellers
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(plan1);
		t1.addAtomicBid(plan2);
		
		
		List<Integer> serversUsed3 = new LinkedList<Integer>();							//Sellers
		serversUsed3.add(1);
		serversUsed3.add(2);
		List<Double> costsAssociated3 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated3.add(0.1);
		costsAssociated3.add(0.2);
		AtomicBid plan3 = new SemanticWebType(2, serversUsed1, 0.6, costsAssociated3);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(plan3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		
		DoubleSidedMarket market = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids);
		market.solveIt();
		List<Double> payments = market.computePayments(new DoubleSidedVCGPayments(market.getAllocation(), bids, numberOfBuyers, numberOfSellers));
		
		assertTrue( payments.size() == 5);
		assertTrue( Math.abs(payments.get(0) - 0.5) < 1e-6  );
		assertTrue( Math.abs(payments.get(1) - 0.3) < 1e-6  );
		assertTrue( Math.abs(payments.get(2) - (-0.4)) < 1e-6  );
		assertTrue( Math.abs(payments.get(3) - (-0.7)) < 1e-6  );
		assertTrue( Math.abs(payments.get(4) - (-0.5)) < 1e-6  );
	}
	
	@Test
	public void testDoubleSidedMarketPaymentsVCG1() throws Exception 
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();							//Sellers
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(plan1);
		t1.addAtomicBid(plan2);
		
		
		List<Integer> serversUsed3 = new LinkedList<Integer>();							//Sellers
		serversUsed3.add(1);
		serversUsed3.add(2);
		List<Double> costsAssociated3 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated3.add(0.1);
		costsAssociated3.add(0.2);
		AtomicBid plan3 = new SemanticWebType(2, serversUsed1, 0.6, costsAssociated3);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(plan3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		
		DoubleSidedMarket market = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids);
		market.solveIt();
		Allocation allocation = market.getAllocation();
		DoubleSidedVCGPayments vcg = new DoubleSidedVCGPayments(allocation, bids, numberOfBuyers, numberOfSellers);
		List<Double> payments = vcg.computePayments();
		
		assertTrue( payments.size() == 5);
		assertTrue( Math.abs(payments.get(0) - 0.5) < 1e-6  );
		assertTrue( Math.abs(payments.get(1) - 0.3) < 1e-6  );
		assertTrue( Math.abs(payments.get(2) - (-0.4)) < 1e-6  );
		assertTrue( Math.abs(payments.get(3) - (-0.7)) < 1e-6  );
		assertTrue( Math.abs(payments.get(4) - (-0.5)) < 1e-6  );
		assertTrue( vcg.isBudgetBalanced() == false );
		
		List<Double> vcgDiscounts = vcg.getVCGDiscounts();
		assertTrue( vcgDiscounts.size() == 5 );
		assertTrue( Math.abs( vcgDiscounts.get(0) - 0.1 ) < 1e-6 );
		assertTrue( Math.abs( vcgDiscounts.get(1) - 0.3 ) < 1e-6 );
		assertTrue( Math.abs( vcgDiscounts.get(2) - 0.3 ) < 1e-6 );
		assertTrue( Math.abs( vcgDiscounts.get(3) - 0.4 ) < 1e-6 );
		assertTrue( Math.abs( vcgDiscounts.get(4) - 0.1 ) < 1e-6 );
	}
	
	@Test
	public void testDoubleSidedMarketPaymentsThreshold() throws Exception 
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();							//Sellers
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType t1 = new CombinatorialType();
		t1.addAtomicBid(plan1);
		t1.addAtomicBid(plan2);
		
		
		List<Integer> serversUsed3 = new LinkedList<Integer>();							//Sellers
		serversUsed3.add(1);
		serversUsed3.add(2);
		List<Double> costsAssociated3 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated3.add(0.1);
		costsAssociated3.add(0.2);
		AtomicBid plan3 = new SemanticWebType(2, serversUsed1, 0.6, costsAssociated3);
		
		CombinatorialType t2 = new CombinatorialType();
		t2.addAtomicBid(plan3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(t1);
		bids.add(t2);
		
		DoubleSidedMarket market = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids);
		market.solveIt();
		Allocation allocation = market.getAllocation();
		ThresholdPayments threshold = new ThresholdPayments(allocation, bids, numberOfBuyers, numberOfSellers);
		List<Double> payments = threshold.computePayments();
		
		for(Double p : payments)
			System.out.println("-> " + p);
		
		assertTrue( payments.size() == 5);
		assertTrue( threshold.isBudgetBalanced() == true );
		assertTrue( Math.abs(payments.get(0) - 0.6) < 1e-6  );
		assertTrue( Math.abs(payments.get(1) - 0.5) < 1e-6  );
		assertTrue( Math.abs(payments.get(2) - (-0.2)) < 1e-6  );
		assertTrue( Math.abs(payments.get(3) - (-0.5)) < 1e-6  );
		assertTrue( Math.abs(payments.get(4) - (-0.4)) < 1e-6  );
	}
	
	@Test
	public void testDoubleSidedMarketBNE_Prep() throws Exception 
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();							//Sellers
		serversUsed1.add(1);
		serversUsed1.add(2);
		List<Double> costsAssociated1 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated1.add(0.1);
		costsAssociated1.add(0.2);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, 0.3, costsAssociated1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costsAssociated2 = new LinkedList<Double>();
		costsAssociated2.add(0.1);
		costsAssociated2.add(0.4);		
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, 0.6, costsAssociated2);
		
		CombinatorialType b1 = new CombinatorialType(); 								//Buyer's type
		b1.addAtomicBid(plan1);
		b1.addAtomicBid(plan2);
		b1.setTypeComponent("isSeller", 0.0);
		
		List<Integer> serversUsed3 = new LinkedList<Integer>();							//Sellers
		serversUsed3.add(1);
		serversUsed3.add(2);
		List<Double> costsAssociated3 = new LinkedList<Double>();						//Costs of sellers
		costsAssociated3.add(0.1);
		costsAssociated3.add(0.2);
		AtomicBid plan3 = new SemanticWebType(2, serversUsed1, 0.6, costsAssociated3);
		
		CombinatorialType b2 = new CombinatorialType();
		b2.addAtomicBid(plan3);
		b2.setTypeComponent("isSeller", 0.0);
		
		CombinatorialType s1 = new CombinatorialType();
		s1.addAtomicBid(plan1);
		s1.addAtomicBid(plan3);
		s1.setTypeComponent("isSeller", 1.0);
		s1.setTypeComponent("sellerId", 1.0);
		
		CombinatorialType s2 = new CombinatorialType();
		s2.addAtomicBid(plan1);
		s2.addAtomicBid(plan3);
		s2.setTypeComponent("isSeller", 1.0);
		s2.setTypeComponent("sellerId", 2.0);
		
		CombinatorialType s3 = new CombinatorialType();
		s3.addAtomicBid(plan2);
		s3.setTypeComponent("isSeller", 1.0);
		s3.setTypeComponent("sellerId", 3.0);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		assertTrue(b1.getAtom(0) == s1.getAtom(0));
		assertTrue(b1.getAtom(0) != s1.getAtom(1));
		
		b1.getAtom(0).setValue(8);
		assertTrue(b1.getAtom(0) == s1.getAtom(0));
		assertTrue(b1.getAtom(0).getValue() == 8.0);
		assertTrue(s1.getAtom(0).getValue() == 8.0);
		b1.getAtom(0).setValue(0.3);
		
		assertTrue( (Double)b1.getTypeComponent("isSeller") == 0.0 );
		assertTrue( (Double)b2.getTypeComponent("isSeller") == 0.0 );
		assertTrue( (Double)s1.getTypeComponent("isSeller") == 1.0 );
		assertTrue( (Double)s2.getTypeComponent("isSeller") == 1.0 );
		assertTrue( (Double)s3.getTypeComponent("isSeller") == 1.0 );
		
		assertTrue( (Double)s1.getTypeComponent("sellerId") == 1.0 );
		assertTrue( (Double)s2.getTypeComponent("sellerId") == 2.0 );
		assertTrue( (Double)s3.getTypeComponent("sellerId") == 3.0 );
		
		int s1Id = ((Double)s1.getTypeComponent("sellerId")).intValue();
		assertTrue( (Double)plan1.getTypeComponent("Cost" + s1Id  ) == 0.1 );
		int s2Id = ((Double)s2.getTypeComponent("sellerId")).intValue();
		assertTrue( (Double)plan1.getTypeComponent("Cost" + s2Id  ) == 0.2 );
		
		assertTrue( (Double)plan3.getTypeComponent("Cost" + s1Id  ) == 0.1 );
		assertTrue( (Double)plan3.getTypeComponent("Cost" + s2Id  ) == 0.2 );
		
		s1.getAtom(0).setTypeComponent("Cost" + s1Id, 0.5);
		assertTrue( (Double)plan1.getTypeComponent("Cost" + s1Id  ) == 0.5 );
		s1.getAtom(0).setTypeComponent("Cost" + s1Id, 0.1);
		
		
		DoubleSidedMarket market = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids);
		market.solveIt();
		
		Allocation allocation = market.getAllocation();
		ThresholdPayments threshold = new ThresholdPayments(allocation, bids, numberOfBuyers, numberOfSellers);
		List<Double> payments = threshold.computePayments();
		
		for(Double p : payments)
			System.out.println("-> " + p);
		
		assertTrue( payments.size() == 5);
		assertTrue( threshold.isBudgetBalanced() == true );
		assertTrue( Math.abs(payments.get(0) - 0.6) < 1e-6  );
		assertTrue( Math.abs(payments.get(1) - 0.5) < 1e-6  );
		assertTrue( Math.abs(payments.get(2) - (-0.2)) < 1e-6  );
		assertTrue( Math.abs(payments.get(3) - (-0.5)) < 1e-6  );
		assertTrue( Math.abs(payments.get(4) - (-0.4)) < 1e-6  );
	}*/
	/*
	@Test
	public void testPlanner() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner  = new SimpleErrorPlanner();
		assertTrue( (planner.getNTuples(0) >= 1) && (planner.getNTuples(0) <= 10) );
		assertTrue( (planner.getNTuples(1) >= 1) && (planner.getNTuples(1) <= 10) );
		assertTrue( (planner.getNTuples(2) >= 1) && (planner.getNTuples(2) <= 10) );
		assertTrue( (planner.getNTuples(3) >= 1) && (planner.getNTuples(3) <= 10) );
		planner.reset(numberOfBuyers, numberOfSellers, bids);
		
		int nTuples1 = planner.getNTuples(0);
		int nTuples2 = planner.getNTuples(1);
		int nTuples3 = planner.getNTuples(2);
		int nTuples4 = planner.getNTuples(3);
		
		planner.injectError(0);
		assertTrue( planner.getNTuples(0) <= nTuples1 );
		assertTrue( planner.getNTuples(1) <= nTuples2 );
		assertTrue( planner.getNTuples(2) <= nTuples3 );
		assertTrue( planner.getNTuples(3) <= nTuples4 );
		
		planner.injectError(0);
		assertTrue( planner.getNTuples(0) <= nTuples1 );
		assertTrue( planner.getNTuples(1) <= nTuples2 );
		assertTrue( planner.getNTuples(2) <= nTuples3 );
		assertTrue( planner.getNTuples(3) <= nTuples4 );
		
		planner.injectError(1);
		assertTrue( planner.getNTuples(0) <= nTuples1 );
		assertTrue( planner.getNTuples(1) <= nTuples2 );
		assertTrue( planner.getNTuples(2) <= nTuples3 );
		assertTrue( planner.getNTuples(3) <= nTuples4 );
		
		planner.injectError(1);
		assertTrue( planner.getNTuples(0) <= nTuples1 );
		assertTrue( planner.getNTuples(1) <= nTuples2 );
		assertTrue( planner.getNTuples(2) <= nTuples3 );
		assertTrue( planner.getNTuples(3) <= nTuples4 );
		
		planner.withdrawError();
		assertTrue( planner.getNTuples(0) == nTuples1 );
		assertTrue( planner.getNTuples(1) == nTuples2 );
		assertTrue( planner.getNTuples(2) == nTuples3 );
		assertTrue( planner.getNTuples(3) == nTuples4 );		
	}
	*//*
	@Test
	public void testPlanner1() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		
		while(true)
		{
			Planner planner  = new SimpleErrorPlanner();
			planner.reset(numberOfBuyers, numberOfSellers, bids);
			
			int nTuples1 = planner.getNTuples(0);
			int nTuples2 = planner.getNTuples(1);
			int nTuples3 = planner.getNTuples(2);
			int nTuples4 = planner.getNTuples(3);
			
			int nTuplesErr1 = 0;
			int nTuplesErr2 = 0;
			int nTuplesErr3 = 0;
			int nTuplesErr4 = 0;
			boolean isInjected = planner.injectError(0);
			if(isInjected)
			{
				nTuplesErr1 = planner.getNTuples(0);
				nTuplesErr2 = planner.getNTuples(1);
				nTuplesErr3 = planner.getNTuples(2);
				nTuplesErr4 = planner.getNTuples(3);
				assertTrue( nTuplesErr1 <= nTuples1 );
				assertTrue( nTuplesErr2 <= nTuples2 );
				assertTrue( nTuplesErr3 <= nTuples3 );
				assertTrue( nTuplesErr4 <= nTuples4 );
			}
			
			planner.withdrawError();
			assertTrue( planner.getNTuples(0) == nTuples1 );
			assertTrue( planner.getNTuples(1) == nTuples2 );
			assertTrue( planner.getNTuples(2) == nTuples3 );
			assertTrue( planner.getNTuples(3) == nTuples4 );
			
			planner.injectError(0);
			if( isInjected)
			{
				assertTrue( nTuplesErr1 == planner.getNTuples(0) );
				assertTrue( nTuplesErr2 == planner.getNTuples(1) );
				assertTrue( nTuplesErr3 == planner.getNTuples(2) );
				assertTrue( nTuplesErr4 == planner.getNTuples(3) );
				break;
			}
		}
	}*/
	/*
	@Test
	public void testGeneralPlanner() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		List<Type> plans = planner.generatePlans();
		
		System.out.println("Plans: " + plans.toString());
		planner.injectError(0);
		plans = planner.generatePlans();
		System.out.println("PlansErr: " + plans.toString());
		planner.withdrawError();
		plans = planner.getPlans();
		System.out.println("PlansNoErr: " + plans.toString());
		
		bids.remove(3);
		planner.reset(numberOfBuyers, numberOfSellers, bids);
		plans = planner.generatePlans();
		System.out.println("Without 3: " + plans.toString());
		
		((GeneralErrorPlanner)planner).makeNonInjectable();
		planner.injectError(0);
		plans = planner.generatePlans();
		System.out.println("Without 3 but with error: " + plans.toString());
	}*/
	
	
	@Test
	public void testGeneralPlannerReset() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(5);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234);
		planner.reset(bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		//System.out.println("Plansss: " + plans.toString());
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);
		
		
		//2. Remove the seller with id=3 from all plans while keeping him in the setup (numberOfSellers is the same) and generate new plans for this reduced set
		bids.remove(2);
		planner.reset(bids);
		plans = planner.generatePlans();
		
		assertTrue( plans.get(0).getNumberOfAtoms() == 3 );
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.7) < 1e-6);
	}
	
	@Test
	public void testGeneralPlannerMemento() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(5);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.7) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);
		
		//3. Withdraw the error
		planner.withdrawError();
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);

		//4. Remove one agent and generate plans for the rest of them
		bids.remove(3);
		planner.reset(  bids);
		plans = planner.generatePlans();		
		assertTrue( plans.get(0).getNumberOfAtoms() == 2 );
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.5) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);		
				
		//5. Make it non-enjectable and try to inject an error
		((GeneralErrorPlanner)planner).makeNonInjectable();
		planner.injectError(0);
		plans = planner.generatePlans();
		assertTrue( plans.get(0).getNumberOfAtoms() == 2 );
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.5) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);	
	}

	@Test
	public void testGeneralPlannerPayments() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(5);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.7) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);
		
		
		Allocation allocation = new Allocation();									//Allocate the 1st plan
		List<Double> sellersCosts = new LinkedList<Double>();
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1"));
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2"));
		allocation.addAllocatedAgent(1, plans.get(0).getAtom(0).getInterestingSet(), items, plans.get(0).getAtom(0).getValue(), sellersCosts);
		
		assertTrue( allocation.getAllocatedWelfare() == -1.5 );								//The allocation is inefficient due to error injection
		
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments( allocation, bids, plans, numberOfBuyers, numberOfSellers, planner);
		vcgPayments.computePayments();
		List<Double> vcgDiscounts = vcgPayments.getVCGDiscounts();
		
		assertTrue( vcgDiscounts.size() == 3 );										//All discounts are equal as agents are pivotal
		assertTrue( vcgDiscounts.get(0) == -1.5 );
		assertTrue( vcgDiscounts.get(1) == -1.5 );
		assertTrue( vcgDiscounts.get(2) == -1.5 );
		

		//Create a new planner to compute plans with no errors injected
		Planner plannerNew = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		plannerNew.restoreFromMemento( planner.getStateMemento(0));
		plannerNew.makeNonInjectable();
		
		plannerNew.setNumberOfPlans(5);
		plannerNew.setMinSellersPerPlan(2);
		plannerNew.setMaxSellersPerPlan(2);
		plans = plannerNew.generatePlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.9) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.7) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 0.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(3).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(3).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(3).getValue() - 1.2) < 1e-6);
		//Plan 5
		assertTrue( plans.get(0).getAtom(4).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(4).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(4).getValue() - 0.7) < 1e-6);		
	}

	@Test
	public void testGeneralPlannerPaymentsCorrection() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.245);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(3);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 523356);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.7149999) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 2.94) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 1.7149999) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(1);
		plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.7149999) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 2.45) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 1.7149999) < 1e-6);
		
		
		Allocation allocation = new Allocation();									//Allocate the 1st plan
		List<Double> sellersCosts = new LinkedList<Double>();
		sellersCosts.add((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1"));
		sellersCosts.add((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2"));
		allocation.addAllocatedAgent(1, plans.get(0).getAtom(1).getInterestingSet(), items, plans.get(0).getAtom(1).getValue(), sellersCosts);
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - (-0.05)) < 1e-6 );								//The allocation is inefficient due to error injection
		
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments( allocation, bids, plans, numberOfBuyers, numberOfSellers, planner);
		vcgPayments.computePayments();
		List<Double> vcgDiscounts = vcgPayments.getVCGDiscounts();
				
		assertTrue( vcgDiscounts.size() == 3 );										//All discounts are equal as agents are pivotal
		assertTrue( Math.abs(vcgDiscounts.get(0) - (-0.05)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(1) - (-0.05)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(2) - (-0.05)) < 1e-6 );
		
		// Now correct the discounts using PC-TRIM:
		IPaymentCorrectionStrategy pc = new TrimStrategy( vcgDiscounts );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
				
		assertTrue( vcgDiscounts.get(0) == 0.0);
		assertTrue( vcgDiscounts.get(1) == 0.0);
		assertTrue( vcgDiscounts.get(2) == 0.0);
		
		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-VCG:
		pc = new VCGStrategy( planner, bids, numberOfBuyers, numberOfSellers);
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();

		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.04) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.04) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.04) < 1e-6);

		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-PENALTY:
		pc = new PenaltyStrategy( planner, bids, allocation ,numberOfBuyers, numberOfSellers );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
		
		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.04) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.00) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.04) < 1e-6);
	}
	
	@Test
	public void testSimplePlannerMemento() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new SimpleErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234);
		
		//1. Generate plans without errors
		planner.reset( bids);
		List<Type> plans = planner.generatePlans();
		assertTrue(plans.get(0).getNumberOfAtoms() == 2);
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.7) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.0) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.7) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();

		assertTrue(plans.get(0).getNumberOfAtoms() == 2);
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 0.8) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.7) < 1e-6);
				
		//3. Withdraw the error
		planner.withdrawError();
		plans = planner.getPlans();
		//Plan 1
		assertTrue(plans.get(0).getNumberOfAtoms() == 2);
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.7) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.0) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.7) < 1e-6);	
		
		//4. Remove one agent and generate plans for the rest of them
		bids.remove(3);
		planner.reset(  bids);
		plans = planner.generatePlans();		
		assertTrue( plans.get(0).getNumberOfAtoms() == 1 );
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.7) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.0) < 1e-6);
		
		//5. Make it non-enjectable and try to inject an error
		((SimpleErrorPlanner)planner).makeNonInjectable();
		planner.injectError(0);
		plans = planner.generatePlans();
		assertTrue( plans.get(0).getNumberOfAtoms() == 1 );
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.7) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.0) < 1e-6);
	}
	
	@Test
	public void testSimplePlannerPaymentsCorrection() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.215);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.172);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new SimpleErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 523356);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.505) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 0.344) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.645) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.075) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 0.344) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.645) < 1e-6);
				
				
		
		Allocation allocation = new Allocation();									//Allocate the 1st plan
		List<Double> sellersCosts = new LinkedList<Double>();
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1"));
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2"));
		allocation.addAllocatedAgent(1, plans.get(0).getAtom(0).getInterestingSet(), items, plans.get(0).getAtom(0).getValue(), sellersCosts);
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - (-0.025)) < 1e-6 );								//The allocation is inefficient due to error injection
		
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments( allocation, bids, plans, numberOfBuyers, numberOfSellers, planner);
		vcgPayments.computePayments();
		List<Double> vcgDiscounts = vcgPayments.getVCGDiscounts();
				
		assertTrue( vcgDiscounts.size() == 3 );										//All discounts are equal as agents are pivotal
		assertTrue( Math.abs(vcgDiscounts.get(0) - (-0.025)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(1) - (-0.026)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(2) - (-0.025)) < 1e-6 );
		
		// Now correct the discounts using PC-TRIM:
		IPaymentCorrectionStrategy pc = new TrimStrategy( vcgDiscounts );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
				
		assertTrue( vcgDiscounts.get(0) == 0.0);
		assertTrue( vcgDiscounts.get(1) == 0.0);
		assertTrue( vcgDiscounts.get(2) == 0.0);
		
		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-VCG:
		pc = new VCGStrategy( planner, bids, numberOfBuyers, numberOfSellers);
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
		

		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.005) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.004) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.005) < 1e-6);

		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-PENALTY:
		pc = new PenaltyStrategy( planner, bids, allocation ,numberOfBuyers, numberOfSellers );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
		
		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.005) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.00) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.005) < 1e-6);
	}
	
	/*
	 * Test for all three payment correction rules with a simple planner
	 */
	@Test
	public void testSimplePlannerPaymentsCorrection1() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.23);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.172);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new SimpleErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 523356);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.61) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 0.344) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.69) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.15) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 0.3) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 0.344) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.69) < 1e-6);
				
				
		
		Allocation allocation = new Allocation();									//Allocate the 1st plan
		List<Double> sellersCosts = new LinkedList<Double>();
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1"));
		sellersCosts.add((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2"));
		allocation.addAllocatedAgent(1, plans.get(0).getAtom(0).getInterestingSet(), items, plans.get(0).getAtom(0).getValue(), sellersCosts);
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - (0.05)) < 1e-6 );			//The allocation is inefficient due to error injection
		
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments( allocation, bids, plans, numberOfBuyers, numberOfSellers, planner);
		vcgPayments.computePayments();
		List<Double> vcgDiscounts = vcgPayments.getVCGDiscounts();
		assertTrue( vcgDiscounts.size() == 3 );										//All discounts are equal as agents are pivotal
		assertTrue( Math.abs(vcgDiscounts.get(0) - (0.05)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(1) - (0.004)) < 1e-6 );
		assertTrue( Math.abs(vcgDiscounts.get(2) - (0.05)) < 1e-6 );
		
		// Now correct the discounts using PC-TRIM:
		IPaymentCorrectionStrategy pc = new TrimStrategy( vcgDiscounts );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();

		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.05 ) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.004) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.05 ) < 1e-6);
		
		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-VCG:
		pc = new VCGStrategy( planner, bids, numberOfBuyers, numberOfSellers);
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();

		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.11)  < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.064) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.11)  < 1e-6);

		vcgDiscounts = vcgPayments.getVCGDiscounts();								//Incorrect discounts
		
		//Now correct the discounts using PC-PENALTY:
		pc = new PenaltyStrategy( planner, bids, allocation ,numberOfBuyers, numberOfSellers );
		pc.correctPayments();
		vcgDiscounts = pc.getCorrectedVCGDiscounts();
		
		assertTrue( Math.abs(vcgDiscounts.get(0) - 0.11) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(1) - 0.04) < 1e-6);
		assertTrue( Math.abs(vcgDiscounts.get(2) - 0.11) < 1e-6);
		
	}
	
	/*
	 * Planner test
	 */
	@Test
	public void testJoinPlanner() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);											//a query
		AtomicBid atomB1 = new AtomicBid(1, items, 0.245);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//Buyer's type
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomS1 = new AtomicBid(2, items, 0.2);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//Seller's type
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, 0.3);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//Seller's type
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, 0.4);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//Seller's type
		s3.addAtomicBid(atomS3);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new JoinErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(3);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 523356);
		planner.reset( bids);
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.47) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 8.575) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 2.45) < 1e-6);
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(1);
		plans = planner.getPlans();

		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 0.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.47) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 3 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 6.125) < 1e-6);
		//Plan 3
		assertTrue( plans.get(0).getAtom(2).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(2).getInterestingSet().get(1) == 4 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(2).getValue() - 2.45) < 1e-6);		
	}
	
	/*
	 * Test scenario:
	 * 1. create a planner factory
	 * 2. generate a planner and plans with a given seed
	 * 3. create another planner and generate plans with the same seed
	 * 4. ensure that the plans are same 
	 */
	@Test
	public void testPlannerFactory() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Type> bids = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);									//a query
		
		AtomicBid atomB1 = new AtomicBid(1, items, 0.6);
		atomB1.setTypeComponent("isSeller", 0.0);
		atomB1.setTypeComponent("Distribution", 1.0);
		atomB1.setTypeComponent("minValue", 0.0);
		atomB1.setTypeComponent("maxValue", 1.0);
		CombinatorialType b1 = new CombinatorialType(); //Buyer's type
		b1.addAtomicBid(atomB1);
		
		bids.add(b1);
		Random generator = new Random();
		generator.setSeed(123456);
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( 2 + i , items, generator.nextDouble() );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();//Seller's type
			s.addAtomicBid(atomS);
			bids.add(s);
		}
		
		int seed = 7;
		
		IPlannerFactory plannerFactory = new GeneralPlannerFactory(numberOfBuyers, numberOfSellers, bids, true, 2, 2, 2, 3.);
		Planner generalErrorPlanner1 = plannerFactory.producePlanner(seed);
		List<Type> plans1 = generalErrorPlanner1.getPlans();
		
		Planner generalErrorPlanner2 = plannerFactory.producePlanner(seed);
		List<Type> plans2 = generalErrorPlanner2.getPlans();
		
		assertTrue(plans1.size() == plans2.size());
		assertTrue(plans1.get(0).getAtom(0).getAgentId() == plans2.get(0).getAtom(0).getAgentId());
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).size() == plans2.get(0).getAtom(0).getInterestingSet(0).size());
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).get(0) == plans2.get(0).getAtom(0).getInterestingSet(0).get(0));
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).get(1) == plans2.get(0).getAtom(0).getInterestingSet(0).get(1));
		assertTrue(plans1.get(0).getAtom(0).getValue() == plans2.get(0).getAtom(0).getValue());
		assertTrue((double)(plans1.get(0).getAtom(0).getTypeComponent("Cost1")) == (double)(plans2.get(0).getAtom(0).getTypeComponent("Cost1")));
		assertTrue((double)(plans1.get(0).getAtom(0).getTypeComponent("Cost2")) == (double)(plans2.get(0).getAtom(0).getTypeComponent("Cost2")));
		
		assertTrue(plans1.get(0).getAtom(1).getAgentId() == plans2.get(0).getAtom(1).getAgentId());
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).size() == plans2.get(0).getAtom(1).getInterestingSet(0).size());
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).get(0) == plans2.get(0).getAtom(1).getInterestingSet(0).get(0));
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).get(1) == plans2.get(0).getAtom(1).getInterestingSet(0).get(1));
		assertTrue(plans1.get(0).getAtom(1).getValue() == plans2.get(0).getAtom(1).getValue());
		assertTrue((double)(plans1.get(0).getAtom(1).getTypeComponent("Cost1")) == (double)(plans2.get(0).getAtom(1).getTypeComponent("Cost1")));
		assertTrue((double)(plans1.get(0).getAtom(1).getTypeComponent("Cost2")) == (double)(plans2.get(0).getAtom(1).getTypeComponent("Cost2")));
	}
	
	@Test
	public void testUtilityEstimator() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Type> bids = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);									//a query
		
		AtomicBid atomB1 = new AtomicBid(1, items, 0.6);
		atomB1.setTypeComponent("isSeller", 0.0);
		atomB1.setTypeComponent("Distribution", 1.0);
		atomB1.setTypeComponent("minValue", 0.0);
		atomB1.setTypeComponent("maxValue", 1.0);
		CombinatorialType b1 = new CombinatorialType(); //Buyer's type
		b1.addAtomicBid(atomB1);
		
		bids.add(b1);
		Random generator = new Random();
		generator.setSeed(123456);
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( 2 + i , items, generator.nextDouble() );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();//Seller's type
			s.addAtomicBid(atomS);
			bids.add(s);
		}
		
		int seed = 7;
		
		IPlannerFactory plannerFactory = new GeneralPlannerFactory(numberOfBuyers, numberOfSellers, bids, true, 2, 2, 2, 3.);
		Planner generalErrorPlanner1 = plannerFactory.producePlanner(seed);
		List<Type> plans1 = generalErrorPlanner1.getPlans();
		
		int localNumberOfSamples = 10;
		double[] s = {1., 1.};
		//UtilityEstimator utilityEstimator = new UtilityEstimator(localNumberOfSamples, bids.get(0), s, IMechanismFactory mechanismFactory,
		//		List<Type> agentsTypes, ShavingStrategy[] shavingStrategies, List<List<Double>> shaves, List<List<Type>> bins, int seed)
				
				
		Planner generalErrorPlanner2 = plannerFactory.producePlanner(seed);
		List<Type> plans2 = generalErrorPlanner2.getPlans();
		
		assertTrue(plans1.size() == plans2.size());
		assertTrue(plans1.get(0).getAtom(0).getAgentId() == plans2.get(0).getAtom(0).getAgentId());
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).size() == plans2.get(0).getAtom(0).getInterestingSet(0).size());
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).get(0) == plans2.get(0).getAtom(0).getInterestingSet(0).get(0));
		assertTrue(plans1.get(0).getAtom(0).getInterestingSet(0).get(1) == plans2.get(0).getAtom(0).getInterestingSet(0).get(1));
		assertTrue(plans1.get(0).getAtom(0).getValue() == plans2.get(0).getAtom(0).getValue());
		assertTrue((double)(plans1.get(0).getAtom(0).getTypeComponent("Cost1")) == (double)(plans2.get(0).getAtom(0).getTypeComponent("Cost1")));
		assertTrue((double)(plans1.get(0).getAtom(0).getTypeComponent("Cost2")) == (double)(plans2.get(0).getAtom(0).getTypeComponent("Cost2")));
		
		assertTrue(plans1.get(0).getAtom(1).getAgentId() == plans2.get(0).getAtom(1).getAgentId());
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).size() == plans2.get(0).getAtom(1).getInterestingSet(0).size());
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).get(0) == plans2.get(0).getAtom(1).getInterestingSet(0).get(0));
		assertTrue(plans1.get(0).getAtom(1).getInterestingSet(0).get(1) == plans2.get(0).getAtom(1).getInterestingSet(0).get(1));
		assertTrue(plans1.get(0).getAtom(1).getValue() == plans2.get(0).getAtom(1).getValue());
		assertTrue((double)(plans1.get(0).getAtom(1).getTypeComponent("Cost1")) == (double)(plans2.get(0).getAtom(1).getTypeComponent("Cost1")));
		assertTrue((double)(plans1.get(0).getAtom(1).getTypeComponent("Cost2")) == (double)(plans2.get(0).getAtom(1).getTypeComponent("Cost2")));
	}

	//@Rule
	//public ExpectedException thrown = ExpectedException.none();
	
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain without uncertainty using a VCG rule
	 */
	@Test
	public void testDoubleAuction2BuyersVCG() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.5);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.1);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.3);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.3);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		planner.makeNonInjectable();
		
		List<Type> plans = planner.getPlans();

		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 6.5) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 6.0) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.7) < 1e-6);
				
		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("VCG");
		dsm.setPaymentCorrectionRule("None");
		dsm.solveIt();
		
		Allocation allocation = dsm.getAllocation();
		assertTrue( allocation.getAuctioneerId(0) == 1 );
		assertTrue( allocation.getAuctioneerIndexById(1) == 0 );
		assertTrue( allocation.getAllocatedWelfare() == 3.0);
		//assertTrue( allocation.getAgentAllocatedWelfareContribution(0) == 3.0 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(allocation.getAuctioneerIndexById(1)).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(allocation.getAuctioneerIndexById(1)).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
		assertTrue( allocation.getAuctioneersAllocatedValue(0) == 6.0 );
		assertTrue( allocation.getNumberOfAllocatedAuctioneers() == 1 );
		assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0) - 1.8) < 1e-6 );
		assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1) - 1.2) < 1e-6 );
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 5 );
		assertTrue( allocation.getTotalAllocatedBiddersValue(0) == 3.0 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 3.0) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-2.7)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-4.2)) < 1e-6 );
		//thrown.expect(RuntimeException.class);
	    //thrown.expectMessage("The index 1 exceeds the number of allocated buyers: 1");
	    //allocation.getAgentId(1);
		//thrown.none();
	}
	
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain without uncertainty using a Threshold rule
	 */
	@Test
	public void testDoubleAuction2BuyersThreshold() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.5);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.1);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.3);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.3);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		planner.makeNonInjectable();
		
		List<Type> plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 6.5) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 6.0) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.1) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.8) < 1e-6);

		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("Threshold");
		dsm.setPaymentCorrectionRule("None");
		dsm.solveIt();
		
		Allocation allocation = dsm.getAllocation();
		assertTrue( allocation.getAuctioneerId(0) == 1 );
		assertTrue( allocation.getAuctioneerIndexById(1) == 0 );
		assertTrue( allocation.getAllocatedWelfare() == 3.0);
		//assertTrue( allocation.getAgentAllocatedWelfareContribution(0) == 3.0 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(allocation.getAuctioneerIndexById(1)).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(allocation.getAuctioneerIndexById(1)).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesOfTrade(0).get(0) == 1 );
		assertTrue( allocation.getAuctioneersAllocatedValue(0) == 6.0 );
		assertTrue( allocation.getNumberOfAllocatedAuctioneers() == 1 );
		assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 0) - 1.8) < 1e-6 );
		assertTrue( Math.abs(allocation.getBiddersAllocatedValue(0, 1) - 1.2) < 1e-6 );
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 5 );
		assertTrue( allocation.getTotalAllocatedBiddersValue(0) == 3.0 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 4.5) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-1.8)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-2.7)) < 1e-6 );
	}
	
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain with uncertainty using a VCG rule
	 */
	/*@Test
	public void testDoubleAuction2BuyersVCG_Uncertainty() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.5);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.1);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.3);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.3);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		
		List<Type> plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 6.5) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 6.0) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.1) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.8) < 1e-6);

		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("VCG");
		dsm.setPaymentCorrectionRule("None");
		dsm.solveIt();
						
		Allocation allocation = dsm.getAllocation();

		assertTrue( allocation.getAgentId(0) == 1 );
		assertTrue( allocation.getAgentIndexById(1) == 0 );
		assertTrue( Math.abs(allocation.getWelfare() - 2.4) < 1e-6);
		assertTrue( Math.abs(allocation.getAgentWelfareContribution(0) - 2.4) < 1e-6 );
		assertTrue( allocation.getAllocatedBundlesById(1).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesById(1).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1 );
		assertTrue( Math.abs(allocation.getBuyersValue(0) - 5.0) < 1e-6 );
		assertTrue( allocation.getNumberOfAllocatedBuyers() == 1 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 0) - 1.4) < 1e-6 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 1) - 1.2) < 1e-6 );
		assertTrue( allocation.getSellersInvolved(0).size() == 2 );
		assertTrue( allocation.getSellersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getSellersInvolved(0).get(1) == 5 );
		assertTrue( Math.abs(allocation.getTotalCost(0) - 2.6) < 1e-6 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 2.6) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-1.7)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-3.6)) < 1e-6 );
	}
	*/
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain with uncertainty using a VCG rule
	 */
	/*@Test
	public void testDoubleAuction2BuyersVCG2_Uncertainty() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.5);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.3);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.3);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		
		List<Type> plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.3) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.5) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 5.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 4.0) < 1e-6);

		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("VCG");
		dsm.setPaymentCorrectionRule("None");
		dsm.solveIt();
								
		Allocation allocation = dsm.getAllocation();

		assertTrue( allocation.getAgentId(0) == 2 );
		assertTrue( allocation.getAgentIndexById(2) == 0 );
		assertTrue( Math.abs(allocation.getWelfare() - 1.6) < 1e-6);
		assertTrue( Math.abs(allocation.getAgentWelfareContribution(0) - 1.6) < 1e-6 );
		assertTrue( allocation.getAllocatedBundlesById(2).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesById(2).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1 );
		assertTrue( Math.abs(allocation.getBuyersValue(0) - 3.0) < 1e-6 );
		assertTrue( allocation.getNumberOfAllocatedBuyers() == 1 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 0) - 1.0) < 1e-6 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 1) - 0.4) < 1e-6 );
		assertTrue( allocation.getSellersInvolved(0).size() == 2 );
		assertTrue( allocation.getSellersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getSellersInvolved(0).get(1) == 5 );
		assertTrue( Math.abs(allocation.getTotalCost(0) - 1.4) < 1e-6 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 1.4) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-1.0)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-2.0)) < 1e-6 );		
	}*/
	
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain with uncertainty using a VCG rule (negative VCG discounts)
	 */
	/*@Test
	public void testDoubleAuction2BuyersVCG3_Uncertainty() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.5);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.28);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.28);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		
		List<Type> plans = planner.getPlans();
		System.out.println(">> " + plans.toString());
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.24) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.3) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 5.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 4.0) < 1e-6);

		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("VCG");
		dsm.setPaymentCorrectionRule("None");
		dsm.solveIt();
								
		Allocation allocation = dsm.getAllocation();

		assertTrue( allocation.getAgentId(0) == 2 );
		assertTrue( allocation.getAgentIndexById(2) == 0 );
		assertTrue( Math.abs(allocation.getWelfare() - 1.6) < 1e-6);
		assertTrue( Math.abs(allocation.getAgentWelfareContribution(0) - 1.6) < 1e-6 );
		assertTrue( allocation.getAllocatedBundlesById(2).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesById(2).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1 );
		assertTrue( Math.abs(allocation.getBuyersValue(0) - 3.0) < 1e-6 );
		assertTrue( allocation.getNumberOfAllocatedBuyers() == 1 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 0) - 1.0) < 1e-6 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 1) - 0.4) < 1e-6 );
		assertTrue( allocation.getSellersInvolved(0).size() == 2 );
		assertTrue( allocation.getSellersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getSellersInvolved(0).get(1) == 5 );
		assertTrue( Math.abs(allocation.getTotalCost(0) - 1.4) < 1e-6 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 1.4) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-0.9)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-2.0)) < 1e-6 );		
	}*/
	
	/*
	 * Scenario:
	 * - create a double auction with two buyers and three sellers. Each buyer is associated with two plans
	 * - solve the auction in a domain with uncertainty using a Threshold rule (negative VCG discounts)
	 */
	/*@Test
	public void testDoubleAuction2BuyersThreshold_Uncertainty() throws Exception
	{
		int numberOfBuyers = 2;
		int numberOfSellers = 3;
		
		List<Integer> items1 = new LinkedList<Integer>();		//a query of the 1st buyer
		items1.add(0);
		
		List<Integer> items2 = new LinkedList<Integer>();		//a query of the 2nd buyer
		items2.add(1);
		
		
		AtomicBid atomB1 = new AtomicBid(1, items1, 0.1);
		atomB1.setTypeComponent("isSeller", 0.0);
		CombinatorialType b1 = new CombinatorialType(); 		//The type of the 1st buyer
		b1.addAtomicBid(atomB1);
		
		AtomicBid atomB2 = new AtomicBid(2, items2, 0.5);
		atomB2.setTypeComponent("isSeller", 0.0);
		CombinatorialType b2 = new CombinatorialType(); 		//The type of the 2nd buyer
		b2.addAtomicBid(atomB2);
		
		AtomicBid atomS11 = new AtomicBid(3, items1, 0.2);
		atomS11.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS12 = new AtomicBid(3, items2, 0.2);
		atomS12.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS11);
		s1.addAtomicBid(atomS12);
		
		AtomicBid atomS21 = new AtomicBid(4, items1, 0.28);
		atomS21.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS22 = new AtomicBid(4, items2, 0.28);
		atomS22.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS21);
		s2.addAtomicBid(atomS22);
		
		AtomicBid atomS31 = new AtomicBid(5, items1, 0.4);
		atomS31.setTypeComponent("isSeller", 1.0);
		AtomicBid atomS32 = new AtomicBid(5, items2, 0.4);
		atomS32.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS31);
		s3.addAtomicBid(atomS32);
		
		List<Type> bids = new LinkedList<Type>();
		bids.add(b1);
		bids.add(b2);
		bids.add(s1);
		bids.add(s2);
		bids.add(s3);
		
		Planner planner = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		planner.setNumberOfPlans(2);
		planner.setMinSellersPerPlan(2);
		planner.setMaxSellersPerPlan(2);
		
		Field field = Planner.class.getDeclaredField("_errorScenarioSeed");
		field.setAccessible(true);
		field.set(planner, 1234560);
		
		planner.reset( bids);
		planner.generatePlans();
		
		List<Type> plans = planner.getPlans();
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.24) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.3) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 5.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 0.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 4.0) < 1e-6);

		DoubleSidedMarket dsm = new DoubleSidedMarket(numberOfBuyers, numberOfSellers, bids, planner);
		dsm.setPaymentRule("Threshold");
		dsm.setPaymentCorrectionRule("Trim");
		dsm.solveIt();
								
		Allocation allocation = dsm.getAllocation();

		assertTrue( allocation.getAgentId(0) == 2 );
		assertTrue( allocation.getAgentIndexById(2) == 0 );
		assertTrue( Math.abs(allocation.getWelfare() - 1.6) < 1e-6);
		assertTrue( Math.abs(allocation.getAgentWelfareContribution(0) - 1.6) < 1e-6 );
		assertTrue( allocation.getAllocatedBundlesById(2).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesById(2).get(0) == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).size() == 1 );
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1 );
		assertTrue( Math.abs(allocation.getBuyersValue(0) - 3.0) < 1e-6 );
		assertTrue( allocation.getNumberOfAllocatedBuyers() == 1 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 0) - 1.0) < 1e-6 );
		assertTrue( Math.abs(allocation.getSellersCost(0, 1) - 0.4) < 1e-6 );
		assertTrue( allocation.getSellersInvolved(0).size() == 2 );
		assertTrue( allocation.getSellersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getSellersInvolved(0).get(1) == 5 );
		assertTrue( Math.abs(allocation.getTotalCost(0) - 1.4) < 1e-6 );
		
		assertTrue( Math.abs(dsm.getPayments()[0] - 2.2) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[1] - (-1.0)) < 1e-6 );
		assertTrue( Math.abs(dsm.getPayments()[2] - (-1.2)) < 1e-6 );		
	}
	*/
}
