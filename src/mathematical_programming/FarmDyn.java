package mathematical_programming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

//import org.apache.commons.io.FileUtils;

import activity.Activity;
import agent.Farm;
import reader.ReadData;

public class FarmDyn implements MP_Interface{

	private static final Logger LOGGER = Logger.getLogger("FARMIND_LOGGING");
	
	File file; 																   // file object to read/write 							
	List<Object> year_price = new ArrayList<Object>();						   // list of yearly prices for model
	List<Double> yearlyPrices = new ArrayList<Double>();					   // list of modeling prices per year
	List<String> listOfYears = new ArrayList<String>();						   // list of modeling price/years
	String strategyFile;													   // strategy file
	String resultsFile;														   // results file
	String gamsModelFile;													   // gams model file, used for editing actual gams script
	String yearlyPriceFile; 												   // price file for reading yearly prices 
	String baseGamsModel;													   // original base gams model of farmydn
	
	public FarmDyn(Properties cmd, int simYear, int memoryLengthAverage) {
		strategyFile = String.format("%s\\p_AllowedStratPrePost.csv",cmd.getProperty("project_folder"));
		resultsFile = String.format("%s\\Grossmargin_P4,00.csv",cmd.getProperty("project_folder"));
		baseGamsModel =  String.format("./%s",cmd.getProperty("project_folder"));
		yearlyPriceFile = String.format("./%s/yearly_prices.csv",cmd.getProperty("data_folder"));
		file = new File( strategyFile) ;				                       // delete last time period's simulation file
		if (file.exists()) {
			file.delete();
		}	
	}
	
	@Override
	public void inputsforMP(Farm farm, List<String> possibleActivity) {						       // copy base model to individual folder and set up activities in incgen file
		
		List<String> allActivities = new ArrayList<String>();
		
		for(Activity act: farm.getActivities()) {
			allActivities.add(act.getName());
		}
		
		// copy model directory to it's own folder so you can set up this individual run
		String baseModel = baseGamsModel + String.format("\\%S","farmdyn_base_model");
		String newModel = baseGamsModel + String.format("\\%S",farm.getFarmName());
		File srcDir = new File(baseModel);
		File destDir = new File(newModel);
//		try {
//			FileUtils.copyDirectory(srcDir, destDir);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		gamsModelFile = String.format("%s\\incgen\\runInc.gms",newModel);
		
		editMPscript(possibleActivity);	
	}
	

