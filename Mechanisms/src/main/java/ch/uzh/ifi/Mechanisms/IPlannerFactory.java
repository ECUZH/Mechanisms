package ch.uzh.ifi.Mechanisms;

/*
 * An interface for the factory producing different types of planners.
 */
public interface IPlannerFactory 
{
	/*
	 * The factory method produces a planner.
	 * @return a planner object
	 */
	public Planner producePlanner();
	
	/*
	 * The factory method produces a planner with a given seed for random
	 * numbers generation.
	 * @return a planner object
	 */
	public Planner producePlanner(long seed);
}
