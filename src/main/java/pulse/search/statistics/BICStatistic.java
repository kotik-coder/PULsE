package pulse.search.statistics;

import static java.lang.Math.log;

/**
 * Bayesian Information Criterion (BIC) algorithm formulated for the Gaussian distribution of residuals.
 *
 */

public class BICStatistic extends ModelSelectionCriterion {

	public BICStatistic(BICStatistic another) {
		super(another);
	}
	
	public BICStatistic(OptimiserStatistic os) {
		super(os);
	}
	
	public BICStatistic() {
		super(new SumOfSquares());
	}
	
	@Override 
	public ModelSelectionCriterion copy() {
		return new BICStatistic(this);
	}
	
	@Override 
	public double penalisingTerm(final int kq, final int n) {
		return (kq + 1)*log(n);
	}
	
	@Override
	public String getDescriptor() {
		return "Bayesian Information Criterion (BIC)";
	}

}