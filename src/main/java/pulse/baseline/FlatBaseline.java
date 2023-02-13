package pulse.baseline;

import static java.lang.String.format;
import java.util.List;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;

/**
 * A flat baseline.
 */
public class FlatBaseline extends AdjustableBaseline {

    private static final long serialVersionUID = -4867631788950622739L;

    /**
     * A primitive constructor, which initialises a {@code CONSTANT} baseline
     * with zero intercept and slope.
     */
    public FlatBaseline() {
        this(0.0);
    }

    /**
     * Creates a flat baseline equal to the argument.
     *
     * @param intercept the constant baseline value.
     */
    public FlatBaseline(double intercept) {
        super(intercept, 0.0);
    }

    @Override
    protected void doFit(List<Double> x, List<Double> y) {
        double intercept = mean(y);
        set(BASELINE_INTERCEPT, derive(BASELINE_INTERCEPT, intercept));
    }

    @Override
    public Baseline copy() {
        return new FlatBaseline((double) getIntercept().getValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " = " + format("%3.2f", getIntercept().getValue());
    }

}
