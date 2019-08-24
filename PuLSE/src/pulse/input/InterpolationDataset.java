package pulse.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pulse.util.DataEntry;

public class InterpolationDataset {

	private List<DataEntry<Double,Double>> dataset;

	public InterpolationDataset() {				
		dataset = new ArrayList<DataEntry<Double,Double>>();
	}
	
	public DataEntry<Double,Double> previousTo(double temperature) {
		DataEntry<Double,Double> entry = null;
		DataEntry<Double,Double> next = null;
		for(Iterator<DataEntry<Double,Double>> it = dataset.iterator(); it.hasNext(); ) {
			next = it.next();
			if(temperature > next.getKey()) 
				entry = next;
			else 
				break;				
		}
		return entry;
	}
	
	public double interpolateAt(double temperature) {
		DataEntry<Double,Double> entry = previousTo(temperature);
		DataEntry<Double,Double> next = dataset.get(dataset.indexOf(entry)+1);
		
		double k = ( next.getValue() - entry.getValue() ) /
				   ( next.getKey() - entry.getKey() );
		
		double b = ( entry.getValue()*next.getKey() - next.getValue()*entry.getKey() ) /
				   ( next.getKey() - entry.getKey() );
		
		return k*temperature + b;
		
	}

	public void add(DataEntry<Double,Double> entry) {
		dataset.add(entry);
	}
	
	public List<DataEntry<Double,Double>> getData() {
		return dataset;			
	}
	
}
