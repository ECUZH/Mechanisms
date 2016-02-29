package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
 * The class implements a general planner for a one sided auction. This planner generates plans with
 * specified marginal costs and probability distribution of the number of records (triples) each
 * seller provides.
 */
public class GeneralProbabilisticPlanner extends Planner 
{

	/*
	 * Constructor
	 * @param numberOfBuyers
	 * @param numberOfSellers
	 * @param types - types of all agents (first buyers, then sellers)
	 */
	public GeneralProbabilisticPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types, long seed)
	{
		_errorScenarioSeed = seed;
		init(numberOfBuyers, numberOfSellers, types);
		reset( types );
		_initTuples = 0;
	}
	
	public void setPlanningStrategy(IPlanningStrategy planningStrategy)
	{
		_planningStrategy = planningStrategy;
	}
	/*
	 * Initialization procedure
	 * @param numberOfBuyers
	 * @param numberOfSellers
	 * @param types - a list of types of all agents (first buyers, then sellers)
	 */
	private void init(int numberOfBuyers, int numberOfSellers, List<Type> types)
	{
		if( types.size() != (numberOfBuyers + numberOfSellers) ) 	throw new RuntimeException("Wrong number of agents specified");
		
		isInjectable = false;
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#reset(java.util.List)
	 */
	@Override
	public void reset(List<Type> types)
	{	
		if(_planningStrategy != null)
			if(_planningStrategy.getNumberOfPlans() < _numberOfPlans)
				_numberOfPlans = _planningStrategy.getNumberOfPlans();
		
		_sellersInPlans = new LinkedList<List<Integer> >();
		_minNumberOfRecordsFromSellers = new LinkedList<List<Integer> >();
		_maxNumberOfRecordsFromSellers = new LinkedList<List<Integer> >();
		_minNumberOfRecordsForBuyer    = new LinkedList<Integer>();
		_maxNumberOfRecordsForBuyer    = new LinkedList<Integer>();
		
		if( _maxSellersPerPlan > _numberOfSellers)
			_maxSellersPerPlan = _numberOfSellers;
		
		_types = types;
	
		// Generate structures of plans for buyers and sellers without assigning values/costs
		int shift = 0;
		for(int k = 0; k < _numberOfBuyers; ++k)
		{
			if( types.get(k-shift).getAgentId() != k+1 )
			{
				shift += 1;
				continue;
			}
			
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				List<Integer> serversUsed = _planningStrategy.getServersUsed(i);			//Sellers for the plan
				List<Integer> minNumberOfRecordsFromSellersEst = _planningStrategy.getMinEstimatedNumberOfRecordsFromSellers(i);
				List<Integer> maxNumberOfRecordsFromSellersEst = _planningStrategy.getMaxEstimatedNumberOfRecordsFromSellers(i);
				int minNumberOfRecordsForBuyer = _planningStrategy.getMinEstimatedNumberOfRecordsForBuyer(i);
				int maxNumberOfRecordsForBuyer = _planningStrategy.getMaxEstimatedNumberOfRecordsForBuyer(i);
				
				Collections.sort(serversUsed);
				_minNumberOfRecordsFromSellers.add(minNumberOfRecordsFromSellersEst);
				_maxNumberOfRecordsFromSellers.add(maxNumberOfRecordsFromSellersEst);
				_minNumberOfRecordsForBuyer.add(minNumberOfRecordsForBuyer);
				_maxNumberOfRecordsForBuyer.add(maxNumberOfRecordsForBuyer);
				_sellersInPlans.add(serversUsed);
				System.out.println("Plan " + i + " serversUsed: " + serversUsed.toString() + " minRec: " + minNumberOfRecordsFromSellersEst.toString() + " maxRec: " + maxNumberOfRecordsFromSellersEst.toString());
			}
			_buyersInPlans.add(k - shift + 1);
		}
		//System.out.println("Sellers in plans: " + _sellersInPlans.toString());
		//System.out.println("Max in plans: " + _maxNumberOfRecords.toString());
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#generatePlans()
	 */
	@Override
	public List<Type> generatePlans() 
	{
		List<Type> plans = new LinkedList<>();
		
		int shift = 0;
		for(int l = 0; l < _numberOfBuyers; ++l)
		{
			if( _types.get(l-shift).getAgentId() != l+1 )
			{
				shift += 1;
				continue;
			}
			CombinatorialType b = new CombinatorialType(); 										//Buyer's type
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				List<Integer> serversUsed = _sellersInPlans.get(i + (l-shift)*_numberOfPlans);	//Sellers for the plan
				List<Double> marginalCosts = new LinkedList<Double>();							//Costs of sellers
				boolean isAllSellersPresent = true;
				
				for(int j = 0; j < serversUsed.size(); ++j)
				{					
					boolean isSellerFound = false;
					for(int k = 0; k < _types.size(); ++k)
						if( _types.get(k).getAgentId() ==  serversUsed.get(j))
						{
							marginalCosts.add( _types.get(k).getAtom(0).getValue() );
							isSellerFound = true;
							break;
						}
					
					if( ! isSellerFound )
					{
						isAllSellersPresent = false;
						break;
					}
				}

				if( isAllSellersPresent )
				{			
					for(int j = 0; j < _types.size(); ++j)
						if( _types.get(j).getAgentId() == l+1 )
						{
							AtomicBid plan = new SemanticWebType(1, serversUsed, _types.get(0).getAtom(0).getValue(), marginalCosts, 
									                             _minNumberOfRecordsFromSellers.get(i), _maxNumberOfRecordsFromSellers.get(i),
									                             _minNumberOfRecordsForBuyer.get(i), _maxNumberOfRecordsForBuyer.get(i));
							b.addAtomicBid(plan);
						}
				}
			}
		
			plans.add(b);
		}
		
		_plans = plans;
		return plans;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#injectError(int)
	 */
	@Override
	public boolean injectError(int planIdx) 
	{
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#withdrawError()
	 */
	@Override
	public void withdrawError() 
	{
		throw new RuntimeException("This planner does not inject errors. Nothing to withdraw.");
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#getInjectedSeller()
	 */
	@Override
	public int getInjectedSeller() 
	{
		throw new RuntimeException("This planner does not inject errors.");
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#dbOperation(int, int)
	 */
	@Override
	public int dbOperation(int a, int b) 
	{
		return 0;
	}

	private List<List<Integer> > _minNumberOfRecordsFromSellers;
	private List<List<Integer> > _maxNumberOfRecordsFromSellers;
	private List<Integer> _minNumberOfRecordsForBuyer;
	private List<Integer> _maxNumberOfRecordsForBuyer;
	
	private IPlanningStrategy _planningStrategy;
}
