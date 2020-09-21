package pulse.search.statistics;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static pulse.input.IndexRange.closestLeft;
import static pulse.input.IndexRange.closestRight;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

public abstract class ResidualStatistic extends Statistic {

	private double statistic;
	private List<double[]> residuals;
	private static String selectedOptimiserDescriptor;

	public ResidualStatistic() {
		super();
		residuals = new ArrayList<>();
		setPrefix("Residuals");
	}

	public double[] transformResiduals(SearchTask task) {
		return task.getResidualStatistic().getResiduals().stream().map(doubleArray -> doubleArray[1])
				.mapToDouble(Double::doubleValue).toArray();
	}

	public void calculateResiduals(SearchTask task) {
		var estimate = task.getProblem().getHeatingCurve();
		var reference = task.getExperimentalCurve();

		residuals.clear();
		var indexRange = reference.getIndexRange();
		var time = reference.getTimeSequence();

		var s = estimate.getSplineInterpolation();

		int startIndex = max(closestLeft(estimate.timeAt(0), time), indexRange.getLowerBound());
		int endIndex = min(closestRight(estimate.timeLimit(), time), indexRange.getUpperBound());

		double interpolated;
		
		for (int i = startIndex; i <= endIndex; i++) {
			/*
			 * find the point on the calculated heating curve which has the closest time
			 * value smaller than the experimental points' time value
			 */

			interpolated = s.value(reference.timeAt(i));

			residuals.add(new double[] { reference.timeAt(i), 
					reference.signalAt(i) - interpolated }); // y_exp - y*

		}

	}

	public List<double[]> getResiduals() {
		return residuals;
	}

	public double residualUpperBound() {
		return residuals.stream().map(array -> array[1]).reduce((a, b) -> b > a ? b : a).get();
	}

	public double residualLowerBound() {
		return residuals.stream().map(array -> array[1]).reduce((a, b) -> a < b ? a : b).get();
	}

	public static void setSelectedOptimiserDescriptor(String selectedTestDescriptor) {
		ResidualStatistic.selectedOptimiserDescriptor = selectedTestDescriptor;
	}

	public static String getSelectedOptimiserDescriptor() {
		return selectedOptimiserDescriptor;
	}

	public NumericProperty getStatistic() {
		return derive(OPTIMISER_STATISTIC, statistic);
	}

	public void setStatistic(NumericProperty statistic) {
		requireType(statistic, OPTIMISER_STATISTIC);
		this.statistic = (double) statistic.getValue();
	}

	public void incrementStatistic(final double increment) {
		this.statistic += increment;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == OPTIMISER_STATISTIC)
			statistic = (double) property.getValue();
	}

}