package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ilog.cplex.IloCplex;

import java.util.List;


/*
 * The class implements a mechanism proposed in the technical report of Zollinger, Basca, Bernstein.
 */
public class ZollingerMechanismFactory implements IMechanismFactory
{

	/*
	 * A simple constructor.
	 * @param numberOfBuyers - the number of buyers in the double auction
	 * @param numberOfSellers - the number of sellers in the double auction
	 * @param planner - a planner to be used to match buyers to different sellers
	 */
	public ZollingerMechanismFactory(int numberOfBuyers, int numberOfSellers, IPlannerFactory plannerFactory, String paymentRule, String allocationRule)
	{
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_plannerFactory = plannerFactory;
		_paymentRule = paymentRule;
		_allocationRule = allocationRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List)
	 */
	@Override
	public Auction produceMechanism(List<Type> types) 
	{
		FragmentedProbabilisticPlanner planner = (FragmentedProbabilisticPlanner)(_plannerFactory.producePlanner());
		planner.reset(types);
		ZollingerMechanism zm = new ZollingerMechanism(_numberOfSellers, planner.generatePlans());
		zm.setAllocationRule(_allocationRule);
		zm.setPaymentRule(_paymentRule);
		return zm;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism(java.util.List, long)
	 */
	@Override
	public Auction produceMechanism(List<Type> types, long seed) 
	{
		FragmentedProbabilisticPlanner planner = (FragmentedProbabilisticPlanner)(_plannerFactory.producePlanner());
		planner.reset(types);
		ZollingerMechanism zm = new ZollingerMechanism(_numberOfSellers, planner.generatePlans());
		zm.setSeed((int)seed);
		zm.setAllocationRule(_allocationRule);
		zm.setPaymentRule(_paymentRule);
		return zm;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#getMehcanismName()
	 */
	@Override
	public String getMehcanismName() 
	{
		return ZollingerMechanism.class.getSimpleName();
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
		throw new RuntimeException("CPLEX is not supported yet");
	}
	
	private int _numberOfBuyers;					//The number of buyers in the double auction
	private int _numberOfSellers;					//The number of sellers in the double auction produced by the factory
	private IPlannerFactory _plannerFactory;		//A planner factory
	private String _paymentRule;					//A payment rule used in auctions produced by this factory
	private String _allocationRule;					//An allocation rule used in auctions produced by this factory
}
