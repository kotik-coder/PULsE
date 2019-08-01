package pulse.search.direction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.search.linear.LinearSolver;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.util.ReflexiveFinder;

public abstract class PathSolver extends PropertyHolder implements Reflexive {
	
	private static double errorTolerance	 = (double)NumericProperty.DEFAULT_ERROR_TOLERANCE.getValue();
	private static double gradientResolution = (double)NumericProperty.DEFAULT_GRADIENT_RESOLUTION.getValue();
	private static BooleanProperty[] globalSearchFlags;
	
	private static LinearSolver linearSolver = null;
	
	static {
	
		ObjectiveFunctionIndex[] values = ObjectiveFunctionIndex.values();
		globalSearchFlags = new BooleanProperty[values.length];
		
		for(int i = 0; i < values.length; i++) 
			globalSearchFlags[i] = new BooleanProperty(values[i].name(), values[i].isActiveByDefault());
		
	}
	
	protected PathSolver() {
		super();				
	}
	
	public double iteration(SearchTask task) {
		//get current parameters
		Vector parameters	= task.objectiveFunction();
		
		//find min direction
		Path p			= task.getPath(); //info with previous grads, hesse, etc.
		Vector dir		= direction(p);
		p.setDirection( dir );
		
		//find step magnitude
		double   minimumPoint = linearSolver.minimum(task);
		p.setMinimumPoint(minimumPoint);
		
		//assign new parameters
		Vector newParams 			= parameters.plus(dir.times(minimumPoint));
			
		task.assign(newParams);
		
		//compute gradients, hessians, etc. with new parameters
		endOfStep(task);
		
		p.incrementStep();
		
		//cals SS
		return task.calculateDeviation();
	} 
	
	public abstract Vector direction(Path p);
	public abstract void endOfStep(SearchTask task);
	
	public static Vector gradient(SearchTask task) {
		
		final Vector params = task.objectiveFunction();
		
		Vector grad				= new Vector(params.dimension());
		
		Vector newParams, shift;
		double ss1, ss2;
		
		final double dx = 2.0*gradientResolution;
		
		for(int i = 0; i < params.dimension(); i++) {
			shift = new Vector(params.dimension());
			shift.set(i, 0.5*dx);
			
			newParams	= params.plus(shift);
			task.assign(newParams);
			ss2			= task.calculateDeviation();
			
			newParams	= params.minus(shift);
			task.assign(newParams);
			ss1			= task.calculateDeviation();
			
			grad.set(i, ( ss2 - ss1 ) / dx );
					
		}			
		
		task.assign(params);
		
		return grad;
		
	}	
	
	public static LinearSolver getLinearSolver() {
		return linearSolver;
	}

	public void setLinearSolver(LinearSolver linearSearch) {
		PathSolver.linearSolver = linearSearch;
		this.updatePropertyNames();
		
	}

	public static NumericProperty getErrorTolerance() {
		return new NumericProperty(errorTolerance, NumericProperty.DEFAULT_ERROR_TOLERANCE);
	}

	public static void setErrorTolerance(NumericProperty errorTolerance) {
		PathSolver.errorTolerance = (double)errorTolerance.getValue();
	}

	public static void setGradientResolution(NumericProperty resolution) {
		PathSolver.gradientResolution = (double) resolution.getValue();
	}
	
	public static NumericProperty getGradientResolution() {
		return new NumericProperty(gradientResolution, NumericProperty.DEFAULT_GRADIENT_RESOLUTION);
	}	
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	public static PathSolver[] findAllDirectionSolvers(String pckgname) {		
		List<Reflexive> ref = new LinkedList<Reflexive>();
		List<PathSolver> p = new LinkedList<PathSolver>();
		
		ref.addAll(ReflexiveFinder.findAllInstances(pckgname));
		
		for(Reflexive r : ref) 
			if(r instanceof PathSolver)
				p.add((PathSolver) r);
		
		return (PathSolver[])p.toArray(new PathSolver[p.size()]);
		
	}
	
	public static PathSolver[] findAllDirectionSolvers() {		
		return findAllDirectionSolvers(PathSolver.class.getPackage().getName());
		
	}
	
	public static void set(String id, BooleanProperty p) {
		boolean flag = (boolean) p.getValue();
		for(BooleanProperty property : globalSearchFlags)
			if(property.getSimpleName().equals(id)) 
				property.setValue(flag);
		
		ObjectiveFunctionIndex index = ObjectiveFunctionIndex.valueOf(id);
		
		for(SearchTask t : TaskManager.getTaskList())
			t.setSearchFlag(index, flag);
	}
	
	public static BooleanProperty[] getSearchFlags() {
		return globalSearchFlags;
	}

	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(9);
		map.put(Messages.getString("PathSolver.0"), Messages.getString("PathSolver.1")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Messages.getString("PathSolver.2"), Messages.getString("PathSolver.3")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Messages.getString("PathSolver.4"), Messages.getString("PathSolver.5")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Messages.getString("PathSolver.6"), Messages.getString("PathSolver.7")); //$NON-NLS-1$ //$NON-NLS-2$
		for(BooleanProperty property : globalSearchFlags) 
			map.put( property.getSimpleName(), Messages.getString("PathSolver.8") + property.getSimpleName() + Messages.getString("PathSolver.9") ); //$NON-NLS-1$ //$NON-NLS-2$
		
		if(linearSolver != null) {
			Map<String,String> mapLinear = linearSolver.propertyNames();		
			Map<String,String> mapComplete = new HashMap<String,String>(10);
			mapComplete.putAll(map);
			mapComplete.putAll(mapLinear);
			return mapComplete;
		}
		return map;
		
	}
		
}
