package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

/**
 * The class implements minimum-revenue execution-contingent Core-selecting payment rule for a simple LLG domain.
 **/
public class ECCoreLLGPayments implements PaymentRule
{
	/*
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers - the number of buyers participating in the auction
	 * @param bids - bids of agents
	 * @param costs - costs of goods
	 * @param jpmf - joint probability mass function for availabilities of individual goods
	 */
	public ECCoreLLGPayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{		
		_allocation = allocation;
		_numberOfBuyers = numberOfBuyers;
		_numberOfItems  = numberOfItems;
		_bids  = bids;
		_costs = costs;
		_jpmf = jpmf;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception, PaymentException
	{
		List<Double> payments = new LinkedList<Double>();
		
		//1. Compute EC-VCG Core constraints
		PaymentRule ecvcgPaymentRule = new ECVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, null);
		List<Double> ecvcgPayments = ecvcgPaymentRule.computePayments();
		//_logger.setLevel(_logLevel);
		//_logger.debug("EC-VCG payments: " + ecvcgPayments.toString());
		
		if(_allocation.getBiddersInvolved(0).size() == 1)
			return ecvcgPayments;
		
		//2. Compute reduced WDP with only a global bidder
		List<Integer> allocatedAgentsIds = _allocation.getBiddersInvolved(0);
		List<Type> bids = new LinkedList<Type>();
		for(int j = 0; j < _numberOfBuyers; ++j)
			if( ! allocatedAgentsIds.contains( _bids.get(j).getAgentId() ) )
				bids.add(_bids.get(j));
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( _numberOfBuyers - allocatedAgentsIds.size(), _numberOfItems, bids, _costs, _jpmf);
		auction.computeWinnerDeterminationLLG(null, null);
		
		//3. Compute the last core constraint p_1 + p_2 >= A
		AllocationEC allocation = (AllocationEC)auction.getAllocation(); 
		double A = allocation.getExpectedWelfare();
		
		double totalExpectedValue = 0.;
		double[] expectedValues = new double[2];
		for(int i = 0; i < allocatedAgentsIds.size(); ++i)
		{
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			int allocatedAtomIdx = _allocation.getAllocatedBundlesByIndex(0).get(i);
			AtomicBid allocatedAtom = _bids.get(allocatedAgentId - 1).getAtom(allocatedAtomIdx);
			A += allocatedAtom.computeCost(_costs)*_allocation.getRealizedRV(0, i);
			
			double expectedValue = allocatedAtom.getValue() * auction.computeExpectedMarginalAvailability(allocatedAtom, null, null);
			if( ecvcgPayments.get(i) > expectedValue )
				throw new PaymentException("Empty Core", 0 );

			totalExpectedValue += expectedValue;
			expectedValues[i] = expectedValue;
		}
		//_logger.setLevel(_logLevel);
		//_logger.debug("p1 + p2 >= " + A);
		
		if( totalExpectedValue < A ) throw new PaymentException("Empty Core", 0);
		
		double d = (A - ecvcgPayments.get(0) - ecvcgPayments.get(1))*Math.cos(Math.PI / 4);
		payments.add( ecvcgPayments.get(0) + d*Math.cos(Math.PI / 4) );
		payments.add( ecvcgPayments.get(1) + d*Math.cos(Math.PI / 4) );
		
		if( ecvcgPayments.get(0) + expectedValues[1] < A )
			if( payments.get(1) > expectedValues[1] )
			{
				payments.set(1, expectedValues[1]);
				payments.set(0, A - payments.get(1));
			}
		if( ecvcgPayments.get(1) + expectedValues[0] < A )
			if( payments.get(0) > expectedValues[0] )
			{
				payments.set(0, expectedValues[0]);
				payments.set(1, A - payments.get(0));
			}
		//_logger.debug("Core payments: " + payments.toString());
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

	private int _numberOfBuyers;						//The number of bidders in the auction
	private int _numberOfItems;							//The number of goods in the auction
	private List<Type> _bids;							//A list of bids submitted by agents
	private List<Double> _costs;						//A list of costs of the goods
	private AllocationEC _allocation;						//Resulting allocation of the auction 
	private JointProbabilityMass _jpmf;					//Joint probability mass function for availabilities of goods
	
}
