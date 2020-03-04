package pulse.search.statistics;

import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import java.util.Set;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.util.Reflexive;

public abstract class NormalityTest extends ResidualStatistic {

	protected double probability;
	protected static double significance  = (double)NumericProperty.theDefault(SIGNIFICANCE).getValue();
	
	private static String selectedTestDescriptor;
	
	protected NormalityTest() {
		probability = (double)NumericProperty.theDefault(PROBABILITY).getValue();
		statistic = (double)NumericProperty.theDefault(TEST_STATISTIC).getValue();
	}
	
	public static NumericProperty getStatisticalSignifiance() {
		return NumericProperty.derive(SIGNIFICANCE, significance);
	}
	
	public static void setStatisticalSignificance(NumericProperty alpha) {
		if(alpha.getType() != SIGNIFICANCE)
			throw new IllegalArgumentException("Illegal argument type: " + alpha.getType());
		NormalityTest.significance = (double)alpha.getValue();
	}
	
	public NumericProperty getProbability() {
		return NumericProperty.derive(PROBABILITY, probability);
	}
	
	public abstract boolean test(SearchTask task);

	public static String getSelectedTestDescriptor() {
		return selectedTestDescriptor;
	}

	public static void setSelectedTestDescriptor(String selectedTestDescriptor) {
		NormalityTest.selectedTestDescriptor = selectedTestDescriptor;
	}
	
	@Override
	public NumericProperty getStatistic() {
		return NumericProperty.derive(TEST_STATISTIC, statistic);
	}

	@Override
	public void setStatistic(NumericProperty statistic) {
		if(statistic.getType() != TEST_STATISTIC)
			throw new IllegalArgumentException("Illegal type: " + statistic.getType());
		this.statistic = (double)statistic.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == TEST_STATISTIC)
			statistic = (double)property.getValue();
	}
	
	public static Set<String> allTestDescriptors() {
		return Reflexive.instancesOf(NormalityTest.class).stream().map(t -> t.getDescriptor()).collect(Collectors.toSet());
	}
	
}