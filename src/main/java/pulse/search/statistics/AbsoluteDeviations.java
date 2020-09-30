package pulse.search.statistics;

import static java.lang.Math.*;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

public class AbsoluteDeviations extends ResidualStatistic {

	@Override
	public void evaluate(SearchTask t) {
		calculateResiduals(t);
		final double statistic = getResiduals().stream().map(r -> abs(r[1]) ).reduce(Double::sum).get() / getResiduals().size();
		setStatistic(derive(OPTIMISER_STATISTIC, statistic));
	}

	@Override
	public String getDescriptor() {
		return "Absolute Deviations";
	}

}
