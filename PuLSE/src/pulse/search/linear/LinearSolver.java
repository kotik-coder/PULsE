package pulse.search.linear;

import java.util.ArrayList;
import java.util.List;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.search.math.Segment;
import pulse.search.math.Vector;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import static pulse.properties.NumericPropertyKeyword.*;

/**
 * The most basic {@code LinearSolver} class, which defines the notion of 
 * the linear resolution, defines the method signature for estimating the 
 * step of the linear search (i.e., the position of the minimum), and provides
 * a simple algorithm to initialise the calculation domain.
 *
 */

public abstract class LinearSolver extends PropertyHolder implements Reflexive {

	protected static double searchResolution = (double) NumericProperty.def(LINEAR_RESOLUTION).getValue();
	
	protected LinearSolver() {	
		super();
	}	
	
	/**
	 * Finds the minimum of the target function on the {@code domain} {@code Segment}. 
	 * @param task the target function is the sum of squared residuals (SSR) for this {@code task}
	 * @return a double, representing the step magnitude that needs to be 
	 * multiplied by the direction of the search determined previously using 
	 * the {@code PathSolver} to arrive at the next set of parameters corresponding
	 * to a lower SSR value of this {@code task}
	 */
	
	public abstract double linearStep(SearchTask task);	
	
	/**
	 * Sets the domain for this linear search on {@code p}. <p>The domain is defined as a {@code Segment}
	 * {@code [0; max]}, where {@code max} determines the maximum magnitude 
	 * of the {@code linearStep}. This value is calculated initially as 
	 * <code>max = 0.5*x<sub>i</sub>/p<sub>i</sub></code>, where <i>i</i> is 
	 * the index of the {@code DIFFUSIVITY NumericProperty}. Later it is corrected
	 * to ensure that the change in the {@code HEAT_LOSS} {@code NumericProperty} 
	 * is less than unity.</p>   
	 * @param x the current set of parameters
	 * @param p the result of the direction search with the {@code PathSolver}
	 * @return a {@code Segment} defining the domain of this search
	 * @see pulse.search.direction.PathSolver.direction(SearchTask)
	 */
	
	public static Segment domain(IndexedVector x, Vector p) {
		int diffusivityIndex = x.getDataIndex(NumericPropertyKeyword.DIFFUSIVITY);
		double alpha = 0.5*x.get(diffusivityIndex)/Math.abs(p.get(diffusivityIndex));
		
		final double UPPER_LIMIT_LOSSES = 1.0;
		int heatLossIndex = x.getDataIndex(NumericPropertyKeyword.HEAT_LOSS);
		
		if(heatLossIndex > -1)
			while(Math.pow(x.get(heatLossIndex) + p.get(heatLossIndex)*alpha, 2) > UPPER_LIMIT_LOSSES)
				alpha /= 2;
		
		return new Segment(0, alpha);
	}
	
	/**
	 * <p>The linear resolution determines the minimum distance between any 
	 * two points belonging to the {@code domain} of this search while they
	 * still are considered separate. In case of a partitioning method,
	 * e.g. the golden-section search, this determines the partitioning limit.
	 * Note different {@code PathSolver}s can have different sensitivities to the 
	 * linear search and may require different linear resolutions to work
	 * effectively.</p> 
	 * @return a {@code NumericProperty} with the current value of the linear resolution
	 * @see domain(IndexedVector,Vector)
	 */
	
	public static NumericProperty getLinearResolution() {
		return NumericProperty.derive(LINEAR_RESOLUTION, searchResolution);
	}

	public static void setLinearResolution(NumericProperty searchError) {
		LinearSolver.searchResolution = (double)searchError.getValue();
	}
		
	@Override
	public String toString() {
		return this.getClass().getSimpleName();	
	}

	/**
	 * The {@code LINEAR_RESOLUTION} is the single listed parameter for this class.
	 * @see pulse.properties.NumericPropertyKeyword 
	 */
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(LINEAR_RESOLUTION));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case LINEAR_RESOLUTION : setLinearResolution(property); break;
		}
	}
	
}