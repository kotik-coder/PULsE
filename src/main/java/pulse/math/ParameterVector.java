package pulse.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.math.linear.Vector;
import pulse.math.transforms.Transformable;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
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
     * Constructs an {@code IndexedVector} based on {@code v} and a list of
     * keyword {@code indices}
     *
     * @param v the vector to be copied
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

    /**
     * Copy constructor
     *
     * @param v another vector
     */
    public ParameterVector(ParameterVector v) {
        this(v.dimension());
        final int n = dimension();
        for (int i = 0; i < n; i++) {
            this.set(i, v.get(i));
        }
        System.arraycopy(v.indices, 0, indices, 0, n);
        System.arraycopy(v.transforms, 0, transforms, 0, n);
        System.arraycopy(v.bounds, 0, bounds, 0, n);
    }

    /**
     * Creates an empty ParameterVector with a dimension of {@code n}
     *
     * @param n dimension
     */
    private ParameterVector(final int n) {
        super(n);
        indices = new NumericPropertyKeyword[n];
        transforms = new Transformable[n];
        bounds = new Segment[n];
    }

    @Override
    public void set(final int i, final double x) {
        set(i, x, false);
    }
    
    /**
     * Sets the <i>i</i>-th parameter value to {@code x} without applying the 
     * transform. Sets the bound for this value as the default bound for {@code key}. 
     * @param i the index of the parameter
     * @param x value to be set
     * @param key type of property
     */
    
    public void set(final int i, final double x, NumericPropertyKeyword key) {
        set(i, x);
        setParameterBounds(i, Segment.boundsFrom(key));
    }

    /**
     * Sets the <math><i>i</i></math>-component of this vector to {@code x} or
     * its corresponding transform, if the latter is defined and
     * {@code ignoreTransform} is {@code false}.
     *
     * @param i index of the value and its transform
     * @param x the non-transformed value, which needs to be assigned to the
     * i-th component
     * @param ignoreTransform if {@code} false, will ignore exiting transform.
     */
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

    /**
     * Performs an inverse transform corresponding to the index {@code i} of
     * this vector.
     *
     * @param i the index of the transform
     * @return the inverse transform of {@code get(i) } if the transform is
     * defined, {@code get(i)} otherwise.
     */
    public double inverseTransform(final int i) {
        return transforms[i] != null ? transforms[i].inverse(get(i)) : get(i);
    }

    /**
     * Gets the transformable of the i-th component
     *
     * @param i index of the component
     * @return the corresponding {@code Transforamble}
     */
    public Transformable getTransform(final int i) {
        return transforms[i];
    }

    public void setTransform(final int i, Transformable transformable) {
        transforms[i] = transformable;
    }

    public Segment getParameterBounds(final int i) {
        return bounds[i];
    }

    /**
     * If transform of {@code i} is not null, applies the transformation to the
     * component bounds
     *
     * @param i the index of the component
     * @return the transformed bounds
     */
    public Segment getTransformedBounds(final int i) {
        return transforms[i] != null
                ? new Segment(transforms[i].transform(bounds[i].getMinimum()),
                        transforms[i].transform(bounds[i].getMaximum()))
                : getParameterBounds(i);
    }

    /**
     * Sets the bounds of i-th component of this vector.
     *
     * @param i the index of the component
     * @param segment new parameter bounds
     */
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

    /**
     * This will assign a new list of indices to this vector
     *
     * @param indices a list of indices
     */
    private void assign(List<NumericPropertyKeyword> indices) {
        this.indices = indices.toArray(new NumericPropertyKeyword[indices.size()]);
        bounds = new Segment[this.indices.length];
        transforms = new Transformable[this.indices.length];
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Indices: ");
        for (var key : indices) {
            sb.append(key).append(" ; ");
        }
        sb.append(System.lineSeparator());
        sb.append(" Values: ").append(super.toString());
        return sb.toString();
    }
    
    /**
     * Finds any elements of this vector which do not pass sanity checks.
     * @return a list of malformed numeric properties
     * @see pulse.properties.NumericProperties.isValueSensible()
     */

    public List<NumericProperty> findMalformedElements() {
        var list = new ArrayList<NumericProperty>();
        
        for (int i = 0; i < dimension(); i++) {
            var property = NumericProperties.derive(getIndex(i), inverseTransform(i));
            if (!property.validate()) {
                list.add(property);
            }
        }
        
        return list;
    }

    public Segment[] getBounds() {
        return bounds;
    }

}
