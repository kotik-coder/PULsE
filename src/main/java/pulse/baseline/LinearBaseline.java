package pulse.baseline;

import static java.lang.String.format;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;

import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;

/**
 * A linear {@code Baseline} which specifies the {@code intercept} and
 * {@code slope} parameters.
 * <p>
 * The mathematical equivalent is the following expression:
 * {@code g(x) = intercept + slope * x}. The {@code NumericPropertyKeyword}
 * associated with the {@code intercept} and {@code slope} parameters can be
 * used as fitting variables.
 * </p>
 *
 * @see pulse.HeatingCurve
 * @see pulse.tasks.SearchTask
 * @see pulse.math.ParameterVector
 */
public class LinearBaseline extends AdjustableBaseline {

    private double slope;

    /**
     * A primitive constructor, which initialises a {@code CONSTANT} baseline
     * with zero intercept and slope.
     */
    public LinearBaseline() {
        super(0.0);
    }

    /**
     * A constructor, which allows to specify all three parameters in one go.
     *
     * @param intercept the intercept is the value of the Baseline's linear
     * function at {@code x = 0}
     * @param slope the slope determines the inclination angle of the Baseline's
     * graph.
     */
    public LinearBaseline(double intercept, double slope) {
        super(intercept);
        this.slope = slope;
    }

    /**
     * Calculates the linear function {@code g(x) = intercept + slope*time}
     *
     * @param x the argument of the linear function
     * @return the result of this simple calculation
     */
    @Override
    public double valueAt(double x) {
        final double intercept = (double) getIntercept().getValue();
        return intercept + x * slope;
    }

    @Override
    protected void doFit(List<Double> x, List<Double> y, int size) {
        double meanx = mean(x);
        double meany = mean(y);

        double x1;
        double y1;
        double xxbar = 0.0;
        double xybar = 0.0;

        for (int i = 0; i < size; i++) {
            x1 = x.get(i);
            y1 = y.get(i);
            xxbar += (x1 - meanx) * (x1 - meanx);
            xybar += (x1 - meanx) * (y1 - meany);
        }

        slope = xybar / xxbar;
        double intercept = meany - slope * meanx;

        set(BASELINE_INTERCEPT, derive(BASELINE_INTERCEPT, intercept));
        set(BASELINE_SLOPE, derive(BASELINE_SLOPE, slope));
    }

    /**
     * Provides getter accessibility to the slope as a NumericProperty
     *
     * @return a NumericProperty derived from
     * NumericPropertyKeyword.BASELINE_SLOPE with a value equal to slop
     */
    public NumericProperty getSlope() {
        return derive(BASELINE_SLOPE, slope);
    }

    /**
     * Checks whether {@code slope} is a baseline slope property and updates the
     * respective value of this baseline.
     *
     * @param slope a {@code NumericProperty} of the {@code BASELINE_SLOPE} type
     * @see set
     */
    public void setSlope(NumericProperty slope) {
        requireType(slope, BASELINE_SLOPE);
        this.slope = (double) slope.getValue();
        firePropertyChanged(this, slope);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " = " + format("%3.2f + t * ( %3.2f )", getIntercept().getValue(), slope);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        if (type == BASELINE_SLOPE) {
            setSlope(property);
            this.firePropertyChanged(this, property);
        }

    }

    @Override
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);

        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            if (key == BASELINE_SLOPE) {
                output.set(i, slope, BASELINE_SLOPE);
            }

        }

    }

    /**
     * Assigns parameter values of this {@code Problem} using the optimisation
     * vector {@code params}. Only those parameters will be updated, the types
     * of which are listed as indices in the {@code params} vector.
     *
     * @param params the optimisation vector, containing a similar set of
     * parameters to this {@code Problem}
     * @see listedTypes()
     */
    @Override
    public void assign(ParameterVector params) {
        super.assign(params);

        for (int i = 0, size = params.dimension(); i < size; i++) {

            if (params.getIndex(i) == BASELINE_SLOPE) {
                setSlope(derive(BASELINE_SLOPE, params.get(i)));
            }

        }

    }

    /**
     * @return a set containing {@code BASELINE_INTERCEPT} and
     * {@code BASELINE_SLOPE} keywords
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(BASELINE_SLOPE);
        return set;
    }

    @Override
    public Baseline copy() {
        return new LinearBaseline((double) this.getIntercept().getValue(), this.slope);
    }

}
