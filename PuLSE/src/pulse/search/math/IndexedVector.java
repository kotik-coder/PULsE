package pulse.search.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

import static pulse.properties.NumericPropertyKeyword.*;

public class IndexedVector extends Vector {

	private List<NumericPropertyKeyword> indices;
	
	 public IndexedVector(int n) {
		super(n);
		indices = new ArrayList<NumericPropertyKeyword>(n);
	 }
	 
	 public IndexedVector(List<NumericPropertyKeyword> indices) {
		this(indices.size());
		assign(indices);
	 }
	 
	 public IndexedVector(Vector v, List<NumericPropertyKeyword> indices) {
		super(v);
		this.indices = indices;
	 }
	 
	 public void set(NumericPropertyKeyword index, double x) {
		 super.set( indices.indexOf(index), x);
	 }
	 
	 public List<NumericPropertyKeyword> getIndices() {
		 return indices;
	 }
	 
	 public NumericPropertyKeyword getIndex(int dataIndex) {
		 return indices.get(dataIndex);
	 }
	 
	 public int getDataIndex(NumericPropertyKeyword index) {
		 return indices.indexOf(index);
	 }
	 
	 public double get(NumericPropertyKeyword index) {
		 return super.get( indices.indexOf(index));
	 }
	
	 public void assign(List<NumericPropertyKeyword> indices) {
		 this.indices.addAll(indices);
	 }
	 
	public static IndexedVector parameterBoundaries(List<NumericPropertyKeyword> activeParameters) {
			IndexedVector v = new IndexedVector(activeParameters);
			double lSq = Math.pow((double)NumericProperty.def(THICKNESS).getValue(), 2);
			
			for(NumericPropertyKeyword activeIndex : activeParameters) { 
			
				switch(activeIndex) {
					case HEAT_LOSS			: v.set(activeIndex, (double) NumericProperty.def(HEAT_LOSS).getMaximum()); break; 
					case DIFFUSIVITY		: v.set(activeIndex, (double) NumericProperty.def(DIFFUSIVITY).getMaximum()/lSq); break;
					case BASELINE_INTERCEPT	: v.set(activeIndex, (double) NumericProperty.def(BASELINE_INTERCEPT).getMaximum()); break;
					case BASELINE_SLOPE		: v.set(activeIndex, (double) NumericProperty.def(BASELINE_SLOPE).getMaximum()); break;
					case MAXTEMP	 		: v.set(activeIndex, (double) NumericProperty.def(MAXTEMP).getMaximum()); break;
					default			 		: throw new IllegalArgumentException("Type " + activeIndex + " unknown"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
			}
			
			return v;
				
	}
	
	public boolean contains(NumericPropertyKeyword key) {
		return indices.contains(key);
	}	

}