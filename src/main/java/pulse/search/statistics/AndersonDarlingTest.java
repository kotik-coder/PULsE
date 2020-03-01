package pulse.search.statistics;

import pulse.tasks.SearchTask;
import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.NormalDist;

public class AndersonDarlingTest extends NormalityTest {
		
	public AndersonDarlingTest() {
		super();
	}
	
	@Override
	public boolean test(SearchTask task) {
		double[] residuals = super.transformResiduals(task);
		var nd = new NormalDist(0.0, sd);
		var testResult = GofStat.andersonDarling(residuals, nd);
		statistic = testResult[0];
		probability = testResult[1];
		return probability > significance;
	}

}