	@Override
	// throwaway pricing average and memoryLength average value as it is not used in the Swissland model
	public void runModel(Properties cmd, int nFarm, int year, boolean pricingAverage, int memoryLengthAverage) {
		Runtime runtime = Runtime.getRuntime();						           // java runtime to run commands
				
		File f = new File(resultsFile);
		f.delete();
		
		LOGGER.info("Starting MP model");
		
		try {
			String name = System.getProperty("os.name").toLowerCase();
			if (name.startsWith("win") ){
				createRunGamsBatch(cmd, "win");								   // generate run_gams.bat based on input control files
				runtime.exec("cmd /C" + "run_gams.bat");					   // actually run command
			}
			if (name.startsWith("mac")) {
				createRunGamsBatch(cmd, "mac");			
				runtime.exec("/bin/bash -c ./run_gams_mac.command");		   // actually run command
			}
			
			LOGGER.info("Waiting for output generated by MP model");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createRunGamsBatch(Properties cmd, String OS) {
		if (cmd.getProperty("debug").equals("1")) {
			if (OS.equals("win")) {
				LOGGER.info("Creating run_gams.bat file for debug");
				File f = new File("run_gams.bat");
				f.delete();
				FileWriter fw;
				try {
					fw = new FileWriter(f,true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter writer = new PrintWriter(bw);
					writer.println("copy \".\\data\\Grossmargin_P4,00.csv\" .\\projdir");
					LOGGER.fine("copy \".\\data\\Grossmargin_P4,00.csv\" .\\projdir");
					
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (OS.equals("mac")) {
				LOGGER.info("Creating run_gams_mac file");
				File f = new File("run_gams_mac.command");
				f.delete();
				FileWriter fw;
				try {
					fw = new FileWriter(f,true);
					BufferedWriter bw = new BufferedWriter(fw);
					PrintWriter writer = new PrintWriter(bw);
					writer.println("#!/bin/bash");
					writer.println("cp ./data/Grossmargin_P4,00.csv ./projdir/Grossmargin_P4,00.csv");
					LOGGER.fine("cp ./data/Grossmargin_P4,00.csv ./projdir/Grossmargin_P4,00.csv");
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			LOGGER.info("Creating run_gams.bat file for actual gams system");
			File f = new File("run_gams.bat");
			f.delete();
			FileWriter fw;
			try {
				fw = new FileWriter(f,true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter writer = new PrintWriter(bw);
				if (cmd.getProperty("project_folder") == null)  {
					LOGGER.severe("Please include parameter project_folder into the control file.");
					System.exit(0);
				}
				String proj = "cd " + cmd.getProperty("project_folder");
				writer.println(proj);				
				writer.println("gams exp_starter.gms --task=\"Single farm run\"  -errorLog=99 -lo=3 --scen=incgen/runInc --ggig=on --baseBreed=\"falsemyBasBreed\" ");
				
				LOGGER.fine("in command file: " + proj + " gams Fit_StratABM_Cal");
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public List<Double> readMPIncomes(Properties cmd, List<Farm> allFarms) {
		
		List<Double> incomesFromMP = new ArrayList<Double>();				       // list of all agents' incomes produced by the MP
		BufferedReader Buffer = null;	 									       // read input file
		String Line;														       // read each line of the file individually
		ArrayList<String> dataArray;										       // separate data line
				
		File f = new File("projdir\\DataModelIn\\data_FARMIND.gms");					   // actual results file
		while (!f.exists()) {try {
			Thread.sleep(1000);												       // wait until the MP finishes running
		} catch (InterruptedException e) {
			e.printStackTrace();
		}}

		try {
			Buffer = new BufferedReader(new FileReader("projdir\\DataModelIn\\data_FARMIND.gms"));
			
			Line = Buffer.readLine();
			Line = Buffer.readLine();
			while ( ( (Line = Buffer.readLine()) != null ) && Line.matches(".*\\d+.*") ) {    
				dataArray = CSVtoArrayList(Line);						          // Read farm's parameters line by line
				incomesFromMP.add( Double.parseDouble(dataArray.get(1)) );		
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}									       

		try {
			Buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return incomesFromMP;
	}
	
	@Override
	public List<ArrayList<Activity>> readMPActivities(Properties cmd, List<Farm> allFarms) {
		List<ArrayList<Activity>> activitiesFromMP = new ArrayList<ArrayList<Activity>>();   // list of all agents' final activities selected by the MP
		BufferedReader Buffer = null;	 									       // read input file
		String Line;														       // read each line of the file individually
		ArrayList<String> dataArray;										       // separate data line
		ReadData reader = new ReadData(cmd);
		List<Activity> allPossibleActivities = reader.getActivityList();		   // generated activity list with ID and name 
		HashMap<String, ArrayList<Activity>> map = new HashMap<String, ArrayList<Activity>>();
		
		File f = new File("projdir\\DataModelIn\\data_FARMINDLandData.gms");	   // actual results file
		while (!f.exists()) {try {
			Thread.sleep(1000);												       // wait until the MP finishes running
		} catch (InterruptedException e) {
			e.printStackTrace();
		}}

		try {
			Buffer = new BufferedReader(new FileReader("projdir\\DataModelIn\\data_FARMINDLandData.gms"));
			
			Line = Buffer.readLine();
			Line = Buffer.readLine();
			while (( (Line = Buffer.readLine()) != null ) && Line.matches(".*\\d+.*")) {                       
				dataArray = CSVtoArrayList(Line);						          // file has 'farm','activity' on each row.
				
				ArrayList<Activity> farmActivityList = new ArrayList<Activity>();
				if (map.get(dataArray.get(0)) != null) {
					farmActivityList = map.get(dataArray.get(0));              // if farm already in map, reuse so we can add next activity
				}
								
				for(int i = 0; i < allPossibleActivities.size(); i++) { 
					String name = dataArray.get(1);
					if (allPossibleActivities.get(i).getName().equals(name) ) {
						int ID = allPossibleActivities.get(i).getID();
						Activity p = new Activity(ID, name); 
						farmActivityList.add(p);
					}
				}
				
				map.put(dataArray.get(0), farmActivityList);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}									       

		try {
			Buffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// convert map to ordered list
		for (Farm farm:allFarms) {	
			if (map.get(farm.getFarmName()) != null) {
				activitiesFromMP.add(map.get(farm.getFarmName()));
			} else {
				activitiesFromMP.add( getExitActivity() );
			}
		}

		return activitiesFromMP;
	}

	@Override
	public ArrayList<Activity> getExitActivity() {
		ArrayList<Activity> activities = new ArrayList<Activity>();	   	 	       // list of all farm activities selected by MP model
		Activity exit = new Activity(0,"exit_activity");
		activities.add(exit);
	
		return activities;
	}

	/**
	 * edit the MP gams script with the updated year and price information
	 * @param nFarm: number of farms
	 * @param year: which year in iteration so we can select the proper price information
	 */
    private void editMPscript(List<String> possibleActivity) {	
		try {
            BufferedReader oldScript = new BufferedReader(new FileReader( gamsModelFile));
            String line;
            String script = "";
            String act = possibleActivity.get(0);
            for (int i = 1; i < possibleActivity.size(); i++) {
            	act = act + "," + possibleActivity.get(i);
            }
            
            System.out.println(act);
            
            while ((line = oldScript.readLine()) != null) {
            	
                // Edit line in the gams script with the number of agents 
                if (line.contains("'Single farm run'.'Farm branches'")) {
                    line = String.format("'Single farm run'.'Farm branches' '%S' ", act);
                }
                script += line + '\n';
            }
            
            oldScript.close();
            FileOutputStream newScript = new FileOutputStream(gamsModelFile);
            newScript.write(script.getBytes());
            newScript.close();
        }
		
        catch (IOException ioe) {
        	ioe.printStackTrace();
        }
    	    	
	}
    
	/**
	 * This function converts data from CSV file into array structure 
	 * @param CSV String from input CSV file to break into array
	 * @return Result ArrayList of strings 
	 */
	private static ArrayList<String> CSVtoArrayList(String CSV) {		       
		ArrayList<String> Result = new ArrayList<String>();
		
		if (CSV != null) {
			String[] splitData = CSV.split("\\s*,\\s*");
			for (int i = 0; i < splitData.length; i++) {
				if (!(splitData[i] == null) || !(splitData[i].length() == 0)) {
					Result.add(splitData[i].trim());
				}
			}
		}
		return Result;
	}
}
