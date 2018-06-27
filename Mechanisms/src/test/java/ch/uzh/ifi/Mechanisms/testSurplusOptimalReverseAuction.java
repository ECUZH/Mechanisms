package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class testSurplusOptimalReverseAuction 
{

	/**
	 * Two sellers, two databases, one buyer.
	 * @throws Exception
	 */
	@Test
	public void testBORA1() throws Exception
	{
		int[] dbIDs = {1, 2};
		
		int[] sellerIds = {1, 2};
		double[] sellerCosts = {1.5, 0.5};
		
		List<Integer> bundle1 = Arrays.asList(dbIDs[0]);
		AtomicBid atom1 = new AtomicBid(sellerIds[0], bundle1, sellerCosts[0]);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		List<Integer> bundle2 = Arrays.asList(dbIDs[1]);
		AtomicBid atom2 = new AtomicBid(sellerIds[1], bundle2, sellerCosts[1]);
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2);
		
		List<List<Double> > inducedValues = new ArrayList<List<Double> >();
		
		List<Double> inducedValuesDB1 = new ArrayList<Double>();
		inducedValuesDB1.add(0.);
		inducedValuesDB1.add(0.);
		inducedValuesDB1.add(0.);
		inducedValuesDB1.add(2.5);
		
		List<Double> inducedValuesDB2 = new ArrayList<Double>();
		inducedValuesDB2.add(0.);
		inducedValuesDB2.add(0.);
		inducedValuesDB2.add(0.);
		inducedValuesDB2.add(2.5);
		
		inducedValues.add(inducedValuesDB1);
		inducedValues.add(inducedValuesDB2);
		
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, inducedValues);
		auction.computeWinnerDetermination();
		
		assertTrue(auction.getAllocation().getNumberOfAllocatedAuctioneers() == 1);
		assertTrue(auction.getAllocation().getAuctioneerId(0) == 0);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).size() == 2);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(0) == 1);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(1) == 2);
		assertTrue(auction.getAllocation().getAuctioneersAllocatedValue(0) == 5);
		
		List<Double> payments = auction.computePayments();
		assertTrue( payments.size() == 2);
		assertTrue( payments.get(0) == 2.0);
		assertTrue( payments.get(1) == 1.0);
	}

	/**
	 * Three sellers, two databases, one buyer.
	 * @throws Exception
	 */
	@Test
	public void testBORA2() throws Exception
	{
		int[] dbIDs = {1, 2};
		
		int[] sellerIds = {1, 2, 3};
		double[] sellerCosts = {1.5, 0.5, 0.4};
		
		List<Integer> bundle1 = Arrays.asList(dbIDs[0]);
		AtomicBid atom1 = new AtomicBid(sellerIds[0], bundle1, sellerCosts[0]);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		List<Integer> bundle2 = Arrays.asList(dbIDs[1]);
		AtomicBid atom2 = new AtomicBid(sellerIds[1], bundle2, sellerCosts[1]);
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 1., 1./3.);
		
		AtomicBid atom3 = new AtomicBid(sellerIds[2], bundle2, sellerCosts[2]);
		SellerType seller3 = new SellerType(atom3, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2, seller3);
		
		List<List<Double> > inducedValues = new ArrayList<List<Double> >();
		
		List<Double> inducedValuesDB1 = new ArrayList<Double>();
																// Sellers    321
		inducedValuesDB1.add(0.);								// Allocation 000
		inducedValuesDB1.add(0.);								// Allocation 001
		inducedValuesDB1.add(0.);								// Allocation 010
		inducedValuesDB1.add(2.5);								// Allocation 011
		inducedValuesDB1.add(0.);								// Allocation 100
		inducedValuesDB1.add(2.5);								// Allocation 101
		inducedValuesDB1.add(0.);								// Allocation 110
		inducedValuesDB1.add(2.5);								// Allocation 111
		
		List<Double> inducedValuesDB2 = new ArrayList<Double>();
																// Sellers    321
		inducedValuesDB2.add(0.);								// Allocation 000
		inducedValuesDB2.add(0.);								// Allocation 001
		inducedValuesDB2.add(0.);								// Allocation 010
		inducedValuesDB2.add(2.5);								// Allocation 011
		inducedValuesDB2.add(0.);								// Allocation 100
		inducedValuesDB2.add(2.5);								// Allocation 101
		inducedValuesDB2.add(0.);								// Allocation 110
		inducedValuesDB2.add(2.5);								// Allocation 111	
		
		inducedValues.add(inducedValuesDB1);
		inducedValues.add(inducedValuesDB2);
		
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, inducedValues);
		auction.computeWinnerDetermination();
		
		assertTrue(auction.getAllocation().getNumberOfAllocatedAuctioneers() == 1);
		assertTrue(auction.getAllocation().getAuctioneerId(0) == 0);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).size() == 2);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(0) == 1);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(1) == 3);
		assertTrue(auction.getAllocation().getAuctioneersAllocatedValue(0) == 5);
		
		List<Double> payments = auction.computePayments();
		assertTrue( payments.size() == 2);
		assertTrue( payments.get(0) == 2.1);
		assertTrue( payments.get(1) == 0.5);
	}

	/**
	 * Four sellers, two databases, one buyer.
	 * @throws Exception
	 */
	@Test
	public void testBORA3() throws Exception
	{
		int[] dbIDs = {1, 2};
		
		int[] sellerIds = {1, 2, 3, 4};
		double[] sellerCosts = {1.5, 1.4, 0.5, 0.4};
		
		List<Integer> bundle1 = Arrays.asList(dbIDs[0]);
		List<Integer> bundle2 = Arrays.asList(dbIDs[1]);
		AtomicBid atom1 = new AtomicBid(sellerIds[0], bundle1, sellerCosts[0]);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		AtomicBid atom2 = new AtomicBid(sellerIds[1], bundle1, sellerCosts[1]);
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 1., 1./3.);
		
		AtomicBid atom3 = new AtomicBid(sellerIds[2], bundle2, sellerCosts[2]);
		SellerType seller3 = new SellerType(atom3, Distribution.UNIFORM, 1., 1./3.);
		
		AtomicBid atom4 = new AtomicBid(sellerIds[3], bundle2, sellerCosts[3]);
		SellerType seller4 = new SellerType(atom4, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2, seller3, seller4);
		
		List<List<Double> > inducedValues = new ArrayList<List<Double> >();
		
		List<Double> inducedValuesDB1 = new ArrayList<Double>();
		double dbValue = 2.5;
																// Sellers    4321
		inducedValuesDB1.add(0.);								// Allocation 0000
		inducedValuesDB1.add(0.);								// Allocation 0001
		inducedValuesDB1.add(0.);								// Allocation 0010
		inducedValuesDB1.add(0.);								// Allocation 0011
		inducedValuesDB1.add(0.);								// Allocation 0100
		inducedValuesDB1.add(dbValue);							// Allocation 0101
		inducedValuesDB1.add(dbValue);							// Allocation 0110
		inducedValuesDB1.add(dbValue);							// Allocation 0111
		inducedValuesDB1.add(0.);								// Allocation 1000
		inducedValuesDB1.add(dbValue);							// Allocation 1001
		inducedValuesDB1.add(dbValue);							// Allocation 1010
		inducedValuesDB1.add(dbValue);							// Allocation 1011
		inducedValuesDB1.add(0.);								// Allocation 1100
		inducedValuesDB1.add(dbValue);							// Allocation 1101
		inducedValuesDB1.add(dbValue);							// Allocation 1110
		inducedValuesDB1.add(dbValue);							// Allocation 1111
		
		List<Double> inducedValuesDB2 = new ArrayList<Double>();
																// Sellers    4321
		inducedValuesDB2.add(0.);								// Allocation 0000
		inducedValuesDB2.add(0.);								// Allocation 0001
		inducedValuesDB2.add(0.);								// Allocation 0010
		inducedValuesDB2.add(0.);								// Allocation 0011
		inducedValuesDB2.add(0.);								// Allocation 0100
		inducedValuesDB2.add(dbValue);							// Allocation 0101
		inducedValuesDB2.add(dbValue);							// Allocation 0110
		inducedValuesDB2.add(dbValue);							// Allocation 0111
		inducedValuesDB2.add(0.);								// Allocation 1000
		inducedValuesDB2.add(dbValue);							// Allocation 1001
		inducedValuesDB2.add(dbValue);							// Allocation 1010
		inducedValuesDB2.add(dbValue);							// Allocation 1011
		inducedValuesDB2.add(0.);								// Allocation 1100
		inducedValuesDB2.add(dbValue);							// Allocation 1101
		inducedValuesDB2.add(dbValue);							// Allocation 1110
		inducedValuesDB2.add(dbValue);							// Allocation 1111
		
		inducedValues.add(inducedValuesDB1);
		inducedValues.add(inducedValuesDB2);
		
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, inducedValues);
		auction.computeWinnerDetermination();
		
		assertTrue(auction.getAllocation().getNumberOfAllocatedAuctioneers() == 1);
		assertTrue(auction.getAllocation().getAuctioneerId(0) == 0);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).size() == 2);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(0) == 2);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(1) == 4);
		assertTrue(auction.getAllocation().getAuctioneersAllocatedValue(0) == 5);
		
		List<Double> payments = auction.computePayments();
		assertTrue( payments.size() == 2);
		assertTrue( payments.get(0) == 1.5);
		assertTrue( payments.get(1) == 0.5);
	}
	
	
	/**
	 * Three sellers, two databases, one buyer. Similar to testBORA2 but the seller with the largest cost
	 * wins due to the "better" cost distribution.
	 * @throws Exception
	 */
	@Test
	public void testBORA4() throws Exception
	{
		int[] dbIDs = {1, 2};
		
		int[] sellerIds = {1, 2, 3};
		double[] sellerCosts = {1.5, 0.5, 0.4};
		
		List<Integer> bundle1 = Arrays.asList(dbIDs[0]);
		AtomicBid atom1 = new AtomicBid(sellerIds[0], bundle1, sellerCosts[0]);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		List<Integer> bundle2 = Arrays.asList(dbIDs[1]);
		AtomicBid atom2 = new AtomicBid(sellerIds[1], bundle2, sellerCosts[1]);
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 0.5, 1./12. *(0.5*0.5));
		
		AtomicBid atom3 = new AtomicBid(sellerIds[2], bundle2, sellerCosts[2]);
		SellerType seller3 = new SellerType(atom3, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2, seller3);
		
		List<List<Double> > inducedValues = new ArrayList<List<Double> >();
		
		List<Double> inducedValuesDB1 = new ArrayList<Double>();
																// Sellers    321
		inducedValuesDB1.add(0.);								// Allocation 000
		inducedValuesDB1.add(0.);								// Allocation 001
		inducedValuesDB1.add(0.);								// Allocation 010
		inducedValuesDB1.add(2.5);								// Allocation 011
		inducedValuesDB1.add(0.);								// Allocation 100
		inducedValuesDB1.add(2.5);								// Allocation 101
		inducedValuesDB1.add(0.);								// Allocation 110
		inducedValuesDB1.add(2.5);								// Allocation 111
		
		List<Double> inducedValuesDB2 = new ArrayList<Double>();
																// Sellers    321
		inducedValuesDB2.add(0.);								// Allocation 000
		inducedValuesDB2.add(0.);								// Allocation 001
		inducedValuesDB2.add(0.);								// Allocation 010
		inducedValuesDB2.add(2.5);								// Allocation 011
		inducedValuesDB2.add(0.);								// Allocation 100
		inducedValuesDB2.add(2.5);								// Allocation 101
		inducedValuesDB2.add(0.);								// Allocation 110
		inducedValuesDB2.add(2.5);								// Allocation 111	
		
		inducedValues.add(inducedValuesDB1);
		inducedValues.add(inducedValuesDB2);
		
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, inducedValues);
		auction.computeWinnerDetermination();
		
		assertTrue(auction.getAllocation().getNumberOfAllocatedAuctioneers() == 1);
		assertTrue(auction.getAllocation().getAuctioneerId(0) == 0);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).size() == 2);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(0) == 1);
		assertTrue(auction.getAllocation().getBiddersInvolved(0).get(1) == 2);
		assertTrue(auction.getAllocation().getAuctioneersAllocatedValue(0) == 5);
		
		List<Double> payments = auction.computePayments();
		assertTrue( payments.size() == 2);
		assertTrue( payments.get(0) == 2.125);
		assertTrue( Math.abs(payments.get(1) - 0.525) < 1e-6);
	}
	
	/**
	 * Different distributions of sellers, second price.
	 */
	/*@Test
	public void testSecondPrice() 
	{
		
	}
	
	*/
}
