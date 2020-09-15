package pulse.baseline;

import static java.lang.String.format;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.math.IndexedVector;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/**
 * A simple constant baseline with no slope. The intercept value can be used as
 * an optimisation variable.
 *
 */

public class FlatBaseline extends Baseline {

	private double intercept;

	/**
	 * A primitive constructor, which initialises a {@code CONSTANT} baseline with
	 * zero intercept and slope.
	 */

	public FlatBaseline() {
		// intentionally blank
	}

	/**
	 * Creates a flat baseline equal to the argument.
	 * 
	 * @param intercept the constant baseline value.
	 */

	public FlatBaseline(double intercept) {
		this.intercept = intercept;
	}

	/**
	 * @return the constant value of this {@code FlatBaseline}
	 */

	@Override
	public double valueAt(double x) {
		return intercept;
	}

	@Override
	protected void doFit(List<Double> x, List<Double> y, int size) {
		intercept = mean(y);
		set(BASELINE_INTERCEPT, derive(BASELINE_INTERCEPT, intercept));
	}

	protected double mean(List<Double> x) {
		double sum = 0.0;
		final double len = x.size();
		for (int i = 0; i < len; i++) {
			sum += x.get(i);
		}
		return sum / len;
	}

	/**
	 * Provides getter accessibility to the intercept as a NumericProperty
	 * 
	 * @return a NumericProperty derived from
	 *         NumericPropertyKeyword.BASELINE_INTERCEPT where the value is set to
	 *         that of {@code slope}
	 */

	public NumericProperty getIntercept() {
		return derive(BASELINE_INTERCEPT, intercept);
	}

	/**
	 * Checks whether {@code intercept} is a baseline intercept property and updates
	 * the respective value of this baseline.
	 * 
	 * @param intercept a {@code NumericProperty} of the {@code BASELINE_INTERCEPT}
	 *                  type
	 * @see set
	 */

	public void setIntercept(NumericProperty intercept) {
		requireType(intercept, BASELINE_INTERCEPT);
		this.intercept = (double) intercept.getValue();
		firePropertyChanged(this, intercept);
	}

	/**
	 * Lists the {@code intercept} as accessible property for this
	 * {@code FlatBaseline}.
	 * 
	 * @see PropertyHolder
	 */

	@Override
	public List<Property> listedTypes() {
		return new ArrayList<Property>(Arrays.asList(getIntercept()));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " = " + format("%3.2f", intercept);
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == BASELINE_INTERCEPT) {
			setIntercept(property);
			this.firePropertyChanged(this, property);
		}
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		for (int i = 0, size = output[0].dimension(); i < size; i++) {

			if (output[0].getIndex(i) == BASELINE_INTERCEPT) {
				output[0].set(i, intercept);
				output[1].set(i, 5);
			}

		}

	}

	@Override
	public void assign(IndexedVector params) {
		for (int i = 0, size = params.dimension(); i < size; i++) {

			if (params.getIndex(i) == BASELINE_INTERCEPT)
				setIntercept(derive(BASELINE_INTERCEPT, params.get(i)));

		}

	}

}