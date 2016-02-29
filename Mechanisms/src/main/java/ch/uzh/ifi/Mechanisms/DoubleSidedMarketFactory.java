package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;

import ilog.cplex.IloCplex;

import java.util.List;

/*
 * The factory produces different Double Auction mechanisms with a fixed number of agents.
 */
public class DoubleSidedMarketFactory implements IMechanismFactory
{

	/*
	 * A simple constructor.
	 * @param numberOfBuyers - the number of buyers in the double auction
	 * @param numberOfSellers - the number of sellers in the double auction
	 * @param planner - a planner to be used to match buyers to different sellers
	 */
	public DoubleSidedMarketFactory(int numberOfBuyers, int numberOfSellers, IPlannerFactory plannerFactory, String paymentRule, String paymentCorrectionRule)
	{
		_numberOfBuyers  = numberOfBuyers;
		_numberOfSellers = numberOfSellers;
		_plannerFactory = plannerFactory;
		_paymentRule = paymentRule;
		_paymentCorrectionRule = paymentCorrectionRule;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism()
	 */
	@Override
	public Auction produceMechanism(List<Type> types)
	{
		DoubleSidedMarket dsm = new DoubleSidedMarket(_numberOfBuyers, _numberOfSellers, types, _plannerFactory.producePlanner() );
		dsm.setPaymentRule(_paymentRule);
		dsm.setPaymentCorrectionRule(_paymentCorrectionRule);
		return dsm;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#produceMechanism()
	 */
	@Override
	public Auction produceMechanism(List<Type> types, long seed)
	{
		DoubleSidedMarket dsm = new DoubleSidedMarket(_numberOfBuyers, _numberOfSellers, types, _plannerFactory.producePlanner(seed) );
		dsm.setPaymentRule(_paymentRule);
		dsm.setPaymentCorrectionRule(_paymentCorrectionRule);
		return dsm;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#getMehcanismName()
	 */
	@Override
	public String getMehcanismName() 
	{
		return DoubleSidedMarket.class.getSimpleName();
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#isReverse()
	 */
	@Override
	public boolean isReverse() 
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IMechanismFactory#setSolver(ilog.cplex.IloCplex)
	 */
	@Override
	public void setSolver(IloCplex solver) 
	{
		throw new RuntimeException("This implementation does not currently support CPLEX.");
	}
	
	private int _numberOfBuyers;					//The number of buyers in the double auction
	private int _numberOfSellers;					//The number of sellers in the double auction produced by the factory
	private IPlannerFactory _plannerFactory;		//A planner factory
	private String _paymentRule;					//A payment rule used in auctions produced by this factory
	private String _paymentCorrectionRule;			//A payment correction rule used in auctions produced by this factory
}
