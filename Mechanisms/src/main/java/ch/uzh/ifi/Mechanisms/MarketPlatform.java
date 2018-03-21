package ch.uzh.ifi.Mechanisms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.ParametrizedQuasiLinearAgent;
import ch.uzh.ifi.MechanismDesignPrimitives.ProbabilisticAllocation;
import ch.uzh.ifi.MechanismDesignPrimitives.SellerType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

/**
 * The class models the market platform. It provides methods for evaluation of the market demand, 
 * estimation of the aggregate market value, computation of values of DBs, equilibrium search etc.
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
	public MarketPlatform(List<ParametrizedQuasiLinearAgent> buyers, List<SellerType> sellers)
	{
		_buyers  = buyers;
		_sellers = sellers;
	}
	
	/**
	 * The method performs an iterative posted price search procedure.
	 * @return the posted price
	 * @throws Exception 
	 */
	public double tatonementPriceSearch() throws Exception
	{	
		// Initial probabilistic allocation of sellers: everyone is allocated with equal probability
		ProbabilisticAllocation probAllocation = new ProbabilisticAllocation();
		List<Integer> bidders = new LinkedList<Integer>();
		List<Double> allocationProbabilities = new LinkedList<Double>();
		List<Integer> bundles = new LinkedList<Integer>();
		for(int j = 0; j < _sellers.size(); ++j)
		{
			bidders.add(_sellers.get(j).getAgentId());
			bundles.add(_sellers.get(j).getAtom(0).getInterestingSet().get(0));
			allocationProbabilities.add(1.0);
		}
		probAllocation.addAllocatedAgent(0, bidders, bundles, allocationProbabilities);
		probAllocation.normalize();

		// Initialization
		_numberOfDBs = probAllocation.getNumberOfGoods();
		double price = 0.;
		
		// Iterative price/allocation update procedure
		double diff = 0.;
		for(int i = 0; i < _MAX_ITER; ++i)
		{
			// List of outcomes in surplus optimal reverse auctions for different DBs
			List<Allocation> allocations = new LinkedList<Allocation>();
			List<Double> payments = new LinkedList<Double>();
			
			//-------
			List<Double> externalitiesOfDBs = new LinkedList<Double>();			
			for(int j = 0; j < _numberOfDBs; ++j)
				externalitiesOfDBs.add( computeExternalityOfDB(j, price, probAllocation));
			
			
			double marketDemandForRows = computeMarketDemand(price, probAllocation, true).get(1);
			double aggregateValue = computeAggregateValue(marketDemandForRows, probAllocation);
			double externalitiesTotal = externalitiesOfDBs.stream().reduce(0., (i1, i2) -> i1+i2);
			//-------
			
			// For every DB solve the surplus optimal auction
			for(int j = 0; j < _numberOfDBs; ++j)						// TODO: here I have an assumption that Ids of DBs are between 0 and 1
			{
				// First, find all sellers producing the DB
				List<Type> sellersInvolved = new LinkedList<Type>();
				for(int k = 0; k < _sellers.size(); ++k)
					if( _sellers.get(k).getInterestingSet(0).get(0) == j )
						sellersInvolved.add(_sellers.get(k));
				
				// Second, compute the value of the market platform for the DB
				double dbValue = 0;//computeValueOfDB(j, price, probAllocation);
				//---------------
				if( Math.abs(externalitiesOfDBs.get(j)) < 1e-6 )
					dbValue =  0.;
				else
					dbValue = externalitiesOfDBs.get(j) / externalitiesTotal * aggregateValue;
				//---------------
				//_logger.debug("Value for DB_"+j+" is " + dbValue);
				
				// Third, instantiate a surplus optimal reverse auction for these sellers
				SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(sellersInvolved, dbValue);

				// Finally, solve the auction
				auction.solveIt();
				if( auction.getAllocation().getNumberOfAllocatedAuctioneers() > 0 )
				{
					allocations.add(auction.getAllocation());
					payments.add(auction.getPayments()[0]);
				}
			}
			
			// Compute the gradient for allocation probabilities
			diff = 0.;
			for(int j = 0; j < _sellers.size(); ++j)
			{
				double allocProbNew = 0.;
				
				// Check if the seller is still allocated
				for(int k = 0; k < allocations.size(); ++k)
					if(allocations.get(k).getBiddersInvolved(0).contains( _sellers.get(j).getAgentId() ))
					{
						allocProbNew = 1;
						break;
					}
				
				diff += Math.pow((allocProbNew - allocationProbabilities.get(j)) * _STEP, 2);
				
				allocationProbabilities.set(j, allocationProbabilities.get(j) + (allocProbNew - allocationProbabilities.get(j)) * _STEP);				
				//_logger.debug("New allocation probability: " + allocationProbabilities.get(j));
			}
			
			// Compute the gradient for the price
			double totalPayment = 0.;
			for(int j = 0; j < payments.size(); ++j)
				totalPayment += payments.get(j);

			double totalPaid = computeMarketDemand(price, probAllocation, false).get(1)*price;
			//_logger.debug("Total payment: " + totalPayment + "; Total received: " + totalPaid); 
			price = price + (totalPayment - totalPaid)*_STEP;
			diff += Math.pow((totalPayment - totalPaid)*_STEP, 2);
			
			probAllocation.resetAllocationProbabilities(allocationProbabilities);
			
			//_logger.debug("New price" + price);
			//BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			//String s = bufferRead.readLine();
			
			if ( diff < _TOL)
				break;
		}
		
		//_logger.debug("Found price: " + price + "; diff="+ diff);
		return price;
	}
	
	/**
	 * The method computes the market demand for both goods at the given price level and given allocation of DBs.
	 * @param price the price of the good 1 (good 0 is money with p0 = 1 - normalized)
	 * @param allocation probabilistic allocation of sellers (DBs)
	 * @return the market demand for all goods
	 */
	public List<Double> computeMarketDemand(double price, ProbabilisticAllocation allocation, boolean updateProbDistribution)
	{
		//_logger.debug("computeMarketDemand("+price + ", " + Arrays.toString(allocation.getAllocationProbabilities().toArray()) + ")");
		List<Double> marketDemand = Arrays.asList(0., 0.);
		List<Double> prices = Arrays.asList(1., price);
		
		if( updateProbDistribution )
		{
			_buyers.get(0).updateAllocProbabilityDistribution(allocation);
			for(ParametrizedQuasiLinearAgent buyer: _buyers)
				buyer.setAllocProbabilityDistribution( _buyers.get(0).getAllocProbabilityDistribution());
		}
		
		for(ParametrizedQuasiLinearAgent buyer: _buyers)
		{
			List<Double> consumptionBundle = buyer.solveConsumptionProblem(prices, allocation);
			marketDemand.set(0, marketDemand.get(0) + consumptionBundle.get(0));
			marketDemand.set(1, marketDemand.get(1) + consumptionBundle.get(1));
			_logger.debug("Demand of i=" + buyer.getAgentId() + " given price p= "+ price +" x0: " + consumptionBundle.get(0) + "; x1: " + consumptionBundle.get(1));
		}
		//_logger.debug("Market demand: " + marketDemand);
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
		//_logger.debug("computeAggregateValue( "+quantity +", " + Arrays.toString(allocation.getAllocationProbabilities().toArray())+")");
		double value = 0.;
		
		//To analyze the maximal inverse demand, first one need to compute the maximal prices
		List<Double> marginalValues = new ArrayList<Double>();
		
		_buyers.get(0).updateAllocProbabilityDistribution(allocation);
		for(ParametrizedQuasiLinearAgent buyer: _buyers)
		{
			buyer.setAllocProbabilityDistribution( _buyers.get(0).getAllocProbabilityDistribution());
			marginalValues.add(buyer.computeExpectedMarginalValue(allocation));
		}
		
		Collections.sort(marginalValues);
		Collections.reverse(marginalValues);
		
		for(int i = 1; i < marginalValues.size(); ++i)						// Remove duplicates
			if( Math.abs(marginalValues.get(i) - marginalValues.get(i-1)) < 1e-6)
			{
				marginalValues.remove(i);
				i -= 1;
			}
		
		//Now integrate the maximal inverse demand
		double price = 0.;
		double marketDemandHigh = 0.;
		for( int i = 0; i < marginalValues.size(); ++i )
		{
			price = marginalValues.get(i);
			double marketDemandLow = marketDemandHigh;//computeMarketDemand(price + 1e-8, allocation).get(1);
			marketDemandHigh  = computeMarketDemand(price - 1e-8, allocation, false).get(1);

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
		//_logger.debug("Computed Aggregate value is  " + value);
		return value;
	}
	
	/**
	 * The method computes the value of the specified database that is proportional to the positive externality the DB imposes
	 * on buyers given current market prices and allocation of other DBs.
	 * @param dbId id of the database
	 * @param price current market price per row of a query answer
	 * @param allocation current probabilistic allocation of sellers
	 * @return the value of the specified DB.
	 * @throws Exception 
	 */
	public double computeValueOfDB(int dbId, double price, ProbabilisticAllocation allocation) throws Exception
	{
		//_logger.debug("computeValueOfDB("+dbId + ", "+price +", " + Arrays.toString(allocation.getAllocationProbabilities().toArray()) +")");
		double marketDemandForRows = computeMarketDemand(price, allocation, true).get(1);
		
		double externality = computeExternalityOfDB(dbId, price, allocation);
//		double valueOfDB = externality;
		List<Double> externalitiesOfDBs = new LinkedList<Double>();
		
		int numberOfDBs = allocation.getNumberOfGoods();
		for(int i = 0; i < numberOfDBs; ++i)
			externalitiesOfDBs.add( computeExternalityOfDB(i, price, allocation));
		
		//_logger.debug("Externality = " + externality + ". Other externalities: " + valuesOfDBs.toString());
		if( Math.abs(externality) < 1e-6 )
			return 0.;
		
		double valueOfDB = externality / externalitiesOfDBs.stream().reduce(0., (i, j) -> i+j) * computeAggregateValue(marketDemandForRows, allocation);
		//_logger.debug("Computed Value is " + valueOfDB + " = " + computeAggregateValue(marketDemand, allocation) + " - " + computeAggregateValue(marketDemand, allocationReduced));
		return valueOfDB;
	}
	
	/**
	 * The method computes the positive externality that the DB imposes
	 * on buyers given current market prices and allocation of other DBs.
	 * @param dbId id of the database
	 * @param price current market price per row of a query answer
	 * @param allocation current probabilistic allocation of sellers
	 * @return the positive externality of the specified DB.
	 * @throws Exception 
	 */
	public double computeExternalityOfDB(int dbId, double price, ProbabilisticAllocation allocation) throws Exception
	{
		_logger.debug("computeValueOfDB("+dbId + ", "+price +", " + Arrays.toString(allocation.getAllocationProbabilities().toArray()) +")");
		double marketDemandForRows = computeMarketDemand(price, allocation, true).get(1);
		
		// An allocation in which DB with id=dbId is not allocated
		ProbabilisticAllocation allocationReduced = new ProbabilisticAllocation();
		allocationReduced.addAllocatedAgent(allocation.getAuctioneerId(0), 
											allocation.getBiddersInvolved(0),
				                            allocation.getAllocatedBundlesOfTrade(0), 
				                            allocation.getAllocationProbabilities());
		allocationReduced.deallocateBundle(dbId);
		
		double marketDemandForRowsReduced = computeMarketDemand(price, allocationReduced, true).get(1);
		
		double aggregateValue = computeAggregateValue(marketDemandForRows, allocation);
		double aggregateValueReduced = computeAggregateValue(marketDemandForRowsReduced, allocationReduced);
		double externality = aggregateValue - aggregateValueReduced;
		
		_logger.debug("Computed externality is " + externality + " = " + aggregateValue + " - " + aggregateValueReduced);
		if( externality < 0 ) throw new RuntimeException("DB has a neggative externality: " + externality);
		
		return externality;
	}
	
	/**
	 * The method sets the tolerance level for the gradient descent.
	 * @param tol tolerance level
	 */
	public void setToleranceLvl(double tol)
	{
		_TOL = tol;
	}
	
	private List<ParametrizedQuasiLinearAgent> _buyers;				// Buyers
	private List<SellerType> _sellers;								// Sellers
	private int _numberOfDBs;										// Number of databases
	private int _MAX_ITER = 1000;									// Max number of gradient descent iterations
	private double _STEP = 0.01;									// Step of the gradient descent
	private double _TOL = 1e-5;										// Tolerance of the gradient descent
}
