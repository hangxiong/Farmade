package reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * Contains a name and ID list for all products with a matching hashmap for each individual farm with the matching products
 * row 1: Vector of product names
 * row 2: Farm1: [values for each product] vector of values associated with farm and product
 * row 3: Farm2: [values for each product] 
 * Each farm contains a copy of this list
 * These values for could be product preference, years of experience, or a different metric
 * @author kellerke
 */

public class FarmProductMatrix {
	private List<String> productName = new ArrayList<String>();
	private Map<String,Integer[]> productMap = new HashMap<String,Integer[]>();

	/** 
	 * Given a farmID and a product, return the value of that matrix cell
	 * 
	 * @param FarmID of the specific farm
	 * @param Product name to get value of from matrix
	 * @return value of that cell in the matrix
	 */
	public int getFarmProductValue(String FarmID, String Product) {
		int val;
		int index = productName.indexOf(Product);
		val = productMap.get(FarmID)[index];	 							   // get(FarmID) returns int array
		return val;
	}
	
	/** 
	 * Given a farmID and a product, set the value of that cell
	 * 
	 * @param FarmID of the specific farm
	 * @param Product name 
	 * @param value to set for farm and product combination
	 */
	public void setFarmProductValue(String FarmID, String Product, int value) {
		int index = productName.indexOf(Product);
		Integer[] array = productMap.get(FarmID);
		array[index] = value;
		
		productMap.replace(FarmID, array);
	}
	
	public List<String> getProductName() {
		return productName;
	}

	public void setProductName(List<String> productName) {
		this.productName = productName;
	}
	
	public Map<String,Integer[]> getProductmap() {
		return productMap;
	}
	
	/**
	 * Create the actual product cost lists
	 * @param matrixRow is an array list starting with a farm name and continuing with integer costs
	 */
	public void setProductMap(ArrayList<String> matrixRow) {
		String name = matrixRow.get(0);
		Integer[] values = new Integer[matrixRow.size() - 1];
		
		for (int i = 1; i < matrixRow.size(); i++) {
			values[i-1] = Integer.valueOf(matrixRow.get(i));
		}
		
		this.productMap.put(name, values);
	}

	public void setProductMap(Map<String, Integer[]> localProductMap) {
		this.productMap = localProductMap;
	}
	
}
