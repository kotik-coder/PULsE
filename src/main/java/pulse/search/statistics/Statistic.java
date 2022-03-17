package pulse.search.statistics;

import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * A statistic is an abstract class that hosts the {@code evaluate} method to
 * validate the results of a {@code SearchTask}.
 *
 */
public abstract class Statistic extends PropertyHolder implements Reflexive {

    public abstract void evaluate(SearchTask t);

}
