package ch.uzh.ifi.Mechanisms;

import ilog.cplex.IloCplex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
	 * @throws Exception 
	 */
	public MarketPlatform(List<ParametrizedQuasiLinearAgent> buyers, List<SellerType> sellers) throws Exception
	{
		_buyers  = buyers;
		_sellers = sellers;
		_numberOfThreads = 1;
		_cplexSolver = new IloCplex();
		
		ProbabilisticAllocation probAllocation = new ProbabilisticAllocation();		//Allocation of DBs
		List<Integer> bidders = new LinkedList<Integer>();
		List<Integer> bundles = new LinkedList<Integer>();
		List<Double> allocationProbabilities = new LinkedList<Double>();						//Allocation probabilities of sellers
		
		//Initial conditions: every seller produces her database with Prob = 1.
		for(int j = 0; j < _sellers.size(); ++j)
		{
			bidders.add(_sellers.get(j).getAgentId());
			bundles.add(_sellers.get(j).getAtom(0).getInterestingSet().get(0));
			allocationProbabilities.add(1.0);
		}		
			
		probAllocation.addAllocatedAuctioneer(0, bidders, bundles, allocationProbabilities);
		probAllocation.normalize();													// ????????????????????????
	
		// Initialization
		_numberOfDBs = probAllocation.getNumberOfGoods();
	}
	
	/**
	 * The method performs an iterative posted price search procedure.
	 * @return the posted price
	 * @throws Exception 
	 */
	public double tatonementPriceSearch(double startPrice) throws Exception
	{	
		// Initialization
		double price = startPrice;
		double diff = 0.;
		
		// Iterative price/allocation update procedure
		for(int i = 0; i < _MAX_ITER; ++i)
		{			
			// List of outcomes in surplus optimal reverse auctions for different DBs
			Allocation allocation = new Allocation();
			
			// Compute the excess demand for money
			double excessDemandMoney = computeExcessDemand(allocation, price); 
			price = Math.max(0., price + excessDemandMoney * _STEP  );
			
			diff = Math.pow(excessDemandMoney, 2);
						
			System.out.println("New price: " + price + " z="+ excessDemandMoney + " " + (Math.signum(excessDemandMoney)>0?"Increased":"Decreased"));
//			BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
//			String s = bufferRead.readLine();
			
			if ( Math.sqrt(diff /*/ (_sellers.size() + 1)*/) < _TOL)
			{
				//System.out.println("New allocation probabilities: " + Arrays.toString(_allocationProbabilities.toArray()));
				break;
			}
			if(i == _MAX_ITER - 1) System.out.println("Reached MAX_ITER.");
		}
		
		_logger.debug("Found price: " + price + "; diff="+ diff);
		return price;
	}
	
	/**
	 * The method computes the excess demand for money.
	 * @param allocations will store the allocation decision of the BORA auction
	 * @param price current posted price
	 * @return excess demand for money
	 * @throws Exception
	 */
	private double computeExcessDemand(Allocation allocation, double price) throws Exception
	{
		_logger.debug("computeExcessDemand(allocation, " + " price=" + price + ")");
		double excessDemand = 0.;
		_marketDemandMoney = new HashMap<Integer, Double>();
		_marketDemandRows = new HashMap<Integer, Double>();
		_aggregateValue = new HashMap<Integer, Double>();
		
		// First, compute the induced values DBs for different deterministic allocations of DBs given the current posted price
		List< List<Double> > inducedValues = computeValuesOfDBs(price);
		
		//Now, solve the BORA auction with the induced values of DBs and compute the total payment to be accrued to sellers
		_logger.debug("Instantiate BORA...");
		SurplusOptimalReverseAuction auction = new SurplusOptimalReverseAuction(_sellers, inducedValues);
		auction.setSolver(_cplexSolver);
		auction.solveIt();
		
		allocation = auction.getAllocation();
		double totalPayment = 0.;
		for(int i = 0; i < auction.getPayments().length; ++i)
			totalPayment += auction.getPayments()[i];
		
		//Compute the binary representation of the optimal det. allocation of DBs of the BORA auction
		int detAllocDBs = 0;
		for(int i = 0; i < allocation.getBiddersInvolved(0).size(); ++i)
		{
			int dbBit = 1 << (allocation.getAllocatedBundlesOfTrade(0).get(i) - 1);
			detAllocDBs = detAllocDBs | dbBit;
		}
		//System.out.println("Solution to BORA: " + allocation.getNumberOfAllocatedAuctioneers() + ", " + allocation.getBiddersInvolved(0).size() + "; detAlloc=" + detAllocDBs);
		
		double totalPaid = computeMarketDemand(price, detAllocDBs).get(1) * price;
		
		excessDemand = totalPayment - totalPaid; 
		_logger.debug("Total payment to sellers: " + totalPayment + "; Total received from buyers: " + totalPaid + ". Excess Demand: " + excessDemand);
		
		return excessDemand;
	}
	
	/**
	 * The method computes the market demand for both goods at the given price level and given allocation of DBs.
	 * @param price the price of the good 1 (good 0 is money with p0 = 1 - normalized)
	 * @param allocation probabilistic allocation of sellers (DBs)
	 * @param updateProbDistribution true of the probability distribution over deterministic allocations needs to be recomputed according to the new 
	 * probabilistic allocation of sellers
	 * @return the market demand for all goods
	 */
	public List<Double> computeMarketDemand(double price, int detAllocDBs)
	{
		_logger.debug("computeMarketDemand("+price + ", " + detAllocDBs + ")");
		
		if(_marketDemandMoney.containsKey(detAllocDBs))
		{
			_logger.debug("cashed");
			return Arrays.asList(_marketDemandMoney.get(detAllocDBs), _marketDemandRows.get(detAllocDBs));
		}
		
		double[] marketDemandMoney = new double[_numberOfThreads];
		double[] marketDemandRows  = new double[_numberOfThreads];
		
		// First, update the probability distribution and expected values/thresholds according to the new probabilistic allocation
		for(int i = 0; i < _buyers.size(); ++i)
		{
			_buyers.get(i).setNumberOfGoods(_numberOfDBs);
			_buyers.get(i).updateAllocProbabilityDistribution(detAllocDBs, _numberOfDBs);
		}
		
		// Compute the total market demand
		try
		{
			List<Thread> threads = new LinkedList<Thread>();
			for(int i = 0; i < _numberOfThreads; ++i)
			{
				Thread thread = new Thread(new DemandWorker("Thread", i, price, marketDemandMoney, marketDemandRows) );
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
		
		double totalMarketDemandMoney = 0.;
		double totalMarketDemandRows = 0.;
		for(int i = 0; i < _numberOfThreads; ++i)
		{
			totalMarketDemandMoney += marketDemandMoney[i];
			totalMarketDemandRows += marketDemandRows[i];
		}
		_marketDemandMoney.put(detAllocDBs, totalMarketDemandMoney);
		_marketDemandRows.put(detAllocDBs, totalMarketDemandRows);

		_logger.debug("Market demand: " + totalMarketDemandMoney + " " + totalMarketDemandRows);
		return Arrays.asList(totalMarketDemandMoney, totalMarketDemandRows);
	}
	
	/**
	 * The method computes the aggregate value function at the given quantity and with the given probabilistic allocation.
	 * @param totalQuantityDemanded the quantity of good 1 to be consumed
	 * @param allocation the probabilistic allocation of sellers/DBs
	 * @return the aggregate value
	 */
	public double computeAggregateValue(double price, double totalQuantityDemanded, int detAlloc)
	{
		_logger.debug("computeAggregateValue( "+price+", "+totalQuantityDemanded +", " + detAlloc+ ")");
		if(_aggregateValue.containsKey(detAlloc))
		{
			_logger.debug("cashed");
			return _aggregateValue.get(detAlloc);
		}
		double value = 0.;
		
		//To analyze the maximal inverse demand, first one need to compute the maximal prices
		//List<Double> marginalValues = new CopyOnWriteArrayList<Double>();
		
		_buyers.get(0).setNumberOfThreads(_numberOfThreads);
		for(int i = 0; i < _buyers.size(); ++i)
			_buyers.get(i).updateAllocProbabilityDistribution(detAlloc, _numberOfDBs);
		//_buyers.get(0).updateAllocProbabilityDistribution(allocation);
		
		/*try
		{
			List<Thread> threads = new LinkedList<Thread>();
			for(int i = 0; i < _numberOfThreads; ++i)
			{
				Thread thread = new Thread(new ExpectationsUpdateWorker("Thread", i) );
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
		}	*/	
/*		for(ParametrizedQuasiLinearAgent buyer: _buyers)
		{
			marginalValues.add(buyer.computeExpectedMarginalValue(allocation));
		}
		
		Collections.sort(marginalValues);
		Collections.reverse(marginalValues);
		
		for(int i = 1; i < marginalValues.size(); ++i)						// Remove duplicates
			if( Math.abs(marginalValues.get(i) - marginalValues.get(i-1)) < 1e-6)
			{
				marginalValues.remove(i);
				i -= 1;
			}*/
		
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
		
		_vals = new double[_numberOfThreads];
		
		try
		{
			List<Thread> threads = new LinkedList<Thread>();
			for(int i = 0; i < _numberOfThreads; ++i)
			{
				Thread thread = new Thread(new ValueWorker("Thread", i, price) );
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
		_logger.debug("Computed Aggregate value is  " + value);
		
		_aggregateValue.put(detAlloc, value);
		return value;
	}
	
	
	// TODO: this method is not used anymore. It's functionality is hidden under the hood of the computeExcessDemand()
	/**
	 * The method computes the value of the specified database that is proportional to the positive externality the DB imposes
	 * on buyers given current market prices and allocation of other DBs.
	 * @param dbId id of the database
	 * @param price current market price per row of a query answer
	 * @param allocation current probabilistic allocation of sellers
	 * @return the value of the specified DB.
	 * @throws Exception 
	 */
	public List< List<Double> > computeValuesOfDBs(double price) throws Exception
	{
		_logger.debug("computeValueOfDB(" + price +")");
		
		//double marketDemandForRows = computeMarketDemand(price, alloc).get(1);
// For plotting the demand curve		
//		List<Double> quantity = new ArrayList<Double>();
//		for(int p = 0; p < 100; p++)
//		{
//			System.out.println("p="+p);
//			quantity.add( computeMarketDemand(p*0.1, allocation, true).get(1) );
//		}
//		System.out.println(quantity.toString());
//		BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
//		String s = bufferRead.readLine();

		List< List<Double> > inducedValues = new ArrayList<List<Double> >();
		for(int k = 0; k < _numberOfDBs; ++k)
		{
			List<Double> inducedValuesK = new ArrayList<Double>();
			inducedValues.add(inducedValuesK);
		}


		int numberOfDeterministicAllocations = (int)Math.pow(2, _numberOfDBs);
		for(int j = 0; j < numberOfDeterministicAllocations; ++j)
		{
			// Here, j represent the binary encoding of a deterministic allocation of DBs
			System.out.println(j);
			//long startTime = System.nanoTime();
			double marketDemandForRows = computeMarketDemand(price, j).get(1);
			//long stopTime = System.nanoTime();
			//System.out.println((stopTime - startTime) / Math.pow(10, 9));

			// Compute externalities of DBs
			List<Double> externalitiesOfDBs = new LinkedList<Double>();
			for(int k = 0; k < _numberOfDBs; ++k)
			{
				int dbIdK = k + 1;
				
				double probAllocation = 0.;
				int bit = 1 << k;
				if( (j & bit) > 0)
				{
					probAllocation = 1;
					externalitiesOfDBs.add( computeExternalityOfDB(dbIdK, price, marketDemandForRows, j) * probAllocation );
				}
				else
				{
					externalitiesOfDBs.add( 0. );
				}
			}
			
			// Compute values of DBs
			for(int k = 0; k < _numberOfDBs; ++k)
			{
				int dbId = k + 1;
				_logger.debug("Compute induced values for DB_" + dbId + " for different det. allocations...");
				//System.out.println("Compute value for DB_" + dbId + "; detAlloc=" + j);
				
				double valueOfDB = 0.;
				if( externalitiesOfDBs.get(dbId-1) > 0)
					valueOfDB = externalitiesOfDBs.get(dbId-1) / externalitiesOfDBs.stream().reduce(0., (i, q) -> i+q) * computeAggregateValue(price, marketDemandForRows, j);
				
				inducedValues.get(k).add(valueOfDB);
				_logger.debug("V_" + dbId + " for det. allocation " + j + " is " + valueOfDB + "="+ externalitiesOfDBs.get(dbId-1) + "/...");
			}
			
		}
		long stopTime = System.nanoTime();
//		System.out.println("Time=" + (stopTime - startTime) / Math.pow(10, 9));

		return inducedValues;
	}
	
	/**
	 * The method computes the positive externality that the DB imposes
	 * on buyers given current market prices and allocation of other DBs.
	 * @param dbId id of the database
	 * @param price current market price per row of a query answer
	 * @param allocation current probabilistic allocation of sellers
	 * @param marketDemandForRows market demand for rows given current allocation
	 * @return the positive externality of the specified DB.
	 * @throws Exception 
	 */
	public double computeExternalityOfDB(int dbId, double price, double marketDemandForRows, int alloc) throws Exception
	{
		_logger.debug("computeExternalityOfDB(dbId="+dbId + ", p="+price + ", marketDemandForRows=" + marketDemandForRows +", alloc="+alloc+")");
		
		int detAllocDBs = alloc;
		int detAllocReducedDBs = detAllocDBs;
		int bit = 1 << (dbId-1);
		detAllocReducedDBs = detAllocReducedDBs & ( ~bit );
		
		// Compute market demand for rows if dbId is not allocated
		double marketDemandForRowsReduced = computeMarketDemand(price, detAllocReducedDBs).get(1);
		
		// Compute aggregate values in both cases
		double aggregateValue = computeAggregateValue(price, marketDemandForRows, detAllocDBs);
		double aggregateValueReduced = computeAggregateValue(price, marketDemandForRowsReduced, detAllocReducedDBs);
		
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
	
	/**
	 * 
	 * @author Dmitry Moor
	 */
	private class ValueWorker implements Runnable
	{
		private Thread _thread;										// A thread object
		private String _threadName;									// The thread's name
		private int _threadId;										// A thread id
		private double _price;
		private int _idxLow;
		private int _idxHigh;
		
		public ValueWorker(String name, int threadId, double price)
		{
			_threadName = name + threadId;
			_threadId = threadId;
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
				List<Double> optBundle = _buyers.get(i).solveConsumptionProblem(_price);
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
	
	/**
	 * 
	 * @author Dmitry Moor
	 */
	private class ExpectationsUpdateWorker implements Runnable
	{
		private Thread _thread;										// A thread object
		private String _threadName;									// The thread's name
		private int _threadId;										// A thread id
		private int _idxLow;										// Lower index of the buyer for the thread
		private int _idxHigh;										// Upper index of the buyer for the thread
		
		public ExpectationsUpdateWorker(String name, int threadId)
		{
			_threadName = name + threadId;
			_threadId = threadId;			
			_idxLow = _threadId * _buyers.size() / _numberOfThreads;
			_idxHigh = (_threadId + 1) * _buyers.size() / _numberOfThreads - 1;
		}
		
		@Override
		public void run() 
		{
			for( int i = _idxLow; i <= _idxHigh; ++i )
				_buyers.get(i).setAllocProbabilityDistribution( _buyers.get(0).getAllocProbabilityDistribution());				
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
	
	/**
	 * 
	 * @author Dmitry Moor
	 */
	private class DemandWorker implements Runnable
	{
		private Thread _thread;										// A thread object
		private String _threadName;									// The thread's name
		private int _threadId;										// A thread id
		private double _price;										// Price per row
		private double[] _marketDemandMoney;						// Shared array
		private double[] _marketDemandRows;							// Shared array
		private int _idxLow;										// Lower index of the buyer for the thread
		private int _idxHigh;										// Upper index of the buyer for the thread
		
		public DemandWorker(String name, int threadId, double price, double[] marketDemandMoney, double[] marketDemandRows)
		{
			_threadName = name + threadId;
			_threadId = threadId;
			_marketDemandMoney = marketDemandMoney;
			_marketDemandRows = marketDemandRows;
			_price = price;
			
			_idxLow = _threadId * _buyers.size() / _numberOfThreads;
			_idxHigh = (_threadId + 1) * _buyers.size() / _numberOfThreads - 1;
		}
		
		@Override
		public void run() 
		{   
			double moneyDemand = 0.;
			double rowsDemand = 0.;
			for( int i = _idxLow; i <= _idxHigh; ++i )
			{
				List<Double> optBundle = _buyers.get(i).solveConsumptionProblem(_price);				
				moneyDemand += optBundle.get(0);
				rowsDemand += optBundle.get(1);
			}
			_marketDemandMoney[_threadId] = moneyDemand;
			_marketDemandRows[_threadId] = rowsDemand;
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
	
	public IloCplex getSolver()
	{
		return _cplexSolver;
	}
	
	private List<ParametrizedQuasiLinearAgent> _buyers;				// Buyers
	private List<SellerType> _sellers;								// Sellers
	private int _numberOfDBs;										// Number of databases
	private int _MAX_ITER = 10000;									// Max number of gradient descent iterations
	private double _STEP = 0.01;									// Step of the gradient descent
	private double _TOL = 1e-7;										// Tolerance of the gradient descent
	private int _numberOfThreads;									// Number of threads
	private double[] _vals;
	private Map<Integer, Double> _marketDemandMoney;				// CASH
	private Map<Integer, Double> _marketDemandRows;					// CASH
	private Map<Integer, Double> _aggregateValue;					// CASH
	
	private IloCplex _cplexSolver;
}
