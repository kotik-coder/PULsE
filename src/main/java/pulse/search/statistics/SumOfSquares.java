package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

/**
 * The standard optimality criterion of the L2 norm condition, or simply
 * ordinary least squares.
 *
 */
public class SumOfSquares extends OptimiserStatistic {

    public SumOfSquares() {
        super();
    }

    public SumOfSquares(SumOfSquares sos) {
        super(sos);
    }

    /**
     * Calculates the sum of squared deviations using {@code curve} as
     * reference.
     * <p>
     * This calculates
     * <math><munderover><mo>&#x2211;</mo><mrow><mi>i</mi><mo>=</mo><msub><mi>i</mi><mn>1</mn></msub></mrow><msub><mi>i</mi><mn>2</mn></msub></munderover><mo>(</mo><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo><mo>-</mo><mi>T</mi><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><msup><mo>)</mo><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msup><msup><mo>)</mo><mn>2</mn></msup></math>,
     * where
     * <math><msubsup><mi>T</mi><mi>i</mi><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msubsup></math>
     * is the temperature value corresponding to the {@code time} at index
     * {@code i} for the reference {@code curve}. Note that the time
     * <math><msub><mi>t</mi><mi>i</mi></msub></math> corresponds to the
     * <b>reference's</b> time list, which generally does not match to that of
     * this heating curve. The
     * <math><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo></math>
     * is the interpolated value.
     *
     * @param t The task containing the reference and calculated curves
     * @see calculateResiduals()
     */
    @Override
    public void evaluate(SearchTask t) {
        calculateResiduals(t);
        final double statistic = getResiduals().stream().map(r -> r[1] * r[1])
                                .reduce(Double::sum).get() / getResiduals().size();
        setStatistic(derive(OPTIMISER_STATISTIC, statistic));
    }

    @Override
    public String getDescriptor() {
        return "Ordinary Least Squares";
    }

    @Override
    public double variance() {
        return (double) getStatistic().getValue();
    }

    @Override
    public OptimiserStatistic copy() {
        return new SumOfSquares(this);
    }

}
