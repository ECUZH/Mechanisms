package ch.uzh.ifi.Mechanisms;

import java.util.List;

public interface IPlanningStrategy 
{
	public int getNumberOfPlans();
	
	public int getNumberOfSellers(int planIdx);
	
	public List<Integer> getServersUsed(int planIdx);
	
	public List<Integer> getMinEstimatedNumberOfRecordsFromSellers(int planIdx);
	
	public List<Integer> getMaxEstimatedNumberOfRecordsFromSellers(int planIdx);
	
	public int getMinEstimatedNumberOfRecordsForBuyer(int planIdx);
	
	public int getMaxEstimatedNumberOfRecordsForBuyer(int planIdx);
}
