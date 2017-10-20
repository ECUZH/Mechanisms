package ch.uzh.ifi.Mechanisms;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;

/**
 * The class implements a surplus optimal reverse auction with a single item.
 * @author Dmitry Moor
 *
 */
public class SurplusOptimalReverseAuction implements Auction 
{

	private static final Logger _logger = LogManager.getLogger(SurplusOptimalReverseAuction.class);
	
	public SurplusOptimalReverseAuction()
	{
		
	}
	
	
	@Override
	public void solveIt() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setupReservePrices(List<Double> reservePrices) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double[] getPayments() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Allocation getAllocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetTypes(List<Type> agentsTypes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetPlanner(Planner planner) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPaymentRule() {
		// TODO Auto-generated method stub
		return null;
	}

}
