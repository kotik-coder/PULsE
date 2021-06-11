package pulse.search.statistics;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.MODEL_CRITERION;
import static pulse.properties.NumericPropertyKeyword.MODEL_WEIGHT;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.util.PropertyEvent;

/**
 * An abstract superclass for the BIC and AIC statistics.
 *
 */

public abstract class ModelSelectionCriterion extends Statistic {

	private OptimiserStatistic os;
	private int kq; 				//the number of parameters (dimensionality of the search vector)
	private final static double PENALISATION_FACTOR = 1.0 + log(2 * PI);
	private double criterion;
	
	public ModelSelectionCriterion(OptimiserStatistic os) {
		super();
		setOptimiser(os);
	}
	
	public ModelSelectionCriterion(ModelSelectionCriterion another) {
		this.os = another.os.copy();
		this.kq = another.kq;
		this.criterion = another.criterion;
	}
	
	@Override
	public void evaluate(SearchTask t) {
		kq = t.alteredParameters().size(); //number of parameters
		calcCriterion();
	}
	
	/**
	 * This calculates either the AIC or BIC statistic, which only differ
	 * by the penalising term.
	 * @see penalisingTerm()
	 */
	
	public void calcCriterion() {
		final int n = os.getResiduals().size(); //sample size
		criterion = n * log(os.variance()) + penalisingTerm(kq,n) + n * PENALISATION_FACTOR;
		this.tellParent(new PropertyEvent(null, this, getStatistic()));
	}
	
	/**
	 * The penalising term, which is different depending on implementation.
	 * @param k the number of model variables
	 * @param n the sample size
	 * @return the penalising term
	 */
	
	public abstract double penalisingTerm(int k, int n);
	
	public abstract ModelSelectionCriterion copy();
	
	/**
	 * Calculates the weight (in the Akaike sense) when comparing the model associated
	 * with this statistic with other models represented by statistics of the same type. 
	 * @param the selection statistics of the same type as this one
	 * @return a {@code NumericProperty} of the {@code MODEL_WEIGHT} type, which is the probability
	 * this model is the best one.
	 */
	
	public NumericProperty weight(List<ModelSelectionCriterion> all) {
		if(all.stream().anyMatch(s -> s.getClass() != this.getClass()))
			throw new IllegalArgumentException("Cannot mix different model selection criteria!");
		final double sum = all.stream().map(criterion -> criterion.probability(all)).reduce( (a, b) -> a + b).get();
		return derive(MODEL_WEIGHT, probability(all)/sum);
	}
	
	/**
	 * Calculates the probability that this model is the best among {@code all} others. 
	 * @param all statistics from models that will be compared with this one
	 * @return the probability, which is a decimal value within the [0,1] range.
	 */
	
	public double probability(List<ModelSelectionCriterion> all) {
		final double min = all.stream().map(criterion -> (double)criterion.getStatistic().getValue()).reduce( (a, b) -> a < b ? a : b).get();
		final double di = (double)this.getStatistic().getValue() - min;
		return exp(-0.5*di);
	}

	@Override
	public String getDescriptor() {
		return "Akaike Information Criterion (AIC)";
	}

	public int getNumVariables() {
		return kq;
	}

	public OptimiserStatistic getOptimiser() {
		return os;
	}

	public void setOptimiser(OptimiserStatistic os) {
		this.os = os;
	}
	
	public void setStatistic(NumericProperty p) {
		requireType(p, MODEL_CRITERION);
		this.criterion = (double)p.getValue();
	}
	
	public NumericProperty getStatistic() {
		return derive(MODEL_CRITERION, criterion);
	}
	
	@Override
	public void set(NumericPropertyKeyword key, NumericProperty p) {
		if(key == MODEL_CRITERION)
			setStatistic(p);
	}
	
}