package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ilog.cplex.IloCplex;

import java.util.List;

/*
 * The factory produces different probabilistic reverse auction instances with a fixed number of agents.
 */
public class ProbabilisticReverseCAFactory implements IMechanismFactory
{

	/*
	 * A simple constructor.
	 * @param numberOfBuyers - the number of buyers in the double auction
	 * @param numberOfSellers - the number of sellers in the double auction
	 * @param planner - a planner to be used to match buyers to different sellers
	 */
	public ProbabilisticReverseCAFactory(int numberOfBuyers, int numberOfSellers, IPlannerFactory plannerFactory, String paymentRule)
	{
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_plannerFactory = plannerFactory;
		_paymentRule = paymentRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List)
	 */
	@Override
	public Auction produceMechanism(List<Type> types) 
	{
		GeneralProbabilisticPlanner planner = (GeneralProbabilisticPlanner)(_plannerFactory.producePlanner());
		planner.reset(types);
		ProbabilisticReverseCA rca = new ProbabilisticReverseCA(_numberOfSellers, planner.generatePlans());
		rca.setPaymentRule(_paymentRule);
		return rca;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List, long)
	 */
	@Override
	public Auction produceMechanism(List<Type> types, long seed) 
	{
		GeneralProbabilisticPlanner planner = (GeneralProbabilisticPlanner)(_plannerFactory.producePlanner());
		planner.reset(types);
		ProbabilisticReverseCA rca = new ProbabilisticReverseCA(_numberOfSellers, planner.generatePlans());
		rca.setSeed((int)seed);
		rca.setPaymentRule(_paymentRule);
		return rca;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#getMehcanismName()
	 */
	@Override
	public String getMehcanismName() 
	{
		return ProbabilisticReverseCA.class.getSimpleName();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#setSolver(ilog.cplex.IloCplex)
	 */
	@Override
	public void setSolver(IloCplex solver) 
	{
		throw new RuntimeException("CPLEX is not supported now.");
	}
	
	private int _numberOfBuyers;					//The number of buyers in the double auction
	private int _numberOfSellers;					//The number of sellers in the double auction produced by the factory
	private IPlannerFactory _plannerFactory;		//A planner factory
	private String _paymentRule;					//A payment rule used in auctions produced by this factory
}
