package main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import decision.DecisionResult;
import reader.ReadData;
import activity.Activity;
import agent.Farm;

/** 
 * This class contains the main() method of this programme. 
 * Full simulation runs inside of the main() method by creating farm objects and making decisions for each farm.
 *
 */
public class Consumat {
	static long line_counter = 0;
	static int file_counter = 1;
	static String origFileName = createFileName();										           // file name for logging
	static String FileName = origFileName + String.format("%d",0);								   // given enough lines in the log file, a new file is needed.
		
	public static void main(String[] args) {
		System.out.println("Starting Model");

		if (args.length < 1) {
			System.out.println("input number of iterations");
			System.exit(0);
		} 
		
		if (args.length > 1) {
			System.out.println("ABM version number: 1.1");
		}

		ReadData reader = new ReadData();									   // read all input data files
		List<Farm>     allFarms = reader.getFarms();					       // build set of farms 
		List<Double> simulatedIncomeForFarms = new ArrayList<Double>();		   // list of all farm incomes
		
		initializePopulationIncomeChange(allFarms);						       // initialize the farms with the input values before starting the full ABM simulation
		
		for (int year = 1; year <= Integer.parseInt(args[0]); year++) {		   // run simulation for a set of years, getting updated income and activities	
			System.out.println(String.format("year %d", year));				
			
			prepareInputsforMP(allFarms, year, simulatedIncomeForFarms);
			runMP(allFarms);
			simulatedIncomeForFarms = readMPResults(allFarms);
			updatePopulationIncomeChange(allFarms,simulatedIncomeForFarms);    // after time step update the percent change for population
		}

		System.out.println("Complete"); 
	}
	
	/**
	 * This function updates incomes for farms and then make the farm decision and resulting strategy choice for each farm. 
	 * Each farm then appends this choice to the simulation control file
	 * 
	 * @param allFarms list of all farms in the system
	 * @param year indicator of time period
	 * @param simulatedIncomeForFarms income generated by the simulation of the previous period
	 */
	private static void prepareInputsforMP(List<Farm> allFarms, int year, List<Double> simulatedIncomeForFarms) {
		double income = 0;																	       // specific income of farm 		
		int farmIndex = 0;																		   // index of specific farm in list

		File f = new File("p_allowedStratPrePost.csv");										       // delete last time period's simulation file
		if (f.exists()) {f.delete();}
		
		for (Farm farm : allFarms) {
			if (year == 1) {															           // ignore first year as we already have that initialized with input file
				income = -1;
			} else {
				income = simulatedIncomeForFarms.get(farmIndex);
			}
			
			farm.updateFarmParameters(allFarms, income);
			List<String> possibleActivitySet = farm.makeDecision(allFarms);      
			
			System.out.print(farm.getFarmName() + " current activity: ");
			for (Activity act: farm.getCurrentActivity() ) System.out.print(act.getName() + " ");
			System.out.print("\n");
			
			System.out.print(farm.getFarmName() + " possible activity: ");
			for (String act: possibleActivitySet) System.out.print(act + " ");
			System.out.print("\n");
			
			DecisionResult decision = new DecisionResult(farm.getPreferences().getDataElementName(), farm.getFarmName(), year, farm.getLearningRate(), farm.getStrategy(), farm.getIncomeHistory().get(0), farm.getCurrentActivity(), possibleActivitySet, farm);

			line_counter++;
			if (line_counter > 1000000) {
				FileName = origFileName + String.format("%d",file_counter);
				file_counter++;
				line_counter = 0;
			} 
			decision.appendLogFile(FileName);
			decision.appendMPInput();										   // create a file 'p_allowedStrat' which contains the gams options for each farm
			farm.updateExperience();                              			   // each time period update experience
			farm.updateAge();                              				       // each time period update experience
			farmIndex++;
		}

		System.out.println("Created Gams simulation file");
	}
	
	/** 
	 * This function runs GAMS simulation based on input file created while looping through all farms.
	 * @param allFarms list of all farms in system
	 */
	private static void runMP(List<Farm> allFarms) {
		Runtime runtime = Runtime.getRuntime();								   // java runtime to run commands
		
		File f = new File("Grossmargin_P4,00.csv");							   // delete previous results file before new run
		f.delete();
		
		try {
			String name = System.getProperty("os.name").toLowerCase();
			if (name.startsWith("win") ){
				runtime.exec("cmd /C" + "run_gams.bat");						   // actually run command
			}
			if (name.startsWith("mac")) {
				runtime.exec("/bin/bash -c ./run_gams_mac.command");				   // actually run command
			}
			System.out.println("Starting gams model");
			System.out.println("Waiting for gams results to be generated");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * This function reads GAMS file and set the income and activities based on the file.
	 * 
	 * @param allFarms list of all farms in system
	 * @return incomes is a list of all incomes for each farm
	 */
	@SuppressWarnings("unchecked")
	private static List<Double> readMPResults(List<Farm> allFarms) {
		List<Double> incomes = new ArrayList<Double>();						   // list of all farm incomes
		List<Activity> activities = new ArrayList<Activity>();	   	 	       // list of all farm activities selected by MP model
		ReadData reader = new ReadData();									   // object to read data files	   
		
		List<Object> data = reader.readMPOutputFiles();			               // read data file generated by MP

		incomes = (List<Double>) data.get(0);
		activities = (List<Activity>) data.get(1);
		
		for (int i = 0; i < allFarms.size(); i++) {
			List<Activity> act = new ArrayList<Activity>();
			act.add(activities.get(i));
			allFarms.get(i).setCurrentActivity(act);
		}
		return incomes;
	}

	/** 
	 * This function initializes the income growth rate of the population (in a region) for all farms.
	 * 
	 * @param allFarms list of all farms in region
	 */
	private static void initializePopulationIncomeChange(List<Farm> allFarms) {
		double historicalPopulationAverage = 0;
		List<Double> initIncome = new ArrayList<Double>();
		double thisYearAverage = 0;
		double percentChange;
		
		for (Farm farm: allFarms) {
			List<Double> income = new ArrayList<Double>(farm.getIncomeHistory());
			initIncome.add(income.get(0));
			income.remove(0);
			historicalPopulationAverage = historicalPopulationAverage + mean(income);
		}
		historicalPopulationAverage = historicalPopulationAverage/allFarms.size();
		thisYearAverage = mean(initIncome);
		
		percentChange = (thisYearAverage - historicalPopulationAverage) / historicalPopulationAverage;
		
		for (Farm farm: allFarms) {
			farm.setRegionIncomeChangePercent(percentChange);
		}
	}
	
	/** 
	 * This function updates the income growth rates based on new income for farms.
	 * 
	 * @param allFarms list of all farms in region
	 * @param thisYearIncome list of income values for all farms
	 */
	private static void updatePopulationIncomeChange(List<Farm> allFarms, List<Double> thisYearIncome) {
		double historicalRegionAverage = 0;
		double thisYearAverage = mean(thisYearIncome);
		double percentChange;
		
		for (Farm farm: allFarms) {
			List<Double> income = new ArrayList<Double>(farm.getIncomeHistory());
			income.remove(0);
			historicalRegionAverage = historicalRegionAverage + mean(income);
		}
		historicalRegionAverage = historicalRegionAverage/allFarms.size();
		
		percentChange = (thisYearAverage - historicalRegionAverage) / historicalRegionAverage;
		
		for (Farm farm: allFarms) {
			farm.setRegionIncomeChangePercent(percentChange);
		}
	}
	
	/** 
	 * This function calculates the mean of provided list 
	 * @param list of values to calculate mean with
	 * @return mean
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
