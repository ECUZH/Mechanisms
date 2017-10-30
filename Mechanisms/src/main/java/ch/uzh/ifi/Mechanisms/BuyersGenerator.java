package ch.uzh.ifi.Mechanisms;

import java.util.Arrays;

import ch.uzh.ifi.MechanismDesignPrimitives.IParametrizedValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.LinearThresholdValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;

public class BuyersGenerator 
{

	/**
	 * Constructor
	 * @param numberOfDBs
	 */
	public BuyersGenerator(int numberOfDBs, double endowment)
	{
		_numberOfDBs = numberOfDBs;
		_endowment = endowment;
	}
	
	/**
	 * The method generates a new buyer.
	 * @return
	 */
	public ParametrizedQuasiLinearAgent generateBuyer(int id)
	{
		int nDeterministicAllocations = (int)Math.pow(2, _numberOfDBs); 
		int allocations[] = new int[nDeterministicAllocations];			// Possible deterministic allocations of DBs
		LinearThresholdValueFunction[] valueFunctions = new LinearThresholdValueFunction[nDeterministicAllocations];
		
		for(int i = 0; i < nDeterministicAllocations; ++i)				// Binary encodings of different allocations
		{	
			allocations[i] = i;
			
			double[] alloc = new double[_numberOfDBs];					// Convert binary representation from int to double[]
			for(int j = 0; j < _numberOfDBs; ++j)
				if( ((i >> j) & 0b1 ) > 0 )
					alloc[_numberOfDBs - 1 - j] = 1;
				else 
					alloc[_numberOfDBs - 1 - j] = 0;
			
			int nAllocated = (int)Arrays.stream(alloc).sum();			// Number of allocated DBs
			
			int threshold = 0;
			double marginalValue = 0.;
			if(i > 0)
			{
				int idxPrev = (int)Math.pow(2, nAllocated-1) - 1;
				marginalValue = _valFactor * Math.random() + valueFunctions[idxPrev].getMarginalValue();
				threshold = (int)(valueFunctions[idxPrev].getMarginalValue() * valueFunctions[idxPrev].getThreshold() / marginalValue + 
						_thresholdFactor * Math.random());
			}
			
			LinearThresholdValueFunction v = new LinearThresholdValueFunction(marginalValue, threshold, alloc);
			valueFunctions[i] = v;
		}
		
		// Parameterized value functions for the buyer
		ParametrizedQuasiLinearAgent buyer = new ParametrizedQuasiLinearAgent(id, _endowment, allocations, valueFunctions);
		return buyer;
	}
	
	private int _numberOfDBs;					// Number of DBs
	private double _endowment;					// Endowment of the buyer
	private double _valFactor = 2.;
	private double _thresholdFactor = 2.;
}
