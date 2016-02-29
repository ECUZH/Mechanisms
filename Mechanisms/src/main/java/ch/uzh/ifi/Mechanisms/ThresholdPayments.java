package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

import java.util.LinkedList;
import java.util.List;

public class ThresholdPayments implements PaymentRule 
{

	/*
	 * Constructor
	 * @param allocation - current allocation
	 * @param allBids - a list of all bids
	 * @param numberOfBuyers - the number of buyers in the market
	 * @param numberOfSellers - the number of sellers in the market
	 */
	public ThresholdPayments(Allocation allocation, List<Type> allBids, List<Type> allPlans, int numberOfBuyers, int numberOfSellers, Planner planner, String pcRule)
	{
		if(allBids.size() != numberOfBuyers + numberOfSellers) 
			throw new RuntimeException("Wrong number of bids: " + allBids.size()+"/"+(numberOfBuyers + numberOfSellers));
		
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_allocation = allocation;
		_allBids = allBids;
		_allPlans = allPlans;
		_planner = planner;
		
		_buyersPayments  = new LinkedList<Double>();
		_sellersPayments = new LinkedList<Double>();
		_paymentCorrectionRule = pcRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception 
	{
		double welfare = _allocation.getAllocatedWelfare();
		
		if( welfare < 0)   
			throw new PaymentException("Negative welfare", _allocation.getAllocatedWelfare());

		_planner.makeNonInjectable();									//The error is already injected. Don't inject it again
		DoubleSidedVCGPayments vcgPayments = new DoubleSidedVCGPayments(_allocation, _allBids, _allPlans, _numberOfBuyers, _numberOfSellers, _planner);
		List<Double> payments = vcgPayments.computePayments();
		List<Double> vcgDiscounts = vcgPayments.getVCGDiscounts();
		
		if( vcgDiscounts.size() <= 0)   		throw new PaymentException("No VCG discounts for " + _allPlans.toString() + " " + _allocation.getAllocatedBundlesOfTrade(0), _allocation.getAllocatedWelfare());
		if( vcgDiscounts.get(0) == Double.NaN) 	throw new RuntimeException("Incorrect vcg discounts: " + vcgDiscounts.toString());
		
		if( _paymentCorrectionRule.equals("Trim") )
		{
			IPaymentCorrectionStrategy pc = new TrimStrategy( vcgDiscounts );
			pc.correctPayments();
			vcgDiscounts = pc.getCorrectedVCGDiscounts();
		}
		else if (_paymentCorrectionRule.equals("VCG") )
		{
			IPaymentCorrectionStrategy pc = new VCGStrategy( _planner, _allBids, _numberOfBuyers, _numberOfSellers );
			pc.correctPayments();
			vcgDiscounts = pc.getCorrectedVCGDiscounts();
		}
		else if( _paymentCorrectionRule.equals("Penalty") )
		{
			IPaymentCorrectionStrategy pc = new PenaltyStrategy( _planner, _allBids, _allocation,  _numberOfBuyers, _numberOfSellers);
			pc.correctPayments();
			vcgDiscounts = pc.getCorrectedVCGDiscounts();
		}
		else if( _paymentCorrectionRule.equals("None") )
		{
			;
		}
		else
		{
			throw new RuntimeException("Incorrect payment correction rule specified: " + _paymentCorrectionRule);
		}
		
		//Compute corrected discounts (fast solution by optimizing the Lagrange f-n of the problem)
		int K = vcgDiscounts.size();
		double Ct = 0.;
		for(int i = 0; i < K; ++i)
			Ct += vcgDiscounts.get(i);
		
		Ct -= welfare;
		Ct /= K;

		List<Double> correctedDiscounts = new LinkedList<Double>();
		if( _paymentCorrectionRule != "Relaxed" )
		{
			for(int i = 0; i < vcgDiscounts.size(); ++i)
				correctedDiscounts.add( Math.max(0, vcgDiscounts.get(i) - Ct) ) ;
			
			//Now normalize if needed
			double total = 0.;
			for(int i = 0; i < correctedDiscounts.size(); ++i)
				total += correctedDiscounts.get(i);
			
			if( total > 1e-6 )
				for(int i = 0; i < correctedDiscounts.size(); ++i)
					correctedDiscounts.set(i, correctedDiscounts.get(i) * welfare / total);
		}
		else
		{
			for(int i = 0; i < vcgDiscounts.size(); ++i)
				correctedDiscounts.add( vcgDiscounts.get(i) - Ct ) ;
		}

		//Compute payments for buyers
		for(int i = 0; i < _allocation.getNumberOfAllocatedAuctioneers(); ++i)
		{
			double itsDiscount = correctedDiscounts.get(i);
			double itsValue = 0.;
			int itsId = _allocation.getAuctioneerId(i);
			int itsBundle = _allocation.getAllocatedBundlesOfTrade(i).get(0);
				
			for(int j = 0; j < _allPlans.size(); ++j)
				if( _allPlans.get(j).getAgentId() == itsId )
					itsValue = _allPlans.get(j).getAtom(itsBundle).getValue();
				
			_buyersPayments.add( itsValue - itsDiscount);	
		}
		
		//Compute payments for sellers
		int sellerIdx = 0;
		for(int i = 0; i < _numberOfSellers; ++i)
		{
			boolean isAllocated = false;
			double itsValue = 0.;
				
			for(int k = 0; k < _allocation.getNumberOfAllocatedAuctioneers(); ++k)							//Compute the value for the seller i
			{
				int winnerId = _allocation.getAuctioneerId(k);
				int winnersAtom = _allocation.getAllocatedBundlesOfTrade(_allocation.getAuctioneerIndexById(winnerId)).get(0);
				AtomicBid atom = _allPlans.get( winnerId-1 ).getAtom( winnersAtom );
				if( atom.getInterestingSet().contains( _allBids.get(_numberOfBuyers+i).getAgentId() ) )	//If the seller participates in this deal
				{
					isAllocated = true;
					int sellerIndex = atom.getInterestingSet().indexOf( _allBids.get(_numberOfBuyers+i).getAgentId() );
					itsValue += (double)atom.getTypeComponent("Cost"+(sellerIndex+1));
				}
			}
			
			if( isAllocated )
			{
				double itsDiscount = correctedDiscounts.get(_allocation.getNumberOfAllocatedAuctioneers() + sellerIdx);
				itsValue = -1*itsValue;
				_sellersPayments.add( itsValue - itsDiscount);
				
				//System.out.println("VCG Payments: " + payments.toString() + " VCG discounts: " + vcgDiscounts.toString() + " i="+i + " itsValue=" + itsValue + " its payment = " + (itsValue - itsDiscount));
				sellerIdx += 1;
			}
		}
		
		payments = new LinkedList<Double>();
		payments.addAll(_buyersPayments);
		payments.addAll(_sellersPayments);

		vcgDiscounts = correctedDiscounts;
		
		if(! isBudgetBalanced())
		{
			throw new RuntimeException("payments are not BB " + payments.toString() + " vcgDiscounts=" + vcgDiscounts.toString() );
		}
		return payments;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		List<Double> payments = new LinkedList<Double>();
		payments.addAll(_buyersPayments);
		payments.addAll(_sellersPayments);
		
		double totalPayment = 0.;
		for(Double p : payments)
			totalPayment += p;
		
		return totalPayment >= -1e-3 ? true : false;
	}
	
	private String _paymentCorrectionRule;								//"Penalty";//"VCG";//"Trim";
	
	private List<Type> _allBids;										//Bids of all buyers and sellers
	private List<Type> _allPlans;										//Aggregated bids of all buyers and sellers (all possible deals)
	private int _numberOfBuyers;										//The number of buyers in the market
	private int _numberOfSellers;										//The number of sellers in the market
	private Allocation _allocation;										//Current allocation
	private Planner _planner;
	
	private List<Double> _buyersPayments;								//A vector of payments of buyers
	private List<Double> _sellersPayments;								//A vector of payments of sellers	
}
