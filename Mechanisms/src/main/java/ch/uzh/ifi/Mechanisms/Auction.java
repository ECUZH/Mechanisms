package ch.uzh.ifi.Mechanisms;

import java.util.List;
import ch.uzh.ifi.MechanismDesignPrimitives.Allocation;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;;

/**
 * Auction interface
 * @author Dmitry Moor
 */
public interface Auction 
{	
	/**
	 * The method triggers WDP and payments computation.
	 */
	public void solveIt() throws Exception;
	
	/**
	 * The method sets up reserve prices for individual goods and adds reserve prices on bundles of goods. These prices are computed
	 * as additive prices of individual goods within the bundle. The reserve prices are computed not for all possible bundles but only
	 * for those which are present in bids submitted by all agents.
	 * @param reservePrices - a list of reserve prices for different items (all units within an item have same price)
	 */
	public void setupReservePrices( List<Double> reservePrices );
	
	/**
	 * The method returns payments of allocated agents. The order of payments corresponds to the order
	 * of winner Ids returned by getWinnerIds().
	 * @return an array containing payments of allocated agents.
	 * @throws Exception
	 */
	public double[] getPayments() throws Exception;
	
	/**
	 * The method returns the solution of the WDP.
	 * @return the allocation of items among bidders
	 */
	public Allocation getAllocation();
	
	/**
	 * The method resets types of agents for multiple runs of the same auction.
	 * (E.g. useful when only distribution of bidder's values are known)
	 * @param agentsTypes - a list of new types of agents
	 */
	public void resetTypes( List<Type> agentsTypes );
	
	/**
	 * The method resets the planner, i.e., the entity in combinatorial excahges (and double auctions)
	 * which matches bidders to different auctioneers.
	 * @param planner the planner of the combinatorial exchange
	 */
	public void resetPlanner(Planner planner);
	
	/**
	 * The method returns the payment rule used by the auction.
	 * @return the name of the payment rule
	 */
	public String getPaymentRule();
	
	/**
	 * @return true if the m-m is BB and false otherwise.
	 */
	default public boolean isBudgetBalanced() {return true;};
	
	/**
	 * The method indicates if the auction is forward or reverse.
	 * @return true if the auction is reverse or false otherwise
	 */
	default public boolean isReverse() {return false;};
	
	/**
	 * The method indicates whether the auction satisfies ex-post IR property.
	 * @return true if the auction is ex-post IR and false otherwise
	 */
	default public boolean isExPostIR() {return true;};
}
