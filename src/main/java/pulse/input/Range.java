package pulse.input;

import static java.lang.Math.max;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.Optimisable;
import pulse.util.PropertyHolder;

/**
 * The actual physical range defined as a {@code Segment} with minimum and
 * maximum values. This is used purely in context of the time sequence defined
 * by the {@code ExperimentalData}.
 *
 */
public class Range extends PropertyHolder implements Optimisable {

    private Segment segment;

    /**
     * Constructs a {@code Range} from the minimum and maximum values of
     * {@code data}.
     *
     * @param data a list of double values
     */
    public Range(List<Double> data) {
        double min = data.stream().reduce((a, b) -> a < b ? a : b).get();
        double max = data.stream().reduce((a, b) -> b > a ? b : a).get();
        segment = new Segment(min, max);
    }

    /**
     * Constructs a new {@code Range} based on the segment specified by
     * {@code a} and {@code b}
     *
     * @param a a double value
     * @param b another double value
     */
    public Range(double a, double b) {
        this.segment = new Segment(a, b);
    }

    /**
     * Resets the minimum and maximum values of this range to those specified by
     * the elements of {@code data}, the indices of which correspond to the
     * lower and upper bound of the {@code IndexRange}.
     *
     * @param range an object specifying the start/end indices in regard to the
     * {@code data} list
     * @param data a list of double values (usually, a time sequence)
     */
    public void reset(IndexRange range, List<Double> data) {
        segment.setMaximum(data.get(range.getUpperBound()));
        segment.setMinimum(data.get(range.getLowerBound()));
    }

    /**
     * Gets the numeric property defining the lower bound of this range.
     *
     * @return the lower bound (usually referring to a time sequence).
     */
    public NumericProperty getLowerBound() {
        return derive(LOWER_BOUND, segment.getMinimum());
    }

    /**
     * Gets the numeric property defining the upper bound of this range.
     *
     * @return the upper bound (usually referring to a time sequence).
     */
    public NumericProperty getUpperBound() {
        return derive(UPPER_BOUND, segment.getMaximum());
    }

    /**
     * Sets the lower bound and triggers {@code firePropertyChanged}.
     *
     * @param p a numeric property with the required {@code LOWER_BOUND} type.
     */
    public void setLowerBound(NumericProperty p) {
        requireType(p, LOWER_BOUND);
        
        if( boundLimits(false).contains( ((Number)p.getValue()).doubleValue())) {
            segment.setMinimum((double) p.getValue());
            firePropertyChanged(this, p);
        }
        
    }

    /**
     * Sets the upper bound and triggers {@code firePropertyChanged}.
     *
     * @param p a numeric property with the required {@code UPPER_BOUND} type.
     */
    public void setUpperBound(NumericProperty p) {
        requireType(p, UPPER_BOUND);
        
        if( boundLimits(true).contains( ((Number)p.getValue()).doubleValue()) ) {
            segment.setMaximum((double) p.getValue());
            firePropertyChanged(this, p);
        }
        
    }

    /**
     * Gets the segment representing this range
     *
     * @return a segment
     */
    public Segment getSegment() {
        return segment;
    }

    /**
     * Updates the lower bound of this range using the information contained in
     * {@code p}. Since this is not fail-safe, the method has been made
     * protected.
     *
     * @param p a {@code NumericProperty} representing the laser pulse width.
     */
    protected void updateMinimum(NumericProperty p) {
        if (p == null) {
            return;
        }

        requireType(p, PULSE_WIDTH);
        double pulseWidth = (double) p.getValue();
        segment.setMinimum(max(segment.getMinimum(), pulseWidth));

    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        switch (type) {
            case LOWER_BOUND:
                setLowerBound(property);
                break;
            case UPPER_BOUND:
                setUpperBound(property);
                break;
            default:
                // do nothing
                break;
        }
    }

    /**
     * Lists lower and upper bounds as properties.
     *
     * @see PropertyHolder
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(LOWER_BOUND);
        set.add(UPPER_BOUND);
        return set;
    }    
    
    /**
     * Calculates the allowed range for either the upper or lower bound.
     * @param isUpperBound if {@code true}, will calculate the range for the upper bound, otherwise -- for the lower one.,
     * @return a Segment range of limits for the specific bound
     */
    
    public Segment boundLimits(boolean isUpperBound) {
        
        var curve = (ExperimentalData) this.getParent();
        var seq = curve.getTimeSequence();
        double tHalf = curve.getHalfTime();
        
        Segment result = null;
        if(isUpperBound)
            result = new Segment(2.5 * tHalf, seq.get(seq.size() - 1));
        else
            result = new Segment( Math.max(-0.15 * tHalf, seq.get(0)), 0.75 * tHalf);
        
        return result;
    }

    /*
	 * TODO put relative bounds in a constant field Consider creating a Bounds
	 * class, or putting them in the XML file
     */
    /**
     * The optimisation vector contain both the lower and upper bounds with the
     * absolute constraints equal to a fourth of their values.
     *
     * @param output the vector to be updated
     * @param flags a list of active flags
     */
    @Override
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        
        Segment bounds;
        
        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            switch (key) {
                case UPPER_BOUND:
                    output.set(i, segment.getMaximum());
                    bounds = boundLimits(true);
                    break;
                case LOWER_BOUND:
                    output.set(i, segment.getMinimum());
                    bounds = boundLimits(false);
                    break;
                default:
                    continue;
            }
            
            var transform = new StickTransform(bounds);

            output.setParameterBounds(i, bounds);
            output.setTransform(i, transform);

        }

    }

    /**
     * Tries to assign the upper and lower bound based on {@code params}.
     *
     * @param params an {@code IndexedVector} which may contain the bounds.
     * @throws SolverException
     */
    @Override
    public void assign(ParameterVector params) throws SolverException {
        NumericProperty p = null;

        for (int i = 0, size = params.dimension(); i < size; i++) {

            p = derive( params.getIndex(i), params.inverseTransform(i) );

            switch (params.getIndex(i)) {
                case UPPER_BOUND:
                    setUpperBound(p);
                    break;
                case LOWER_BOUND:
                    setLowerBound(p);
                    break;
                default:
            }

        }

    }

    @Override
    public String toString() {
        return "Range given by: " + segment.toString();
    }

}
