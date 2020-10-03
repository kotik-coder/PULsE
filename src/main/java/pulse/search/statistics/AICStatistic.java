package pulse.search.statistics;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

/**
 * AIC algorithm: Banks, H. T., &amp; Joyner, M. L. (2017). Applied Mathematics
 * Letters, 74, 33–45. doi:10.1016/j.aml.2017.05.005
 * 
 */

public class AICStatistic extends SumOfSquares {

	private int kq;
	private final static double PENALISATION_FACTOR = 1.0 + log(2 * PI);

	@Override
	public void evaluate(SearchTask t) {
		kq = t.alteredParameters().size(); //number of variables
		super.evaluate(t);
		final double n = getResiduals().size(); //sample size
		final double ssr = (double)getStatistic().getValue(); //sum of squared residuals divided by n
		//TODO check formula! SSR = divided by n
		final double stat = n * log(ssr) + 2.0 * (kq + 1) + n * PENALISATION_FACTOR;
		this.setStatistic(derive(OPTIMISER_STATISTIC, stat));
	}

	@Override
	public String getDescriptor() {
		return "Akaike Information Criterion (AIC)";
	}

	public int getNumVariables() {
		return kq;
	}

}
