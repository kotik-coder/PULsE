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
	private double[] ssr;
	
	/**
	 * The default size of this buffer, as specified in the {@code .xml} defining
	 * other default {@code NumericPropert}ies.
	 */
	
	public final static int DEFAULT_SIZE = (int)NumericProperty.
			def(NumericPropertyKeyword.BUFFER_SIZE).getValue();	
	
	/**
	 * Creates a {@code Buffer} with a default size.
	 */
	
	public Buffer() {
		this(DEFAULT_SIZE);
	}
	
	/**
	 * Creates a {@code Buffer} with size {@code n}. 
	 * @param size the size of the buffer.
	 */
	
	public Buffer(int size) {
		this.data	= new IndexedVector[size];
		ssr			= new double[size];
	}
	
	/**
	 * Retrieves the contents of this {@code Buffer}.
	 * @return the data
	 */
	
	public IndexedVector[] getData() {
		return data;
	}
	
	/**
	 * (Over)writes a buffer cell corresponding to the {@code bufferElement} with
	 * the current set of parameters of {@code SearchTask}. 
	 * @param t the {@code SearchTask} 
	 * @param bufferElement the {@code bufferElement} which will be written over
	 */

	public void fill(SearchTask t, int bufferElement) {		
		ssr[bufferElement] 	= (double)t.getSumOfSquares().getValue();
		data[bufferElement]	= t.searchVector();		
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
	 * Calculated the average sum of squared residuals (SSR).
	 * @return the mean SSR value
	 */
	
	public double averageSSR() {
		
		double av = 0;
		
		for(double ss : ssr)				
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
	
	public NumericProperty getSize() {
		return NumericProperty.derive(NumericPropertyKeyword.BUFFER_SIZE, data.length);
	}
	
	/**
	 * Clears the data arrays.
	 */
	
	public void clear() {
		data	= new IndexedVector[data.length];
		ssr		= new double[ssr.length];
	}
	
	/**
	 * Sets a new size for this {@code Buffer}.
	 * @param newSize a {@code NumericProperty} of the type {@code BUFFER_SIZE}.
	 */
	
	public void setSize(NumericProperty newSize) {
		int size	= (int)newSize.getValue();
		this.data	= new IndexedVector[size];
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