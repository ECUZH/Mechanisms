package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import java.util.List;

public class SimplePlannerFactory implements IPlannerFactory
{

	/*
	 * Constructor
	 * @param numberOfBuyers - the number of buyers
	 * @param numberOfSellers - the number of sellers
	 * @param types - types of all agents
	 * @param isInjectable - true if an error can be injected into plans and false otherwise
	 */
	public SimplePlannerFactory(int numberOfBuyers, int numberOfSellers, List<Type> types, boolean isInjectable)
	{
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_types = types;
		_isInjectable = isInjectable;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlannerFactory#producePlanner()
	 */
	@Override
	public Planner producePlanner() 
	{
		Planner planner = new SimpleErrorPlanner(_numberOfBuyers, _numberOfSellers, _types);
		if( _isInjectable )
			planner.makeInjectable();
		else
			planner.makeNonInjectable();
		
		return planner;
	}
	
	@Override
	public Planner producePlanner(long seed) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean _isInjectable;									//A flag indicating if an error can be injected into plans
	private List<Type> _types;										//The list of types reported to the planner	

	private int _numberOfBuyers;									//The number of buyers
	private int _numberOfSellers;									//The number of sellers
}
