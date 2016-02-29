package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
 * The class implements a general planner for a Double Auction. A planner uses its own unique seed for generating
 * of random numbers. This makes different planners produce different matchings for same agents/agents' types etc.
 */
public class GeneralErrorPlanner extends Planner
{	
	/*
	 * Constructor
	 */
	public GeneralErrorPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types)
	{
		init(numberOfBuyers, numberOfSellers, types);
		reset( types );
		_initTuples = 0;
	}
	
	/*
	 * Constructor which specifies the seed to be used by the planner to
	 * generate random plans.
	 */
	public GeneralErrorPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types, long seed)
	{
		init(numberOfBuyers, numberOfSellers, types);
		_errorScenarioSeed = seed;
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
		
		_sellerToInject = -1;
		isInjectable = true;
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_errorVarianceFactor = 3.;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "GeneralErrorPlanner";
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#reset(int, int, java.util.List)
	 */
	@Override
	public void reset(List<Type> types) 
	{			
		_sellersInPlans = new LinkedList<List<Integer> >();
		_nTuples = new LinkedList<List<Integer> >();
		
		if( _maxSellersPerPlan > _numberOfSellers)
			_maxSellersPerPlan = _numberOfSellers;
		
		_types = types;
	
		// Generate structures of plans for buyers and sellers without values/costs
		int shift = 0;
		for(int k = 0; k < _numberOfBuyers; ++k)
		{
			if( types.get(k-shift).getAgentId() != k+1 )
			{
				shift += 1;
				continue;
			}
			
			Random generator = new Random();
			generator.setSeed(_errorScenarioSeed + 141990 + types.get(k-shift).getAgentId()-1 );
			
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				int numberOfSellersPerPlan = _minSellersPerPlan + generator.nextInt(_maxSellersPerPlan - _minSellersPerPlan + 1);
				List<Integer> serversUsed = new LinkedList<Integer>();						//Sellers for the plan
				List<Integer> numberOfTuplesEst = new LinkedList<Integer>();
				
				for(int j = 0; j < numberOfSellersPerPlan; ++j)
				{
					int sellerId = 0;
					while(true)
					{
						sellerId =  _numberOfBuyers + 1 + generator.nextInt(_numberOfSellers) ;
						if( ! serversUsed.contains(sellerId) )								//No duplicated sellers per plan
							break;
					}
					
					serversUsed.add(sellerId);
					numberOfTuplesEst.add( 1 + generator.nextInt(10) );
				}
				Collections.sort(serversUsed);
				_nTuples.add(numberOfTuplesEst);
				_sellersInPlans.add(serversUsed);
			}
			_buyersInPlans.add(k - shift + 1);
		}
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
			CombinatorialType b = new CombinatorialType(); 									//Buyer's type
			for(int i = 0; i < _numberOfPlans; ++i)
			{
				List<Integer> serversUsed = _sellersInPlans.get(i + (l-shift)*_numberOfPlans);//Sellers for the plan
				List<Double> costsAssociated = new LinkedList<Double>();					//Costs of sellers
				List<Integer> numberOfTuples = _nTuples.get(i + (l-shift)*_numberOfPlans);
				int totalTuplesPerPlan = _initTuples;
				boolean isAllSellersPresent = true;
				
				for(int j = 0; j < serversUsed.size(); ++j)
				{
					totalTuplesPerPlan = dbOperation(totalTuplesPerPlan, numberOfTuples.get(j));
					
					boolean isSellerFound = false;
					for(int k = 0; k < _types.size(); ++k)
						if( _types.get(k).getAgentId() ==  serversUsed.get(j))
						{
							costsAssociated.add( numberOfTuples.get(j)*_types.get(k).getAtom(0).getValue() );
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
							AtomicBid plan = new SemanticWebType(l+1, serversUsed, totalTuplesPerPlan * _types.get(l-shift).getAtom(0).getValue(), costsAssociated);
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
	 * @see Mechanisms.Planner#injectError()
	 */
	@Override
	public boolean injectError(int planIdx)
	{	
		if(isInjectable == false)
			return false;
		
		if( planIdx > _numberOfPlans * _numberOfBuyers) 
			throw new RuntimeException("No plan with index " + planIdx + " exists.");
		
		Random generator = new Random();
		generator.setSeed(_errorScenarioSeed);
		
		int sellerToInject = generator.nextInt( _nTuples.get(planIdx).size() );
		int itsEstimate  = _nTuples.get(planIdx).get(sellerToInject);
		generator.setSeed( _errorScenarioSeed + 10 * sellerToInject);
		
		_nTuples.get(planIdx).set(sellerToInject, Math.max(0, itsEstimate - (int)Math.round( Math.abs( ((double)itsEstimate /_errorVarianceFactor) * generator.nextGaussian()))));
		
		//System.out.println("Inject an error into plan " + planIdx + ". sellerToInject="+sellerToInject + ", itsEstimate="+itsEstimate+
		//		   ", newEstimate=" + _nTuples.get(planIdx).get(sellerToInject));
		
		_sellerToInject = sellerToInject;
		_plans = generatePlans();
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#withdrawError()
	 */
	@Override
	public void withdrawError()
	{
		if( _savedStates.size() != 0 )
			restoreFromMemento( getStateMemento(0) );
		
		_plans = generatePlans();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#dbOperation(int, int)
	 */
	@Override
	public int dbOperation(int a, int b)
	{
		return a + b;
	}
	
	public int getInjectedSeller()
	{
		return _sellerToInject;
	}
	
	public void setErrorVarianceFactor(double errorVarianceFactor)
	{
		_errorVarianceFactor = errorVarianceFactor;
	}

	private int _sellerToInject;							//The id of a seller in which an error was injected
	private double _errorVarianceFactor;					//The factor influences on the variance of error injection distribution
}
