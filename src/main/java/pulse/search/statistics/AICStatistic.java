package pulse.search.statistics;

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
	private final static double PENALISATION_FACTOR = Math.log(2.0 * Math.PI) + 1.0;

	@Override
	public void evaluate(SearchTask t) {
		kq = t.alteredParameters().size();
		super.evaluate(t);
		double n = getResiduals().size();
		final double stat = n * Math.log((double)getStatistic().getValue()) + 2.0 * (kq + 1) + n * PENALISATION_FACTOR;
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
