package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class testFragmentedProbabilisticPlanner 
{

	/*
	 * Scenario:
	 * 
	 */
	@Test
	public void test() 
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
		
		//System.out.println("Plans: " + plans.toString());
		for(int i = 0; i < plans.get(0).getNumberOfAtoms(); ++i)
		{
			ComplexSemanticWebType plan = (ComplexSemanticWebType)plans.get(0).getAtom(i);
			assertTrue(plan.getNumberOfFragments() == 2);
			for(int j = 0; j < plan.getNumberOfFragments(); ++j)
			{
				assertTrue(plan.getFragment(j).getNumberOfSellers() == 2);
				for(int k = 0; k < plan.getFragment(j).getNumberOfSellers(); ++k)
					if( plan.getFragment(j).getSellerId(k) == 2 )
						assertTrue( (plan.getFragment(j).getCost(k) - 0.1) < 1e-6);
					else if ( plan.getFragment(j).getSellerId(k) == 3 )
						assertTrue( (plan.getFragment(j).getCost(k) - 0.2) < 1e-6);
					else if ( plan.getFragment(j).getSellerId(k) == 4 )
						assertTrue( (plan.getFragment(j).getCost(k) - 0.3) < 1e-6);
					else if ( plan.getFragment(j).getSellerId(k) == 5 )
						assertTrue( (plan.getFragment(j).getCost(k) - 0.4) < 1e-6);
			}
		}
	}

}
