package pulse.math;

import java.util.ArrayList;
import java.util.List;

import pulse.math.linear.Vector;
import pulse.properties.NumericPropertyKeyword;

/**
 * A wrapper subclass that assigns {@code NumericPropertyKeyword}s to specific
 * components of the vector. Used when constructing the optimisation vector.
 */

public class IndexedVector extends Vector {

	private List<NumericPropertyKeyword> indices;

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

	private IndexedVector(final int n) {
		super(n);
		indices = new ArrayList<>(n);
	}

	/**
	 * Finds the component of this vector that corresponds to {@code index} and sets
	 * its value to {@code x}
	 * 
	 * @param index the keyword associated with a component of this
	 *              {@code IndexedVector}
	 * @param x     the new value of this component
	 */

	public void set(NumericPropertyKeyword index, final double x) {
		final int i = indexOf(index);
		super.set(i, x);
	}

	/**
	 * Retrieves the keyword associated with the {@code dataIndex}
	 * 
	 * @param dataIndex an index pointing to a component of this vector
	 * @return a keyword describing this component
	 */

	public NumericPropertyKeyword getIndex(final int dataIndex) {
		return indices.get(dataIndex);
	}

	/**
	 * Gets the data index that corresponds to the keyword {@code index}
	 * 
	 * @param index a keyword-index of the component
	 * @return a numeric index associated with the original {@code Vector}
	 */

	private int indexOf(NumericPropertyKeyword index) {
		return indices.indexOf(index);
	}

	/**
	 * Gets the component at this {@code index}
	 * 
	 * @param index a keyword-index of a component
	 * @return the respective component
	 */

	public double get(NumericPropertyKeyword index) {
		return super.get(indexOf(index));
	}

	/**
	 * Gets the full list of indices recognised by this {@code IndexedVector}.
	 * 
	 * @return the full list of {@code NumericPropertyKeyword} indices.
	 */

	public List<NumericPropertyKeyword> getIndices() {
		return indices;
	}

	private void assign(List<NumericPropertyKeyword> indices) {
		this.indices.addAll(indices);
	}
	
	@Override
	public String toString() {
		var sb  = new StringBuilder();
		sb.append("Indices: ");
		for(var key : indices) {
			sb.append(key + " ; ");
		}
		sb.append(System.lineSeparator());
		sb.append(" Values: " + super.toString());
		return sb.toString();
	}

}