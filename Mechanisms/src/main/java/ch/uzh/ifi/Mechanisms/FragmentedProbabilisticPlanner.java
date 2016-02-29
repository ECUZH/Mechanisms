package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.ComplexSemanticWebType;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.QueryFragment;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
 * The class implements a planner which generates plans consisting of multiple query fragments.
 * This planner generates plans with specified marginal costs and probability distribution of 
 * the number of records (triples) each seller provides.
 */
public class FragmentedProbabilisticPlanner extends Planner
{
	/*
	 * Constructor
	 * @param numberOfBuyers
	 * @param numberOfSellers
	 * @param types - types of all agents (first buyers, then sellers)
	 */
	public FragmentedProbabilisticPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types, long seed)
	{
		_errorScenarioSeed = seed;
		init(numberOfBuyers, numberOfSellers, types);
		reset( types );
		_initTuples = 0;
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
		_minSellersPerFragment = 2;
		_maxSellersPerFragment = 2;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#reset(java.util.List)
	 */
	@Override
	public void reset(List<Type> types)
	{
		_sellersInPlans = new LinkedList<List<List<Integer> > >();
		_minNumberOfRecords = new LinkedList<List<List<Integer> > >();
		_maxNumberOfRecords = new LinkedList<List<List<Integer> > >();
		
		if( _maxSellersPerFragment > _numberOfSellers)
			_maxSellersPerFragment = _numberOfSellers;
		
		_types = types;
	
		// Generate structures of plans for buyers and sellers without assigning values/costs
		int shift = 0;
		for(int k = 0; k < _numberOfBuyers; ++k)
		{
			if( types.get(k-shift).getAgentId() != k+1 )			//Used if some agents were removed and the ids are not consistent anymore
			{
				shift += 1;
				continue;
			}
			
			Random generator = new Random();
			generator.setSeed(_errorScenarioSeed + 141990 + types.get(k-shift).getAgentId()-1 );
			
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				List<List<Integer> > minNumberOfRecordsInPlan = new LinkedList<List<Integer> >();
				List<List<Integer> > maxNumberOfRecordsInPlan = new LinkedList<List<Integer> >();
				List<List<Integer> > sellersInFragments = new LinkedList<List<Integer> >();
				
				for(int q = 0; q < _numberOfFragments; ++q)
				{
					int numberOfSellersPerFragment = _minSellersPerFragment + generator.nextInt(_maxSellersPerFragment - _minSellersPerFragment + 1);
					List<Integer> serversUsedForFragment = new LinkedList<Integer>();				//Sellers for the fragment
					List<Integer> minNumberOfRecordsPerFragmentEst = new LinkedList<Integer>();
					List<Integer> maxNumberOfRecordsPerFragmentEst = new LinkedList<Integer>();

					for(int j = 0; j < numberOfSellersPerFragment; ++j)
					{
						int sellerId = 0;
						while(true)
						{
							sellerId = _numberOfBuyers + 1 + generator.nextInt(_numberOfSellers) ;
							if( ! serversUsedForFragment.contains(sellerId) )						//No duplicated sellers per fragment
								break;
						}
						
						serversUsedForFragment.add(sellerId);
						maxNumberOfRecordsPerFragmentEst.add( 1 + generator.nextInt(10) );
						minNumberOfRecordsPerFragmentEst.add( 0 );
					}
					
					Collections.sort(serversUsedForFragment);
					minNumberOfRecordsInPlan.add(minNumberOfRecordsPerFragmentEst);
					maxNumberOfRecordsInPlan.add(maxNumberOfRecordsPerFragmentEst);
					sellersInFragments.add(serversUsedForFragment);
				}
				
				_sellersInPlans.add( sellersInFragments);
				_minNumberOfRecords.add(minNumberOfRecordsInPlan);
				_maxNumberOfRecords.add(maxNumberOfRecordsInPlan);
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
			CombinatorialType b = new CombinatorialType(); 													//Buyer's type
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				List<QueryFragment> queryFragments = new LinkedList<QueryFragment>();
				List<Integer> serversUsedByPlan = new LinkedList<Integer>();
				for(int k = 0; k < _numberOfFragments; ++k)
				{
					List<Integer> serversUsed = _sellersInPlans.get(i + (l-shift)*_numberOfPlans).get(k);	//Sellers for the query fragment
					for(int j = 0; j < serversUsed.size(); ++j)
						if( ! serversUsedByPlan.contains(serversUsed.get(j)))
							serversUsedByPlan.add(serversUsed.get(j));
					
					List<Double> marginalCosts = new LinkedList<Double>();									//Costs of sellers
					
					for(int j = 0; j < serversUsed.size(); ++j)
					{					
						for(int q = 0; q < _types.size(); ++q)
							if( _types.get(q).getAgentId() ==  serversUsed.get(j))
							{
								marginalCosts.add( _types.get(q).getAtom(0).getValue() );
								break;
							}
					}

					QueryFragment queryFragment = new QueryFragment(serversUsed, marginalCosts, _minNumberOfRecords.get(i).get(k), _maxNumberOfRecords.get(i).get(k));
					queryFragments.add(queryFragment);
				}
				
				ComplexSemanticWebType plan = new ComplexSemanticWebType(1, serversUsedByPlan, _types.get(0).getAtom(0).getValue(), queryFragments);
				b.addAtomicBid(plan);
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
		//Nothing to withdraw
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#getInjectedSeller()
	 */
	@Override
	public int getInjectedSeller() 
	{
		// TODO Auto-generated method stub
		return 0;
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
	
	public void setMinSellersPerFragment(int minSellersPerFragment)
	{
		_minSellersPerFragment = minSellersPerFragment;
	}
	
	public void setMaxSellersPerFragment(int maxSellersPerFragment)
	{
		_maxSellersPerFragment = maxSellersPerFragment;
	}

	private List<List<List<Integer> > > _minNumberOfRecords;		//For all plans for all fragments for all agents in plans
	private List<List<List<Integer> > > _maxNumberOfRecords;
	private List<List<List<Integer> > > _sellersInPlans;
	
	private int _minSellersPerFragment;
	private int _maxSellersPerFragment;
	private int _numberOfFragments = 2;
}
