package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import ch.uzh.ifi.GraphAlgorithms.Graph;
import ch.uzh.ifi.GraphAlgorithms.Vertex;
import ch.uzh.ifi.GraphAlgorithms.VertexCell;
import ch.uzh.ifi.GraphAlgorithms.VertexMDP;

/**
 * The class implement the Markov Decision Process for setting the posted price repeatedly over the time horizon T.
 * @author Dmitry Moor
 *
 */
public class MDP 
{

	/**
	 * Constructor
	 * @param numberOfActions number of different price levels available at each state (controls)
	 * @param T time horizon
	 * @param maxPrice the maximum possible price level (upper bound for the controls region)
	 * @param Pa probability of arrival at time t
	 * @param V constant coefficient of the value function, VALUE = V * (X)^alpha
	 * @param alpha exponent coefficient of the value function, VALUE = V * (X)^alpha
	 */
	public MDP(int numberOfActions, int T, double maxPrice, double Pa, double V, double alpha)
	{
		_numberOfActions = numberOfActions;
		_T = T;
		_maxPrice = maxPrice;
		_Pa = Pa;
		_V = V;
		_alpha = alpha;
		
		// 1. Create the MDP graph
		List<Vertex> vertices = new LinkedList<Vertex>();
		List<Integer> numberOfStatesByT = new ArrayList<Integer>();
		numberOfStatesByT.add(1);
		int vertexId = 1;
		
		// 1.1. Create vertices
		for(int t = 0; t <= _T; ++t)
		{
			for(int j = 0; j < numberOfStatesByT.get(t); ++j)
			{
				Vertex v = new VertexMDP( vertexId, t);
				vertices.add(v);
				vertexId += 1;
			}
			numberOfStatesByT.add( numberOfStatesByT.get(numberOfStatesByT.size()-1) * _numberOfActions * 2); 
		}
		
		// 1.2. Create edges
		List< List<VertexCell> > adjLists = new LinkedList<List<VertexCell> >();
		for(Vertex v: vertices)
		{
			List<VertexCell> adjList = new LinkedList<VertexCell>();
			int numberOfSublings = numberOfStatesByT.get(1);
			
			int firstNodeInCurrLayer = getFirstNode(numberOfStatesByT, ((VertexMDP)v).getTime() );
			int firstNodeInNextLayer = getFirstNode(numberOfStatesByT, ((VertexMDP)v).getTime() + 1 );
		
			int firstSublingID = 0;
			if( firstNodeInNextLayer < vertices.size() ) 
				firstSublingID = firstNodeInNextLayer + (v.getID() - firstNodeInCurrLayer) * numberOfSublings;
						
			if( ((VertexMDP)v).getTime() < _T )
				for(int i = 0; i < numberOfSublings; ++i)
				{
					adjList.add(new VertexCell( vertices.get(firstSublingID-1 + i), 0));
				}
			adjLists.add(adjList);
		}
		
		//1.3. Create the graph
		_mdpGraph = new Graph(vertices, adjLists);
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return _mdpGraph.toString();
	}
	
	/**
	 * The method solves the MDP by performing the value iteration.
	 */
	public void solveByValueIteration()
	{
		int numberOfIterations = _T;
		
		for(int j = 0; j < _T; ++j)
		{
			
		}
	}
	
	/**
	 * The method returns the number of states of the MDP.
	 * @return the number of states of the MDP
	 */
	public int getNumberOfStates()
	{
		return _mdpGraph.getVertices().size();
	}
	
	
	/**
	 * The method returns the ID of the first node at the specified level.
	 * @param numberOfStatesByT a list with numbers of nodes per each level (time step)
	 * @param level leval of the tree
	 * @return
	 */
	private int getFirstNode(List<Integer> numberOfStatesByT, int level)
	{
		int firstNodeID = 1;
		for(int i = 0; i < level; ++i)
			firstNodeID += numberOfStatesByT.get(i);
		
		return firstNodeID;
	}
	
	private int _numberOfActions;						// Number of actions available in each state
	private int _T;										// Time horizon
	
	private Graph _mdpGraph;

	private double _maxPrice;							// Upper bound for the price range (controls)
	private double _Pa;									// Probability of arrival at time t
	
	private double _V;									// Value function constant parameter 
	private double _alpha;								// Value function exponent parameter
	
}
