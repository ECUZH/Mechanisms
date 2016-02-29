package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;
import ch.uzh.ifi.MechanismDesignPrimitives.QueryFragment;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class testZollingerMechanism {

	/*@Test
	public void testNonFragmented() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		double marginalValue1 = 0.6;
		double marginalCost1 = 0.89;
		double marginalCost2 = 0.31;
		double marginalCost3 = 0.06;
		AtomicBid atomB = new AtomicBid(1, items, marginalValue1);
		atomB.setTypeComponent("isSeller", 0.0);
		CombinatorialType b = new CombinatorialType(); 			//The type of the buyer
		b.addAtomicBid(atomB);
		
		AtomicBid atomS1 = new AtomicBid(2, items, marginalCost1);
		atomS1.setTypeComponent("isSeller", 1.0);
		CombinatorialType s1 = new CombinatorialType(); 		//The type of the 1st seller
		s1.addAtomicBid(atomS1);
		
		AtomicBid atomS2 = new AtomicBid(3, items, marginalCost2);
		atomS2.setTypeComponent("isSeller", 1.0);
		CombinatorialType s2 = new CombinatorialType(); 		//The type of the 2nd seller
		s2.addAtomicBid(atomS2);
		s2.addAtomicBid(atomS2);
		
		AtomicBid atomS3 = new AtomicBid(4, items, marginalCost3);
		atomS3.setTypeComponent("isSeller", 1.0);
		CombinatorialType s3 = new CombinatorialType(); 		//The type of the 3rd seller
		s3.addAtomicBid(atomS3);
		s3.addAtomicBid(atomS3);
		
		List<Type> plans = new LinkedList<Type>();
		
		CombinatorialType bid = new CombinatorialType();
		
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		List<Double> marginalCostsAssociated1 = new LinkedList<Double>();
		List<Integer> minNumberOfTuples1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfTuples1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(4);
		marginalCostsAssociated1.add(marginalCost1);
		marginalCostsAssociated1.add(marginalCost3);
		minNumberOfTuples1.add(0);
		minNumberOfTuples1.add(0);
		maxNumberOfTuples1.add(3);
		maxNumberOfTuples1.add(3);
		AtomicBid plan1 = new SemanticWebType(1, serversUsed1, marginalValue1, marginalCostsAssociated1, minNumberOfTuples1, maxNumberOfTuples1);
		
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		List<Double> marginalCostsAssociated2 = new LinkedList<Double>();
		List<Integer> minNumberOfTuples2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfTuples2 = new LinkedList<Integer>();
		serversUsed2.add(3);
		serversUsed2.add(4);
		marginalCostsAssociated2.add(marginalCost2);
		marginalCostsAssociated2.add(marginalCost3);
		minNumberOfTuples2.add(0);
		minNumberOfTuples2.add(0);
		maxNumberOfTuples2.add(5);
		maxNumberOfTuples2.add(1);
		AtomicBid plan2 = new SemanticWebType(1, serversUsed2, marginalValue1, marginalCostsAssociated2, minNumberOfTuples2, maxNumberOfTuples2);
		
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		plans.add(bid);								//bid corresponds to all semantic web plans for a particular buyer 
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setPaymentRule("FirstPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		//Test if allocated plan is correct
		assertTrue( allocation.getAgentId(0) == 1 );
		assertTrue( allocation.getNumberOfAllocatedBuyers() == 1);
		assertTrue( allocation.getAllocatedBundlesById(1).size() == 1 );
		assertTrue( allocation.getSellersInvolved(0).size() == 2 );
		assertTrue( allocation.getSellersInvolved(0).get(0) == 3 );
		assertTrue( allocation.getSellersInvolved(0).get(1) == 4 );
		assertTrue( Math.abs( allocation.getExpectedWelfare() - 0.995) < 1e-6 );
		assertTrue( Math.abs( allocation.getAllocatedWelfare()- 1.160) < 1e-6 );
		
		//Test if the number of actually allocated triples is correct
		assertTrue( ((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getActuallyAllocatedTuples(0) == 4 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getActuallyAllocatedTuples(1) == 0 );

		//Test expected costs
		assertTrue( Math.abs(((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getExpectedCost(0) - 0.775)< 1e-6 );
		assertTrue( Math.abs(((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getExpectedCost(1) - 0.03)< 1e-6 );
		
		//Test an actual cost after allocation (not an expected one)
		assertTrue( Math.abs(((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getActualCost(0) - 1.24)< 1e-6 );
		assertTrue( Math.abs(((SemanticWebType)plans.get(0).getAtom( allocation.getAllocatedBundlesById(1).get(0) )).getActualCost(1) - 0)< 1e-6 );
		
		//Another way of how to test an actual cost after allocation (not an expected one)
		assertTrue( Math.abs( allocation.getSellersAllocatedCost(0, 0) - 1.24) < 1e-6 );
		assertTrue( Math.abs( allocation.getSellersAllocatedCost(0, 1) - 0) < 1e-6 );
		
		//Test an allocated and expected values of a buyer for this plan
		assertTrue( Math.abs( allocation.getBuyersAllocatedValue(0) - 2.4) < 1e-6 );
		assertTrue( Math.abs( allocation.getBuyersExpectedValue(0) - 1.8) < 1e-6 );

		//Test First Price payments
		assertTrue( Math.abs(ca.getPayments()[0] - 0.775) < 1e-6 );
		assertTrue( Math.abs(ca.getPayments()[1] - 0.03 ) < 1e-6 );
	}*/

	@Test
	public void testUtilityMaximizationSP() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		List<Integer> serversUsed = new LinkedList<Integer>();
		serversUsed.add(2);
		serversUsed.add(3);
		serversUsed.add(4);
		serversUsed.add(5);
		
		List<QueryFragment> queryFragmetnsP1 = new LinkedList<QueryFragment>();
		List<QueryFragment> queryFragmetnsP2 = new LinkedList<QueryFragment>();
		
		//Query Fragment #1
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(3);
		List<Double> costs1 = new LinkedList<Double>();
		costs1.add(0.1);
		costs1.add(0.45);
		List<Integer> minNumberOfRecords1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords1 = new LinkedList<Integer>();
		minNumberOfRecords1.add(0);
		minNumberOfRecords1.add(0);
		maxNumberOfRecords1.add(2);
		maxNumberOfRecords1.add(2);
		QueryFragment queryFragment1 = new QueryFragment(serversUsed1, costs1, minNumberOfRecords1, maxNumberOfRecords1);
		
		//Query Fragment #2
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costs2 = new LinkedList<Double>();
		costs2.add(0.4);
		costs2.add(0.1);
		List<Integer> minNumberOfRecords2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords2 = new LinkedList<Integer>();
		minNumberOfRecords2.add(0);
		minNumberOfRecords2.add(0);
		maxNumberOfRecords2.add(2);
		maxNumberOfRecords2.add(2);
		QueryFragment queryFragment2 = new QueryFragment(serversUsed2, costs2, minNumberOfRecords2, maxNumberOfRecords2);

		//Query Fragment #3
		List<Integer> serversUsed3 = new LinkedList<Integer>();
		serversUsed3.add(4);
		serversUsed3.add(5);
		List<Double> costs3 = new LinkedList<Double>();
		costs3.add(0.2);
		costs3.add(0.4);
		List<Integer> minNumberOfRecords3 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords3 = new LinkedList<Integer>();
		minNumberOfRecords3.add(0);
		minNumberOfRecords3.add(0);
		maxNumberOfRecords3.add(2);
		maxNumberOfRecords3.add(2);
		QueryFragment queryFragment3 = new QueryFragment(serversUsed3, costs3, minNumberOfRecords3, maxNumberOfRecords3);
		
		//Query Fragment #4
		List<Integer> serversUsed4 = new LinkedList<Integer>();
		serversUsed4.add(4);
		serversUsed4.add(5);
		List<Double> costs4 = new LinkedList<Double>();
		costs4.add(0.4);
		costs4.add(0.2);
		List<Integer> minNumberOfRecords4 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords4 = new LinkedList<Integer>();
		minNumberOfRecords4.add(0);
		minNumberOfRecords4.add(0);
		maxNumberOfRecords4.add(2);
		maxNumberOfRecords4.add(2);
		QueryFragment queryFragment4 = new QueryFragment(serversUsed4, costs4, minNumberOfRecords4, maxNumberOfRecords4);

		queryFragmetnsP1.add(queryFragment1);
		queryFragmetnsP1.add(queryFragment2);
		queryFragmetnsP2.add(queryFragment3);
		queryFragmetnsP2.add(queryFragment4);
				
		ComplexSemanticWebType plan1 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP1);
		ComplexSemanticWebType plan2 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP2);
		
		CombinatorialType bid = new CombinatorialType();
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		List<Type> plans = new LinkedList<Type>();
		plans.add(bid);
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("BuyerUtility");
		ca.setPaymentRule("SecondPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 4 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 5 );

		assertTrue( ca.getPayments().length == 2 );
		assertTrue( ca.getPayments()[0] == 0.4 );
		assertTrue( ca.getPayments()[1] == 0.4 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 1.4) < 1e-6 );
	}
	
	@Test
	public void testSWMaximizationSP() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		List<Integer> serversUsed = new LinkedList<Integer>();
		serversUsed.add(2);
		serversUsed.add(3);
		serversUsed.add(4);
		serversUsed.add(5);
		
		List<QueryFragment> queryFragmetnsP1 = new LinkedList<QueryFragment>();
		List<QueryFragment> queryFragmetnsP2 = new LinkedList<QueryFragment>();
		
		//Query Fragment #1
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(3);
		List<Double> costs1 = new LinkedList<Double>();
		costs1.add(0.1);
		costs1.add(0.45);
		List<Integer> minNumberOfRecords1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords1 = new LinkedList<Integer>();
		minNumberOfRecords1.add(0);
		minNumberOfRecords1.add(0);
		maxNumberOfRecords1.add(2);
		maxNumberOfRecords1.add(2);
		QueryFragment queryFragment1 = new QueryFragment(serversUsed1, costs1, minNumberOfRecords1, maxNumberOfRecords1);
		
		//Query Fragment #2
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costs2 = new LinkedList<Double>();
		costs2.add(0.4);
		costs2.add(0.1);
		List<Integer> minNumberOfRecords2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords2 = new LinkedList<Integer>();
		minNumberOfRecords2.add(0);
		minNumberOfRecords2.add(0);
		maxNumberOfRecords2.add(2);
		maxNumberOfRecords2.add(2);
		QueryFragment queryFragment2 = new QueryFragment(serversUsed2, costs2, minNumberOfRecords2, maxNumberOfRecords2);

		//Query Fragment #3
		List<Integer> serversUsed3 = new LinkedList<Integer>();
		serversUsed3.add(4);
		serversUsed3.add(5);
		List<Double> costs3 = new LinkedList<Double>();
		costs3.add(0.2);
		costs3.add(0.4);
		List<Integer> minNumberOfRecords3 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords3 = new LinkedList<Integer>();
		minNumberOfRecords3.add(0);
		minNumberOfRecords3.add(0);
		maxNumberOfRecords3.add(2);
		maxNumberOfRecords3.add(2);
		QueryFragment queryFragment3 = new QueryFragment(serversUsed3, costs3, minNumberOfRecords3, maxNumberOfRecords3);
		
		//Query Fragment #4
		List<Integer> serversUsed4 = new LinkedList<Integer>();
		serversUsed4.add(4);
		serversUsed4.add(5);
		List<Double> costs4 = new LinkedList<Double>();
		costs4.add(0.4);
		costs4.add(0.2);
		List<Integer> minNumberOfRecords4 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords4 = new LinkedList<Integer>();
		minNumberOfRecords4.add(0);
		minNumberOfRecords4.add(0);
		maxNumberOfRecords4.add(2);
		maxNumberOfRecords4.add(2);
		QueryFragment queryFragment4 = new QueryFragment(serversUsed4, costs4, minNumberOfRecords4, maxNumberOfRecords4);

		queryFragmetnsP1.add(queryFragment1);
		queryFragmetnsP1.add(queryFragment2);
		queryFragmetnsP2.add(queryFragment3);
		queryFragmetnsP2.add(queryFragment4);
				
		ComplexSemanticWebType plan1 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP1);
		ComplexSemanticWebType plan2 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP2);
		
		CombinatorialType bid = new CombinatorialType();
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		List<Type> plans = new LinkedList<Type>();
		plans.add(bid);
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("SocialWelfare");
		ca.setPaymentRule("SecondPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );

		assertTrue( ca.getPayments().length == 2 );
		assertTrue( ca.getPayments()[0] == 0.45 );
		assertTrue( ca.getPayments()[1] == 0.4 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 1.6) < 1e-6 );
	}
	
	@Test
	public void testSWMaximizationSP1() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		List<Integer> serversUsed = new LinkedList<Integer>();
		serversUsed.add(2);
		serversUsed.add(3);
		serversUsed.add(4);
		serversUsed.add(5);
		
		List<QueryFragment> queryFragmetnsP1 = new LinkedList<QueryFragment>();
		List<QueryFragment> queryFragmetnsP2 = new LinkedList<QueryFragment>();
		
		//Query Fragment #1
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(3);
		List<Double> costs1 = new LinkedList<Double>();
		costs1.add(0.1);
		costs1.add(0.45);
		List<Integer> minNumberOfRecords1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords1 = new LinkedList<Integer>();
		minNumberOfRecords1.add(0);
		minNumberOfRecords1.add(0);
		maxNumberOfRecords1.add(2);
		maxNumberOfRecords1.add(2);
		QueryFragment queryFragment1 = new QueryFragment(serversUsed1, costs1, minNumberOfRecords1, maxNumberOfRecords1);
		
		//Query Fragment #2
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costs2 = new LinkedList<Double>();
		costs2.add(0.09);
		costs2.add(0.1);
		List<Integer> minNumberOfRecords2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords2 = new LinkedList<Integer>();
		minNumberOfRecords2.add(0);
		minNumberOfRecords2.add(0);
		maxNumberOfRecords2.add(2);
		maxNumberOfRecords2.add(2);
		QueryFragment queryFragment2 = new QueryFragment(serversUsed2, costs2, minNumberOfRecords2, maxNumberOfRecords2);

		//Query Fragment #3
		List<Integer> serversUsed3 = new LinkedList<Integer>();
		serversUsed3.add(4);
		serversUsed3.add(5);
		List<Double> costs3 = new LinkedList<Double>();
		costs3.add(0.2);
		costs3.add(0.4);
		List<Integer> minNumberOfRecords3 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords3 = new LinkedList<Integer>();
		minNumberOfRecords3.add(0);
		minNumberOfRecords3.add(0);
		maxNumberOfRecords3.add(2);
		maxNumberOfRecords3.add(2);
		QueryFragment queryFragment3 = new QueryFragment(serversUsed3, costs3, minNumberOfRecords3, maxNumberOfRecords3);
		
		//Query Fragment #4
		List<Integer> serversUsed4 = new LinkedList<Integer>();
		serversUsed4.add(4);
		serversUsed4.add(5);
		List<Double> costs4 = new LinkedList<Double>();
		costs4.add(0.4);
		costs4.add(0.2);
		List<Integer> minNumberOfRecords4 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords4 = new LinkedList<Integer>();
		minNumberOfRecords4.add(0);
		minNumberOfRecords4.add(0);
		maxNumberOfRecords4.add(2);
		maxNumberOfRecords4.add(2);
		QueryFragment queryFragment4 = new QueryFragment(serversUsed4, costs4, minNumberOfRecords4, maxNumberOfRecords4);

		queryFragmetnsP1.add(queryFragment1);
		queryFragmetnsP1.add(queryFragment2);
		queryFragmetnsP2.add(queryFragment3);
		queryFragmetnsP2.add(queryFragment4);
				
		ComplexSemanticWebType plan1 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP1);
		ComplexSemanticWebType plan2 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP2);
		
		CombinatorialType bid = new CombinatorialType();
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		List<Type> plans = new LinkedList<Type>();
		plans.add(bid);
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("SocialWelfare");
		ca.setPaymentRule("SecondPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );

		assertTrue( ca.getPayments().length == 1 );
		assertTrue( ca.getPayments()[0] == 0.55 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 1.61) < 1e-6 );
	}
	
	@Test
	public void testUtilityMaximizationFP() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		List<Integer> serversUsed = new LinkedList<Integer>();
		serversUsed.add(2);
		serversUsed.add(3);
		serversUsed.add(4);
		serversUsed.add(5);
		
		List<QueryFragment> queryFragmetnsP1 = new LinkedList<QueryFragment>();
		List<QueryFragment> queryFragmetnsP2 = new LinkedList<QueryFragment>();
		
		//Query Fragment #1
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(3);
		List<Double> costs1 = new LinkedList<Double>();
		costs1.add(0.1);
		costs1.add(0.45);
		List<Integer> minNumberOfRecords1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords1 = new LinkedList<Integer>();
		minNumberOfRecords1.add(0);
		minNumberOfRecords1.add(0);
		maxNumberOfRecords1.add(2);
		maxNumberOfRecords1.add(2);
		QueryFragment queryFragment1 = new QueryFragment(serversUsed1, costs1, minNumberOfRecords1, maxNumberOfRecords1);
		
		//Query Fragment #2
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costs2 = new LinkedList<Double>();
		costs2.add(0.4);
		costs2.add(0.1);
		List<Integer> minNumberOfRecords2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords2 = new LinkedList<Integer>();
		minNumberOfRecords2.add(0);
		minNumberOfRecords2.add(0);
		maxNumberOfRecords2.add(2);
		maxNumberOfRecords2.add(2);
		QueryFragment queryFragment2 = new QueryFragment(serversUsed2, costs2, minNumberOfRecords2, maxNumberOfRecords2);

		//Query Fragment #3
		List<Integer> serversUsed3 = new LinkedList<Integer>();
		serversUsed3.add(4);
		serversUsed3.add(5);
		List<Double> costs3 = new LinkedList<Double>();
		costs3.add(0.2);
		costs3.add(0.4);
		List<Integer> minNumberOfRecords3 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords3 = new LinkedList<Integer>();
		minNumberOfRecords3.add(0);
		minNumberOfRecords3.add(0);
		maxNumberOfRecords3.add(2);
		maxNumberOfRecords3.add(2);
		QueryFragment queryFragment3 = new QueryFragment(serversUsed3, costs3, minNumberOfRecords3, maxNumberOfRecords3);
		
		//Query Fragment #4
		List<Integer> serversUsed4 = new LinkedList<Integer>();
		serversUsed4.add(4);
		serversUsed4.add(5);
		List<Double> costs4 = new LinkedList<Double>();
		costs4.add(0.4);
		costs4.add(0.2);
		List<Integer> minNumberOfRecords4 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords4 = new LinkedList<Integer>();
		minNumberOfRecords4.add(0);
		minNumberOfRecords4.add(0);
		maxNumberOfRecords4.add(2);
		maxNumberOfRecords4.add(2);
		QueryFragment queryFragment4 = new QueryFragment(serversUsed4, costs4, minNumberOfRecords4, maxNumberOfRecords4);

		queryFragmetnsP1.add(queryFragment1);
		queryFragmetnsP1.add(queryFragment2);
		queryFragmetnsP2.add(queryFragment3);
		queryFragmetnsP2.add(queryFragment4);
				
		ComplexSemanticWebType plan1 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP1);
		ComplexSemanticWebType plan2 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP2);
		
		CombinatorialType bid = new CombinatorialType();
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		List<Type> plans = new LinkedList<Type>();
		plans.add(bid);
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("BuyerUtility");
		ca.setPaymentRule("FirstPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );

		assertTrue( ca.getPayments().length == 2 );
		assertTrue( ca.getPayments()[0] == 0.1 );
		assertTrue( ca.getPayments()[1] == 0.1 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 1.6) < 1e-6 );
	}
	
	@Test
	public void testSWMaximizationFP() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Integer> items = new LinkedList<Integer>();		//a query of the buyer
		items.add(0);
		
		List<Integer> serversUsed = new LinkedList<Integer>();
		serversUsed.add(2);
		serversUsed.add(3);
		serversUsed.add(4);
		serversUsed.add(5);
		
		List<QueryFragment> queryFragmetnsP1 = new LinkedList<QueryFragment>();
		List<QueryFragment> queryFragmetnsP2 = new LinkedList<QueryFragment>();
		
		//Query Fragment #1
		List<Integer> serversUsed1 = new LinkedList<Integer>();
		serversUsed1.add(2);
		serversUsed1.add(3);
		List<Double> costs1 = new LinkedList<Double>();
		costs1.add(0.1);
		costs1.add(0.45);
		List<Integer> minNumberOfRecords1 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords1 = new LinkedList<Integer>();
		minNumberOfRecords1.add(0);
		minNumberOfRecords1.add(0);
		maxNumberOfRecords1.add(2);
		maxNumberOfRecords1.add(2);
		QueryFragment queryFragment1 = new QueryFragment(serversUsed1, costs1, minNumberOfRecords1, maxNumberOfRecords1);
		
		//Query Fragment #2
		List<Integer> serversUsed2 = new LinkedList<Integer>();
		serversUsed2.add(2);
		serversUsed2.add(3);
		List<Double> costs2 = new LinkedList<Double>();
		costs2.add(0.4);
		costs2.add(0.1);
		List<Integer> minNumberOfRecords2 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords2 = new LinkedList<Integer>();
		minNumberOfRecords2.add(0);
		minNumberOfRecords2.add(0);
		maxNumberOfRecords2.add(2);
		maxNumberOfRecords2.add(2);
		QueryFragment queryFragment2 = new QueryFragment(serversUsed2, costs2, minNumberOfRecords2, maxNumberOfRecords2);

		//Query Fragment #3
		List<Integer> serversUsed3 = new LinkedList<Integer>();
		serversUsed3.add(4);
		serversUsed3.add(5);
		List<Double> costs3 = new LinkedList<Double>();
		costs3.add(0.2);
		costs3.add(0.4);
		List<Integer> minNumberOfRecords3 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords3 = new LinkedList<Integer>();
		minNumberOfRecords3.add(0);
		minNumberOfRecords3.add(0);
		maxNumberOfRecords3.add(2);
		maxNumberOfRecords3.add(2);
		QueryFragment queryFragment3 = new QueryFragment(serversUsed3, costs3, minNumberOfRecords3, maxNumberOfRecords3);
		
		//Query Fragment #4
		List<Integer> serversUsed4 = new LinkedList<Integer>();
		serversUsed4.add(4);
		serversUsed4.add(5);
		List<Double> costs4 = new LinkedList<Double>();
		costs4.add(0.4);
		costs4.add(0.2);
		List<Integer> minNumberOfRecords4 = new LinkedList<Integer>();
		List<Integer> maxNumberOfRecords4 = new LinkedList<Integer>();
		minNumberOfRecords4.add(0);
		minNumberOfRecords4.add(0);
		maxNumberOfRecords4.add(2);
		maxNumberOfRecords4.add(2);
		QueryFragment queryFragment4 = new QueryFragment(serversUsed4, costs4, minNumberOfRecords4, maxNumberOfRecords4);

		queryFragmetnsP1.add(queryFragment1);
		queryFragmetnsP1.add(queryFragment2);
		queryFragmetnsP2.add(queryFragment3);
		queryFragmetnsP2.add(queryFragment4);
				
		ComplexSemanticWebType plan1 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP1);
		ComplexSemanticWebType plan2 = new ComplexSemanticWebType(1, serversUsed, 0.9, queryFragmetnsP2);
		
		CombinatorialType bid = new CombinatorialType();
		bid.addAtomicBid(plan1);
		bid.addAtomicBid(plan2);
		
		List<Type> plans = new LinkedList<Type>();
		plans.add(bid);
		
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("SocialWelfare");
		ca.setPaymentRule("FirstPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );
		assertTrue( allocation.getBiddersInvolved(0).get(1) == 3 );

		assertTrue( ca.getPayments().length == 2 );
		assertTrue( ca.getPayments()[0] == 0.1 );
		assertTrue( ca.getPayments()[1] == 0.1 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 1.6) < 1e-6 );
	}
	
	@Test
	public void testZollingerWithPlannerU() throws Exception 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Type> types = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);									//a query
		
		AtomicBid atomB = new AtomicBid( 1, items, 0.9 );
		atomB.setTypeComponent("isSeller", 0.0);
		atomB.setTypeComponent("Distribution", 1.0);
		atomB.setTypeComponent("minValue", 0.0);
		atomB.setTypeComponent("maxValue", 1.0);
		CombinatorialType b = new CombinatorialType(); //Buyer's type
		b.addAtomicBid(atomB);
		types.add(b);
	
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( numberOfBuyers + 1 + i , items,  0.1 * (i+1) );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();//Seller's type
			s.addAtomicBid(atomS);
			types.add(s);
		}
		
		FragmentedProbabilisticPlanner planner = new FragmentedProbabilisticPlanner(numberOfBuyers, numberOfSellers, types, 100000);
		planner.setNumberOfPlans(2);
		planner.reset(types);
		List<Type> plans = planner.generatePlans();
		
		System.out.println("Plans: " + plans.toString());
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("BuyerUtility");
		ca.setPaymentRule("SecondPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		//System.out.println("\n\nPlans after alloc: " + plans.toString());
		Allocation allocation = ca.getAllocation();
		
		//System.out.println("Allocated bundle: " + allocation.getAllocatedBundlesByIndex(0).get(0));
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1);
		assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );

		assertTrue( ca.getPayments().length == 1 );
		assertTrue( Math.abs(ca.getPayments()[0] - 1.2) < 1e-6 );
		
		assertTrue( Math.abs(allocation.getAllocatedWelfare() - 4.8) < 1e-6 );
	}
	
	@Test
	public void testZollingerWithPlannerSW() throws Exception
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 4;
		
		List<Type> types = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);									//a query
		
		AtomicBid atomB = new AtomicBid( 1, items, 0.9 );
		atomB.setTypeComponent("isSeller", 0.0);
		atomB.setTypeComponent("Distribution", 1.0);
		atomB.setTypeComponent("minValue", 0.0);
		atomB.setTypeComponent("maxValue", 1.0);
		CombinatorialType b = new CombinatorialType(); //Buyer's type
		b.addAtomicBid(atomB);
		types.add(b);
	
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( numberOfBuyers + 1 + i , items,  0.1 * (i+1) );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();//Seller's type
			s.addAtomicBid(atomS);
			types.add(s);
		}
		
		FragmentedProbabilisticPlanner planner = new FragmentedProbabilisticPlanner(numberOfBuyers, numberOfSellers, types, 100000);
		planner.setNumberOfPlans(2);
		planner.reset(types);
		List<Type> plans = planner.generatePlans();
		
		System.out.println("Plans: " + plans.toString());
		ZollingerMechanism ca = new ZollingerMechanism(numberOfBuyers, plans);
		ca.setAllocationRule("SocialWelfare");
		ca.setPaymentRule("SecondPrice");
		ca.setSeed(105500);
		ca.solveIt();
		
		Allocation allocation = ca.getAllocation();
		
		assertTrue( allocation.getAllocatedBundlesByIndex(0).get(0) == 1);
		assertTrue( allocation.getBiddersInvolved(0).size() == 1 );
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 2 );

		assertTrue( ca.getPayments().length == 1 );
		assertTrue( Math.abs(ca.getPayments()[0] - 1.2) < 1e-6 );
		
		//assertTrue( Math.abs(allocation.getExpectedWelfare() - 4.8) < 1e-6 );
		
		assertTrue( allocation.getAuctioneersAllocatedValue(0) == 2.7 );
		//assertTrue( allocation.getAuctioneerExpectedValue(0) == 0.9*6 );
		assertTrue( Math.abs( allocation.getAllocatedWelfare() - 2.4) < 1e-6 );
		//assertTrue( Math.abs( allocation.getExpectedWelfare() - ( 0.9*6 - 0.1*6 )) < 1e-6 );
		assertTrue( Math.abs( allocation.getBiddersAllocatedValue(0, 0) - 0.1*3 ) < 1e-6 );
		
		//System.out.println(" >> " + allocation.getSellersExpectedCost(0,0) );
		//assertTrue( Math.abs( allocation.getSellersExpectedCost(0, 0) - 0.1*6 ) < 1e-6 );
	}
}
