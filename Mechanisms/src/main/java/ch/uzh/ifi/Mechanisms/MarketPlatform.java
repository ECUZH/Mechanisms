package ch.uzh.ifi.Mechanisms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
			allocationProbabilities.add(1.0/* - 0.1*Math.random()*/);
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
			
			// For every DB solve the surplus optimal auction
			for(int j = 0; j < _numberOfDBs; ++j)						// TODO: here I have an assumption that Ids of DBs are between 0 and 1
			{
				// First, find all sellers producing the DB
				List<Type> sellersInvolved = new LinkedList<Type>();
				for(int k = 0; k < _sellers.size(); ++k)
					if( _sellers.get(k).getInterestingSet(0).get(0) == j )
						sellersInvolved.add(_sellers.get(k));
				
				// Second, compute the value of the market platform for the DB
				double dbValue = computeValueOfDB(j, price, probAllocation);
				
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
				//System.out.println("New allocation probability: " + allocationProbabilities.get(j));
			}
			
			// Compute the gradient for the price
			double totalPayment = 0.;
			for(int j = 0; j < payments.size(); ++j)
				totalPayment += payments.get(j);

			double totalPaid = computeMarketDemand(price, probAllocation, false).get(1)*price;
			//_logger.debug("Total payment: " + totalPayment + "; Total received: " + totalPaid); 
			price = price + (totalPayment - totalPaid)*_STEP;
			//diff += Math.pow((totalPayment - totalPaid)*_STEP, 2);
			
			probAllocation.resetAllocationProbabilities(allocationProbabilities);
			
			//_logger.debug("New price" + price);
			//System.out.println("New price: " + price + " z="+diff);
			//BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
			//String s = bufferRead.readLine();
			
			if ( diff < _TOL)
				break;
			if(i == _MAX_ITER - 1) System.out.println("Reached MAX_ITER.");
		}
		
		_logger.debug("Found price: " + price + "; diff="+ diff);
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
			List<Double> consumptionBundle = buyer.solveConsumptionProblem(prices);
			marketDemand.set(0, marketDemand.get(0) + consumptionBundle.get(0));
			marketDemand.set(1, marketDemand.get(1) + consumptionBundle.get(1));
			//_logger.debug("Demand of i=" + buyer.getAgentId() + " given price p= "+ price +" x0: " + consumptionBundle.get(0) + "; x1: " + consumptionBundle.get(1) + 
			//		      " (his marginal value is "+ buyer.computeExpectedMarginalValue(allocation) +", threshold: "+ buyer.computeExpectedThreshold(allocation) +")");
		}
		//_logger.debug("Market demand: " + marketDemand);
		return marketDemand;
	}
	
	/**
	 * The method computes the aggregate value function at the given quantity and with the given probabilistic allocation.
	 * @param totalQuantityDemanded the quantity of good 1 to be consumed
	 * @param allocation the probabilistic allocation of sellers/DBs
	 * @return the aggregate value
	 */
	public double computeAggregateValue(double price, double totalQuantityDemanded, ProbabilisticAllocation allocation)
	{
		_logger.debug("computeAggregateValue( "+totalQuantityDemanded +", " + Arrays.toString(allocation.getAllocationProbabilities().toArray())+")");
		double value = 0.;
		
		//To analyze the maximal inverse demand, first one need to compute the maximal prices
		List<Double> marginalValues = new CopyOnWriteArrayList<Double>();
		
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
		/*for(int i = 0 ; i < _buyers.size(); ++i)
		{
			List<Double> optBundle = _buyers.get(i).solveConsumptionProblem(Arrays.asList(1., price), allocation);
			value += _buyers.get(i).computeUtility(allocation, optBundle) - _buyers.get(i).getEndowment();
		}
		value += price * totalQuantityDemanded;
		_logger.debug("Computed Aggregate value is  " + value);*/
		
		
		
		//double price = 0.;
		////double marketDemandHigh = 0.;
		//double marketDemandHigh = computeMarketDemand(marginalValues.get(0), allocation, false).get(1);
		//value += marketDemandHigh * marginalValues.get(0);
		//_logger.debug("Init value: " + value);
		//_logger.debug("The number of integration steps: " + marginalValues.size());
		//for( int i = 1/* 0 */; i < marginalValues.size(); ++i )
		//{
		//	price = marginalValues.get(i);
		//	//double marketDemandLow = marketDemandHigh;          //computeMarketDemand(price + 1e-8, allocation).get(1);
		//	
		//	//List<Double> marketDemandHighList = computeMarketDemand(price, allocation, false);
		//	//marketDemandHigh = marketDemandHighList.get(1);

			
		//	double marketDemandLow = marketDemandHigh;
		//	marketDemandHigh = computeMarketDemand(price, allocation, false).get(1);
		//	
		//	if( marketDemandHigh < totalQuantityDemanded )
		//	{
		//		double dV = 0.5 * (price + marginalValues.get(i-1)) * (marketDemandHigh - marketDemandLow);
		//		value += dV;
		//		//value += _buyers.get(0).getEndowment()*_buyers.size() - marketDemandMoney;
		//		//value += marketDemandMoneyLow - marketDemandMoneyHigh;
		//		_logger.debug("Given price p="+price+" the marketDemandLow="+marketDemandLow+", marketDemandHigh="+marketDemandHigh + 
		//				      ", dV=" + dV + " value="+value + "(q= "+totalQuantityDemanded+ ")" + " m*="+marketDemandHigh);
		//	}
		//	else
		//	{
		//		//value += price * (totalQuantityDemanded - marketDemandLow);
		//		double dV = 0.5* (marginalValues.get(i-1) + marginalValues.get(i-1)*(marketDemandHigh-totalQuantityDemanded)/(marketDemandHigh-marketDemandLow) ) * (totalQuantityDemanded - marketDemandLow);
		//		value += dV;
		//		//value += price * totalQuantityDemanded - _buyers.get(0).getEndowment()*_buyers.size() +  marketDemandMoneyLow;
		//		_logger.debug("Given price p="+price+" the marketDemandLow="+marketDemandLow+", marketDemandHigh="+marketDemandHigh + 
		//			      ", dV=" + dV + " value="+value + "(q= "+totalQuantityDemanded+ ")");
		//		break;
		//	}	
		//}
		
		
		//-----------------------------------
		//    TEST Multithreading
		//-----------------------------------
		_vals = new double[_numberOfThreads];
		
		try
		{
			List<Thread> threads = new LinkedList<Thread>();
			for(int i = 0; i < _numberOfThreads; ++i)
			{
				Thread thread = new Thread(new DemandWorker("Thread", i, price, allocation) );
				threads.add(thread);
			}
			
			for(int i = 0; i < _numberOfThreads; ++i)
				threads.get(i).start();
			
			for(int i = 0; i < _numberOfThreads; ++i)
				threads.get(i).join(0);
		}
		catch(InterruptedException e)
		{
		    e.printStackTrace();
		}

		for(int i = 0; i < _numberOfThreads; ++i)
			value = value + _vals[i];
		value += price * totalQuantityDemanded;
		_logger.debug("Computed Aggregate tstValue is  " + value);
		
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
		_logger.debug("computeValueOfDB("+dbId + ", "+price +", " + Arrays.toString(allocation.getAllocationProbabilities().toArray()) +")");
		double marketDemandForRows = computeMarketDemand(price, allocation, true).get(1);
		
		List<Double> externalitiesOfDBs = new LinkedList<Double>();
		
		int numberOfDBs = allocation.getNumberOfGoods();
		for(int i = 0; i < numberOfDBs; ++i)
		{
			externalitiesOfDBs.add( computeExternalityOfDB(i, price, allocation));
			if( i == dbId && Math.abs(externalitiesOfDBs.get(dbId)) < 1e-6 )
			{
				_logger.debug("Computed Value of dbID = " + dbId + " is 0 (zero externality).");
				return 0.;
			}
		}
		
		double valueOfDB = externalitiesOfDBs.get(dbId) / externalitiesOfDBs.stream().reduce(0., (i, j) -> i+j) * computeAggregateValue(price, marketDemandForRows, allocation);
		_logger.debug("Computed Value of dbID = " + dbId + " is " + valueOfDB);
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
		_logger.debug("computeExternalityOfDB(dbId="+dbId + ", p="+price +", alloc=" + Arrays.toString(allocation.getAllocationProbabilities().toArray()) +")");
		double marketDemandForRows = computeMarketDemand(price, allocation, true).get(1);
		
		// An allocation in which DB with id=dbId is not allocated
		ProbabilisticAllocation allocationReduced = new ProbabilisticAllocation();
		allocationReduced.addAllocatedAgent(allocation.getAuctioneerId(0), 
											allocation.getBiddersInvolved(0),
				                            allocation.getAllocatedBundlesOfTrade(0), 
				                            allocation.getAllocationProbabilities());
		allocationReduced.deallocateBundle(dbId);
		
		double marketDemandForRowsReduced = computeMarketDemand(price, allocationReduced, true).get(1);
		
		double aggregateValue = computeAggregateValue(price, marketDemandForRows, allocation);
		double aggregateValueReduced = computeAggregateValue(price, marketDemandForRowsReduced, allocationReduced);
		double externality = aggregateValue - aggregateValueReduced;
		
		_logger.debug("Computed externality for dbID=" + dbId + " is " + externality + " = " + aggregateValue + " - " + aggregateValueReduced);
		if( externality < 0 && Math.abs(externality/aggregateValue) > 0.01 ) throw new RuntimeException("DB has a neggative externality: " + externality + " aggregateValue="+aggregateValue);
		else if(externality < 0)
			externality = 0;
		
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
	
	/**
	 * 
	 * @param step
	 */
	public void setStep(double step)
	{
		_STEP = step;
	}
	
	
	private class DemandWorker implements Runnable
	{
		private Thread _thread;										//A thread object
		private String _threadName;									//The thread's name
		private int _threadId;										//A thread id
		private ProbabilisticAllocation _alloc;
		private double _price;
		private int _idxLow;
		private int _idxHigh;
		
		public DemandWorker(String name, int threadId, double price, ProbabilisticAllocation allocation)
		{
			_threadName = name + threadId;
			_threadId = threadId;
			_alloc = allocation;
			_price = price;
			
			_idxLow = _threadId * _buyers.size() / _numberOfThreads;
			_idxHigh = (_threadId + 1) * _buyers.size() / _numberOfThreads - 1;
		}
		
		@Override
		public void run() 
		{				
			double value = 0.;
						
			for( int i = _idxLow; i <= _idxHigh; ++i )
			{
				List<Double> optBundle = _buyers.get(i).solveConsumptionProblem(Arrays.asList(1., _price));
				value += _buyers.get(i).computeUtility(optBundle) - _buyers.get(i).getEndowment();
			}
			//_logger.debug("Thread id=" +_threadId + ". idx=["+_idxLow+", "+_idxHigh +"]. Val="+value);
			_vals[_threadId] = value;
		}
		
		public void start()
		{
			if(_thread == null)
			{
				_thread = new Thread(this, _threadName);
				_thread.start();
			}
		}
	}
	
	public void setNumberOfThreads(int nThreads)
	{
		_numberOfThreads = nThreads;
	}
	
	private List<ParametrizedQuasiLinearAgent> _buyers;				// Buyers
	private List<SellerType> _sellers;								// Sellers
	private int _numberOfDBs;										// Number of databases
	private int _MAX_ITER = 10000;									// Max number of gradient descent iterations
	private double _STEP = 0.01;									// Step of the gradient descent
	private double _TOL = 1e-7;										// Tolerance of the gradient descent
	private int _numberOfThreads = 8;
	private double[] _vals;
}
