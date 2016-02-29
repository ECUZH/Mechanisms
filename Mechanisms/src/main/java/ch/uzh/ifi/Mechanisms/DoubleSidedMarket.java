package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.Term;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;

/*
 * The class implements a Double Auction with N buyers and M sellers. Sellers have no capacity constraints and can provide as many items
 * as required by buyers. The mechanism takes aggregated types of all possible trades as an input. An atomic Type object MUST provide:
 * - a "Cost1", "Cost2", ... , "CostK" attributes, which represent costs of sellers participating in this deal (K is the number of sellers 
 *   in the deal)
 * - a "Cost" attribute representing the total cost for the deal
 * - a "Value" attribute representing a value of the buyer which has this type 
 */
public class DoubleSidedMarket implements Auction
{
	/*
	 * Constructor
	 * @param numberOfBuyers
	 * @param numberOfSellers
	 * @param bids - a list of bids of all agents. First come buyers, then sellers.
	 * @param planner - a planner implementing a matching mechanism to match buyers with sellers
	 */
	public DoubleSidedMarket(int numberOfBuyers, int numberOfSellers, List<Type> bids, Planner planner)
	{
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_allocation = new Allocation();
		
		if(planner == null) 
			throw new RuntimeException("No planner provided.");

		_planner = planner;
		_paymentRule = "VCG";							//Default payment rule
		_paymentCorrectionRule = "None";				//Default payment correction rule (for domains with uncertainty)
		
		resetTypes( bids );
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		String str = "#Buyers="+_numberOfBuyers + ", #Sellers=" + _numberOfSellers + "\n";
		for(int i = 0; i < _numberOfBuyers; ++i)
			str += _bids.get(i).toString() + "\n";
		for(int i = 0; i < _numberOfSellers; ++i)
			str += _bids.get(_numberOfBuyers + i).toString() + "\n";
		return str;
	}
	
	/*
	 * The method returns a list of types in the auction
	 * @return a list of types
	 */
	public List<Type> getPlans()
	{
		return _plans;
	}
	
	/*
	 * The method returns a list of buyers
	 */
	public int getNumberOfBuyers()
	{
		return _numberOfBuyers;
	}
	
