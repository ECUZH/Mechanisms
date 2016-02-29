package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import java.util.List;

public class JoinErrorPlanner extends GeneralErrorPlanner
{
	/*
	 * Constructor
	 */
	public JoinErrorPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types)
	{
		super(numberOfBuyers, numberOfSellers, types);
		_initTuples = 1;
	}
	
	@Override
	public int dbOperation(int a, int b)
	{
		return a * b;
	}
	
}
