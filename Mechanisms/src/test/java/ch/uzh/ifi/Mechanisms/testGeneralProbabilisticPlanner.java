package ch.uzh.ifi.Mechanisms;

import static org.junit.Assert.*;

import ch.uzh.ifi.MechanismDesignPrimitives.AtomicBid;
import ch.uzh.ifi.MechanismDesignPrimitives.Type;
import ch.uzh.ifi.MechanismDesignPrimitives.CombinatorialType;
import ch.uzh.ifi.MechanismDesignPrimitives.SemanticWebType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class testGeneralProbabilisticPlanner {

	@Test
	public void testGeneralProbabilisticPlanner() 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Type> bids = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);													//a query
		
		for(int i = 0; i < numberOfBuyers; ++i)
		{
			AtomicBid atomB = new AtomicBid( i+1, items, 0.3 );
			atomB.setTypeComponent("isSeller", 0.0);
			atomB.setTypeComponent("Distribution", 1.0);
			atomB.setTypeComponent("minValue", 0.0);
			atomB.setTypeComponent("maxValue", 1.0);
			CombinatorialType b = new CombinatorialType(); 				//Buyer's type
			b.addAtomicBid(atomB);
			bids.add(b);
		}
		
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( numberOfBuyers + 1 + i , items, 0.1 );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();				//Seller's type
			s.addAtomicBid(atomS);
			bids.add(s);
		}
		
		IPlanningStrategy planningStrategy = new RandomPlanningStrategy(0, numberOfBuyers, numberOfSellers, 2, 2, 5);
		
		GeneralProbabilisticPlanner planner = new GeneralProbabilisticPlanner(numberOfBuyers, numberOfSellers, bids, 0);
		planner.setNumberOfPlans( 5 );
		planner.setPlanningStrategy(planningStrategy);
		planner.reset(bids);
		
		List<Type> plans = planner.generatePlans();
		
		assertTrue(plans.size() == 1);
		assertTrue(plans.get(0).getNumberOfAtoms() == 5);

		System.out.println(">> " + plans.toString());
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getInterestingSet().size() == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getInterestingSet().get(0) == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getInterestingSet().get(1) == 4  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getMaxNumberOfTuples(0) == 3 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getMaxNumberOfTuples(1) == 3 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedSW(2) - 0.6 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedValue(2) - 0.9 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedCosts().get(0) - 0.15) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedCosts().get(1) - 0.15) < 1e-6 );
		
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getInterestingSet().size() == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getInterestingSet().get(0) == 3  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getInterestingSet().get(1) == 4  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getMaxNumberOfTuples(0) == 9 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getMaxNumberOfTuples(1) == 3 );
		/*assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(1)).computeExpectedSW(2) - 1.9 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(1)).computeExpectedValue(2) - 2.85 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(1)).computeExpectedCosts().get(0) - 0.5) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(1)).computeExpectedCosts().get(1) - 0.45) < 1e-6 );
		
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(2)).getInterestingSet().size() == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(2)).getInterestingSet().get(0) == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(2)).getInterestingSet().get(1) == 3  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(2)).getMaxNumberOfTuples(0) == 2 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(2)).getMaxNumberOfTuples(1) == 8 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(2)).computeExpectedSW(2) - 1.0 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(2)).computeExpectedValue(2) - 1.5 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(2)).computeExpectedCosts().get(0) - 0.1) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(2)).computeExpectedCosts().get(1) - 0.4) < 1e-6 );
		
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(3)).getInterestingSet().size() == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(3)).getInterestingSet().get(0) == 3  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(3)).getInterestingSet().get(1) == 4  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(3)).getMaxNumberOfTuples(0) == 4 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(3)).getMaxNumberOfTuples(1) == 3 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(3)).computeExpectedSW(2) - 0.7 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(3)).computeExpectedValue(2) - 1.05 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(3)).computeExpectedCosts().get(0) - 0.2) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(3)).computeExpectedCosts().get(1) - 0.15) < 1e-6 );
		
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(4)).getInterestingSet().size() == 2  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(4)).getInterestingSet().get(0) == 3  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(4)).getInterestingSet().get(1) == 4  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(4)).getMaxNumberOfTuples(0) == 6 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(4)).getMaxNumberOfTuples(1) == 9 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(4)).computeExpectedSW(2) - 1.5 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(4)).computeExpectedValue(2) - 2.25 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(4)).computeExpectedCosts().get(0) - 0.3) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(4)).computeExpectedCosts().get(1) - 0.45) < 1e-6 );*/
	}

	@Test
	public void testGeneralProbabilisticPlannerDB() 
	{
		int numberOfBuyers = 1;
		int numberOfSellers = 3;
		
		List<Type> bids = new LinkedList<Type>();
		List<Integer> items = new LinkedList<Integer>();
		items.add(0);													//a query
		
		for(int i = 0; i < numberOfBuyers; ++i)
		{
			AtomicBid atomB = new AtomicBid( i+1, items, 0.3 );
			atomB.setTypeComponent("isSeller", 0.0);
			atomB.setTypeComponent("Distribution", 1.0);
			atomB.setTypeComponent("minValue", 0.0);
			atomB.setTypeComponent("maxValue", 1.0);
			CombinatorialType b = new CombinatorialType(); 				//Buyer's type
			b.addAtomicBid(atomB);
			bids.add(b);
		}
		
		for(int i = 0; i < numberOfSellers; ++i)
		{
			AtomicBid atomS = new AtomicBid( numberOfBuyers + 1 + i , items, 0.1 );
			atomS.setTypeComponent("isSeller", 1.0);
			atomS.setTypeComponent("Distribution", 1.0);
			atomS.setTypeComponent("minValue", 0.0);
			atomS.setTypeComponent("maxValue", 1.0);
			CombinatorialType s = new CombinatorialType();				//Seller's type
			s.addAtomicBid(atomS);
			bids.add(s);
		}
		
		Connection dbConnection = null;
		try 
        {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("ssl", "true");
            props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
            
            dbConnection = DriverManager.getConnection("jdbc:postgresql://" + DBHOST + "/" + DB, props);
        } 
        catch (ClassNotFoundException e) 
        {
            e.printStackTrace();
        } 
        catch (SQLException e) 
        {
            e.printStackTrace();
        }
		
		String queryId = "1e76cf27-d83b-467f-b7de-7f323039b7a0";
		IPlanningStrategy planningStrategy = new DBPlanningStrategy(queryId, dbConnection, numberOfBuyers, numberOfSellers);
		
		GeneralProbabilisticPlanner planner = new GeneralProbabilisticPlanner(numberOfBuyers, numberOfSellers, bids, 0);
		planner.setNumberOfPlans( 5 );
		planner.setPlanningStrategy(planningStrategy);
		planner.reset(bids);
		
		List<Type> plans = planner.generatePlans();
		
		assertTrue(plans.size() == 1);
		assertTrue(plans.get(0).getNumberOfAtoms() == 3);

		System.out.println(">> " + plans.toString());
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getInterestingSet().size() == 1  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getInterestingSet().get(0) == 4  );

		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).getMaxNumberOfTuples(0) == 4500 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedSW(2) - (bids.get(0).getAtom(0).getValue() - bids.get(1).getAtom(0).getValue()) * 4500 / 2 ) < 1e-6 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedValue(2) - bids.get(0).getAtom(0).getValue() * 4500 / 2 ) < 1e-6 );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedCosts().size() == 1 );
		assertTrue( Math.abs( ((SemanticWebType)plans.get(0).getAtom(0)).computeExpectedCosts().get(0) - bids.get(1).getAtom(0).getValue() * 4500 / 2 ) < 1e-6 );
		
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getInterestingSet().size() == 1  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getInterestingSet().get(0) == 3  );
		assertTrue( ((SemanticWebType)plans.get(0).getAtom(1)).getMaxNumberOfTuples(0) == 9205 );
		
		try 
		{
			dbConnection.close();
		} 
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}
	
	public static final String defaultInputFile = "C:\\Users\\Dmitry\\workspace\\Algorithms32\\MechanismDesignTools\\test\\Tools\\queries.csv";
    public static final String defaultOutputFile = "C:\\Users\\Dmitry\\workspace\\Algorithms32\\MechanismDesignTools\\test\\Tools\\output.csv";

    private static final String DBHOST = "pg.ifi.uzh.ch";
    private static final String DB = "s0691318";
    private static final String USER = "s0691318";
    private static final String PASSWORD = "jiP3p";
}
