package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;
import java.util.LinkedList;
import java.util.List;

public class SecondPricePayments implements IPaymentRule
{
	/*
	 * Constructor
	 */
	SecondPricePayments(Allocation allocation, List<Type> plans, int numberOfSellers)
	{
		_allocation = allocation;
		_plans = plans;
		_numberOfSellers = numberOfSellers;
	}
		
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		List<Double> payments = new LinkedList<Double>();
		ComplexSemanticWebType allocatedPlan = (ComplexSemanticWebType)( _plans.get(0).getAtom( _allocation.getAllocatedBundlesOfTrade(_allocation.getAuctioneerIndexById(1)).get(0)) );
					
		for(int i = 0; i < allocatedPlan.getAllocatedSellers().size(); ++i)
			payments.add( allocatedPlan.getPayment( allocatedPlan.getAllocatedSellers().get(i) ) );
			
		return payments;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}

	private Allocation _allocation;
	private List<Type> _plans;
	private int _numberOfSellers;
}
