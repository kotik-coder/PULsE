package pulse.search.statistics;

import pulse.properties.NumericProperty;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class Statistic extends PropertyHolder implements Reflexive {

	public abstract void evaluate(SearchTask t);
	public abstract NumericProperty getStatistic();
	public abstract void setStatistic(NumericProperty statistic);

}