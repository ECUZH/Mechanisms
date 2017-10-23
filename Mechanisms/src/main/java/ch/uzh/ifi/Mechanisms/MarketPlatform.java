package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;

/**
 * The class corresponds to the market platform entity. It provides methods for evaluation of the market demand, 
 * estimation of the aggregate market value, computation of values of DBs etc.
 * @author Dmitry Moor
 *
 */
public class MarketPlatform 
{

	private static final Logger _logger = LogManager.getLogger(MarketPlatform.class);
	/**
	 * Constructor.
	 * @param buyers list of buyers
	 * @param sellers list of sellers
	 */
	public MarketPlatform(List<ParametrizedQuasiLinearAgent> buyers, List<AtomicBid> sellers)
	{
		_buyers  = buyers;
		_sellers = sellers;
	}
	
	/**
	 * The method computes the market demand at the given price.
	 * @param price the price of the good
	 * @param allocation probabilistic allocation of sellers (DBs)
	 * @return the market demand
	 */
	public List<Double> computeMarketDemand(double price, ProbabilisticAllocation allocation)
	{
		List<Double> marketDemand = Arrays.asList(0., 0.);
		List<Double> prices = Arrays.asList(1., price);
		
		for(ParametrizedQuasiLinearAgent buyer: _buyers)
		{
			List<Double> consumptionBundle = buyer.solveConsumptionProblem(prices, allocation);
			marketDemand.set(0, marketDemand.get(0) + consumptionBundle.get(0));
			marketDemand.set(1, marketDemand.get(1) + consumptionBundle.get(1));
			_logger.debug("Demand of i=" + buyer.getAgentId() + " given price p= "+ price +" x0: " + consumptionBundle.get(0) + "; x1: " + consumptionBundle.get(1));
		}
		
		return marketDemand;
	}
	
	/**
	 * The method computes the aggregate value function at the given quantity and with the given probabilistic allocation.
	 * @param quantity the quantity of good 1 to be consumed
	 * @param allocation the probabilistic allocation of sellers/DBs
	 * @return the aggregate value
	 */
	public double computeAggregateValue(double quantity, ProbabilisticAllocation allocation)
	{
		double value = 0.;
		
		//To analyze the maximal inverse demand, first one need to compute the maximal prices
		List<Double> marginalValues = new ArrayList<Double>();
		for(ParametrizedQuasiLinearAgent buyer: _buyers)
			marginalValues.add(buyer.computeExpectedMarginalValue(allocation));
		
		Collections.sort(marginalValues);
		Collections.reverse(marginalValues);
		
		for(int i = 1; i < marginalValues.size(); ++i)
			if(marginalValues.get(i) == marginalValues.get(i-1))
			{
				marginalValues.remove(i);
				i -= 1;
			}
		
		//Now integrate the maximal inverse demand
		double price = 0.;
		for( int i = 0; i < marginalValues.size(); ++i )
		{
			price = marginalValues.get(i);
			double marketDemandHigh  = computeMarketDemand(price - 1e-8, allocation).get(1);
			double marketDemandLow = computeMarketDemand(price + 1e-8, allocation).get(1);
			if( marketDemandHigh < quantity )
			{
				value += price * (marketDemandHigh - marketDemandLow);
			}
			else
			{
				value += price * (quantity - marketDemandLow);
				break;
			}	
		}

		return value;
	}
	
	/**
	 * The method computes the value of the specified database as the positive externality the DB imposes
	 * on buyers given current market prices and allocation of other DBs.
	 * @param dbId id of the database
	 * @param price current market price per row of a query answer
	 * @param allocation current probabilistic allocation of sellers
	 * @return the value of the specified DB.
	 * @throws Exception 
	 */
	public double computeValueOfDB(int dbId, double price, ProbabilisticAllocation allocation) throws Exception
	{
		double valueOfDB = 0.;		
		double marketDemand = computeMarketDemand(price, allocation).get(1);
		
		ProbabilisticAllocation allocationReduced = new ProbabilisticAllocation();
		allocationReduced.addAllocatedAgent(allocation.getAuctioneerId(0), 
											allocation.getBiddersInvolved(0),
				                            allocation.getAllocatedBundlesOfTrade(0), 
				                            allocation.getAuctioneersAllocatedValue(0),
				                            allocation.getBiddersValues(),
				                            allocation.getAllocationProbabilities());
		allocationReduced.deallocateBundle(dbId);
		valueOfDB = computeAggregateValue(marketDemand, allocation) - computeAggregateValue(marketDemand, allocationReduced);
		
		return valueOfDB;
	}
	
	private List<ParametrizedQuasiLinearAgent> _buyers;				//Buyers
	private List<AtomicBid> _sellers;								//Sellers
}
