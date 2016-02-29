package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ilog.cplex.IloCplex;

import java.util.List;

/*
 * Interface for different mechanisms factories. Each factory produces a mechanism with a fixed number of agents and specified 
 * types of these agents.
 */
public interface IMechanismFactory 
{
	/*
	 * 
	 */
	void setSolver(IloCplex solver);
	
	/*
	 * The interface triggers production of an auction mechanism.
	 * @param types - a list of types of bidders
	 * @return an auction
	 */
	Auction produceMechanism(List<Type> types);

	/*
	 * The interface triggers production of an auction mechanism.
	 * @param types - a list of types of bidders
	 * @param seed - seed used by the mechanism for random number generation
	 * @return an auction
	 */
	Auction produceMechanism(List<Type> types, long seed);
	
	/*
	 * Get the name of a mechanism which should be produced
	 * @return a name of the mechanism which should be produced by the factory
	 */
	String getMehcanismName();
	
	/*
	 * The method is used to check whether the mechanism is forward or not
	 * @return true if a seller (or sellers) is an auctioneer and buyers are bidders and false otherwise
	 */
	boolean isReverse();
}
