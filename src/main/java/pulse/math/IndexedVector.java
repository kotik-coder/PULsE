package pulse.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.properties.NumericPropertyKeyword;

/**
 * A {@code Vector} with components that have been associated with
 * {@code NumericPropertyKeyword}s.
 */

public class IndexedVector extends Vector {

	private List<NumericPropertyKeyword> indices;
	private double[] prefactors;

	private IndexedVector(int n) {
		super(n);
		prefactors = new double[n];
		Arrays.fill(prefactors, 1.0); // by default, the prefactor is unity
		indices = new ArrayList<>(n);
	}

	/**
	 * Constructs an {@code IndexedVector} with the specified list of keywords.
	 * 
	 * @param indices a list of keywords
	 */

	public IndexedVector(List<NumericPropertyKeyword> indices) {
		this(indices.size());
		assign(indices);
	}

	/**
	 * Constructs an {@code IndexedVector} based on {@code v} and a list of keyword
	 * {@code indices}
	 * 
	 * @param v       the vector to be copied
	 * @param indices a list of keyword
	 */

	public IndexedVector(Vector v, List<NumericPropertyKeyword> indices) {
		super(v);
		this.indices = indices;
	}

	/**
	 * Finds the component of this vector that corresponds to {@code index} and sets
	 * its value to {@code x}
	 * 
	 * @param index the keyword associated with a component of this
	 *              {@code IndexedVector}
	 * @param x     the new value of this component
	 */

	public void set(NumericPropertyKeyword index, double x) {
		set(index, x, 1.0);
	}

	public void set(NumericPropertyKeyword index, double x, double prefactor) {
		int i = getDataIndex(index);
		super.set(i, x);
		prefactors[i] = prefactor;
	}

	public double getRawValue(int i) {
		return super.get(i) / prefactors[i];
	}

	public double getRawValue(NumericPropertyKeyword index) {
		return getRawValue(getDataIndex(index));
	}

	public void set(int i, double x, double prefactor) {
		super.set(i, x);
		prefactors[i] = prefactor;
	}

	public List<NumericPropertyKeyword> getIndices() {
		return indices;
	}

	/**
	 * Retrieves the keyword associated with the {@code dataIndex}
	 * 
	 * @param dataIndex an index pointing to a component of this vector
	 * @return a keyword describing this component
	 */

	public NumericPropertyKeyword getIndex(int dataIndex) {
		return indices.get(dataIndex);
	}

	/**
	 * Gets the data index that corresponds to the keyword {@code index}
	 * 
	 * @param index a keyword-index of the component
	 * @return a numeric index associated with the original {@code Vector}
	 */

	public int getDataIndex(NumericPropertyKeyword index) {
		return indices.indexOf(index);
	}

	/**
	 * Gets the component at this {@code index}
	 * 
	 * @param index a keyword-index of a component
	 * @return the respective component
	 */

	public double get(NumericPropertyKeyword index) {
		return super.get(getDataIndex(index));
	}

	public double getPrefactor(NumericPropertyKeyword index) {
		return prefactors[getDataIndex(index)];
	}

	private void assign(List<NumericPropertyKeyword> indices) {
		this.indices.addAll(indices);
	}

	public static IndexedVector concat(IndexedVector v1, IndexedVector v2) {
		List<NumericPropertyKeyword> allIndices = new ArrayList<>(v1.indices);
		allIndices.addAll(v2.indices);
		return new IndexedVector(allIndices);
	}

}