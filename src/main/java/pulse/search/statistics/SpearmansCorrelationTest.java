package pulse.search.statistics;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

/**
 * Wrapper {@code CorrelationTest} class for ApacheCommonsMath Spearmans
 * Correlation.
 *
 */
public class SpearmansCorrelationTest extends CorrelationTest {

    private static final long serialVersionUID = -8027167403407629716L;

    @Override
    public double evaluate(double[] x, double[] y) {
        return (new SpearmansCorrelation()).correlation(x, y);
    }

    @Override
    public String getDescriptor() {
        return "Spearman's Rank Correlation";
    }

}
