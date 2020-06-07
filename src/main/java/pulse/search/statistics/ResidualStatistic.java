package pulse.search.statistics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.IndexRange;
import pulse.tasks.SearchTask;

public abstract class ResidualStatistic extends Statistic {

	private List<double[]> residuals;
	private static String selectedOptimiserDescriptor;

	public ResidualStatistic() {
		super();
		residuals = new LinkedList<double[]>();
		setPrefix("Residuals");
	}

	public double[] transformResiduals(SearchTask task) {
		var residuals = task.getResidualStatistic().getResiduals().stream().map(doubleArray -> doubleArray[1])
				.mapToDouble(Double::doubleValue).toArray();
		return residuals;
	}

	public void calculateResiduals(SearchTask task) {
		HeatingCurve estimate = task.getProblem().getHeatingCurve();
		ExperimentalData reference = task.getExperimentalCurve();

		double interpolated, diff;

		residuals = new ArrayList<double[]>();

		var s = estimate.getSplineInterpolation();
		
		for (int startIndex = Math.max( IndexRange.closest(estimate.timeAt(1), reference.getTimeSequence()), reference.getIndexRange().getLowerBound() ) , 
				 endIndex = reference.getIndexRange()
				.getUpperBound(), i = startIndex; i <= endIndex; i++) {
			/*
			 * find the point on the calculated heating curve which has the closest time
			 * value smaller than the experimental points' time value
			 */
		
			interpolated = s.value( reference.timeAt(i) );
			diff = reference.temperatureAt(i) - interpolated; // y_exp - y*

			residuals.add(new double[] { reference.timeAt(i), diff });

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

}