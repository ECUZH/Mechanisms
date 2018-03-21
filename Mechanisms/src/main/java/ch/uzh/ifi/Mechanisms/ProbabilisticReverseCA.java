package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AllocationWoD;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
 * The class implements a probabilistic reverse combinatorial auction, i.e., a mechanism where
 * allocation happens with uncertainty (e.g., unknown quality of goods) and maximizes an expected
 * social welfare and payments are computed either using expected-VCG rule or expected-Minimum 
 * Core rule.
 */
public class ProbabilisticReverseCA implements Auction {
	
	/*
	 * A simple constructor.
	 * @param numberOfSellers - the number of sellers in the reverse auction
	 * @param plans - the number of plans one of which should be allocated. Each plan fully specifies 
	 *                the sellers participating in it and their contracts.
	 */
	public ProbabilisticReverseCA(int numberOfSellers, List<Type> plans)
	{
		_numberOfSellers = numberOfSellers;
		_plans = plans;
		_paymentRule = "Exp-VCG";
		
		_seed = (int)(1000 * Math.random());
		_randGenerator = new Random();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		String str = "Auction with plans: " + _plans.toString();
		return str;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#solveIt()
	 */
	@Override
	public void solveIt() throws Exception 
	{
		computeWinnerDetermination();
		if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
			switch( _paymentRule )
			{
				case "Exp-VCG"    :     computePayments(new ProbabilisticVCGPayments(_allocation, _plans, _numberOfSellers));
										break;
				case "Exp-Core":		try
										{
											computePayments(new ProbabilisticCorePayments(_allocation, _plans, _numberOfSellers));
										}
										catch(PaymentException e)
										{
											if(e.getMessage().equals("Empty Core") )
											{
												throw e;
											}
										}
										
										/*
										//Compare with VCG
										PaymentRule pr = new ProbabilisticVCGPayments(_allocation, _plans, _numberOfSellers);
										List<Double> vcgPayments = pr.computePayments();
										boolean isVCG = true;
										for(int i = 0; i < _payments.size(); ++i)
										{
											if( Math.abs(_payments.get(i) - vcgPayments.get(i)) > 1e-6 )
											{
												isVCG = false;
												break;
											}
										}
										if(isVCG)
										{
											//System.out.println("");
											//System.out.println("Payments: " + _payments.toString());
											//System.out.println("Payments: " + vcgPayments.toString());
											//System.out.println("Plans: " + _plans.toString());
											//System.out.println("Allocation: E(SW)=" + _allocation.getExpectedWelfare() );
											//System.out.println("Allocation: Sellers: " + _allocation.getSellersInvolved(0) );
											//System.out.println("Allocation: Actually alloc: " + _allocation.getActuallyAllocatedItems(0).toString() );
											throw new PaymentException("VCG is in the Core",0);
										}*/
										break;
				default			  :		throw new Exception("No such payment rule exists: " + _paymentRule);
			}
	}

	/*
	 * The method allocates a single plan which maximizes an expected social welfare.
	 */
	public void computeWinnerDetermination()
	{
		if( _plans.size() == 0)
		{
			_allocation = new AllocationWoD();
			return;
		}
		
		_randGenerator.setSeed(_seed);
		
		double maxExpectedWelfare = 0.;
		int itsIdx = -1;
		int itsAtom = -1;
		for(int i = 0; i < _plans.size(); ++i)									// for every buyer
			for(int j = 0; j < _plans.get(i).getNumberOfAtoms(); ++j)			// for every plan he has value for
			{
				SemanticWebType plan = (SemanticWebType)(_plans.get(i).getAtom(j));
				double expectedSW = plan.computeExpectedSW( plan.getNumberOfSellers() );
				
				if( expectedSW >= maxExpectedWelfare )
				{
					maxExpectedWelfare = expectedSW;
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
				SemanticWebType plan = (SemanticWebType)(_plans.get( itsIdx ).getAtom( itsAtom ));
				List<Integer> actuallyAllocatedTuples = new LinkedList<Integer>();
				for(int i = 0; i < plan.getNumberOfSellers(); ++i)
				{
					actuallyAllocatedTuples.add( plan.getMinNumberOfTuples(i) + _randGenerator.nextInt(plan.getMaxNumberOfTuples(i) - plan.getMinNumberOfTuples(i)) );
					//double av = (plan.getMaxNumberOfTuples(i) - plan.getMinNumberOfTuples(i)) / 2;
					//actuallyAllocatedTuples.add( (int)( plan.getMaxNumberOfTuples(i) - Math.abs( _randGenerator.nextGaussian() * 1e-16)  ) );
				}
				//System.out.println(">> " + actuallyAllocatedTuples.toString());
				if(plan.getNumberOfSellers() < 1)
					throw new RuntimeException("No sellers in plan: " + plan.toString());
				
				plan.setActuallyAllocatedTuples(actuallyAllocatedTuples);
				List<Integer> sellersInvolved = plan.getInterestingSet();
				List<SemanticWebType> allocatedPlans = new LinkedList<SemanticWebType>();
				allocatedPlans.add(plan);
				_allocation.addAllocatedAgent(plan.getAgentId(), sellersInvolved, allocatedBundles,
											  plan.computeExpectedValue(plan.getNumberOfSellers()), plan.computeExpectedCosts());
				_allocation.addAllocatedPlans(allocatedPlans);
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
	public List<Double> computePayments(IPaymentRule pr) throws Exception
	{	
		IPaymentRule paymentRule = pr;
		try
		{
			if( _allocation.getNumberOfAllocatedAuctioneers() > 0 )
				_payments = paymentRule.computePayments();
			else
				_payments = new LinkedList<Double>();
		}
		catch(PaymentException e)
		{
			if( e.getMessage().equals("Empty Core"))
			{
				_payments = computePayments(new ProbabilisticVCGPayments(_allocation, _plans, _numberOfSellers));
				throw e;
			}
			else
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
	public void setupReservePrices(List<Double> reservePrices) 
	{
		throw new RuntimeException("No reserve prices support for now.");
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
	public String getPaymentRule() {
		return _paymentRule;
	}
	
	public void setPaymentRule(String paymentRule) 
	{
		_paymentRule = paymentRule;
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

	/*
	 * (non-Javadoc)
	 * @see ch.uzh.ifi.Mechanisms.Auction#isExPostIR()
	 */
	@Override
	public boolean isExPostIR() 
	{
		return false;
	}
	
	private String _paymentRule;									//Payment rule to be used (expected-VCG or expected-Core)
	
	private int _numberOfSellers;									//The number of sellers in the auction
	private List<Type> _plans;										//Plans generated from the bids submitted by agents
	private AllocationWoD   _allocation;								//Resulting allocation
	private List<Double> _payments;									//A list containing payments of buyers and sellers (first buyers, then sellers)

	private int _seed;												//A seed used to generate realizations of the r.v. of the numbers of records after allocation happened
	private Random _randGenerator;									//Random numbers generator
}
