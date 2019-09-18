package pulse.search.direction;

import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The simplest possible {@code PathSolver}, which assumes that the minimum direction
 * coincides with the inverted gradient. Used in combination with the {@code GoldenSectionSolver}
 * for increased accuracy. 
 * @see pulse.search.linear.GoldenSectionSolver
 * @see <a href="https://en.wikipedia.org/wiki/Gradient_descent">Wikipedia page</a> 
 */

public class SteepestDescentSolver extends PathSolver {
	
	private static SteepestDescentSolver instance = new SteepestDescentSolver();
	
	private SteepestDescentSolver() { super(); }

	/**
	 * The direction of the minimum at the iteration <math><i>k</i></math> 
	 * is calculated simply as the the inverted gradient vector:
	 * <math><b>p</b><sub><i>k</sub> = -<b>g</b><sub><i>k</sub></math>.
	 */
	
	@Override
	public Vector direction(Path p) {
	    return p.getGradient().invert();   //p_k = -g
	}
	
	/**
	 * Calculates the gradient value at the end of each step.
	 */
	
	@Override 
	public void endOfStep(SearchTask task) {
		task.getPath().setGradient(  gradient(task) );
	}
	
	@Override
	public String toString() {
		return Messages.getString("SteepestDescentSolver.Descriptor"); //$NON-NLS-1$
	}	
	
	/**
	 * This class uses a singleton pattern, meaning there is only instance of this class.
	 * @return the single (static) instance of this class
	 */
	
	public static SteepestDescentSolver getInstance() {
		return instance;
	}

}