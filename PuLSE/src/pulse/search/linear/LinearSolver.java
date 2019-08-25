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

public abstract class LinearSolver extends PropertyHolder implements Reflexive {

	protected static double searchResolution = (double) NumericProperty.def(LINEAR_RESOLUTION).getValue();
	
	protected LinearSolver() {	
		super();
	}	
	
	public abstract double minimum(SearchTask task);	
	
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
	
	public static Segment boundaries(IndexedVector x, Vector p) {
		int diffusivityIndex = x.getDataIndex(NumericPropertyKeyword.DIFFUSIVITY);
		double alpha = 0.5*x.get(diffusivityIndex)/Math.abs(p.get(diffusivityIndex));
		
		final double UPPER_LIMIT_LOSSES = 1.0;
		int heatLossIndex = x.getDataIndex(NumericPropertyKeyword.HEAT_LOSS);
		
		if(heatLossIndex > -1)
			while(Math.pow(x.get(heatLossIndex) + p.get(heatLossIndex)*alpha, 2) > UPPER_LIMIT_LOSSES)
				alpha /= 2;
		
		return new Segment(0, alpha);
	}

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
