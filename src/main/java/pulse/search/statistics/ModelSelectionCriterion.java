package pulse.search.statistics;

import static java.lang.Math.PI;
import static java.lang.Math.log;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

public abstract class ModelSelectionCriterion extends SumOfSquares {

	private int kq;
	private final static double PENALISATION_FACTOR = 1.0 + log(2 * PI);

	@Override
	public void evaluate(SearchTask t) {
		kq = t.alteredParameters().size(); //number of variables
		super.evaluate(t);
		final int n = getResiduals().size(); //sample size
		final double ssr = (double)getStatistic().getValue(); //sum of squared residuals divided by n
		final double stat = n * log(ssr) + penalisingTerm(kq,n) + n * PENALISATION_FACTOR;
		this.setStatistic(derive(OPTIMISER_STATISTIC, stat));
	}
	
	public abstract double penalisingTerm(int k, int n);

	@Override
	public String getDescriptor() {
		return "Akaike Information Criterion (AIC)";
	}

	public int getNumVariables() {
		return kq;
	}
	
}