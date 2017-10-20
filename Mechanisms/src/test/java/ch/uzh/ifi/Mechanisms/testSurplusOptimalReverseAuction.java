package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Distribution;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

public class testSurplusOptimalReverseAuction {

	
	@Test
	public void testIID() 
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
		
		double auctioneerValue = 3.;
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(bids.size(), bids, auctioneerValue);
		auction.computeWinnerDetermination();
		Allocation allocation = auction.getAllocation();
		
		assertTrue( allocation.getBiddersInvolved(0).size() == 1);
		assertTrue( allocation.getBiddersInvolved(0).get(0) == 1);
		assertTrue( auction.computePayments().size() == 1 );
		assertTrue( (auction.computePayments().get(0) - 1.5) < 1e-6);		// payment = reserve price
		
	}

}
