package pulse.search.statistics;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import pulse.tasks.SearchTask;
import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.NormalDist;

public class AndersonDarlingTest extends NormalityTest {
	
	@Override
	public boolean test(SearchTask task) {
		calculateResiduals(task);
		double[] residuals = super.transformResiduals(task);
		var nd = new NormalDist(0.0, (new StandardDeviation()).evaluate( residuals ) );
		var testResult = GofStat.andersonDarling(residuals, nd);
		statistic = testResult[0];
		probability = testResult[1];
		return probability > significance;
	}
	
	@Override 
	public String describe() {
		return "Anderson-Darling test";
	}

	@Override
	public void evaluate(SearchTask t) {
		test(t);
	}

}