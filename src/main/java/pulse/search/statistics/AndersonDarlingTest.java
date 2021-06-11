package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import pulse.tasks.SearchTask;
import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.NormalDist;

/**
 * The Anderson-Darling normality test. In this variant of the test, the mean and the variance
 * are assumed to be known. 
 *
 */

public class AndersonDarlingTest extends NormalityTest {

	/**
	 * This uses the SSJ statistical library to calculate the Anderson-Darling test
	 * with the input parameters formed by the {@code task} residuals and a normal distribution
	 * with zero mean and variance equal to the residuals variance.
	 */
	
	@Override
	public boolean test(SearchTask task) {
		calculateResiduals(task);
		
		double[] residuals = super.transformResiduals();
		var nd = new NormalDist(0.0, (new StandardDeviation()).evaluate(residuals));
		var testResult = GofStat.andersonDarling(residuals, nd);
		
		this.setStatistic(derive(TEST_STATISTIC, testResult[0]));
		setProbability(derive(PROBABILITY, testResult[1]));
		
		return significanceTest();
	}

	@Override
	public String getDescriptor() {
		return "Anderson-Darling test";
	}

	@Override
	public void evaluate(SearchTask t) {
		test(t);
	}

}