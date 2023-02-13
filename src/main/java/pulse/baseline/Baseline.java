package pulse.baseline;

import java.util.ArrayList;
import java.util.List;
import pulse.DiscreteInput;

import pulse.input.ExperimentalData;
import pulse.input.IndexRange;
import pulse.input.Range;
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
 * @see pulse.math.ParameterVector
 */
public abstract class Baseline extends PropertyHolder implements Reflexive, Optimisable {

    public final static int MIN_BASELINE_POINTS = 15;

    public abstract Baseline copy();

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
     * This usually runs a simple least-squares estimation of the parameters of
     * this baseline using the specified {@code data} within the time range
     * {@code rangeMin < t < rangeMax}. If no data is available, the method will
     * NOT change the baseline parameters. Upon completion, the method will use
     * the respective {@code set} methods of this class to update the parameter
     * values, triggering whatever events are associated with them.
     * </p>
     *
     * @param x
     * @param y
     */
    protected abstract void doFit(List<Double> x, List<Double> y);

    /**
     * Calls {@code fitTo} using the default time range for the data:
     * {@code -Infinity < t < ZERO_LEFT}, where the upper bound is a small
     * negative constant.
     *
     * @param data the experimental data stretching to negative time values
     * @see fitTo(ExperimentalData,double,double)
     */
    public void fitTo(DiscreteInput data) {
        var filtered = Range.NEGATIVE.filter(data);
        if (filtered[0].size() > MIN_BASELINE_POINTS) {
            doFit(filtered[0], filtered[1]);
        }
    }

    public void fitTo(List<Double> x, List<Double> y) {
        int index = IndexRange.closestLeft(0, x);
        var xx = new ArrayList<>(x.subList(0, index + 1));
        var yy = new ArrayList<>(y.subList(0, index + 1));
        if (xx.size() > MIN_BASELINE_POINTS) {
            doFit(xx, yy);
        }
    }

    @Override
    public String getDescriptor() {
        return "Baseline";
    }

}
