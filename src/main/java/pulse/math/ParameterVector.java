package pulse.math;

import java.util.Arrays;
import java.util.List;

import pulse.math.linear.Vector;
import pulse.math.transforms.Transformable;
import pulse.properties.NumericPropertyKeyword;

/**
 * A wrapper subclass that assigns {@code NumericPropertyKeyword}s to specific
 * components of the vector. Used when constructing the optimisation vector.
 */

public class ParameterVector extends Vector {

	private NumericPropertyKeyword[] indices;
	private Transformable[] transforms;
	private Segment[] bounds;
	
	/**
	 * Constructs an {@code IndexedVector} with the specified list of keywords.
	 * 
	 * @param indices a list of keywords
	 */

	public ParameterVector(List<NumericPropertyKeyword> indices) {
		this(indices.size());
		assign(indices);
	}

	/**
	 * Constructs an {@code IndexedVector} based on {@code v} and a list of keyword
	 * {@code indices}
	 * 
	 * @param v       the vector to be copied
	 * @param prototype the prototype of the parameter vector 
	 */

	public ParameterVector(ParameterVector proto, Vector v) {
		super(v);
		this.indices = new NumericPropertyKeyword[proto.indices.length];
		System.arraycopy(proto.indices, 0, this.indices, 0, proto.indices.length);
		this.bounds = new Segment[proto.bounds.length];
		System.arraycopy(proto.bounds, 0, this.bounds, 0, proto.bounds.length);
		this.transforms = new Transformable[proto.transforms.length];
		System.arraycopy(proto.transforms, 0, this.transforms, 0, proto.transforms.length);
		
	}
	
	public ParameterVector(ParameterVector v) {
		this( v.dimension() );
		final int n = dimension();
		for(int i = 0; i < n; i++)
			this.set(i, v.get(i));
		System.arraycopy(v.indices, 0, indices, 0, n);
		System.arraycopy(v.transforms, 0, transforms, 0, n);
		System.arraycopy(v.bounds, 0, bounds, 0, n);
	}

	private ParameterVector(final int n) {
		super(n);
		indices = new NumericPropertyKeyword[n];
		transforms = new Transformable[n];
		bounds = new Segment[n];
	}

	/**
	 * Applies the corresponding transformation (defined by the respective {@code Transformable}) -- if present,
	 * and sets the result of this transformation to the <math>i</math>th component of this {@code ParameterVector}.
	 */
	
	@Override
	public void set(final int i, final double x) {
		set(i, x, false);
	}
	
	public void set(final int i, final double x, boolean ignoreTransform) {
		final double t = ignoreTransform || transforms[i] == null ? x : transforms[i].transform(x);
		super.set(i, t);
	}

	/**
	 * Retrieves the keyword associated with the {@code dataIndex}
	 * 
	 * @param dataIndex an index pointing to a component of this vector
	 * @return a keyword describing this component
	 */

	public NumericPropertyKeyword getIndex(final int dataIndex) {
		return indices[dataIndex];
	}

	/**
	 * Gets the data index that corresponds to the keyword {@code index}
	 * 
	 * @param index a keyword-index of the component
	 * @return a numeric index associated with the original {@code Vector}
	 */

	private int indexOf(NumericPropertyKeyword index) {
		return getIndices().indexOf(index);
	}

	/**
	 * Gets the component at this {@code index}
	 * 
	 * @param index a keyword-index of a component
	 * @return the respective component
	 */

	public double getParameterValue(NumericPropertyKeyword index) {
		return super.get(indexOf(index));
	}
	
	public double inverseTransform(final int i) {
		return transforms[i] != null ? transforms[i].inverse( get(i) ) : get(i);
	}
	
	public Transformable getTransform(final int i) {
		return transforms[i];
	}
	
	public void setTransform(final int i, Transformable transformable) {
		transforms[i] = transformable;
	}
	
	public Segment getParameterBounds(final int i) {
		return bounds[i];
	}
	
	public Segment getTransformedBounds(final int i) {
		return transforms[i] != null ? 
			new Segment( transforms[i].transform( bounds[i].getMinimum() ), 
						 transforms[i].transform( bounds[i].getMaximum() ) ) :
			getParameterBounds(i);
	}
	
	public void setParameterBounds(int i, Segment segment) {
		bounds[i] = segment;
	}

	/**
	 * Gets the full list of indices recognised by this {@code IndexedVector}.
	 * 
	 * @return the full list of {@code NumericPropertyKeyword} indices.
	 */

	public List<NumericPropertyKeyword> getIndices() {
		return Arrays.asList(indices);
	}

	private void assign(List<NumericPropertyKeyword> indices) {
		this.indices = indices.toArray(new NumericPropertyKeyword[indices.size()]);
		bounds = new Segment[this.indices.length];
		transforms = new Transformable[this.indices.length];
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

	public Segment[] getBounds() {
		return bounds;
	}

}