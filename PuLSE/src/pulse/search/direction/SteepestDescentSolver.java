package pulse.search.direction;

import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;

public class SteepestDescentSolver extends PathSolver {
	
	private static SteepestDescentSolver instance = new SteepestDescentSolver();
	
	private SteepestDescentSolver() {
		super();
	}

	@Override
	public Vector direction(Path p) {
	    return p.getGradient().invert();   //p_k = -g
	}
	
	@Override 
	public void endOfStep(SearchTask task) {
		task.getPath().setGradient(  gradient(task) );
	}
	
	@Override
	public String toString() {
		return Messages.getString("SteepestDescentSolver.Descriptor"); //$NON-NLS-1$
	}	
	
	public static SteepestDescentSolver getInstance() {
		return instance;
	}

}
