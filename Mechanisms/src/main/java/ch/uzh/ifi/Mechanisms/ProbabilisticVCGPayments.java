package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;

import java.util.LinkedList;
import java.util.List;

public class ProbabilisticVCGPayments implements PaymentRule 
{
	/*
	 * 
	 */
	public ProbabilisticVCGPayments(Allocation allocation, List<Type> plans, int numberOfSellers)
	{
		_allocation = allocation;
		_plans = plans;
		_numberOfSellers = numberOfSellers;
	}

	@Override
	public List<Double> computePayments() throws Exception 
	{
		List<Double> payments = new LinkedList<Double>();
		SemanticWebType allocatedPlan = (SemanticWebType)( _plans.get(0).getAtom( _allocation.getAllocatedBundlesByIndex(_allocation.getAuctioneerIndexById(1)).get(0)) );
		
		for(int i = 0; i < allocatedPlan.getNumberOfSellers(); ++i)
		{
			List<Integer> fixedSellers = new LinkedList<Integer>();
			fixedSellers.add(i);
						
			payments.add( allocatedPlan.computeExpectedSW( allocatedPlan.getNumberOfSellers(), fixedSellers) );
			List<Type> reducedPlans = new LinkedList<Type>();
			for(int j = 0; j < _plans.get(0).getNumberOfAtoms(); ++j)
			{
				//System.out.println("i's ID " + allocatedPlan.getInterestingSet().get(i) + " is in " + _plans.get(0).getAtom(j).getInterestingSet().toString());
				if( ! _plans.get(0).getAtom(j).getInterestingSet().contains( allocatedPlan.getInterestingSet().get(i) ) )
					reducedPlans.add( _plans.get(0).getAtom(j) );
			}
			
			ProbabilisticReverseCA ca = new ProbabilisticReverseCA(1, reducedPlans);
			ca.computeWinnerDetermination();
			Allocation reducedAllocation = ca.getAllocation();
			
			payments.set(i, payments.get(i) - reducedAllocation.getAllocatedWelfare());
			//System.out.println(">> p_VCG = " + payments.get(i) + " = "+ allocatedPlan.computeExpectedSW( allocatedPlan.getNumberOfSellers(), fixedSellers) + " - " + reducedAllocation.getExpectedWelfare() );
		}
		
		return payments;
	}

	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}

	private Allocation _allocation;
	private List<Type> _plans;
	private int _numberOfSellers;
}
