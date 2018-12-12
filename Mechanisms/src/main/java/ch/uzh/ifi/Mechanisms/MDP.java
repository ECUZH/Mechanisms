package ch.uzh.ifi.Mechanisms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
		
		// 0. Discretize the price (control) space
		_prices = new ArrayList<Double>();
		for(int i = 0; i < _numberOfActions; ++i)
			_prices.add( 1e-8 + i * _maxPrice / _numberOfActions);
		
		// 1. Create the MDP graph
		List<Vertex> vertices = new ArrayList<Vertex>();
		List<Integer> numberOfStatesByT = new ArrayList<Integer>();
		numberOfStatesByT.add(1);
		int vertexId = 1;
		
		// 1.1. Create vertices
		System.out.println("Create vertices.");
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
		System.out.println("Create edges.");
		int numberOfSublings = numberOfStatesByT.get(1);
		List< List<VertexCell> > adjLists = new ArrayList<List<VertexCell> >(vertices.size());
		for(Vertex v: vertices)
		{
			List<VertexCell> adjList = new ArrayList<VertexCell>(numberOfSublings);
			
			int firstNodeInCurrLayer = getFirstNode(numberOfStatesByT, ((VertexMDP)v).getTime() );
			int firstNodeInNextLayer = getFirstNode(numberOfStatesByT, ((VertexMDP)v).getTime() + 1 );
		
			int firstSublingID = 0;
			if( firstNodeInNextLayer < vertices.size() ) 
				firstSublingID = firstNodeInNextLayer + (v.getID() - firstNodeInCurrLayer) * numberOfSublings;
						
			if( ((VertexMDP)v).getTime() < _T )
				for(int i = 0; i < numberOfSublings; ++i)
				{
					int vertexID = firstSublingID + i;
					adjList.add(new VertexCell( vertices.get( vertexID - 1), 0));
					
					if( vertexID % 2 == 0 )
					{
						((VertexMDP)vertices.get(vertexID-1)).setNumberOfAllocatedDBs( ((VertexMDP)v).getNumberOfAllocatedDBs() + 1 );
						((VertexMDP)vertices.get(vertexID-1)).setPayment( _prices.get(i/2) );
					}
					else
						((VertexMDP)vertices.get(vertexID-1)).setNumberOfAllocatedDBs( ((VertexMDP)v).getNumberOfAllocatedDBs() );
				}
			adjLists.add(adjList);
		}
		
		// 1.3. Create the graph
		System.out.println("Create the graph.");
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
		int numberOfIterations = _T + 1;
		
		for(int j = 0; j < numberOfIterations; ++j)
		{ 	
			System.out.println("Iteration j="+j);
			for(Vertex v: _mdpGraph.getVertices())
			{
				double stateReward = _V * Math.pow(((VertexMDP)v).getNumberOfAllocatedDBs(), _alpha) - ((VertexMDP)v).getPayment();
				double maxExpectedSurplus = maxExpectedFutureSurplus(v);
				((VertexMDP)v).setUtility( stateReward + maxExpectedSurplus );
				//System.out.println(((VertexMDP)v).getUtility());
			}
			//System.out.println(" ");
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
	 * The method returns the ID of the first node at the specified level of the MDP tree.
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
	
	/**
	 * The method returns the maximum expected future surplus that can be reached from the given state v.
	 * @param v the state (vertex) of the MDP tree
	 * @return the expected future surplus
	 */
	public double maxExpectedFutureSurplus(Vertex v)
	{
		double maxExpectedFutureSurp = 0.;
		
		if( _mdpGraph.getAdjacencyLists().get(v.getAdjacencyListIndex()).size() == 0 )			// Leaf node
			maxExpectedFutureSurp = 0.;
		else
			for(int i = 0; i < _numberOfActions; ++i)
			{
				VertexMDP v1 = (VertexMDP)_mdpGraph.getAdjacencyLists().get( v.getAdjacencyListIndex()).get( i*2 )._v;
				VertexMDP v2 = (VertexMDP)_mdpGraph.getAdjacencyLists().get( v.getAdjacencyListIndex()).get( i*2 + 1 )._v;
				if( (_Pa * _prices.get(i)) * v1.getUtility() + (1 - _Pa * _prices.get(i)) * v2.getUtility() > maxExpectedFutureSurp)
					maxExpectedFutureSurp = (_Pa * _prices.get(i)) * v1.getUtility() + (1 - _Pa * _prices.get(i)) * v2.getUtility();
			}
		return maxExpectedFutureSurp;
	}
	
	private int _numberOfActions;						// Number of actions available in each state
	private int _T;										// Time horizon
	
	private Graph _mdpGraph;							// MDP graph

	private double _maxPrice;							// Upper bound for the price range (controls)
	private List<Double> _prices;						// Discretization of the control space
	private double _Pa;									// Probability of arrival at time t
	
	private double _V;									// Value function constant parameter 
	private double _alpha;								// Value function exponent parameter
}
