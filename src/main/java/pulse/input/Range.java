package pulse.input;

import static java.lang.Math.max;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.math.Segment;
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
	 * Constructs a new {@code Range} based on the segment specified by {@code a}
	 * and {@code b}
	 * 
	 * @param a a double value
	 * @param b another double value
	 */

	public Range(double a, double b) {
		this.segment = new Segment(a, b);
	}

	/**
	 * Resets the minimum and maximum values of this range to those specified by the
	 * elements of {@code data}, the indices of which correspond to the lower and
	 * upper bound of the {@code IndexRange}.
	 * 
	 * @param range an object specifying the start/end indices in regard to the
	 *              {@code data} list
	 * @param data  a list of double values (usually, a time sequence)
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
		segment.setMinimum((double) p.getValue());
		firePropertyChanged(this, p);
	}

	/**
	 * Sets the upper bound and triggers {@code firePropertyChanged}.
	 * 
	 * @param p a numeric property with the required {@code UPPER_BOUND} type.
	 */

	public void setUpperBound(NumericProperty p) {
		requireType(p, UPPER_BOUND);
		segment.setMaximum((double) p.getValue());
		firePropertyChanged(this, p);
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
	 * {@code p}. Since this is not fail-safe, the method has been made protected.
	 * 
	 * @param p a {@code NumericProperty} representing the laser pulse width.
	 */

	protected void updateMinimum(NumericProperty p) {
		if (p == null) 
			return;

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

	/*
	 * TODO put relative bounds in a constant field Consider creating a Bounds
	 * class, or putting them in the XML file
	 */

	/**
	 * The optimisation vector contain both the lower and upper bounds with the
	 * absolute constraints equal to a fourth of their values.
	 * 
	 * @param output the vector to be updated
	 * @param flags  a list of active flags
	 */

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		int size = output[0].dimension();

		for (int i = 0; i < size; i++) {

			switch (output[0].getIndex(i)) {
			case UPPER_BOUND:
				output[0].set(i, segment.getMaximum());
				output[1].set(i, 0.75 * segment.length());
				break;
			case LOWER_BOUND:
				output[0].set(i, segment.getMinimum());
				output[1].set(i, 0.75 * segment.length());
				break;
			default:
				continue;
			}

		}

	}

	/**
	 * Tries to assign the upper and lower bound based on {@code params}.
	 * 
	 * @param params an {@code IndexedVector} which may contain the bounds.
	 */

	@Override
	public void assign(IndexedVector params) {

		NumericProperty p = null;

		for (int i = 0, size = params.dimension(); i < size; i++) {

			p = derive(params.getIndex(i), params.get(i));

			switch (params.getIndex(i)) {
			case UPPER_BOUND:
				setUpperBound(p);
				break;
			case LOWER_BOUND:
				setLowerBound(p);
				break;
			default:
				continue;
			}

		}

	}

	@Override
	public String toString() {
		return "Range given by: " + segment.toString();
	}

}