package pulse.search.statistics;

import static java.lang.Math.abs;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

/**
 * A statistical optimality criterion relying on absolute deviations or the L1 norm condition. Similar to the least squares technique, 
 * it attempts to find a function which closely approximates a set of data. However, unlike the L2 norm, it is much more robust to 
 * data outliers.
 *
 */

public class AbsoluteDeviations extends ResidualStatistic {
	
	/**
	 * Calculates the L1 norm statistic, which simply sums up the absolute values of residuals.
	 */
	
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
