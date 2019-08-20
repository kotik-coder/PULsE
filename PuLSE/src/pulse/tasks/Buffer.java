package pulse.tasks;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.math.IndexedVector;
import pulse.search.math.Vector;

public class Buffer {
	
	private IndexedVector[] data;
	private double[] sumOfSquares;
	private static int defaultSize = (int)NumericProperty.BUFFER_SIZE.getValue();
	
	public Buffer() {
		this(defaultSize);
	}
	
	public Buffer(int size) {
		this.data = new IndexedVector[size];
		sumOfSquares = new double[size];
	}
	
	public Vector[] getData() {
		return data;
	}

	public void fill(SearchTask t, int bufferElement) {		
		sumOfSquares[bufferElement] = (double)t.getSumOfSquares().getValue();
		data[bufferElement] = t.objectiveFunction();		
	}

	public boolean isErrorHigh(double errorTolerance) {				
		double[] e = new double[data[0].dimension()];
		NumericPropertyKeyword index;
		for(int i = 0; i < e.length; i++) {
			index = data[0].getIndex(i);
			e[i] = standardDeviation(index)/Math.abs(average(index));			
			if(e[i] > errorTolerance)
				return true;
		}
		
		return false;			
	}
	
	public double average(NumericPropertyKeyword index) {
		
		double av = 0;
		
		for(IndexedVector v : data)
			for(NumericPropertyKeyword i : v.getIndices())
				if(i.equals(index))
					av += v.get(i);
		
		return av/data.length;
						
	}
	
	public double averageSumOfSquares() {
		
		double av = 0;
		
		for(double ss : sumOfSquares)				
			av += ss;
		
		return av/data.length;
						
	}	
	
	protected double standardDeviation(NumericPropertyKeyword index) {
		double sd = 0;
		double av = average(index);
		
		for(IndexedVector v : data) 
			sd += Math.pow(v.get(index) - av, 2);
		
		return Math.sqrt(sd/(data.length - 1));
		
	}
	
	public NumericProperty getSize() {
		return new NumericProperty(data.length, NumericProperty.BUFFER_SIZE);
	}
	
	public void clear() {
		data = new IndexedVector[data.length];
		sumOfSquares = new double[sumOfSquares.length];
	}
	
	public void setSize(NumericProperty newSize) {
		int size = (int)newSize.getValue();
		this.data = new IndexedVector[size];
	}
	
}