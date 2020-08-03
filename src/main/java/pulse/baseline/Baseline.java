package pulse.baseline;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pulse.input.ExperimentalData;
import pulse.input.IndexRange;
import pulse.search.Optimisable;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An abstract class for baseline calculations. Defines an abstract
 * {@code valueAt} method that would return a baseline value at a given moment
 * in time (either before or after the laser pulse). The baseline parameters can
 * be modified within an optimisation loop, hence there are two abstract methods
 * to implement that functionality.
 * 
 * @see pulse.HeatingCurve
 * @see pulse.tasks.SearchTask
 * @see pulse.math.IndexedVector
 */

public abstract class Baseline extends PropertyHolder implements Reflexive, Optimisable {

	/**
	 * Calculates the baseline at the given position.
	 * 
	 * @param x the position on the profile (e.g., the time value)
	 * @return the baseline value
	 */

	public abstract double valueAt(double x);

	/**
	 * Calculates the baseline parameters based on input arguments.
	 * <p>
	 * This will run a simple least-squares estimation of the parameters of this
	 * baseline using the specified {@code data} within the time range
	 * {@code rangeMin < t < rangeMax}. If no data is available, the method will NOT
	 * change the {@code intercept} and {@code slope} values. Upon completion, the
	 * method will use the respective {@code set} methods of this class to update
	 * the parameter values, triggering whatever events are associated with them.
	 * </p>
	 * 
	 * @param x    a list of independent variable values
	 * @param y    a list of dependent variable values
	 * @param size the size of the region
	 */

	protected abstract void doFit(List<Double> x, List<Double> y, int size);

	/**
	 * Selects part of the {@code data} that can be used for baseline estimation
	 * (typically, this means selecting 'negative' time values and the corresponding
	 * signal) data and runs the fitting algorithms,
	 * 
	 * @param data     the experimental data
	 * @param rangeMin the minimum of the time range
	 * @param rangeMax the maximum of the time range
	 */

	public void fitTo(ExperimentalData data, double rangeMin, double rangeMax) {
		var indexRange = data.getIndexRange();

		Objects.requireNonNull(indexRange);

		if (!indexRange.isValid())
			throw new IllegalArgumentException("Index range not valid: " + indexRange);

		List<Double> x = new ArrayList<>();
		List<Double> y = new ArrayList<>();

		int size = 0;

		for (int i = IndexRange.closestLeft(rangeMin, data.getTimeSequence()) + 1, max = min(indexRange.getLowerBound(),
				IndexRange.closestRight(rangeMax, data.getTimeSequence())); i < max; i++, size++) {

			x.add(data.timeAt(i));
			y.add(data.signalAt(i));

		}

		if (size > 0) // do fitting only if data is present
			doFit(x, y, size);

	}

	/**
	 * Calls {@code fitTo} using the default time range for the data:
	 * {@code -Infinity < t < ZERO_LEFT}, where the upper bound is
	 * a small negative constant.
	 * 
	 * @param data the experimental data stretching to negative time values
	 * @see fitTo(ExperimentalData,double,double)
	 */

	public void fitTo(ExperimentalData data) {
		final double ZERO_LEFT = -1E-5;
		fitTo(data, NEGATIVE_INFINITY, ZERO_LEFT);
	}

}