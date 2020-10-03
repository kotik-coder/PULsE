package pulse.search.statistics;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import pulse.tasks.SearchTask;

public class RangePenalisedSSR extends SumOfSquares {

	private final static double PENALISATION_FACTOR = 0.5;

	@Override
	public void evaluate(SearchTask t) {
		super.evaluate(t);

		final double n = getResiduals().size();
		final double n0 = t.getExperimentalCurve().actualNumPoints();

		incrementStatistic(
				(n0 - n) / n0 * (new StandardDeviation().evaluate(transformResiduals())) * PENALISATION_FACTOR);
	}

	@Override
	public String getDescriptor() {
		return "Range Penalised SSR";
	}

}
