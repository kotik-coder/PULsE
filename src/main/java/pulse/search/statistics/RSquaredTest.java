package pulse.search.statistics;

import static java.lang.Math.pow;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import pulse.input.ExperimentalData;
import pulse.properties.NumericProperty;
import pulse.tasks.SearchTask;

/**
 * The coefficient of determination represents the goodness of fit that a
 * {@code HeatingCurve} provides for the {@code ExperimentalData}
 *
 */
public class RSquaredTest extends NormalityTest {

    private SumOfSquares sos;
    private static NumericProperty signifiance = derive(SIGNIFICANCE, 0.2);

    public RSquaredTest() {
        super();
        sos = new SumOfSquares();
    }

    @Override
    public boolean test(SearchTask task) {
        evaluate(task);
        sos = new SumOfSquares();
        return getStatistic().compareTo(signifiance) > 0;
    }

    /**
     * Calculates the coefficient of determination, or simply the
     * <math><msup><mi>R</mi><mn>2</mn></msup></math> value.
     * <p>
     * First, the mean temperature of the {@code data} is calculated. Then, the
     * {@code TSS} (total sum of squares) is calculated as proportional to the
     * variance of data. The residual sum of squares ({@code RSS}) is calculated
     * by calling {@code this.deviationSquares(curve)}. Finally, these values
     * are combined together as: {@code 1 - RSS/TSS}.
     * </p>
     *
     * @param t the task containing the reference data
     * @see <a href=
     *      "https://en.wikipedia.org/wiki/Coefficient_of_determination">Wikipedia
     * page</a>
     */
    @Override
    public void evaluate(SearchTask t) {
        var reference = t.getExperimentalCurve();

        sos.evaluate(t);

        final int start = reference.getIndexRange().getLowerBound();
        final int end = reference.getIndexRange().getUpperBound();

        final double mean = mean(reference, start, end);
        double TSS = 0;

        for (int i = start; i < end; i++) {
            TSS += pow(reference.signalAt(i) - mean, 2);
        }

        TSS /= (end - start);

        setStatistic(derive(TEST_STATISTIC, (1. - (double) sos.getStatistic().getValue() / TSS)));
    }

    private double mean(ExperimentalData data, final int start, final int end) {
        double mean = 0;

        for (int i = start; i < end; i++) {
            mean += data.signalAt(i);
        }

        mean /= (end - start);
        return mean;
    }

    public SumOfSquares getSumOfSquares() {
        return sos;
    }

    public void setSumOfSquares(SumOfSquares sos) {
        this.sos = sos;
    }

    @Override
    public String getDescriptor() {
        return "<html><i>R</i><sup>2</sup> test";
    }

}
