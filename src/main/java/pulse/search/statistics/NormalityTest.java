package pulse.search.statistics;

import static pulse.properties.NumericPropertyKeyword.STATISTIC;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class NormalityTest extends PropertyHolder implements Reflexive {

	protected double statistic; 
	protected double probability;
	protected double sd;
	protected static double significance  = (double)NumericProperty.theDefault(SIGNIFICANCE).getValue(); 
	
	protected NormalityTest() {
		probability = (double)NumericProperty.theDefault(PROBABILITY).getValue();
		statistic = (double)NumericProperty.theDefault(STATISTIC).getValue();
	}
	
	public static NumericProperty getStatisticalSignifiance() {
		return NumericProperty.derive(SIGNIFICANCE, significance);
	}
	
	public static void setStatisticalSignificance(NumericProperty alpha) {
		if(alpha.getType() != SIGNIFICANCE)
			throw new IllegalArgumentException("Illegal argument type: " + alpha.getType());
		NormalityTest.significance = (double)alpha.getValue();
	}

	public NumericProperty getStatistic() {
		return NumericProperty.derive(STATISTIC, statistic);
	}
	
	public NumericProperty getProbability() {
		return NumericProperty.derive(PROBABILITY, probability);
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == SIGNIFICANCE)
			setStatisticalSignificance(property);
	}	
	
	public abstract boolean test(SearchTask task);

	public double[] transformResiduals(SearchTask task) {
		var residuals = task.getProblem().getHeatingCurve().getResiduals()
		.stream().map(doubleArray -> doubleArray[1]).mapToDouble(Double::doubleValue).toArray();
		sd = (new StandardDeviation()).evaluate( residuals );
		return residuals;
	}
	
	public double getStandardDeviation() {
		return sd;
	}
	
}
