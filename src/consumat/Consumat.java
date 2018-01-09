package consumat;

import java.util.List;

import agent.farm.Farm;
import decision.DecisionResult;
import output.BatchOutput;
import product.Product;
import reader.ReadParameters;
import transactioncost.TDMatrix;

public class Consumat {

	public static void main(String[] args) {
		
		ReadParameters reader = new ReadParameters();
		
		TDMatrix matrix = reader.getTDMatrix();
		
		int cost = matrix.getDistance("Wheat", "Maize");
		
		System.out.println(cost);
		
		//List<Crop> crops = reader.getCropList();
		//List<Livestock> livestock = reader.getLivestockList();
		
		List<Farm> farms = reader.getFarms();
		
		for ( int i = 0; i < farms.size(); i++) {

			List<Product> p = farms.get(i).getAction(farms);
			String id = farms.get(i).getFarmId();

			DecisionResult decision = new DecisionResult(id, p);
			
			BatchOutput batch = new BatchOutput(decision);
			batch.write();
		}
	}
}
