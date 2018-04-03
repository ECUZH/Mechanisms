package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.uzh.ifi.MechanismDesignPrimitives.IParametrizedValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.LinearThresholdValueFunction;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;

/**
 * The class implements the functionality for generating buyers' value functions in the combinatorial 
 * data market setting. 
 * @author Dmitry Moor
 *
 */
public class BuyersGenerator 
{

	/**
	 * Constructor
	 * @param numberOfDBs number of data bases in the domain
	 * @param endowment initial endowment of buyers with money
	 * @param seed random seed
	 */
	public BuyersGenerator(int numberOfDBs, double endowment, int seed)
	{
		_numberOfDBs = numberOfDBs;
		_endowment = endowment;
		_seed = seed;
	}
	
	/**
	 * The method generates a new buyer. 
	 * @param id agent id
	 * @return a newly generated buyer
	 */
	public ParametrizedQuasiLinearAgent generateBuyer(int id)
	{
		int nDeterministicAllocations = (int)Math.pow(2, _numberOfDBs); // Number of different deterministic allocations of DBs
		int allocations[] = new int[nDeterministicAllocations];			// Possible deterministic allocations of DBs
		//LinearThresholdValueFunction[] valueFunctions = new LinearThresholdValueFunction[nDeterministicAllocations];
		Map<Integer, LinearThresholdValueFunction> valueFunctions = new HashMap<Integer, LinearThresholdValueFunction>();
		
		for(int i = 0; i < nDeterministicAllocations; ++i)				// Binary encodings of different allocations, e.g., ... 
		{																// ... for 2 databases db1 and db2 possible deterministic ...
			allocations[i] = i;											// ...  allocations are 00, 01, 10, 11.
			
			List<Double> alloc = convertToArray(i);
			
			int nAllocated = (int)alloc.stream().mapToDouble( v -> v ).sum();			// Number of allocated DBs
			int magicNumber1 = 1013;
			int magicNumber2 = 11;
			Random gen = new Random(_seed * 100000 + id * magicNumber1 + /*nAllocated*/ i * magicNumber2 );
			
			int threshold = 0;
			double marginalValue = 0.;
			if(i > 0)
			{
				int idxPrev = (int)Math.pow(2, nAllocated-1) - 1;
				List<Integer> subsetKeys = getSubsets(allocations[i]);
				
				double maxMarginalValue = subsetKeys.stream().map( k -> valueFunctions.get(k).getMarginalValue()).mapToDouble(v -> v).max().orElse(0.);
				double maxThreshold = maxMarginalValue != 0 ? subsetKeys.stream().map( k -> valueFunctions.get(k).getMarginalValue() * valueFunctions.get(k).getThreshold()).mapToDouble( v -> v ).max().orElse(0.) / maxMarginalValue : 0.;
				
				marginalValue = _valFactor * gen.nextDouble() + maxMarginalValue;
				threshold = (int) Math.floor(maxThreshold + _thresholdFactor * gen.nextDouble());
			}
			
			LinearThresholdValueFunction v = new LinearThresholdValueFunction(marginalValue, threshold, alloc);
			valueFunctions.put(allocations[i], v);
		}
		
		List<LinearThresholdValueFunction> valueFunctionsList = new CopyOnWriteArrayList<LinearThresholdValueFunction>();
		for(int i = 0; i < nDeterministicAllocations; ++i)
			valueFunctionsList.add(valueFunctions.get(i));
		
		// Parameterized value functions for the buyer
		ParametrizedQuasiLinearAgent buyer = new ParametrizedQuasiLinearAgent(id, _endowment, valueFunctionsList);
		buyer.setNumberOfGoods(_numberOfDBs);
		return buyer;
	}
	
	
	/**
	 * The method returns binary encodings of the given allocation subsets of size N-1, 
	 * where N is the number of allocated DBs in the given binary allocation.
	 * @param binaryAllocation binary representation of the allocation
	 * @return a list of binary sub-allocations
	 */
	private List<Integer> getSubsets(int binaryAllocation)
	{
		List<Integer> subsetsKeys = new LinkedList<Integer>();
		List<Double> alloc = convertToArray(binaryAllocation);
		
		for(int k = 0; k < _numberOfDBs; ++k)
		{
			List<Double> subsetK = new ArrayList<Double>();
			for( Double v : alloc)
				subsetK.add(v);
			
			subsetK.set(k, 0.);
			
			int binarySubset = convertToBinary(subsetK);
			
			if( binarySubset != binaryAllocation )
				subsetsKeys.add( binarySubset );
		}
		
		return subsetsKeys;
	}
	
	
	private List<Double> convertToArray(int binaryAllocation)
	{
		List<Double> alloc = new ArrayList<Double>(_numberOfDBs);
		
		for(int j = 0; j < _numberOfDBs; ++j)
			alloc.add(0.);
		
		for(int j = 0; j < _numberOfDBs; ++j)
			if( ((binaryAllocation >> j) & 0b1 ) > 0 )
				alloc.set(_numberOfDBs - 1 - j, 1.);
			else 
				alloc.set(_numberOfDBs - 1 - j, 0.);
		
		return alloc;
	}
	
	
	private int convertToBinary(List<Double> alloc)
	{
		int binaryAllocation = 0;
		for(int i = 0; i < alloc.size(); ++i)
			binaryAllocation += (int) (alloc.get(i) * Math.pow(2, _numberOfDBs - i - 1));
		
		return binaryAllocation;
	}
	
	private int _numberOfDBs;					// Number of DBs
	private double _endowment;					// Endowment of the buyer
	private double _valFactor = 2.;
	private double _thresholdFactor = 5.;
	private int _seed;							// Random seed
}
