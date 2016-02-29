package ch.uzh.ifi.Mechanisms;

import ch.uzh.ifi.MechanismDesignPrimitives.AllocationWoD;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;
import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.harvard.econcs.jopt.solver.IMIP;
import edu.harvard.econcs.jopt.solver.IMIPResult;
import edu.harvard.econcs.jopt.solver.MIPInfeasibleException;
import edu.harvard.econcs.jopt.solver.SolveParam;
import edu.harvard.econcs.jopt.solver.client.SolverClient;
import edu.harvard.econcs.jopt.solver.mip.CompareType;
import edu.harvard.econcs.jopt.solver.mip.Constraint;
import edu.harvard.econcs.jopt.solver.mip.MIP;
import edu.harvard.econcs.jopt.solver.mip.Term;
import edu.harvard.econcs.jopt.solver.mip.VarType;
import edu.harvard.econcs.jopt.solver.mip.Variable;


public class ProbabilisticCorePayments implements PaymentRule
{

	public ProbabilisticCorePayments(AllocationWoD allocation, List<Type> plans, int numberOfSellers)
	{
		_allocation = allocation;
		_plans = plans;
		_numberOfSellers = numberOfSellers;
	}
	
	@Override
	public List<Double> computePayments() throws Exception 
	{
		_mip = new MIP();
		_mip.setSolveParam(SolveParam.MIP_DISPLAY, 0);
		_mip.setSolveParam(SolveParam.DISPLAY_OUTPUT, false);
		_mip.setSolveParam(SolveParam.BARRIER_DISPLAY, 0);
		_solverClient = new SolverClient();
		
		List<Double> payments = new LinkedList<Double>();
		SemanticWebType allocatedPlan = (SemanticWebType)(_plans.get(0).getAtom( _allocation.getAllocatedBundlesByIndex(_allocation.getAuctioneerIndexById(1)).get(0)));
		
		//1. Compute exp.-VCG payments
		ProbabilisticVCGPayments vcgRule = new ProbabilisticVCGPayments(_allocation, _plans, _numberOfSellers);
		List<Double> vcgPayments = vcgRule.computePayments();
		if(allocatedPlan.getInterestingSet().size() == 1)							//A single winner
			return vcgPayments;
				
		//2. Compute expected costs of allocated sellers
		List<Double> expCosts = allocatedPlan.getExpectedCosts();
		
		//Test infeasibility of the core payments
		for(int i = 0; i < expCosts.size(); ++i)
			if( expCosts.get(i) > vcgPayments.get(i) )
				throw new PaymentException("Empty Core",0);
		
		//3. Compute min Exp-Core payments
		List<Variable> variables = new LinkedList<Variable >();
		
		//3.1. Create the optimization variables and formulate the objective function:
		for(int i = 0; i < allocatedPlan.getInterestingSet().size(); ++i)				//For every allocated seller
		{
			Variable x = new Variable("p" + i, VarType.DOUBLE, expCosts.get(i), vcgPayments.get(i));
			variables.add(x);
			_mip.add(x);
			_mip.addObjectiveTerm( 1.0, x);
		}
		_mip.setObjectiveMax(true);
		
		//3.2. Check all coalitions involving at lease one allocated seller (except of an empty and grand coalitions)
		int numberOfSellersInvolved = allocatedPlan.getNumberOfSellers();
		for(int i = 0; i < Math.pow(2, numberOfSellersInvolved)-1; ++i)					//For every combination of sellers
		{
			List<Integer> sellersIndexes = getSellersIndexes(i, numberOfSellersInvolved);
			
			double expectedSW = 0.;
			SemanticWebType bestPlan = null;
			for(int j = 0; j < _plans.get(0).getNumberOfAtoms(); ++j)					//Find the best plan involving this coalition
			{
				SemanticWebType plan = (SemanticWebType)(_plans.get(0).getAtom(j));
				boolean coalitionMatch = isMatched(allocatedPlan, sellersIndexes, plan );
				
				//3.3. Add a constraint for this coalition
				if( coalitionMatch )
				{
					if( plan.computeExpectedSW(plan.getNumberOfSellers()) > expectedSW )
					{
						expectedSW = plan.computeExpectedSW(plan.getNumberOfSellers());
						bestPlan = plan;
					}
				}
			}
			
			if( (bestPlan == null) && (sellersIndexes.size() > 0) )
				continue;
			
			List<Integer> fixedSellers = new LinkedList<Integer>();
			for(int j = 0; j < numberOfSellersInvolved; ++j)
				if( ! sellersIndexes.contains(j))
					fixedSellers.add(j);
			
			double rhs = 0.;
			if( (bestPlan == null) && (sellersIndexes.size() == 0) )			//A single buyer coalition
			{
				rhs = allocatedPlan.computeExpectedSW(numberOfSellersInvolved, fixedSellers);
			}
			else
			{
				rhs = allocatedPlan.computeExpectedSW(numberOfSellersInvolved, fixedSellers) - bestPlan.computeExpectedSW(bestPlan.getNumberOfSellers());
			}
			Constraint c = new Constraint(CompareType.LEQ, rhs);
			for(int k = 0; k < allocatedPlan.getInterestingSet().size(); ++k)
				if( ! sellersIndexes.contains(k) )
					c.addTerm( 1.0, variables.get(k));

			//System.out.println("Coalition #"+i+". Its indexes: " + sellersIndexes.toString()
			//		           + ((bestPlan != null) ? ". Its best plan: " + bestPlan.toString() : " "));
			//System.out.println("Add constraint: rhs = " + c.toString()+"="+(allocatedPlan.computeExpectedSW(numberOfSellersInvolved, fixedSellers))+
			//		           "-"+ (  (bestPlan != null) ? (bestPlan.computeExpectedSW(bestPlan.getNumberOfSellers())):" "));
			_mip.add(c);
		}

		//3.4. Find the 2nd best plan
		/*SemanticWebType secondBestPlan = null;
		double maxSW = 0.;
		for(int i = 0; i < _plans.get(0).getNumberOfAtoms(); ++i)
		{
			SemanticWebType plan = (SemanticWebType)(_plans.get(0).getAtom(i));
			if( (plan.computeExpectedSW( plan.getNumberOfSellers()) > maxSW) && (i != _allocation.getAllocatedBundlesById(1).get(0) ) )
			{
				maxSW = plan.computeExpectedSW( plan.getNumberOfSellers());
				secondBestPlan = plan;
			}
		}	
		
		//if(2ndbestplan == null) throw exception
		
		double rhs = allocatedPlan.computeExpectedSW(allocatedPlan.getNumberOfSellers()) - secondBestPlan.computeExpectedSW(secondBestPlan.getNumberOfSellers());
		for(int i = 0; i < allocatedPlan.getNumberOfSellers(); ++i)
			rhs += allocatedPlan.getActuallyAllocatedTuples(i) * allocatedPlan.getCost(i);
		
		Constraint c = new Constraint(CompareType.LEQ, rhs);
		for(int j = 0; j < allocatedPlan.getInterestingSet().size(); ++j)
			c.addTerm(new Term( (Variable)(variables.get(j)), 1.0 ));
		
		_mip.add(c, constraintID++);
 */
		
		//Launch CPLEX to solve the problem:
		try
		{
			_result = _solverClient.solve(_mip);
		}
		catch(MIPInfeasibleException e1)
		{
			throw new PaymentException("Empty Core", 0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("MIP: " + _mip.toString());
			System.out.println("expCosts: " + expCosts.toString());
			System.out.println("vcg: " + vcgPayments.toString());
			System.out.println("Plans: " + _plans.toString() );
			System.out.println("Allocation: " + _allocation.getNumberOfAllocatedAuctioneers() + " " + _allocation.getActuallyAllocatedItems(0).toString() );
		}
		Map m = _result.getValues();
		//_allocation = new Allocation();
		
		for(int i = 0; i < allocatedPlan.getNumberOfSellers(); ++i)
			payments.add( (double) m.get("p"+i) );
		
		return payments;
	}

	/*
	 * (non-Javadoc)
	 * @see Mechanisms.PaymentRule#isBudgetBalanced()
	 */
	@Override
	public boolean isBudgetBalanced() 
	{
		return true;
	}
	
	/*
	 * The method returns a list of indexes of sellers involved into a specified coalition.
	 * @param coalitionIndex - the index of a coalition under consideration
	 * @return a list of indexes of sellers involved into the coalition
	 */
	private List<Integer> getSellersIndexes(int coalitionIndex, int numberOfSellers)
	{
		List<Integer> sellersIndexes = new LinkedList<Integer>();
		
		for(int i = 0; i < numberOfSellers; ++i)
		{
			int sellerBinaryCode = (int)Math.pow(2, i);
			if( (sellerBinaryCode & coalitionIndex) > 0 )
				sellersIndexes.add(i);
		}
		
		return sellersIndexes;
	}
	
	/*
	 * The method returns true if a particular plan matches the following criteria:
	 * 1. all sellers in the coalition under consideration which are also present in the winning coalition have to 
	 *    be present in the plan;
	 * 2. all sellers which are present in the winning coalition but are not present in the current coalition (or
	 *    in other words, are present in the difference between the winning and current coalitions) have to be 
	 *    absent in the plan. 
	 * @param allocatedPlan - the plan allocated by the auction
	 * @param sellersIndexes- indexes (but not IDs) of sellers in the current coalition
	 * @param planToCheck   - a plan to be checked on the matching criteria (this plan creates the coalitional value)
	 */
	private boolean isMatched(SemanticWebType allocatedPlan, List<Integer> sellersIndexes, AtomicBid planToCheck)
	{
		boolean isMatchedCheck = true;
		
		//First, check if all sellers from the coalition (sellersIndexes) are also present in the plan to be checked
		for(int i = 0; i < sellersIndexes.size(); ++i)
		{
			int sellerIdToCheck = allocatedPlan.getInterestingSet().get( sellersIndexes.get(i) );
			if( ! planToCheck.getInterestingSet().contains( sellerIdToCheck ) )
			{
				isMatchedCheck = false;
				break;
			}
		}
		
		//Second, check that for all other sellers involved in the winning coalition but not involved in the current coalition
		//are also not present in the plan under consideration
		for(int i = 0; i < allocatedPlan.getNumberOfSellers(); ++i)
		{
			if(  ! sellersIndexes.contains(i)  )
			{
				int sellerIdToCheck = allocatedPlan.getInterestingSet().get( i );
				if( planToCheck.getInterestingSet().contains( sellerIdToCheck ) )
				{
					isMatchedCheck = false;
					break;
				}
			}
		}
		
		return isMatchedCheck;
	}
	
	private AllocationWoD _allocation;
	private List<Type> _plans;
	private int _numberOfSellers;
	
	SolverClient _solverClient;										//A CPLEX solver client
	private IMIPResult _result;										//A data structure for the solution
	private IMIP _mip = new MIP();									//A data structure for the mixed integer program
	static int constraintID = 0;									//Constraints counter
}
