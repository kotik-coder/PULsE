package pulse.search.statistics;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.inference.TestUtils;

import pulse.tasks.SearchTask;

public class KSTest extends NormalityTest {

	private double[] residuals;
	private NormalDistribution nd;

	@Override
	public boolean test(SearchTask task) {
		evaluate(task);
		probability = TestUtils.kolmogorovSmirnovTest(nd, residuals);
		return probability > significance;

	}

	@Override
	public void evaluate(SearchTask t) {
		calculateResiduals(t);
		residuals = transformResiduals(t);

		double sd = (new StandardDeviation()).evaluate(residuals);
		nd = new NormalDistribution(0.0, sd); // null hypothesis: normal distribution with zero mean and empirical
												// standard dev
		statistic = TestUtils.kolmogorovSmirnovStatistic(nd, residuals);
	}

	@Override
	public String getDescriptor() {
		return "Kolmogorov-Smirnov test";
	}

}