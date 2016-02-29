package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/*
 * A class for a simple planner which generates plans for 1 buyer and 4 sellers with a fixed structure.
 */
public class SimpleErrorPlanner extends Planner
{
	/*
	 * A constructor.
	 */
	public SimpleErrorPlanner(int numberOfBuyers, int numberOfSellers, List<Type> types)
	{
		if( (numberOfBuyers != 1) || (numberOfSellers != 3) )  throw new RuntimeException("Wrong number of agents specified.");
		
		isInjectable = true;
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_numberOfPlans = 2;
		_minSellersPerPlan = 2;
		_maxSellersPerPlan = 2;
		reset( types );
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#reset(int, int, java.util.List)
	 */
	@Override
	public void reset( List<Type> types ) 
	{
		_types = types;
		_sellersInPlans = new LinkedList<List<Integer> >();
		_nTuples = new LinkedList<List<Integer> >();
		
		if( _maxSellersPerPlan > _numberOfSellers)
			_maxSellersPerPlan = _numberOfSellers;
		
		Random generator = new Random();
		generator.setSeed(_errorScenarioSeed + 141990);
		
		List<Integer> nTuples1 = new LinkedList<Integer>();
		List<Integer> nTuples2 = new LinkedList<Integer>();
		List<Integer> serversUsed1 = new LinkedList<Integer>();						//Sellers IDs for the 1st plan
		List<Integer> serversUsed2 = new LinkedList<Integer>();						//Sellers IDs for the 2nd plan
		
		serversUsed1.add(2);
		serversUsed1.add(3);
		serversUsed2.add(3);
		serversUsed2.add(4);
		
		nTuples1.add( 1 + generator.nextInt(10) );
		nTuples1.add( 1 + generator.nextInt(10) );
		nTuples2.add( 1 + generator.nextInt(10) );
		nTuples2.add( 1 + generator.nextInt(10) );
				
		_nTuples.add(nTuples1);
		_nTuples.add(nTuples2);
		
		_sellersInPlans.add(serversUsed1);
		_sellersInPlans.add(serversUsed2);
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#generatePlans()
	 */
	@Override
	public List<Type> generatePlans()
	{
		List<Type> plans = new LinkedList<>(); 
		
		boolean isFirstPlan  = false;
		boolean isSecondPlan = false;
		
		//Check if the 1st plan can be satisfied, i.e., a buyer and two sellers a re present
		for(Type t: _types)
			if( t.getAgentId() == 1 )
				for(Type s : _types)
					if( s.getAgentId() == 2)
						for(Type q : _types)
							if( q.getAgentId() == 3 )
							{
								isFirstPlan = true;
								break;
							}
		
		//Check if the 2nd plan can be satisfied, i.e., a buyer and two sellers a re present
		for(Type t: _types)
			if( t.getAgentId() == 1 )
				for(Type s : _types)
					if( s.getAgentId() == 3)
						for(Type q : _types)
							if( q.getAgentId() == 4 )
							{
								isSecondPlan = true;
								break;
							}
		
		CombinatorialType b1 = new CombinatorialType(); 									//Buyer's type
		
		if( isFirstPlan )
		{
			List<Integer> serversUsed = _sellersInPlans.get(0);
			List<Double> costsAssociated = new LinkedList<Double>();						//Costs of sellers
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 2 )
					costsAssociated.add( _nTuples.get(0).get(0) * _types.get(i).getAtom(0).getValue()  );
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 3 )
					costsAssociated.add( _nTuples.get(0).get(1) * _types.get(i).getAtom(0).getValue()  );
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 1 )
				{
					double tuplesReceived = _nTuples.get(0).get(0)+_nTuples.get(0).get(1);
					AtomicBid plan1 = new SemanticWebType(1, serversUsed, tuplesReceived * _types.get(0).getAtom(0).getValue(), costsAssociated);
					b1.addAtomicBid(plan1);
				}
		}
		
