package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationEC;
import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

public class ECCCoreLLGPayments implements PaymentRule
{
	
	private static final Logger _logger = LogManager.getLogger(ECCCoreLLGPayments.class);
	
	/*
	 * Constructor.
	 * @param allocation - an allocation of the auction
	 * @param numberOfBuyers - the number of buyers participating in the auction
	 * @param bids - bids of agents
	 * @param costs - costs of goods
	 * @param jpmf - joint probability mass function for availabilities of individual goods
	 */
	public ECCCoreLLGPayments(AllocationEC allocation, int numberOfBuyers, int numberOfItems, List<Type> bids, List<Double> costs, JointProbabilityMass jpmf)
	{	
		_logger.debug("ECCCoreLLGPayments::ECCCoreLLGPayments(...)");
		
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
	public List<Double> computePayments() throws Exception 
	{
		_logger.debug("-> computePayments()");
		List<Double> payments = new LinkedList<Double>();
		
		//1. Compute EC-VCG Core constraints
		PaymentRule eccvcgPaymentRule = new ECCVCGPayments(_allocation, _numberOfBuyers, _numberOfItems, _bids, _costs, _jpmf, null);
		List<Double> eccvcgPayments = eccvcgPaymentRule.computePayments();
		_logger.debug("ECC-VCG payments: " + eccvcgPayments.toString());
		
		if(_allocation.getBiddersInvolved(0).size() == 1)
			return eccvcgPayments;
		
		//2. Compute reduced WDP with only a global bidder
		List<Integer> allocatedAgentsIds = _allocation.getBiddersInvolved(0);
		List<Type> bids = new LinkedList<Type>();
		for(int j = 0; j < _numberOfBuyers; ++j)
			if( ! allocatedAgentsIds.contains( _bids.get(j).getAgentId() ) )
				bids.add(_bids.get(j));
		
		List<Integer> allocatedAvailabilitiesPerGood = new LinkedList<Integer>();
		List<Double> realizationsOfAvailabilitiesPerGood = new LinkedList<Double>();
		for(int j = 0; j < _allocation.getBiddersInvolved(0).size(); ++j)
		{
			int bidderId = _allocation.getBiddersInvolved(0).get(j);				
			int itsAllocatedAtom = _allocation.getAllocatedBundlesOfTrade(0).get(j);
			AtomicBid allocatedBundle = _bids.get( bidderId-1 ).getAtom( itsAllocatedAtom );
			
			for(int k = 0; k < allocatedBundle.getInterestingSet().size(); ++k)
			{
				int goodId = allocatedBundle.getInterestingSet().get(k);
				allocatedAvailabilitiesPerGood.add( goodId );
				realizationsOfAvailabilitiesPerGood.add(_allocation.getRealizedRVsPerGood(0).get(goodId-1));
			}
		}
		
		ProbabilisticCAXOR auction = new ProbabilisticCAXOR( _numberOfBuyers - allocatedAgentsIds.size(), _numberOfItems, bids, _costs, _jpmf);
		auction.computeWinnerDeterminationLLG(allocatedAvailabilitiesPerGood, realizationsOfAvailabilitiesPerGood);
		
		//3. Compute the last core constraint p_1 + p_2 >= A
		AllocationEC allocation = (AllocationEC)auction.getAllocation(); 
		double A = allocation.getExpectedWelfare();
		
		double totalRealizedValue = 0.;
		double[] realizedValues = new double[2];
		for(int i = 0; i < allocatedAgentsIds.size(); ++i)
		{
			int allocatedAgentId = _allocation.getBiddersInvolved(0).get(i);
			int allocatedAtomIdx = _allocation.getAllocatedBundlesOfTrade(0).get(i);
			AtomicBid allocatedAtom = _bids.get(allocatedAgentId - 1).getAtom(allocatedAtomIdx);
			A += allocatedAtom.computeCost(_costs) * _allocation.getRealizedRV(0, i);
			
			double realizedValue = allocatedAtom.getValue() * _allocation.getRealizedRV(0, i);
			if( eccvcgPayments.get(i) > realizedValue )
				throw new PaymentException("Empty Core", 0 );

			totalRealizedValue += realizedValue;
			realizedValues[i] = realizedValue;
		}
		_logger.debug("p1 + p2 >= " + A);
		
		if( totalRealizedValue < A ) throw new PaymentException("Empty Core", 0);
		
		double d = (A - eccvcgPayments.get(0) - eccvcgPayments.get(1))*Math.cos(Math.PI / 4);
		payments.add( eccvcgPayments.get(0) + d*Math.cos(Math.PI / 4) );
		payments.add( eccvcgPayments.get(1) + d*Math.cos(Math.PI / 4) );
		
		if( eccvcgPayments.get(0) + realizedValues[1] < A )
			if( payments.get(1) > realizedValues[1] )
			{
				payments.set(1, realizedValues[1]);
				payments.set(0, A - payments.get(1));
			}
		if( eccvcgPayments.get(1) + realizedValues[0] < A )
			if( payments.get(0) > realizedValues[0] )
			{
				payments.set(0, realizedValues[0]);
				payments.set(1, A - payments.get(0));
			}
		_logger.debug("Core payments: " + payments.toString());
		_logger.debug("<- computePayments()");
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
