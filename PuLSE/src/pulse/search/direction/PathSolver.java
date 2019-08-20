package pulse.search.direction;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.linear.LinearSolver;
import pulse.search.math.IndexedVector;
import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class PathSolver extends PropertyHolder implements Reflexive {
	
	private static double errorTolerance	 = (double)NumericProperty.ERROR_TOLERANCE.getValue();
	private static double gradientResolution = (double)NumericProperty.GRADIENT_RESOLUTION.getValue();

	private static LinearSolver linearSolver = null;
	private static List<Flag> globalSearchFlags = Flag.defaultList();
	
	protected PathSolver() {
		super();				
	}
	
	public static void reset() {
		linearSolver = null;
		globalSearchFlags = Flag.defaultList(); 
	}
	
	public double iteration(SearchTask task) {
		//get current parameters
		IndexedVector parameters = task.objectiveFunction();
		
		//find min direction
		Path p			= task.getPath(); //info with previous grads, hesse, etc.
		Vector dir		= direction(p);
		p.setDirection( dir );
		
		//find step magnitude
		double   minimumPoint = linearSolver.minimum(task);
		p.setMinimumPoint(minimumPoint);
		
		//assign new parameters
		Vector newParams 			= parameters.plus(dir.times(minimumPoint));
			
		task.assign( new IndexedVector(newParams, parameters.getIndices()) );
		
		//compute gradients, hessians, etc. with new parameters
		endOfStep(task);
		
		p.incrementStep();
		
		//cals SS
		return task.calculateDeviation();
	} 
	
	public abstract Vector direction(Path p);
	public abstract void endOfStep(SearchTask task);
	
	public static Vector gradient(SearchTask task) {
		
		final IndexedVector params = task.objectiveFunction();
		
		Vector grad				= new Vector(params.dimension());
		
		Vector newParams, shift;
		double ss1, ss2;
		
		final double dx = 2.0*gradientResolution;
		
		for(int i = 0; i < params.dimension(); i++) {
			shift = new Vector(params.dimension());
			shift.set(i, 0.5*dx);
			
			newParams	= params.plus(shift);
			task.assign( new IndexedVector(newParams, params.getIndices()) );
			ss2			= task.calculateDeviation();
			
			newParams	= params.minus(shift);
			task.assign( new IndexedVector(newParams, params.getIndices()) );
			ss1			= task.calculateDeviation();
			
			grad.set(i, ( ss2 - ss1 ) / dx );
					
		}			
		
		task.assign( params );
		
		return grad;
		
	}	
	
	public static LinearSolver getLinearSolver() {
		return linearSolver;
	}

	public void setLinearSolver(LinearSolver linearSearch) {
		PathSolver.linearSolver = linearSearch;
		super.parameterListChanged();
	}

	public static NumericProperty getErrorTolerance() {
		return new NumericProperty(errorTolerance, NumericProperty.ERROR_TOLERANCE);
	}

	public static void setErrorTolerance(NumericProperty errorTolerance) {
		PathSolver.errorTolerance = (double)errorTolerance.getValue();
	}

	public static void setGradientResolution(NumericProperty resolution) {
		PathSolver.gradientResolution = (double) resolution.getValue();
	}
	
	public static NumericProperty getGradientResolution() {
		return new NumericProperty(gradientResolution, NumericProperty.GRADIENT_RESOLUTION);
	}	
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	public static List<Flag> getSearchFlags() {
		return globalSearchFlags;
	}
	
	@Override
	public List<Property> genericProperties() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Property> original = super.genericProperties();
		original.addAll(globalSearchFlags);
		return original;
	}
	
	public static Flag getFlag(NumericPropertyKeyword index) {
		return globalSearchFlags.stream().filter(flag -> flag.getType() == index).findFirst().get();
	}
	
	public static void setSearchFlag(List<Flag> originalList, Flag flag) {
		for(Flag f : originalList) 
			if(f.getType() == flag.getType()) 
				f.setValue(flag.getValue());		
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = super.listedParameters();
		list.add(NumericProperty.GRADIENT_RESOLUTION);
		list.add(NumericProperty.ERROR_TOLERANCE);
		for(Flag property : globalSearchFlags) 
			list.add( property );
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case GRADIENT_RESOLUTION : setGradientResolution(property); break;
		case ERROR_TOLERANCE : setErrorTolerance(property); break;
		}
	}
	
	@Override
	public boolean internalHolderPolicy() {
		return false;
	}
	
	public static List<NumericPropertyKeyword> activeParameters() {
		return PathSolver.getSearchFlags().stream()
				.filter(flag -> (boolean)flag.getValue())
				.map(flag -> flag.getType()).collect(Collectors.toList());
	}
		
}