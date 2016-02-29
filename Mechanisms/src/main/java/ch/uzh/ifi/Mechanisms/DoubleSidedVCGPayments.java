package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

import java.util.LinkedList;
import java.util.List;

public class DoubleSidedVCGPayments implements PaymentRule
{

	/*
	 * Constructor
	 */
	public DoubleSidedVCGPayments(Allocation allocation, List<Type> allBids, List<Type> allPlans, int numberOfBuyers, int numberOfSellers, Planner planner)
	{
		if(allBids.size() != numberOfBuyers + numberOfSellers) 
			throw new RuntimeException("Wrong number of bids: " + allBids.size()+"/"+(numberOfBuyers + numberOfSellers));
			
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_allocation = allocation;
		_allBids = allBids;
		if( planner == null )   throw new RuntimeException("No Planner");
		_planner = planner;
		
		_buyersPayments  = new LinkedList<Double>();
		_sellersPayments = new LinkedList<Double>();
		_buyersDiscounts = new LinkedList<Double>();
		_sellersDiscounts= new LinkedList<Double>();
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#computePayments()
	 */
	@Override
	public List<Double> computePayments() throws Exception
	{
		double W = _allocation.getAllocatedWelfare();
		
		//Compute discounts for buyers
		for( int i = 0 ; i < _numberOfBuyers; ++i)
		{
			if( _allocation.isAllocated( _allBids.get(i).getAgentId() ) == false )
				continue;

			Type allocatedBuyer = _allBids.get(i);
			_allBids.remove(i);													//Use all bids except of i
			
			double Wi = 0.;
			if( _numberOfBuyers - 1 > 0 )
			{
				_planner.makeNonInjectable();									//Now the planner is non-injectable but the error is injected
				DoubleSidedMarket dsm = new DoubleSidedMarket( _numberOfBuyers-1, _numberOfSellers, _allBids, _planner);//Solve the auction without Agent i
				dsm.computeWinnerDetermination();
				Wi = dsm.getAllocation().getAllocatedWelfare();					//Allocation without Agent i
			}
			
			double agentValue = _allocation.getAuctioneersAllocatedValue(0);
			double payment = agentValue - (W-Wi);
			if( payment < - (1e-4) ) throw new RuntimeException("Negative VCG payments: " + payment);
			
			_buyersDiscounts.add( W-Wi);
			_buyersPayments.add(payment < 0 ? 0 : payment);
			_allBids.add(i, allocatedBuyer);
		}
		
		//Compute discounts for sellers
		for( int i = 0; i < _allocation.getBiddersInvolved(0).size(); ++i)
		{
			List<Type> bids = new LinkedList<Type >();
			for(int j = 0; j < _allBids.size(); ++j)
				if( _allBids.get(j).getAgentId() != _allocation.getBiddersInvolved(0).get(i) )
					bids.add( _allBids.get(j) );
			
			double Wi = 0.;
			_planner.makeNonInjectable();										//Now the planner is non-injectable but the error is injected
			DoubleSidedMarket dsm = new DoubleSidedMarket(_numberOfBuyers, _numberOfSellers-1, bids, _planner);//Solve the CA without Agent i
			dsm.computeWinnerDetermination();
			Wi = dsm.getAllocation().getAllocatedWelfare();						//Allocation without Agent i

			double val = _allocation.getBiddersAllocatedValue(0, i);
			double payment = -1*val - (W-Wi);
			
			_sellersDiscounts.add(W-Wi);
			_sellersPayments.add(payment);
		}

		List<Double> payments = new LinkedList<Double>();
		payments.addAll(_buyersPayments);
		payments.addAll(_sellersPayments);
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
		
		return totalPayment >= 0 ? true : false;
	}
	
	/*
	 * 
	 */
	public List<Double> getVCGDiscounts()
	{
		List<Double> discounts = new LinkedList<Double>();
		discounts.addAll(_buyersDiscounts);
		discounts.addAll(_sellersDiscounts);
		return discounts;
	}
	
	/*
	 * 
	 */
	public List<Double> getBuyersPayments()
	{
		return _buyersPayments;
	}
	
	/*
	 * 
	 */
	public List<Double> getSellersPayments()
	{
		return _sellersPayments;
	}
	
	private List<Type> _allBids;										//Bids of all buyers and sellers
	private int _numberOfBuyers;										//The number of buyers in the market
	private int _numberOfSellers;										//The number of sellers in the market
	private Allocation _allocation;										//Current allocation
	private Planner _planner;											//A planner
	
	private List<Double> _buyersPayments;								//A list of payments of buyers
	private List<Double> _buyersDiscounts;								//A list of discounts of buyers
	private List<Double> _sellersPayments;								//A list of payments of sellers
	private List<Double> _sellersDiscounts;								//A list of discounts of sellers
	
}
