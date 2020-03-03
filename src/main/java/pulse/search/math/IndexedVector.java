package pulse.search.math;

import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * A {@code Vector} with components that have been associated with
 * {@code NumericPropertyKeyword}s.
 */

public class IndexedVector extends Vector {

	private List<NumericPropertyKeyword> indices;
	
	 private IndexedVector(int n) {
		super(n);
		indices = new ArrayList<NumericPropertyKeyword>(n);
	 }
	 
	 /**
	  * Constructs an {@code IndexedVector} with the specified list of keywords.
	  * @param indices a list of keywords
	  */
	 
	 public IndexedVector(List<NumericPropertyKeyword> indices) {
		this(indices.size());
		assign(indices);
	 }
	 
	 /**
	  * Constructs an {@code IndexedVector} based on {@code v}
	  * and a list of keyword {@code indices}
	  * @param v the vector to be copied
	  * @param indices a list of keyword
	  */
	 
	 public IndexedVector(Vector v, List<NumericPropertyKeyword> indices) {
		super(v);
		this.indices = indices;
	 }
	 
	 /**
	  * Finds the component of this vector that corresponds to {@code index}
	  * and sets its value to {@code x}
	  * @param index the keyword associated with a component of this {@code IndexedVector}
	  * @param x the new value of this component
	  */
	 
	 public void set(NumericPropertyKeyword index, double x) {
		 super.set( indices.indexOf(index), x);
	 }
	 
	 public List<NumericPropertyKeyword> getIndices() {
		 return indices;
	 }
	 
	 /**
	  * Retrieves the keyword associated with the {@code dataIndex}
	  * @param dataIndex an index pointing to a component of this vector
	  * @return a keyword describing this component
	  */
	 
	 public NumericPropertyKeyword getIndex(int dataIndex) {
		 return indices.get(dataIndex);
	 }
	 
	 /**
	  * Gets the data index that corresponds to the keyword {@code index}
	  * @param index a keyword-index of the component 
	  * @return a numeric index associated with the original {@code Vector} 
	  */
	 
	 public int getDataIndex(NumericPropertyKeyword index) {
		 return indices.indexOf(index);
	 }
	 
	 /**
	  * Gets the component at this {@code index}
	  * @param index a keyword-index of a component 
	  * @return the respective component
	  */
	 
	 public double get(NumericPropertyKeyword index) {
		 return super.get( getDataIndex(index) );
	 }
	
	 private void assign(List<NumericPropertyKeyword> indices) {
		 this.indices.addAll(indices);
	 }
	 
	 /**
	  * Creates an {@code IndexedVector} with its components set as the maximum sensible (ceiling) 
	  * values that can ever occur for each of the listed {@code activeParameters}.
	  * @param activeParameters a list of keywords which correspond to some default values (as loaded from .xml)
	  * @return an {@code IndexedVector} representing the 'ceiling', all components of which
	  * represent the maximum sensible values for each of the {@code activeParameters}. 
	  */
	 
	public static IndexedVector ceiling(List<NumericPropertyKeyword> activeParameters) {
			IndexedVector v = new IndexedVector(activeParameters);
			double lSq = Math.pow((double)NumericProperty.def(THICKNESS).getValue(), 2);
			
			for(NumericPropertyKeyword activeIndex : activeParameters) { 
			
				switch(activeIndex) {
					case HEAT_LOSS			: v.set(activeIndex, (double) NumericProperty.theDefault(HEAT_LOSS).getMaximum()); break; 
					case DIFFUSIVITY		: v.set(activeIndex, (double) NumericProperty.theDefault(DIFFUSIVITY).getMaximum()/lSq); break;
					case BASELINE_INTERCEPT	: v.set(activeIndex, (double) NumericProperty.theDefault(BASELINE_INTERCEPT).getMaximum()); break;
					case BASELINE_SLOPE		: v.set(activeIndex, (double) NumericProperty.theDefault(BASELINE_SLOPE).getMaximum()); break;
					case MAXTEMP	 		: v.set(activeIndex, (double) NumericProperty.theDefault(MAXTEMP).getMaximum()); break;
					default			 		: throw new IllegalArgumentException("Type " + activeIndex + " unknown");
				}
				
			}
			
			return v;
				
	}
	
	public static IndexedVector concat(IndexedVector v1, IndexedVector v2) {
		List<NumericPropertyKeyword> allIndices = 
				new ArrayList<NumericPropertyKeyword>(v1.indices);
		allIndices.addAll(v2.indices);
		return new IndexedVector( allIndices );
	}
	
}