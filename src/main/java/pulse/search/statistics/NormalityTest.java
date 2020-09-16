package pulse.search.statistics;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

public abstract class NormalityTest extends ResidualStatistic {

	private double statistic;
	private double probability;
	private static double significance = (double) def(SIGNIFICANCE).getValue();

	private static String selectedTestDescriptor;

	protected NormalityTest() {
		probability = (double) def(PROBABILITY).getValue();
		statistic = (double) def(TEST_STATISTIC).getValue();
	}
	
	public boolean significanceTest() {
		return probability > significance;
	}

	public static NumericProperty getStatisticalSignifiance() {
		return derive(SIGNIFICANCE, significance);
	}

	public static void setStatisticalSignificance(NumericProperty alpha) {
		requireType(alpha, SIGNIFICANCE);
		NormalityTest.significance = (double) alpha.getValue();
	}

	public NumericProperty getProbability() {
		return derive(PROBABILITY, probability);
	}

	public abstract boolean test(SearchTask task);

	@Override
	public NumericProperty getStatistic() {
		return derive(TEST_STATISTIC, statistic);
	}

	@Override
	public void setStatistic(NumericProperty statistic) {
		requireType(statistic, TEST_STATISTIC);
		this.statistic = (double) statistic.getValue();
	}
	
	public void setProbability(NumericProperty probability) {
		requireType(probability, PROBABILITY);
		this.probability = (double) probability.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == TEST_STATISTIC)
			statistic = (double) property.getValue();
	}

	public static String getSelectedTestDescriptor() {
		return selectedTestDescriptor;
	}

	public static void setSelectedTestDescriptor(String selectedTestDescriptor) {
		NormalityTest.selectedTestDescriptor = selectedTestDescriptor;
	}

}