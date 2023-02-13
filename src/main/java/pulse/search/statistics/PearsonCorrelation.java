package pulse.search.statistics;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Wrapper {@code CorrelationTest} class for ApacheCommonsMath Pearson
 * Correlation.
 *
 */
public class PearsonCorrelation extends CorrelationTest {

    private static final long serialVersionUID = 4819197257434836120L;

    @Override
    public double evaluate(double[] x, double[] y) {
        return (new PearsonsCorrelation()).correlation(x, y);
    }

    @Override
    public String getDescriptor() {
        return "Pearson's Product-Moment Correlation";
    }

}
