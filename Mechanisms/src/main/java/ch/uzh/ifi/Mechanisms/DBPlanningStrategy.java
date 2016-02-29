package ch.uzh.ifi.Mechanisms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/*
 * The class implements a planning strategy for a given query based on the correspondent data stored in a database.
 */
public class DBPlanningStrategy implements IPlanningStrategy 
{
	/*
	 * @param queryId - an id of a query to be extracted from the database
	 * @param dbConnection - a connector to the database
	 * @param numberOfBuyers
	 * @param number of sellers
	 */
	public DBPlanningStrategy(String queryId, Connection dbConnection, int numberOfBuyers, int numberOfSellers)
	{
		_queryId = queryId;
		_dbConnection = dbConnection;
		_sellerIds = new HashMap<String, Integer>();
		_numberOfBuyers = numberOfBuyers;
		_numberOfSellers = numberOfSellers;

		//1. Resolve sellers' names
		try 
		{
			Statement statement = _dbConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT host FROM planSellerCost WHERE queryId='" + queryId + "'");
		
	        int sellerId = _numberOfBuyers + 1;
	        while (resultSet.next())
	        {
	            String host = resultSet.getString("host");
	            if( ! _sellerIds.containsKey(host) )
	            {
	            	_sellerIds.put(host, sellerId);
	            	sellerId += 1;
	            }
	        }
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		
		//2. Identify the number of plans available in the database for this query
		List<Integer> planIds = new LinkedList<Integer>();
		try 
		{
			Statement statement = _dbConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT planId FROM planSellerCost WHERE queryId='" + queryId + "'");
		
	        while (resultSet.next())
	        {
	            int planId = resultSet.getInt("planId");
	            if( ! planIds.contains(planId) )
	            	planIds.add(planId);
	        }
	        _numberOfPlans = planIds.size();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getNumberOfPlans()
	 */
	@Override
	public int getNumberOfPlans() 
	{
		return _numberOfPlans;
	}
	
	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getNumberOfSellers(int)
	 */
	@Override
	public int getNumberOfSellers(int planIdx) 
	{
		int numberOfSellers = 0;
		try 
		{
			Statement statement = _dbConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT planId, host, cost FROM planSellerCost WHERE queryId='" + _queryId + "'");
            
			while (resultSet.next())
			{
				int planId  = resultSet.getInt("planId");
            	if(planIdx == planId)
            		numberOfSellers += 1;
			}
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		
		return numberOfSellers;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getServersUsed(int)
	 */
	@Override
	public List<Integer> getServersUsed(int planIdx) 
	{
		List<Integer> serversUsed = new LinkedList<Integer>();
		
		try 
		{
			Statement statement = _dbConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT planId, host FROM planSellerCost WHERE queryId='" + _queryId + "'");
		
	        while (resultSet.next())
	        {
	            String host = resultSet.getString("host");
	            int planId  = resultSet.getInt("planId");
	            if( (planId == planIdx) && (! serversUsed.contains( _sellerIds.get(host)) )  )
	            	serversUsed.add( _sellerIds.get(host) );
	        }
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return serversUsed;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMinEstimatedNumberOfRecords()
	 */
	@Override
	public List<Integer> getMinEstimatedNumberOfRecordsFromSellers(int planIdx) 
	{
		List<Integer> minNumberOfRecordsEst = new LinkedList<Integer>();
		for(int j = 0; j < getNumberOfSellers(planIdx); ++j)
			minNumberOfRecordsEst.add( 0 );
		return minNumberOfRecordsEst;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMaxEstimatedNumberOfRecords(int)
	 */
	@Override
	public List<Integer> getMaxEstimatedNumberOfRecordsFromSellers(int planIdx) 
	{
		List<Integer> maxNumberOfRecordsEst = new LinkedList<Integer>();
		List<Integer> serversUsed = getServersUsed(planIdx);
		try 
		{
			Statement statement = _dbConnection.createStatement();
		
			for(int i = 0; i < serversUsed.size(); ++i)
			{
				ResultSet resultSet = statement.executeQuery("SELECT planId, host, cost FROM planSellerCost WHERE queryId='" + _queryId + "'");
		        while (resultSet.next())
		        {
		            String host = resultSet.getString("host");
		            int planId  = resultSet.getInt("planId");
		            int cost  = resultSet.getInt("cost");
		            
		            if( (planId == planIdx) && ( serversUsed.get(i) == _sellerIds.get(host) ) )
		            	maxNumberOfRecordsEst.add(cost);
		        }
			}
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return maxNumberOfRecordsEst;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMinEstimatedNumberOfRecordsForBuyer(int)
	 */
	@Override
	public int getMinEstimatedNumberOfRecordsForBuyer(int planIdx) 
	{
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.IPlanningStrategy#getMaxEstimatedNumberOfRecordsForBuyer(int)
	 */
	@Override
	public int getMaxEstimatedNumberOfRecordsForBuyer(int planIdx) 
	{
		int maxEstimatedNumberOfRecordsForBuyer = 0;
		try 
		{
			Statement statement = _dbConnection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT planId, value FROM planBuyerValue WHERE queryId='" + _queryId + "'");
		
	        while (resultSet.next())
	        {
	            int planId  = resultSet.getInt("planId");
	            int value   = resultSet.getInt("value");
	            if( planId == planIdx )
	            {
	            	maxEstimatedNumberOfRecordsForBuyer = value;
	            	break;
	            }
	        }
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
		return maxEstimatedNumberOfRecordsForBuyer;
	}
	
	private Connection _dbConnection = null;					//A database connector
	private String _queryId;									//An id of a query
	private Map<String, Integer> _sellerIds;					//A mapping from sellers' names to sellers' ids 
	private int _numberOfBuyers;								//A number of buyers
	private int _numberOfSellers;								//A number of sellers
	private int _numberOfPlans;									//A number of plans available in the database for the query
}