	/*
	 * The method returns a planner used by the double auction
	 */
	public Planner getPlanner()
	{
		return _planner;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#resetTypes(java.util.List)
	 */
	@Override
	public void resetTypes(List<Type> agentsTypes) 
	{
		_payments = new LinkedList<Double>();
		_bids = agentsTypes;

		_planner.reset(_bids);
		_planner.generatePlans();
		//_planner.withdrawError();
		_plans = _planner.getPlans();
	}
	
	public void resetPlanner(Planner planner)
	{
		_planner = planner;
	}
	
	/*
	 * The method used to specify a payment rule which should be used in the auction.
	 * @param paymentRule - a payment rule to be used by the auction
	 */
	public void setPaymentRule(String paymentRule)
	{
		_paymentRule = paymentRule;
	}

	public String getPaymentRule()
	{
		return _paymentRule;
	}
	
	/*
	 * The method used to specify a payment correction rule which should be used in the auction in a domain
	 * with uncertainty.
	 * @param paymentCorrectionRule - a payment correction rule
	 */
	public void setPaymentCorrectionRule(String paymentCorrectionRule)
	{
		_paymentCorrectionRule = paymentCorrectionRule;
	}

	public String getPaymentCorrectionRule()
	{
		return _paymentCorrectionRule;
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
				case "VCG"        : 	computePayments(new DoubleSidedVCGPayments(_allocation, _bids, _plans, _numberOfBuyers, _numberOfSellers, _planner));
										break;
				case "Threshold":		computePayments(new ThresholdPayments(_allocation, _bids, _plans, _numberOfBuyers, _numberOfSellers, _planner, _paymentCorrectionRule));
										break;
				default			  :		throw new Exception("No such payment rule exists: " + _paymentRule);
			}
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#getPayments()
	 */
	@Override
	public double[] getPayments() throws Exception
	{
		if( (_payments == null) || (_payments.size() == 0) ) throw new Exception("Payments were not computed.");
		
		double[] paymentsArray = new double[_payments.size()];
		for(int i = 0 ; i < _payments.size(); ++i)
			paymentsArray[i] = _payments.get(i);
		
		return paymentsArray;
	}
	
	/*
	 * The method allocates a single plan which maximizes the social welfare.
	 */
	public void computeWinnerDetermination()
	{
		if( _plans.size() == 0)
		{
			_allocation = new Allocation();
			return;
		}
		
		double maxWelfare = 0.;
		int itsIdx = -1;
		int itsAtom = -1;
		for(int i = 0; i < _plans.size(); ++i)								// for every buyer
			for(int j = 0; j < _plans.get(i).getNumberOfAtoms(); ++j)		// for every plan he has value for
				if( _plans.get(i).getAtom(j).getValue() - (Double)_plans.get(i).getAtom(j).getTypeComponent("Cost") > maxWelfare )
				{
					maxWelfare = _plans.get(i).getAtom(j).getValue() - (Double)_plans.get(i).getAtom(j).getTypeComponent("Cost");
					itsIdx  = i;
					itsAtom = j;
				}
		
		_allocation = new Allocation();

		if(itsIdx >= 0)
		{
			_planner.saveToMemento(); 										//Save the current state before an error is injected
			_planner.injectError(itsAtom + itsIdx*_planner.getNumberOfPlans());//Inject an error in the allocated plan
			_plans = _planner.getPlans();
			List<Integer> allocatedBundles = new LinkedList<Integer>();
			allocatedBundles.add(itsAtom);
			try
			{
				AtomicBid atom = _plans.get( itsIdx ).getAtom( itsAtom );
				List<Integer> sellersInvolved = atom.getInterestingSet();
				List<Double> sellersCosts = new LinkedList<Double>();
				
				for(int i = 0; i < sellersInvolved.size(); ++i)
					sellersCosts.add( (Double)atom.getTypeComponent("Cost" + (i+1)) );
				
				_allocation.addAllocatedAgent(_plans.get(itsIdx).getAtom(itsAtom).getAgentId(), sellersInvolved, allocatedBundles,
											  _plans.get(itsIdx).getAtom(itsAtom).getValue(), sellersCosts);
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
			_payments = paymentRule.computePayments();
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
	 * The method prints out the results of the winner determination problem.
	 */
	public void printResults()
	{
		//double obj = _result.getObjectiveValue();
		//System.out.println("Objective: " + obj + ". Wellfare: " + _allocation.getWelfare() );
		
		//Map m = _result.getValues();
		//for(int i = 0; i < _numberOfBuyers; ++i)
		//	for(int j = 0; j < _bids.get(i).getNumberOfAtoms(); ++j )
		//		System.out.println("x"+i+"_"+j+" is " + m.get("x"+i+"_"+j));
		
		double revenue = 0.;
		for(Double p : _payments)
			revenue += p;
		if( _payments.size() != 0 )
			System.out.println("Total revenue: " + revenue);
		else
			System.out.println("Payments were not computed.");
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
	
	@Override
	public void setupReservePrices(List<Double> reservePrices) 
	{
		// TODO Auto-generated method stub
	}
	
	private List<Type> preprocessing( List<Type> bids )
	{
		List<Type> processedBids = new LinkedList<Type>();
		for(Type t1 : bids)
		{
			Type tN = new CombinatorialType();
			for(int i = 0; i < t1.getNumberOfAtoms(); ++i)
			{
				Type atom1 = t1.getAtom(i);
			}
		}
		
		return processedBids;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Auction#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return false;
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
	
	private String _paymentRule;
	private String _paymentCorrectionRule;
	
	private int _numberOfBuyers;									//The number of buyers in the market
	private int _numberOfSellers;									//The number of sellers in the market
	private List<Type> _bids;										//Bids submitted by agents
	private List<Type> _plans;										//Plans generated from the bids submitted by agents
	private Planner _planner;

	private List<Double> _payments;									//A list containing payments of buyers and sellers (first buyers, then sellers)
	private Allocation   _allocation;								//Resulting allocation 

	static int constraintID = 0;									//Constraints counter
}
