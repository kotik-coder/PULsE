package pulse.search.statistics;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class SpearmansCorrelationTest extends CorrelationTest {
	
	@Override
	public double evaluate(double[] x, double[] y) {
		return (new SpearmansCorrelation()).correlation(x,y);
	}

	@Override
	public String getDescriptor() {
		return "Spearman's Rank Correlation";
	}

}
