package pulse.search.direction;

import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The simplest possible {@code PathSolver}, which assumes that the minimum
 * direction coincides with the inverted gradient. Used in combination with the
 * {@code GoldenSectionSolver} for increased accuracy.
 * 
 * @see pulse.search.linear.GoldenSectionOptimiser
 * @see <a href="https://en.wikipedia.org/wiki/Gradient_descent">Wikipedia
 *      page</a>
 */

public class SteepestDescentOptimiser extends CompositePathOptimiser {

	private static SteepestDescentOptimiser instance = new SteepestDescentOptimiser();

	private SteepestDescentOptimiser() {
		super();
		//init gradient solver
		this.setSolver( p -> {
			
			Vector dir = p.getGradient().inverted(); // p_k = -g
			p.setDirection(dir);
			return dir;
			
		});
	}

	/**
	 * Calculates the gradient value at the end of each step.
	 * 
	 * @throws SolverException
	 */

	@Override
	public void prepare(SearchTask task) throws SolverException {
		task.getPath().setGradient(gradient(task));
	}

	@Override
	public String toString() {
		return Messages.getString("SteepestDescentSolver.Descriptor");
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static SteepestDescentOptimiser getInstance() {
		return instance;
	}

	/**
	 * Creates a new {@code Path} instance for storing the gradient, direction, and
	 * minimum point for this {@code PathSolver}.
	 * 
	 * @param t the search task
	 * @return a {@code Path} instance
	 */

	@Override
	public Path createPath(SearchTask t) {
		this.configure(t);
		return new Path(t);
	}

}