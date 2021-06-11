package pulse.search.statistics;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Wrapper {@code CorrelationTest} class for ApacheCommonsMath Pearson Correlation.
 *
 */


public class PearsonCorrelation extends CorrelationTest {

	@Override
	public double evaluate(double[] x, double[] y) {
		return (new PearsonsCorrelation()).correlation(x, y);
	}

	@Override
	public String getDescriptor() {
		return "Pearson's Product-Moment Correlation";
	}

}