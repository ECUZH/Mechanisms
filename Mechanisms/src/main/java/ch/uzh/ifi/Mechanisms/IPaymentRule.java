package ch.uzh.ifi.Mechanisms;

import java.util.List;

public interface IPaymentRule 
{
	/**
	 * The method computes and returns payments of allocated agents (and only allocated agents!).
	 * @return list of payments of allocated agents
	 */
	public List<Double> computePayments() throws Exception;
	
	/**
	 * The method checks if the computed payments are BB or not.
	 * @return true if the computed payments are budget balanced and false otherwise
	 */
	public boolean isBudgetBalanced();
}