		if( isSecondPlan )
		{
			List<Integer> serversUsed = _sellersInPlans.get(1);
			List<Double> costsAssociated = new LinkedList<Double>();
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 3 )
					costsAssociated.add(_nTuples.get(1).get(0) * _types.get(i).getAtom(0).getValue() );
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 4 )
					costsAssociated.add(_nTuples.get(1).get(1) * _types.get(i).getAtom(0).getValue() );
			for(int i = 0; i < _types.size(); ++i)
				if( _types.get(i).getAgentId() == 1 )
				{
					double tuplesReceived = _nTuples.get(1).get(0)+_nTuples.get(1).get(1);
					AtomicBid plan2 = new SemanticWebType(1, serversUsed, tuplesReceived * _types.get(i).getAtom(0).getValue() , costsAssociated);
					b1.addAtomicBid(plan2);
				}
		}
		
		if( isFirstPlan || isSecondPlan )
			plans.add(b1);
		
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
		if( ! isInjectable )
			return false;
		
		Random generator = new Random();
		generator.setSeed(_errorScenarioSeed);
		
		if( planIdx == 0)
		{
			double coin = generator.nextDouble();
			if(coin < 0.5)																			//Inject an error in seller A
			{
				generator.setSeed( _errorScenarioSeed + 10);
				_nTuples.get(0).set(0, Math.max(0, _nTuples.get(0).get(0) - (int)Math.round( Math.abs( ((double)_nTuples.get(0).get(0)/3.) * generator.nextGaussian()))) );
				_sellerToInject = 0;
			}
			else																					//Inject an error in seller B
			{
				generator.setSeed( _errorScenarioSeed + 1400);
				_nTuples.get(0).set(1, Math.max(0, _nTuples.get(0).get(1) - (int)Math.round( Math.abs( ((double)_nTuples.get(0).get(1)/3.) * generator.nextGaussian()))) );
				_sellerToInject = 1;
			}
		}
		else if(planIdx == 1)
		{
			double coin = generator.nextDouble();
			if(coin < 0.5)																			//Inject an error in seller B
			{
				generator.setSeed( _errorScenarioSeed + 100020);
				_nTuples.get(1).set(0, Math.max(0, _nTuples.get(1).get(0) - (int)Math.abs( ((double)_nTuples.get(1).get(0)/3.) * generator.nextGaussian())) );
				_sellerToInject = 0;
			}
			else																					//Inject an error in seller C
			{
				generator.setSeed( _errorScenarioSeed + 10003000);
				_nTuples.get(1).set(1, Math.max(0, _nTuples.get(1).get(1) - (int)Math.abs( ((double)_nTuples.get(1).get(1)/3.) * generator.nextGaussian()))  );
				_sellerToInject = 1;
			}
		}
		else
		{
			throw new RuntimeException("wrong plan idx");
		}
		_plans = generatePlans();
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#withdrawError()
	 */
	public void withdrawError()
	{
		if( _savedStates.size() != 0 )
			restoreFromMemento( getStateMemento(0) );
		
		_plans = generatePlans();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#getPlans()
	 */
	public List<Type> getPlans()
	{
		return _plans;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#setNumberOfPlans(int)
	 */
	public void setNumberOfPlans(int numberOfPlans)
	{
		throw new RuntimeException("The number of plans for SimpleErrorPlanner is fixed (2)");
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#setMinSellersPerPlan(int)
	 */
	public void setMinSellersPerPlan(int numberOfSellers)
	{
		throw new RuntimeException("The number of sellers per plan for SimpleErrorPlanner is fixed (2)");
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.Planner#setMaxSellersPerPlan(int)
	 */
	public void setMaxSellersPerPlan(int numberOfSellers)
	{
		throw new RuntimeException("The number of sellers per plan for SimpleErrorPlanner is fixed (2)");
	}
	
	public int getInjectedSeller()
	{
		return _sellerToInject;
	}
	
	private int _sellerToInject;							//The id of a seller in which an error was injected

	@Override
	public int dbOperation(int a, int b) {
		// TODO Auto-generated method stub
		return 0;
	}
}
