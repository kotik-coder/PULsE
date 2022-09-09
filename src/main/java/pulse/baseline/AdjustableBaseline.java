package pulse.baseline;

import java.util.List;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;
import java.util.Set;
import pulse.math.Parameter;

import pulse.math.ParameterVector;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import static pulse.properties.NumericProperty.requireType;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;
import pulse.util.PropertyHolder;

/**
 * A baseline that can shift in the vertical direction.
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public abstract class AdjustableBaseline extends Baseline {

    private double intercept;
    private double slope;

    /**
     * Creates a flat baseline equal to the argument.
     *
     * @param intercept the constant baseline value.
     */
    public AdjustableBaseline(double intercept, double slope) {
        this.intercept = intercept;
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
        return intercept + x * slope;
    }

    protected double mean(List<Double> x) {
        return x.stream().mapToDouble(d -> d).average().getAsDouble();
    }

    /**
     * Provides getter accessibility to the intercept as a NumericProperty
     *
     * @return a NumericProperty derived from
     * NumericPropertyKeyword.BASELINE_INTERCEPT where the value is set to that
     * of {@code slope}
     */
    public NumericProperty getIntercept() {
        return derive(BASELINE_INTERCEPT, intercept);
    }

    /**
     * Checks whether {@code intercept} is a baseline intercept property and
     * updates the respective value of this baseline.
     *
     * @param intercept a {@code NumericProperty} of the
     * {@code BASELINE_INTERCEPT} type
     * @see set
     */
    public void setIntercept(NumericProperty intercept) {
        requireType(intercept, BASELINE_INTERCEPT);
        this.intercept = (double) intercept.getValue();
        firePropertyChanged(this, intercept);
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

    /**
     * Lists the {@code intercept} as accessible property for this
     * {@code FlatBaseline}.
     *
     * @see PropertyHolder
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(BASELINE_INTERCEPT);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == BASELINE_INTERCEPT) {
            setIntercept(property);
            this.firePropertyChanged(this, property);
        }
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        for (Parameter p : output.getParameters()) {

            if (p != null) {

                var key = p.getIdentifier().getKeyword();

                if (key == BASELINE_INTERCEPT) {
                    p.setValue(intercept);
                }

            }

        }

    }

    @Override
    public void assign(ParameterVector params) {
        for (Parameter p : params.getParameters()) {

            if (p.getIdentifier().getKeyword() == BASELINE_INTERCEPT) {
                setIntercept(
                        derive(BASELINE_INTERCEPT, p.inverseTransform())
                );
            }

        }

    }

}
