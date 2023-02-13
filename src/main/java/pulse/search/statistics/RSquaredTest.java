package pulse.search.statistics;

import static java.lang.Math.pow;
import java.util.List;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import pulse.properties.NumericProperty;
import pulse.search.GeneralTask;

/**
 * The coefficient of determination represents the goodness of fit that a
 * {@code HeatingCurve} provides for the {@code ExperimentalData}
 *
 */
public class RSquaredTest extends NormalityTest {

    private static final long serialVersionUID = -2022982190434832373L;
    private SumOfSquares sos;
    private static NumericProperty signifiance = derive(SIGNIFICANCE, 0.2);

    public RSquaredTest() {
        super();
        sos = new SumOfSquares();
    }

    @Override
    public boolean test(GeneralTask task) {
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
    public void evaluate(GeneralTask t) {
        var yr = t.getInput().getY();
        sos.evaluate(t);

        final double mean = mean(yr);
        double TSS = 0;
        int size = yr.size();

        for (int i = 0; i < size; i++) {
            TSS += pow(yr.get(i) - mean, 2);
        }

        TSS /= size;

        setStatistic(derive(TEST_STATISTIC, (1. - (double) sos.getStatistic().getValue() / TSS)));
    }

    private double mean(List<Double> input) {
        return input.stream().mapToDouble(d -> d).average().getAsDouble();
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
