package pulse.tasks;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.util.PropertyHolder;

/**
 * A {@code Buffer} is used to estimate the convergence of the 
 * reverse problem solution, by comparing the variance of the properties to
 * a pre-specified error tolerance.
 * @see pulse.tasks.SearchTask.run()
 */

public class Buffer extends PropertyHolder {
	
	private IndexedVector[] data;
	private double[] statistic;
	private static int size = (int)NumericProperty.theDefault(NumericPropertyKeyword.BUFFER_SIZE).getValue();	
	
	/**
	 * Creates a {@code Buffer} with a default size.
	 */
	
	public Buffer() {
		init();
	}
	
	/**
	 * Retrieves the contents of this {@code Buffer}.
	 * @return the data
	 */
	
	public IndexedVector[] getData() {
		return data;
	}
		
	public void init() {
		this.data	= new IndexedVector[size];
		statistic	= new double[size];
	}
	
	/**
	 * (Over)writes a buffer cell corresponding to the {@code bufferElement} with
	 * the current set of parameters of {@code SearchTask}. 
	 * @param t the {@code SearchTask} 
	 * @param bufferElement the {@code bufferElement} which will be written over
	 */

	public void fill(SearchTask t, int bufferElement) {		
		statistic[bufferElement]	= (double)t.getResidualStatistic().getStatistic().getValue();
		data[bufferElement]			= t.searchVector()[0];		
	}
	
	/**
	 * Determines whether the relative error (variance divided by mean)
	 * for any of the properties in this buffer is higher than the 
	 * expect {@code errorTolerance}.
	 * @param errorTolerance the maximum tolerated relative error.
	 * @return {@code true} if convergence has not been reached.
	 */

	public boolean isErrorTooHigh(double errorTolerance) {				
		double[] e = new double[data[0].dimension()];
		NumericPropertyKeyword index;
		
		final double eSq = errorTolerance*errorTolerance;
		
		for(int i = 0; i < e.length; i++) {
			index	= data[0].getIndex(i);
			e[i]	= variance(index)/Math.pow(average(index), 2);			
			
			if(e[i] > eSq)
				return true;
			
		}
		
		return false;
		
	}
	
	/**
	 * Calculates the average for the {@code index} -- if the respective
	 * {@code NumericProperty} is contained in the {@code IndexedVector} data
	 * of this {@code Buffer}.
	 * @param index a symbolic index (keyword)
	 * @return the mean of the data sample for the specific type of {@code NumericPropert}ies
	 */
	
	public double average(NumericPropertyKeyword index) {

		double av = 0;
		
		for(IndexedVector v : data) 
			av += v.get(index);						
		
		return av/data.length;
						
	}
	
	/**
	 * Calculated the average statistic value
	 * @return the mean statistic value.
	 */
	
	public double averageStatistic() {
		
		double av = 0;
		
		for(double ss : statistic)				
			av += ss;
		
		return av/data.length;
						
	}	

	/**
	 * Calculates the variance for the {@code index} -- if the respective
	 * {@code NumericProperty} is contained in the {@code IndexedVector} data
	 * of this {@code Buffer}.
	 * @param index a symbolic index (keyword).
	 * @return the variance of the data sample for the specific type of {@code NumericPropert}ies.
	 */
	
	public double variance(NumericPropertyKeyword index) {		
		double sd = 0;
		double av = average(index);
		
		for(IndexedVector v : data) 
			sd += Math.pow(v.get(index) - av, 2);
		
		return sd/data.length;
		
	}
	
	/**
	 * Gets the buffer size (a NumericProperty derived from {@code BUFFER_SIZE}.
	 * @return the buffer size property
	 * @see pulse.properties.NumericPropertyKeyword
	 */
	
	public static NumericProperty getSize() {
		return NumericProperty.derive(NumericPropertyKeyword.BUFFER_SIZE, size);
	}
	
	/**
	 * Clears the data arrays.
	 */
	
	public void clear() {
		data	= new IndexedVector[data.length];
		statistic		= new double[statistic.length];
	}
	
	/**
	 * Sets a new size for this {@code Buffer}.
	 * @param newSize a {@code NumericProperty} of the type {@code BUFFER_SIZE}.
	 */
	
	public static void setSize(NumericProperty newSize) {
		if(newSize.getType() != NumericPropertyKeyword.BUFFER_SIZE)
			throw new IllegalArgumentException("Illegal argument type: " + newSize.getType());
		Buffer.size = ( (Number)newSize.getValue() ).intValue();				
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == NumericPropertyKeyword.BUFFER_SIZE)
			setSize(property);
	}
	
	/**
	 * The {@code BUFFER_SIZE} is the single listed parameter for this class.
	 * @see pulse.properties.NumericPropertyKeyword 
	 */
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.BUFFER_SIZE));
		return list;
	}	
	
}