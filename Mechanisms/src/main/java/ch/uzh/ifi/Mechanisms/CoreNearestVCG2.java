package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

import java.util.LinkedList;
import java.util.List;

public class CoreNearestVCG2 implements PaymentRule
{

	/*
	 * 
	 */
	public CoreNearestVCG2(Allocation allocation, List<Type> allBids, List<Integer> quantitiesOfItems, int numberOfItems, List<Double> costs)
	{
		_allocation = allocation;
		_bids = allBids;
		_unitsOfItems = quantitiesOfItems;
		_numberOfItems = numberOfItems;
		_numberOfAgents = _bids.size();
		_payments = new LinkedList<Double>();
		_costs = costs;

	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		PaymentRule vcgRule = new VCGPayments(_allocation, _bids, _unitsOfItems, _numberOfItems, _costs); 
		List<Double> vcg = vcgRule.computePayments();
		
		if( _allocation.getBiddersInvolved(0).size() == 1)					//Only one winner => VCG payment
		{
			assert(vcg.size() == 1);
			_payments.add( vcg.get(0) );
			return _payments;
		}
		
		List<Type> bids = new LinkedList<Type>();
		for(int j = 0; j < _numberOfAgents; ++j)
			if( !_allocation.getBiddersInvolved(0).contains(  _bids.get(j).getAgentId() ) )	//unallocated bidders
				bids.add(_bids.get(j));

		double allocatedCosts = 0.;
		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			int bidderId = _allocation.getBiddersInvolved(0).get(i);
			int itsAllocatedBundle = _allocation.getAllocatedBundlesByIndex(0).get(i);
			AtomicBid bundle = _bids.get(bidderId-1).getAtom(itsAllocatedBundle);
			allocatedCosts += computeCost(bundle);
		}
		
		CAXOR ca = new CAXOR( _numberOfAgents - _allocation.getBiddersInvolved(0).size(), _numberOfItems, bids, _costs);
		ca.setPaymentRule("VCG_LLG");
		ca.computeWinnerDeterminationLLG();
		double Wi = ca.getAllocation().getAllocatedWelfare();
		
		double coeff = ((allocatedCosts + Wi) - vcg.get(1) - vcg.get(0))/2;
		//double coeff = 0.0;
		//for( Double VCGi : vcg )
		//	coeff += VCGi;
		//coeff *= -1.0;
		//coeff += Wi;
		//coeff /= _allocation.getBiddersInvolved(0).size();
			
		_payments = new LinkedList<Double>();

		for(int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
			_payments.add( vcg.get( i ) + coeff);

		return _payments;
	}

	@Override
	public boolean isBudgetBalanced() 
	{
		if( _payments == null) throw new RuntimeException("Payments are not computed yet");
		
		double totalPayment = 0.;
		for(Double p : _payments)
			totalPayment += p;
		
		return totalPayment >= 0 ? true : false;
	}
	
	/*
	 * The method computes the additive cost of a given bundle.
	 * @param atom - an atomic bid of an agent containing the bundle
	 * @return the cost of the bundle, i.e., the sum of costs of all items in the bundle
	 */
	public double computeCost(AtomicBid atom)
	{
		double cost = 0.;
		
		for(int item : atom.getInterestingSet())
			cost += _costs.get( item - 1 );
		
		return cost;
	}
	
	
	private List<Double> _costs;
	private List<Double> _payments;
	private List<Type> _bids;
	private List<Integer> _unitsOfItems;
	private int _numberOfItems;
	private int _numberOfAgents;
	private Allocation _allocation;
}
