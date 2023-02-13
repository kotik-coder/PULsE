package pulse.math.filters;

import java.awt.geom.Point2D;
import java.io.Serializable;
import static java.lang.Double.valueOf;
import static java.util.Collections.max;
import java.util.Comparator;
import java.util.stream.Collectors;
import pulse.DiscreteInput;
import pulse.baseline.FlatBaseline;
import pulse.input.IndexRange;

public class HalfTimeCalculator implements Serializable {

    private static final long serialVersionUID = 8302980290467110065L;
    private final Filter filter;
    private final DiscreteInput data;
    private Point2D max;
    private double halfTime;

    /**
     * A fail-safe factor.
     */
    public final static double FAIL_SAFE_FACTOR = 10.0;

    private static final Comparator<Point2D> pointComparator
            = (p1, p2) -> valueOf(p1.getY()).compareTo(valueOf(p2.getY()));

    public HalfTimeCalculator(DiscreteInput input) {
        this.data = input;
        this.filter = new RunningAverage();
    }

    /**
     * Calculates the approximate half-rise time used for crude estimation of
     * thermal diffusivity.
     * <p>
     * This uses the {@code runningAverage} method by applying the default
     * reduction factor of {@value REDUCTION_FACTOR}. The calculation is based
     * on finding the approximate value corresponding to the half-maximum of the
     * temperature. The latter is calculated using the running average curve.
     * The index corresponding to the closest temperature value available for
     * that curve is used to retrieve the half-rise time (which also has the
     * same index). If this fails, i.e. the associated index is less than 1,
     * this will print out a warning message and still assign a value to the
     * half-time variable equal to the acquisition time divided by a fail-safe
     * factor {@value FAIL_SAFE_FACTOR}.
     * </p>
     *
     * @see getHalfTime()
     */
    public void calculate() {
        var baseline = new FlatBaseline();
        baseline.fitTo(data);

        var filtered = filter.process(data);

        max = max(filtered, pointComparator);
        double halfMax = (max.getY() + baseline.valueAt(0)) / 2.0;

        int indexLeft = IndexRange.closestLeft(halfMax,
                filtered.stream().map(point -> point.getY())
                        .collect(Collectors.toList()));

        if (indexLeft < 1 || indexLeft > filtered.size() - 2) {
            halfTime = filtered.get(filtered.size() - 1).getX() / FAIL_SAFE_FACTOR;
        } else {
            //extrapolate
            Point2D p1 = filtered.get(indexLeft);
            Point2D p2 = filtered.get(indexLeft + 1);

            halfTime = (halfMax - p1.getY()) / (p2.getY() - p1.getY())
                    * (p2.getX() - p1.getX()) + p1.getX();
        }

    }

    /**
     * Retrieves the half-time value of this dataset, which is equal to the time
     * needed to reach half of the signal maximum.
     *
     * @return the half-time value.
     */
    public final double getHalfTime() {
        return halfTime;
    }

    public final Point2D getFilteredMaximum() {
        return max;
    }

    public DiscreteInput getData() {
        return data;
    }

}
