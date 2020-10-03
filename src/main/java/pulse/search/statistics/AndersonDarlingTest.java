package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import pulse.tasks.SearchTask;
import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.NormalDist;

public class AndersonDarlingTest extends NormalityTest {

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