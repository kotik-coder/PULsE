package pulse.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pulse.util.DataEntry;

/**
 * An {@code InterpolationDataset} stores data in a {@code List} of {@code DataEntry<Double,Double>} objects (each containing a 'key' and a 'value')
 * and provides means to interpolate between the 'values' using the 'keys'. This is used mainly to interpolate between
 * available data for thermal properties loaded in tabular representation, e.g. the density and specific heat tables. 
 */

public class InterpolationDataset {

	private List<DataEntry<Double,Double>> dataset;

	/**
	 * Creates an empty {@code InterpolationDataset}. 
	 */
	
	public InterpolationDataset() {				
		dataset = new ArrayList<DataEntry<Double,Double>>();
	}
	
	/**
	 * Iterates over the {@code List} of {@code DataEntry} objects to find one 
	 * that has the closest {@code getKey() < key} value to the argument {@code key}.
	 * @param key the key, which is the upper bound for the search.
	 * @return a {@code DataEntry} object, satisfying the conditions above.
	 */
	
	public DataEntry<Double,Double> previousTo(double key) {
		DataEntry<Double,Double> entry = null;
		DataEntry<Double,Double> next = null;
		for(Iterator<DataEntry<Double,Double>> it = dataset.iterator(); it.hasNext(); ) {
			next = it.next();
			if(key > next.getKey()) 
				entry = next;
			else 
				break;				
		}
		return entry;
	}
	
	/**
	 * Provides an interpolated value at {@code key} based on the available data in the {@code DataEntry List}.
	 * <p> The interpolation is linear, i.e. {@code result = k*key + b}, where {@code k} and {@code b} are the 
	 * parameters of the linear function calculated based on the value of the adjacent {@code DataEntry} objects.
	 * The adjacent {@code DataEntries} are calculated using the {@code previousTo} method. When found,
	 * a simple set of equations (<math>2x2</math>) is solved to calculate the {@code k} and {@code b} values,
	 * which are then substituted in the linear equation.</p>  
	 * @param key the argument, at which interpolation needs to be done (e.g. temperature)
	 * @return a double, representing the interpolated value
	 * @see previousTo
	 */
	
	public double interpolateAt(double key) {
		DataEntry<Double,Double> entry = previousTo(key);
		DataEntry<Double,Double> next = dataset.get(dataset.indexOf(entry)+1);
		
		double k = ( next.getValue() - entry.getValue() ) /
				   ( next.getKey() - entry.getKey() );
		
		double b = ( entry.getValue()*next.getKey() - next.getValue()*entry.getKey() ) /
				   ( next.getKey() - entry.getKey() );
		
		return k*key + b;
		
	}
	
	/**
	 * Adds {@code entry} to this {@code InterpolationDataset}.
	 * @param entry the entry to be added
	 */

	public void add(DataEntry<Double,Double> entry) {
		dataset.add(entry);
	}
	
	/**
	 * Extracts all data available in this {@code InterpolationDataset}.
	 * @return the {@code List} of data.
	 */
	
	public List<DataEntry<Double,Double>> getData() {
		return dataset;			
	}
	
}
