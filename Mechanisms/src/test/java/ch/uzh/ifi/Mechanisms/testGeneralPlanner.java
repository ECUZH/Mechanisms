package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class testGeneralPlanner {

	/*
	 * Scenario:
	 * - the test creates a planner for 2 buyers and 3 sellers. Every buyer is associated with 2 plans
	 * - inject an error into each of 4 plans (one-by-one)
	 * - restore the initial plans (before the error was injected)
	 */
	@Test
	public void testGeneralPlanner2Buyers() throws Exception
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
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();
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
		
		//2. Save the state of the planner with no errors and inject an error
		planner.saveToMemento();
		planner.injectError(0);
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.1) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);
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
				
		
		Planner plannerNew = new GeneralErrorPlanner(numberOfBuyers, numberOfSellers, bids);
		plannerNew.restoreFromMemento( planner.getStateMemento(0));
		plannerNew.setNumberOfPlans(2);
		plannerNew.setMinSellersPerPlan(2);
		plannerNew.setMaxSellersPerPlan(2);
		plans = plannerNew.generatePlans();
		
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
		
		
		//Inject an error into the 2nd plan
		planner.withdrawError();
		planner.injectError(1);
		plans = planner.getPlans();
		
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
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - 1.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.0) < 1e-6);
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
				
		
		//Inject an error into the 3rd plan
		planner.withdrawError();
		planner.injectError(2);
		plans = planner.getPlans();
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
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 0.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.3) < 1e-6);				
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.7) < 1e-6);
		
		
		//Inject an error into the 2nd plan
		planner.withdrawError();
		planner.injectError(3);
		plans = planner.getPlans();
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
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.5) < 1e-6);				
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - 0.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.6) < 1e-6);
	}
	
	/*
	 * Scenario:
	 * - the test creates a planner for 2 buyers and 3 sellers. Every buyer is associated with 2 plans
	 * - remove a single agent 
	 * - reset the planner
	 * - check if the reset is correct
	 * - do this for every agent
	 */
	@Test
	public void testGeneralPlanner2BuyersReset() throws Exception
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
		
		//1. Generate plans without errors
		List<Type> plans = planner.generatePlans();

		int seller1OfBuyer1Plan1 = 4;
		int seller2OfBuyer1Plan1 = 5;
		int seller1OfBuyer1Plan2 = 3;
		int seller2OfBuyer1Plan2 = 5;
		int seller1OfBuyer2Plan1 = 3;
		int seller2OfBuyer2Plan1 = 5;
		int seller1OfBuyer2Plan2 = 4;
		int seller2OfBuyer2Plan2 = 5;
		
		double costOfBuyer1Plan1Seller1 = 2.4;
		double costOfBuyer1Plan1Seller2 = 2.0;
		double costOfBuyer1Plan2Seller1 = 1.8;
		double costOfBuyer1Plan2Seller2 = 1.2;
		double costOfBuyer2Plan1Seller1 = 1.2;
		double costOfBuyer2Plan1Seller2 = 3.6;
		double costOfBuyer2Plan2Seller1 = 0.9;
		double costOfBuyer2Plan2Seller2 = 1.6;
		
		//Plan 1
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == seller1OfBuyer1Plan1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == seller2OfBuyer1Plan1 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - costOfBuyer1Plan1Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - costOfBuyer1Plan1Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.3) < 1e-6);
		//Plan 2
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == seller1OfBuyer1Plan2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == seller2OfBuyer1Plan2 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - costOfBuyer1Plan2Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - costOfBuyer1Plan2Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 1.2) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == seller1OfBuyer2Plan1 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == seller2OfBuyer2Plan1 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - costOfBuyer2Plan1Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - costOfBuyer2Plan1Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(0) == seller1OfBuyer2Plan2 );
		assertTrue( plans.get(1).getAtom(1).getInterestingSet().get(1) == seller2OfBuyer2Plan2 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost1") - costOfBuyer2Plan2Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getTypeComponent("Cost2") - costOfBuyer2Plan2Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(1).getValue() - 0.7) < 1e-6);
		
		
		//2. Remove b1		
		bids.remove(0);
		planner.reset(bids);
		planner.generatePlans();
		plans = planner.getPlans();
		//Plan 3
		assertTrue( plans.size() == 1);
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == seller1OfBuyer2Plan1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == seller2OfBuyer2Plan1 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - costOfBuyer2Plan1Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - costOfBuyer2Plan1Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.5) < 1e-6);
		//Plan 4
		assertTrue( plans.get(0).getAtom(1).getAgentId() == 2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(0) == seller1OfBuyer2Plan2 );
		assertTrue( plans.get(0).getAtom(1).getInterestingSet().get(1) == seller2OfBuyer2Plan2 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost1") - costOfBuyer2Plan2Seller1) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getTypeComponent("Cost2") - costOfBuyer2Plan2Seller2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(1).getValue() - 0.7) < 1e-6);
		
		//3. Remove b2
		bids.add(0, b1);
		bids.remove(1);
		planner.reset(bids);
		planner.generatePlans();
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.size() == 1);
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
		
		//4. Remove s1
		bids.add(1, b2);
		bids.remove(2);
		planner.reset(bids);
		planner.generatePlans();
		plans = planner.getPlans();
		//Plan 1
		assertTrue( plans.size() == 2 );
		assertTrue( plans.get(0).getNumberOfAtoms() == 1);
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 2.4) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 2.0) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.3) < 1e-6);
		//Plan 4
		assertTrue( plans.get(1).getNumberOfAtoms() == 1);
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 4 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 0.9) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 1.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 0.7) < 1e-6);
		
		//4. Remove s2
		bids.add(2, s1);
		bids.remove(3);
		planner.reset(bids);
		planner.generatePlans();
		plans = planner.getPlans();
		//Plan 2
		assertTrue( plans.get(0).getAtom(0).getAgentId() == 1 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(0).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost1") - 1.8) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getTypeComponent("Cost2") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(0).getAtom(0).getValue() - 1.2) < 1e-6);
		//Plan 3
		assertTrue( plans.get(1).getAtom(0).getAgentId() == 2 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(0) == 3 );
		assertTrue( plans.get(1).getAtom(0).getInterestingSet().get(1) == 5 );
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost1") - 1.2) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getTypeComponent("Cost2") - 3.6) < 1e-6);
		assertTrue( Math.abs((Double)plans.get(1).getAtom(0).getValue() - 1.5) < 1e-6);
		
		//4. Remove s2
		bids.add(3, s2);
		bids.remove(4);
		planner.reset(bids);
		planner.generatePlans();
		plans = planner.getPlans();
		assertTrue( plans.get(0).getNumberOfAtoms() == 0);
		assertTrue( plans.get(1).getNumberOfAtoms() == 0);
	}
	
}
