package pulse.tasks;

import java.util.HashMap;
import java.util.Map;

import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.search.math.Index;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.SearchCriterion;
import pulse.search.math.Vector;

public class Buffer {
	
	private Vector[] data;
	private static int defaultSize = (int)NumericProperty.DEFAULT_BUFFER_SIZE.getValue();
	private static Map<Index,Integer> indexMap;
	
	public Buffer() {
		this(defaultSize);
	}
	
	public Buffer(int size) {
		this.data = new Vector[size];
		for(int i = 0; i < size; i++)
			data[i] = new Vector();
	}
	
	public Vector[] getData() {
		return data;
	}

	public void fill(SearchTask t, int bufferElement) {
		
		indexMap 				= new HashMap<Index,Integer>();
		BooleanProperty[] flags = t.getSearchFlags();
		
		int j = -1;
		
		for(int i = 0; i < flags.length; i++) {
		
			if(! (boolean)flags[i].getValue() )
				continue;
	
			indexMap.put(ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()), ++j);
			
		}
		
		indexMap.put(SearchCriterion.R_SQUARED, ++j);
		
		Vector f = t.objectiveFunction();
		Vector v = new Vector(f.dimension() + 1);
		
		for(int i = 0; i < f.dimension(); i++)
			v.set(i, f.get(i));
		
		v.set(v.dimension() - 1, (double)t.getSumOfSquares().getValue());
		
		data[bufferElement]		= v;
			
	}

	public boolean isErrorHigh(double errorTolerance) {
		if(indexMap == null)
			return true;
		
		double[] e = new double[data[0].dimension()];
		for(int i = 0; i < e.length; i++) {
			e[i] = standardDeviation(i)/Math.abs(average(i));
			if(e[i] > errorTolerance)
				return true;
		}
		
		return false;
			
	}
	
	public boolean contains(Index index) {
		return indexMap.get(index) == null ? false : true;
	}
	
	public double average(Index index) {
		
		if(indexMap.isEmpty())
			return 0;
		
		return average(indexMap.get(index));
	
	}
	
	public double average(int index) {
		double av = 0;

		for(Vector v : data) 
			av += v.dimension() > index ? v.get(index) : 0.0;
		
		return av / data.length;
	}
	
	protected double standardDeviation(int index) {
		double sd = 0;
		double av = average(index);
		
		for(Vector v : data) 
			sd += Math.pow(v.get(index) - av, 2);
		
		return Math.sqrt(sd/(data.length - 1));
		
	}
	
	public NumericProperty getSize() {
		return new NumericProperty(data.length, NumericProperty.DEFAULT_BUFFER_SIZE);
	}
	
	public void clear() {
		for(int i = 0; i < data.length; i++)
			data[i] = new Vector();
		indexMap = null;
	}
	
	public void setSize(NumericProperty newSize) {
		int size = (int)newSize.getValue();
		this.data = new Vector[size];
		for(int i = 0; i < size; i++)
			data[i] = new Vector();
	}
	
}