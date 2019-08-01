package pulse.search.linear;

import java.util.HashMap;
import java.util.Map;

import pulse.properties.NumericProperty;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Segment;
import pulse.search.math.Vector;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;

public abstract class LinearSolver extends PropertyHolder {

	protected static double searchResolution = (double) NumericProperty.DEFAULT_LINEAR_RESOLUTION.getValue();
	
	protected LinearSolver() {	
		super();
	}	
	
	public abstract double minimum(SearchTask task);	
	
	public static NumericProperty getLinearResolution() {
		return new NumericProperty(searchResolution, NumericProperty.DEFAULT_LINEAR_RESOLUTION);
	}

	public static void setLinearResolution(NumericProperty searchError) {
		LinearSolver.searchResolution = (double)searchError.getValue();
	}
		
	@Override
	public String toString() {
		return this.getClass().getSimpleName();	
	}
	
	public static Segment boundaries(ObjectiveFunctionIndex[] types, Vector x, Vector p) {
		double alpha = 0.5*x.get(0)/Math.abs(p.get(0)); //thermal diffusivity
		
		final double UPPER_LIMIT_LOSSES = 1.0;
		
		for(int i = 0; i < types.length; i++) { 
			if(types[i] != ObjectiveFunctionIndex.HEAT_LOSSES)
				continue;
			
			while(Math.pow(x.get(i) + p.get(i)*alpha, 2) > UPPER_LIMIT_LOSSES)
				alpha /= 2;
			
		}
		
		return new Segment(0, alpha);
	}

	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(3);
		map.put(Messages.getString("LinearSolver.0"), Messages.getString("LinearSolver.1"));		 //$NON-NLS-1$ //$NON-NLS-2$
		return map;
	}

	
}
