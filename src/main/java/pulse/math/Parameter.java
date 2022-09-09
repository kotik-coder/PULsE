package pulse.math;

import pulse.math.transforms.Transformable;

/**
 * Parameter class
 */
public class Parameter {

    private ParameterIdentifier index;
    private Transformable transform;
    private Segment bound;
    private double value;

    public Parameter(ParameterIdentifier index, Transformable transform, Segment bound) {
        this.index = index;
        this.transform = transform;
        this.bound = bound;
    }
    
    public Parameter(ParameterIdentifier index) {
        if(index.getKeyword() != null) {
            bound = Segment.boundsFrom(index.getKeyword());
        }
        this.index = index;
    }

    public Parameter(Parameter p) {
        this.index = p.index;
        this.transform = p.transform;
        this.bound = p.bound;
        this.value = p.value;
    }

    public ParameterIdentifier getIdentifier() {
        return index;
    }

    public void setBounds(Segment bounds) {
        this.bound = bounds;
    }

    public Segment getBounds() {
        return bound;
    }

    /**
     * If transform of {@code i} is not null, applies the transformation to the
     * component bounds
     *
     * @param i the index of the component
     * @return the transformed bounds
     */
    public Segment getTransformedBounds() {
        return transform != null
                ? new Segment(transform.transform(bound.getMinimum()),
                        transform.transform(bound.getMaximum()))
                : bound;
    }

    public Transformable getTransform() {
        return transform;
    }

    public void setTransform(Transformable transform) {
        this.transform = transform;
    }

    public double inverseTransform() {
        return transform != null ? transform.inverse(value) : value;
    }

    public Parameter copy() {
        return new Parameter(index, transform, bound);
    }

    public double getApparentValue() {
        return value;
    }

    public void setValue(double value, boolean ignoreTransform) {
        this.value = transform == null || ignoreTransform
                   ? value
                   : transform.transform(value);
    }

    public void setValue(double value) {
        setValue(value, false);
    }

}
