package ch.uzh.ifi.Mechanisms;

import ilog.cplex.IloCplex;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ch.uzh.ifi.MechanismDesignPrimitives.JointProbabilityMass;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.GraphAlgorithms.Graph;

public class CAXORFactory implements IMechanismFactory
{

	/*
	 * A simple constructor.
	 * @param numberOfBuyers - the number of buyers in the double auction
	 * @param numberOfItems - the number of items in the double auction
	 * @param paymentRule - the payment rule to be used by the mechanism
	 * @param costsLimits - a list with upper bounds on costs which should be generated per item
	 * @param grid - a grid modeling dependencies between different items
	 * @param nSamples - the number of samples to be used to generate the joint probability mass function
	 */
	public CAXORFactory(int numberOfBuyers, int numberOfItems, String paymentRule, List<Double> costsLimits, 
									 Graph grid, int nSamples, JointProbabilityMass jpmf, IloCplex solver)
	{
		_numberOfBuyers  = numberOfBuyers;
		_paymentRule = paymentRule;
		_numberOfItems = numberOfItems;
		_costsRanges = costsLimits;
		_jpmf = jpmf;
		_cplexSolver = solver;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#setSolver(ilog.cplex.IloCplex)
	 */
	@Override
	public void setSolver(IloCplex solver) 
	{
		_cplexSolver = solver;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List)
	 */
	@Override
	public Auction produceMechanism(List<Type> types) 
	{
		return produceMechanism(types, System.nanoTime());
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List, long)
	 */
	@Override
	public Auction produceMechanism(List<Type> bids, long seed) 
	{
		List<Double> costs = new LinkedList<>();
		
		Random generator = new Random();
		generator.setSeed(seed);
		
		for(int i = 0; i < _numberOfItems; ++i)
			costs.add( generator.nextDouble() * _costsRanges.get(i) );
		
		assert( _numberOfBuyers == bids.size());
		
		CAXOR ca = new CAXOR(bids.size(), _numberOfItems, bids, costs);
		ca.setPaymentRule(_paymentRule);
		//ca.setSeed(seed);
		return ca;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#getMehcanismName()
	 */
	@Override
	public String getMehcanismName() 
	{
		return CAXOR.class.getSimpleName();	
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return false;
	}

	private int _numberOfBuyers;					//The number of buyers in the double auction
	private int _numberOfItems;						//The number of items to be auctioned off
	private String _paymentRule;					//A payment rule used in the auction produced by this factory
	private List<Double> _costsRanges;				//Range for costs
	private JointProbabilityMass _jpmf;				//Joint probability mass function for availabilities of goods
	private IloCplex _cplexSolver;
}
