package pulse.baseline;

import static java.lang.String.format;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;

import java.util.List;
import java.util.Set;
import pulse.math.Parameter;

import pulse.math.ParameterVector;
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

    /**
     * A primitive constructor, which initialises a {@code CONSTANT} baseline
     * with zero intercept and slope.
     */
    public LinearBaseline() {
        super(0.0, 0.0);
    }
    
    public LinearBaseline(double intercept, double slope) {
        super(intercept, slope);
    }
    
    public LinearBaseline(AdjustableBaseline baseline) {
        super( (double) baseline.getIntercept().getValue(), 
               (double) baseline.getSlope().getValue()
             );
    }

    @Override
    protected void doFit(List<Double> x, List<Double> y) {        
        double meanx = mean(x);
        double meany = mean(y);

        double x1;
        double y1;
        double xxbar = 0.0;
        double xybar = 0.0;

        for (int i = 0, size = x.size(); i < size; i++) {
            x1 = x.get(i);
            y1 = y.get(i);
            xxbar += (x1 - meanx) * (x1 - meanx);
            xybar += (x1 - meanx) * (y1 - meany);
        }
        
        double slope = xybar / xxbar;
        double intercept = meany - slope * meanx;

        set(BASELINE_INTERCEPT, derive(BASELINE_INTERCEPT, intercept));
        set(BASELINE_SLOPE, derive(BASELINE_SLOPE, slope));
    }

    @Override
    public String toString() {
        var slope = getSlope().getValue();
        return getClass().getSimpleName() + " = " + 
                format("%3.2f + t * ( %3.2f )", getIntercept().getValue(), slope);
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
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            if (key == BASELINE_SLOPE) {
                double slope = (double) getSlope().getValue();
                p.setValue(slope);
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

        for (Parameter p : params.getParameters()) {

            var key = p.getIdentifier().getKeyword();
            
            if (key == BASELINE_SLOPE) {
                setSlope( derive(BASELINE_SLOPE, p.inverseTransform() ));
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
        return new LinearBaseline(this);
    }

}