package main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import logging.CSVLog;
import mathematical_programming.MPConnection;
import reader.ReadData;
import activity.Activity;
import agent.Farm;

/** 
 * This class contains the main() method of this program. 
 * Full simulation runs inside of the main() method by creating farm objects and making decisions for each farm.
 *
 */
public class Consumat {
	static long line_counter = 0;
	static int file_counter = 1;
	static String origFileName = createFileName();										           // file name for logging
	static String FileName = origFileName + String.format("%d",0);								   // given enough lines in the log file, a new file is needed.
		
	public static void main(String[] args) {
		System.out.println("Starting FARMIND");

		if (args.length < 1) {
			System.out.println("Input number of iterations");
			System.exit(0);
		} 
		
		if (args.length > 1) {
			System.out.println("FARMIND version number: 1.2.0");
		}

		ReadData reader         = new ReadData();							               		   // read all input data files
		List<Farm>     allFarms = reader.getFarms();					                           // build set of farms 
		List<Double> MP_Incomes = new ArrayList<Double>();		                                   // list of all farm incomes generated by the MP model
		List<ArrayList<Activity>> MP_Activities = new ArrayList<ArrayList<Activity>>();	           // list of all farm activities selected by MP model. MP returns list of activities for each farm, thus a list of lists
		double income = 0;													                       // specific income of farm 		
		ArrayList<Activity> activity = null;											           // specific activity list of the farm
		
		initializePopulationIncomeChangeRate(allFarms);						                           // initialize the farms with the input values before starting the full ABM simulation
		
		int farmIndex = 0;													                       // index of specific farm in list
		for (int year = 1; year <= Integer.parseInt(args[0]); year++) {		                       // run simulation for a set of years, getting updated income and activities	
			System.out.println(String.format("Year %d", year));	
			
			MPConnection MP = new MPConnection();
			
			farmIndex = 0;
			for (Farm farm : allFarms) {
				if (year == 1) {											                       // ignore first year as we already have that initialized with input file
					income = -1;
				} else {
					income = MP_Incomes.get(farmIndex);										       // for all other years get the MP income and the MP activities to update each farm
					activity = MP_Activities.get(farmIndex);
				}
				//System.out.println(String.format("Income=%f", income));
				//System.out.println(String.format("Activity=%s", activity));
				//System.out.println(String.format("farm index=%d", farmIndex));
				
				farm.updateExperience();                              			   // each time period update experience
				farm.updateFarmParameters(allFarms, income, activity);
				
				List<String> possibleActivitySet = farm.decideActivitySet(allFarms);      
				
				CSVLog log = new CSVLog(farm.getPreferences().getDataElementName(), farm.getFarmName(), year, farm.getLearningRate(), farm.getStrategy(), farm.getIncomeHistory().get(0), farm.getCurrentActivity(), possibleActivitySet, farm);
				updateLogFileName();
				log.appendLogFile(FileName);
				
				MP.inputsforMP(farm.getFarmName(), possibleActivitySet);
				
				farm.updateAge();                              				       // each time period update age
				farmIndex++;                                                       // go to next farm in list
			}
			
			MP.runModel();
			MP_Incomes = MP.readMPIncomes();
			MP_Activities = MP.readMPActivities();

			updatePopulationIncomeChangeRate(allFarms, MP_Incomes);    // at end of time step update the percent change for population
		}

		System.out.println("Complete"); 
	}
		
    private static void updateLogFileName() {
		line_counter++;
		if (line_counter > 1000000) {
			FileName = origFileName + String.format("%d", file_counter);
			file_counter++;
			line_counter = 0;
		} 
    }
	
	/** 
	 * This function initializes the income growth rate of the population (in a region) for all farms.
	 * @param allFarms: list of all farms in region
	 */
	private static void initializePopulationIncomeChangeRate(List<Farm> allFarms) {	
		List<Double> differenceIncomeYears = new ArrayList<Double>();
		List<Double> populationYearlyMeanIncome = new ArrayList<Double>();
		
		int memory = allFarms.get(0).getMemory();                              // assume all farms have same memory length
		
		for(int i = 0; i < memory; i++) {
			List<Double> incomeFarmYear = new ArrayList<Double>();
			for (Farm farm: allFarms) {
				incomeFarmYear.add(farm.getIncomeHistory().get(i)); 
			}
			populationYearlyMeanIncome.add(mean(incomeFarmYear));	
		}
		
		for(int i = memory-1; i > 0; i-- ) {
			double diff = (populationYearlyMeanIncome.get(i-1) -  populationYearlyMeanIncome.get(i)) /  populationYearlyMeanIncome.get(i);
			differenceIncomeYears.add( diff );   
		}
		
		for (Farm farm: allFarms) {
			double changeRate = mean(differenceIncomeYears);
			farm.setAveragePopulationIncomeChangeRate(changeRate);
		}
	}
	
	/** 
	 * This function updates the income growth rates based on new income for farms.
	 * @param allFarms: list of all farms in region
	 * @param thisYearIncome: list of income values for all farms
	 */
	private static void updatePopulationIncomeChangeRate(List<Farm> allFarms, List<Double> thisYearIncome) {
		List<Double> differenceIncomeYears = new ArrayList<Double>();
		List<Double> populationYearlyMeanIncome = new ArrayList<Double>();
		
		int memory = allFarms.get(0).getMemory();                              // assume all farms have same memory length
		double currentYearAverageIncome = mean(thisYearIncome);
		populationYearlyMeanIncome.add(currentYearAverageIncome);
				
		// get the average income for the population for each year, but skip the oldest income. 
		// The incomes will get updated for each agent, and the oldest income will be removed. 
		for(int i = 0; i < memory-1; i++) {
			List<Double> incomeFarmYear = new ArrayList<Double>();
			for (Farm farm: allFarms) {
				incomeFarmYear.add(farm.getIncomeHistory().get(i)); 
			}
			populationYearlyMeanIncome.add(mean(incomeFarmYear));	
		}
		
		for(int i = memory-1; i > 0; i-- ) {
			double diff = (populationYearlyMeanIncome.get(i-1) -  populationYearlyMeanIncome.get(i)) /  populationYearlyMeanIncome.get(i);
			differenceIncomeYears.add( diff );   
		}
		
		for (Farm farm: allFarms) {
			double changeRate = mean(differenceIncomeYears);
			farm.setAveragePopulationIncomeChangeRate(changeRate);
		}
	}
	
	/** 
	 * This function calculates the mean of provided list 
	 * @param list of values to calculate mean with
	 * @return mean: average of the input list
	 */
	private static double mean(List<Double> list) {
		double mean = 0;												       // mean value to return
		
		for (int i = 0; i<list.size(); i++) {
			mean = mean + list.get(i);
		}
		
		return mean / list.size();
	}

	/** 
	 * This function creates generic file name so that version number can be appended to end. 
	 * @return fileName
	 */
	public static String createFileName() {
		Calendar now = Calendar.getInstance();                             // Gets the current date and time
		int day = now.get(Calendar.DAY_OF_MONTH); 
		int month = now.get(Calendar.MONTH) + 1;
		int year_file = now.get(Calendar.YEAR);
		int hour = now.get(Calendar.HOUR);
		int minute = now.get(Calendar.MINUTE);	
		String fileName = String.format("Results-%d%02d%02d_%d-%d_v", year_file, month, day, hour, minute);
		
		return fileName;
	}
}
