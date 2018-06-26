package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ilog.concert.IloException;

public class testSurplusOptimalReverseAuction 
{


	/**
	 * Two sellers, two databases, one buyer.
	 * @throws Exception
	 */
	@Test
	public void testConstructor() throws Exception
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
	}

	/**
	 * In this scenario bidders are i.i.d. and the payment is equal to the reserve price.
	 */
	/*@Test
	public void testIIDReservePrice() 
	{
		int dbID1 = 1;
		int sellerId1 = 1;
		double sellerCost1 = 1.0;
		List<Integer> bundle1 = Arrays.asList(dbID1);
		AtomicBid atom1 = new AtomicBid(sellerId1, bundle1, sellerCost1);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		int sellerId2 = 2;
		double sellerCost2 = 1.5;
		List<Integer> bundle2 = Arrays.asList(dbID1);
		AtomicBid atom2 = new AtomicBid(sellerId2, bundle2, sellerCost2);
		
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2);
		
		double auctioneerValue = 3.2;
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, auctioneerValue);
		auction.computeWinnerDetermination();
		Allocation allocation = auction.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( auction.computePayments().size() == 1 );
		assertTrue( (auction.computePayments().get(0) - 1.6) < 1e-6);		// payment = reserve price
	}*/

	/**
	 * In this scenario bidders are i.i.d. and the payment is equal to the second lowest bid.
	 */
	/*@Test
	public void testIIDSecondPrice() 
	{
		int dbID1 = 1;
		int sellerId1 = 1;
		double sellerCost1 = 1.0;
		List<Integer> bundle1 = Arrays.asList(dbID1);
		AtomicBid atom1 = new AtomicBid(sellerId1, bundle1, sellerCost1);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		int sellerId2 = 2;
		double sellerCost2 = 1.5;
		List<Integer> bundle2 = Arrays.asList(dbID1);
		AtomicBid atom2 = new AtomicBid(sellerId2, bundle2, sellerCost2);
		
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 1., 1./3.);

		List<Type> bids = Arrays.asList(seller1, seller2);
		
		double auctioneerValue = 10.;
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, auctioneerValue);
		auction.computeWinnerDetermination();
		Allocation allocation = auction.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( auction.computePayments().size() == 1 );
		assertTrue( (auction.computePayments().get(0) - 2.) < 1e-6);		// payment = second price
	}
	*/
	/**
	 * Different distributions of sellers, second price.
	 */
	/*@Test
	public void testSecondPrice() 
	{
		int dbID1 = 1;
		int sellerId1 = 1;
		double sellerCost1 = 0.8;
		List<Integer> bundle1 = Arrays.asList(dbID1);
		AtomicBid atom1 = new AtomicBid(sellerId1, bundle1, sellerCost1);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		int sellerId2 = 2;
		double sellerCost2 = 1.;
		List<Integer> bundle2 = Arrays.asList(dbID1);
		AtomicBid atom2 = new AtomicBid(sellerId2, bundle2, sellerCost2);
		
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 0.5, 1./12.);

		List<Type> bids = Arrays.asList(seller1, seller2);
		
		double auctioneerValue = 10.;
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, auctioneerValue);
		auction.computeWinnerDetermination();
		Allocation allocation = auction.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( auction.computePayments().size() == 1 );
		assertTrue( (auction.computePayments().get(0) - 1.) < 1e-6);		// payment = second price
	}
	
	*/
	/**
	 * Different distributions of sellers, reserve price.
	 */
	/*@Test
	public void testReservePrice() 
	{
		int dbID1 = 1;
		int sellerId1 = 1;
		double sellerCost1 = 0.8;
		List<Integer> bundle1 = Arrays.asList(dbID1);
		AtomicBid atom1 = new AtomicBid(sellerId1, bundle1, sellerCost1);
		SellerType seller1 = new SellerType(atom1, Distribution.UNIFORM, 1., 1./3.);
		
		int sellerId2 = 2;
		double sellerCost2 = 1.;
		List<Integer> bundle2 = Arrays.asList(dbID1);
		AtomicBid atom2 = new AtomicBid(sellerId2, bundle2, sellerCost2);
		
		SellerType seller2 = new SellerType(atom2, Distribution.UNIFORM, 0.5, 1./12.);

		List<Type> bids = Arrays.asList(seller1, seller2);
		
		double auctioneerValue = 1.8;
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids, auctioneerValue);
		auction.computeWinnerDetermination();
		Allocation allocation = auction.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( auction.computePayments().size() == 1 );
		assertTrue( (auction.computePayments().get(0) - 0.9) < 1e-6);		// payment = second price
	}*/
}
