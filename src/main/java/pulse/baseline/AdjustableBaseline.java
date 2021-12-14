package pulse.baseline;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;
import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;

/**
 * A baseline that can shift in the vertical direction.
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public abstract class AdjustableBaseline extends Baseline {

    private double intercept;

    /**
     * Creates a flat baseline equal to the argument.
     *
     * @param intercept the constant baseline value.
     */
    public AdjustableBaseline(double intercept) {
        this.intercept = intercept;
    }

    /**
     * @return the constant value of this {@code FlatBaseline}
     */
    @Override
    public double valueAt(double x) {
        return intercept;
    }

    protected double mean(List<Double> x) {
        double sum = x.stream().reduce((a, b) -> a + b).get();
        return sum / x.size();
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
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            if (key == BASELINE_INTERCEPT) {
                output.set(i, intercept, key);
            }

        }

    }

    @Override
    public void assign(ParameterVector params) {
        for (int i = 0, size = params.dimension(); i < size; i++) {

            if (params.getIndex(i) == BASELINE_INTERCEPT) {
                setIntercept(derive(BASELINE_INTERCEPT, params.get(i)));
            }

        }

    }

}
