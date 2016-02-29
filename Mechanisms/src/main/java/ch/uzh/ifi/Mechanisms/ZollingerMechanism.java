package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationWoD;
import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ZollingerMechanism implements Auction
{

	/*
	 * A simple constructor.
	 * @param numberOfSellers - the number of sellers in the reverse auction
	 * @param plans - the number of plans one of which should be allocated. Each plan fully specifies 
	 *                the sellers participating in it and their contracts.
	 */
	public ZollingerMechanism(int numberOfSellers, List<Type> plans)
	{
		_numberOfSellers = numberOfSellers;
		_plans = plans;
		_paymentRule = "FirstPrice";
		
		_seed = (int)(1000 * Math.random());
		_randGenerator = new Random();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception 
	{
		switch( _allocationRule )
		{
			case "BuyerUtility" : 		computeWinnerDeterminationU();
										break;
			case "SocialWelfare": 		computeWinnerDeterminationSW();
										break;
			default			 	 :		throw new Exception("No such allocation rule exists: " + _allocationRule);
		}
		
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
			switch( _paymentRule )
			{
				case "FirstPrice" :     computePayments(new FirstPricePayments(_allocation, _plans, _numberOfSellers));
										break;
				case "SecondPrice" :    computePayments(new SecondPricePayments(_allocation, _plans, _numberOfSellers));
										break;
				default			  :		throw new Exception("No such payment rule exists: " + _paymentRule);
			}
	}


	/*
	 * The method allocates a single plan which maximizes a buyer's utility.
	 */
	public void computeWinnerDeterminationU()
	{
		if( _plans.size() == 0)
		{
			_allocation = new AllocationWoD();
			return;
		}
		
		_randGenerator.setSeed(_seed);
		
		double maxExpectedUtility = 0.;
		int itsIdx = -1;
		int itsAtom = -1;
		for(int i = 0; i < _plans.size(); ++i)									// for every buyer
			for(int j = 0; j < _plans.get(i).getNumberOfAtoms(); ++j)			// for every plan he has value for
			{
				ComplexSemanticWebType plan = (ComplexSemanticWebType)(_plans.get(i).getAtom(j));
				if(_paymentRule.equals("SecondPrice"))
					plan.resolveWithSecondPrice();
				else if(_paymentRule.equals("FirstPrice"))
					plan.resolveWithFirstPrice();
				else throw new RuntimeException("Cannot resolve the type: " + _paymentRule);
				
				double expectedUtility = plan.computeExpectedValue() - plan.getPlanExpectedPayment();
				
				if( expectedUtility >= maxExpectedUtility )
				{
					maxExpectedUtility = expectedUtility;
					itsIdx  = i;
					itsAtom = j;
				}
			}
		_allocation = new AllocationWoD();

		if(itsIdx >= 0)
		{
			List<Integer> allocatedBundles = new LinkedList<Integer>();
			allocatedBundles.add(itsAtom);
			try
			{
				ComplexSemanticWebType plan = (ComplexSemanticWebType)(_plans.get( itsIdx ).getAtom( itsAtom ));
				
				List<Integer> actuallyAllocatedTuples = new LinkedList<Integer>();
				for(int i = 0; i < plan.getNumberOfFragments(); ++i)
					actuallyAllocatedTuples.add( plan.getMinNumberOfRecords(i) + _randGenerator.nextInt(plan.getMaxNumberOfRecords(i) - plan.getMinNumberOfRecords(i)) );

				if(plan.getAllocatedSellers().size() < 1)
					throw new RuntimeException("No sellers in plan: " + plan.toString());
				
				plan.setActuallyAllocatedRecords(actuallyAllocatedTuples);
				List<Integer> sellersInvolved = plan.getAllocatedSellers();
				
				List<ComplexSemanticWebType> allocatedPlans = new LinkedList<ComplexSemanticWebType>();
				allocatedPlans.add(plan);
				_allocation.addAllocatedAgent(plan.getAgentId(), sellersInvolved, allocatedBundles,
											  plan.computeExpectedValue(), plan.computeExpectedCosts());
				_allocation.addAllocatedPlans(allocatedPlans, true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * The method allocates a single plan which maximizes a buyer's utility.
	 */
	public void computeWinnerDeterminationSW()
	{
		if( _plans.size() == 0)
		{
			_allocation = new AllocationWoD();
			return;
		}
		
		_randGenerator.setSeed(_seed);
		
		double maxExpectedSW = 0.;
		int itsIdx = -1;
		int itsAtom = -1;
		for(int i = 0; i < _plans.size(); ++i)									// for every buyer
			for(int j = 0; j < _plans.get(i).getNumberOfAtoms(); ++j)			// for every plan he has value for
			{
				ComplexSemanticWebType plan = (ComplexSemanticWebType)(_plans.get(i).getAtom(j));
				
				if(_paymentRule.equals("SecondPrice"))
					plan.resolveWithSecondPrice();
				else if(_paymentRule.equals("FirstPrice"))
					plan.resolveWithFirstPrice();
				else throw new RuntimeException("Cannot resolve the type: " + _paymentRule);
				
				double expectedSW = plan.computeExpectedValue() - plan.computeExpectedTotalCost();
				
				if( expectedSW >= maxExpectedSW )
				{
					maxExpectedSW = expectedSW;
					itsIdx  = i;
					itsAtom = j;
				}
			}
		_allocation = new AllocationWoD();

		if(itsIdx >= 0)
		{
			List<Integer> allocatedBundles = new LinkedList<Integer>();
			allocatedBundles.add(itsAtom);
			try
			{
				ComplexSemanticWebType plan = (ComplexSemanticWebType)(_plans.get( itsIdx ).getAtom( itsAtom ));
				
				List<Integer> actuallyAllocatedTuples = new LinkedList<Integer>();
				for(int i = 0; i < plan.getNumberOfFragments(); ++i)
					actuallyAllocatedTuples.add( plan.getMinNumberOfRecords(i) + _randGenerator.nextInt(plan.getMaxNumberOfRecords(i) - plan.getMinNumberOfRecords(i)) );

				if(plan.getAllocatedSellers().size() < 1)
					throw new RuntimeException("No sellers in plan: " + plan.toString());
				
				plan.setActuallyAllocatedRecords(actuallyAllocatedTuples);
				List<Integer> sellersInvolved = plan.getAllocatedSellers();
				
				List<ComplexSemanticWebType> allocatedPlans = new LinkedList<ComplexSemanticWebType>();
				allocatedPlans.add(plan);
				
				_allocation.addAllocatedAgent(plan.getAgentId(), sellersInvolved, allocatedBundles,
											  plan.computeExpectedValue(), plan.computeExpectedCosts());
				_allocation.addAllocatedPlans(allocatedPlans, true);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * The method computes and returns payments for the market.
	 * @param pr - payment rule to be used
	 * @return a vector of prices for bidders
	 */
	public List<Double> computePayments(PaymentRule pr) throws Exception
	{	
		PaymentRule paymentRule = pr;
		try
		{
			if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
				_payments = paymentRule.computePayments();
			else
				_payments = new LinkedList<Double>();
		}
		catch(PaymentException e)
		{
			_payments = new LinkedList<Double>();
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return _payments;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#setupReservePrices(java.util.List)
	 */
	@Override
	public void setupReservePrices(List<Double> reservePrices) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPayments()
	 */
	@Override
	public double[] getPayments() throws Exception 
	{
		if( (_payments == null) || (_payments.size() == 0) ) 
		{
			System.out.println("Plans: " + _plans.toString());
			System.out.println("Allocation: " + _allocation.toString());
			throw new RuntimeException("Payments were not computed.");
		}
		
		double[] paymentsArray = new double[_payments.size()];
		for(int i = 0 ; i < _payments.size(); ++i)
			paymentsArray[i] = _payments.get(i);
		
		return paymentsArray;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getAllocation()
	 */
	@Override
	public Allocation getAllocation() 
	{
		return _allocation;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetPlanner(Mechanisms.Planner)
	 */
	@Override
	public void resetPlanner(Planner planner) {
		// TODO Auto-generated method stub
		
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPaymentRule()
	 */
	@Override
	public String getPaymentRule() 
	{
		return _paymentRule;
	}

	public String getAllocationRule() 
	{
		return _allocationRule;
	}
	
	/*
	 * 
	 */
	public void setPaymentRule(String paymentRule) 
	{
		_paymentRule = paymentRule;
	}
	
	/*
	 * 
	 */
	public void setAllocationRule(String allocationRule) 
	{
		_allocationRule = allocationRule;
	}
	
	public void setSeed(int seed)
	{
		_seed = seed;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return true;
	}
	
	private String _allocationRule;									//Allocation rule to be used (max SW or max buyer's utility)
	private String _paymentRule;									//Payment rule to be used (First or Second price)
	
	private int _numberOfSellers;									//The number of sellers in the auction
	private List<Type> _plans;										//Plans generated from the bids submitted by agents
	private AllocationWoD   _allocation;								//Resulting allocation
	private List<Double> _payments;									//A list containing payments of buyers and sellers (first buyers, then sellers)

	private int _seed;												//A seed used to generate realizations of the r.v. of the numbers of records after allocation happened
	private Random _randGenerator;									//Random numbers generator
	@Override
	public boolean isExPostIR() {
		// TODO Auto-generated method stub
		return false;
	}
}
