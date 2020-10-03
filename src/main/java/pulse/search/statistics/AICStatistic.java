package pulse.search.statistics;

/**
 * AIC algorithm: Banks, H. T., &amp; Joyner, M. L. (2017). Applied Mathematics
 * Letters, 74, 33–45. doi:10.1016/j.aml.2017.05.005
 * 
 */

public class AICStatistic extends ModelSelectionCriterion {

	@Override
	public double penalisingTerm(final int kq, final int n) {
		return 2.0 * (kq + 1);
	}

	@Override
	public String getDescriptor() {
		return "Akaike Information Criterion (AIC)";
	}

}