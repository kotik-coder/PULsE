package pulse.search.statistics;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class Statistic extends PropertyHolder implements Reflexive {

	protected double statistic;

	public NumericProperty getStatistic() {
		return NumericProperty.derive(NumericPropertyKeyword.OPTIMISER_STATISTIC, statistic);
	}

	public void setStatistic(NumericProperty statistic) {
		if (statistic.getType() != NumericPropertyKeyword.OPTIMISER_STATISTIC)
			throw new IllegalArgumentException("Illegal type: " + statistic.getType());
		this.statistic = (double) statistic.getValue();
	}

	public abstract void evaluate(SearchTask t);

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == NumericPropertyKeyword.OPTIMISER_STATISTIC)
			statistic = (double) property.getValue();
	}

}
