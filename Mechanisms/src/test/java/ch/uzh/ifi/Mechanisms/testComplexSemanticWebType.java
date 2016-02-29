package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import ch.uzh.ifi.MechanismDesignPrimitives.QueryFragment;
import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class testComplexSemanticWebType {

	@Test
	public void test() 
	{
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
		costs1.add(0.4);
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
		costs2.add(0.45);
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
		
		plan1.resolveWithSecondPrice();
		plan2.resolveWithSecondPrice();
		
		assertTrue( plan1.getAllocatedSellers().size() == 2);
		assertTrue( plan1.getAllocatedSellers().get(0) == 2);
		assertTrue( plan1.getAllocatedSellers().get(1) == 3);
		assertTrue( Math.abs(plan1.getPlanExpectedPayment() - 0.85) < 1e-6 );
		
		assertTrue( plan2.getAllocatedSellers().size() == 2);
		assertTrue( plan2.getAllocatedSellers().get(0) == 4);
		assertTrue( plan2.getAllocatedSellers().get(1) == 5);
		assertTrue( Math.abs(plan2.getPlanExpectedPayment() - 0.8) < 1e-6 );
	}

}
