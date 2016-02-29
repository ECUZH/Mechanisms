package ch.uzh.ifi.Mechanisms;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandomPlanningStrategy implements IPlanningStrategy
{

	public RandomPlanningStrategy(long errorScenarioSeed, int numberOfBuyers, int numberOfSellers, int minSellersPerPlan, int maxSellersPerPlan, int numberOfPlans)
	{
		_errorScenarioSeed = errorScenarioSeed;
		
		_generator = new Random();
		_generator.setSeed(_errorScenarioSeed + 141990);
		_minSellersPerPlan = minSellersPerPlan;
		_maxSellersPerPlan = maxSellersPerPlan;
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_numberOfPlans = numberOfPlans;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getNumberOfPlans()
	 */
	@Override
	public int getNumberOfPlans() 
	{
		return _numberOfPlans;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getNumberOfSellers(int)
	 */
	@Override
	public int getNumberOfSellers(int planIdx) 
	{
		return _minSellersPerPlan + _generator.nextInt(_maxSellersPerPlan - _minSellersPerPlan + 1);
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getServersUsed(int)
	 */
	@Override
	public List<Integer> getServersUsed(int planIdx) 
	{
		List<Integer> serversUsed = new LinkedList<Integer>();
		for(int j = 0; j < getNumberOfSellers(planIdx); ++j)
		{
			int sellerId = 0;
			while(true)
			{
				sellerId =  _numberOfBuyers + 1 + _generator.nextInt(_numberOfSellers) ;
				if( ! serversUsed.contains(sellerId) )								//No duplicated sellers per plan
					break;
			}
			serversUsed.add(sellerId);
		}
		return serversUsed;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMinEstimatedNumberOfRecords()
	 */
	@Override
	public List<Integer> getMinEstimatedNumberOfRecordsFromSellers(int planIdx) 
	{
		List<Integer> minNumberOfRecordsEst = new LinkedList<Integer>();
		for(int j = 0; j < getNumberOfSellers(0); ++j)
			minNumberOfRecordsEst.add( 0 );
		return minNumberOfRecordsEst;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMaxEstimatedNumberOfRecords()
	 */
	@Override
	public List<Integer> getMaxEstimatedNumberOfRecordsFromSellers(int planIdx) 
	{
		List<Integer> maxNumberOfRecordsEst = new LinkedList<Integer>();
		for(int j = 0; j < getNumberOfSellers(0); ++j)
			maxNumberOfRecordsEst.add( 1 + _generator.nextInt(10) );
		
		_maxRecordsForBuyer = 0;
		for(int i : maxNumberOfRecordsEst)
			_maxRecordsForBuyer += i;
		
		return maxNumberOfRecordsEst;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMinEstimatedNumberOfRecordsForBuyer(int)
	 */
	@Override
	public int getMinEstimatedNumberOfRecordsForBuyer(int planIdx) 
	{
		List<Integer> minRecordsFromSellers = getMinEstimatedNumberOfRecordsFromSellers(planIdx);
		int minRecordsForBuyer = 0;
		for(int i : minRecordsFromSellers)
			minRecordsForBuyer += i;
		
		return minRecordsForBuyer;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMaxEstimatedNumberOfRecordsForBuyer(int)
	 */
	@Override
	public int getMaxEstimatedNumberOfRecordsForBuyer(int planIdx) 
	{
		return _maxRecordsForBuyer;
	}
	
	private long _errorScenarioSeed;
	private Random _generator;
	private int _minSellersPerPlan;
	private int _maxSellersPerPlan;
	private int _numberOfBuyers;
	private int _numberOfSellers;
	private int _numberOfPlans;
	private int _maxRecordsForBuyer;
}
