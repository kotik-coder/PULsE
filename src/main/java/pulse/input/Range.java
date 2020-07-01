package pulse.input;

import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.LOWER_BOUND;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.math.Segment2D;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;

public class Range extends PropertyHolder {

	private Segment2D segment;

	public void reset(IndexRange range, List<Double> time) {
		segment.setMaximum(time.get(range.getUpperBound()));
		segment.setMinimum(time.get(range.getLowerBound()));
	}

	public Range(List<Double> data) {
		double min = data.stream().reduce((a, b) -> a < b ? a : b).get();
		double max = data.stream().reduce((a, b) -> b > a ? b : a).get();
		segment = new Segment2D(min, max);
	}

	public Range(Segment2D segment) {
		this.segment = segment;
	}

	public Range(double a, double b) {
		this.segment = new Segment2D(a, b);
	}

	public NumericProperty getLowerBound() {
		return derive(LOWER_BOUND, segment.getMinimum());
	}

	public NumericProperty getUpperBound() {
		return derive(UPPER_BOUND, segment.getMaximum());
	}

	public void setLowerBound(NumericProperty p) {
		requireType(p, LOWER_BOUND);
		segment.setMinimum((double) p.getValue());
	}

	public void setUpperBound(NumericProperty p) {
		requireType(p, UPPER_BOUND);
		segment.setMaximum((double) p.getValue());
	}

	public Segment2D getSegment() {
		return segment;
	}

	public void updateMinimum(NumericProperty p) {
		if(p == null)
			return;
		
		double pulseWidth = (double) p.getValue();
		if (segment.getMinimum() < pulseWidth)
			segment.setMinimum(pulseWidth);
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

	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		int size = output[0].dimension();

		for (int i = 0; i < size; i++) {

			switch (output[0].getIndex(i)) {
			case UPPER_BOUND:
				output[0].set(i, segment.getMaximum());
				output[1].set(i, 0.25 * segment.getMaximum());
				break;
			case LOWER_BOUND:
				output[0].set(i, segment.getMinimum());
				output[1].set(i, 0.25 * segment.getMaximum());
				break;
			default:
				continue;
			}

		}

	}

	/**
	 * Assigns parameter values of this {@code Problem} using the optimisation
	 * vector {@code params}. Only those parameters will be updated, the types of
	 * which are listed as indices in the {@code params} vector.
	 * 
	 * @param params the optimisation vector, containing a similar set of parameters
	 *               to this {@code Problem}
	 * @see listedTypes()
	 */

	public void assign(IndexedVector params) {

		NumericProperty p = null;

		for (int i = 0, size = params.dimension(); i < size; i++) {

			p = derive(params.getIndex(i), params.get(i));

			switch (params.getIndex(i)) {
			case UPPER_BOUND:
				setUpperBound(p);
				this.firePropertyChanged(this, p);
				break;
			case LOWER_BOUND:
				setLowerBound(p);
				this.firePropertyChanged(this, p);
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