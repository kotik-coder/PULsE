package pulse.search.direction;

import static pulse.math.Matrix.outerProduct;

import pulse.math.Matrix;
import pulse.math.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The 'advanced' {@code PathSolver} implementing the variable-metric
 * (quasi-Newton) search method.
 * <p>
 * The latter does not only rely on the gradient (first derivatives) of the
 * target function, as commonly used in simpler optimisation methods, such as
 * the steepest descent method, but also accounts for the second-order
 * derivatives. This leads to an additional term in the equation defining the
 * minimum direction. This term is called the 'Hessian' matrix, which is
 * calculated approximately using the BFGS formula. Note that the initial value
 * for the 'Hessian' matrix is an identity matrix. It is recommended to use this
 * {@code PathSolver} in combination with the {@code WolfeSolver}.
 * </p>
 * 
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/Broyden%E2%80%93Fletcher%E2%80%93Goldfarb%E2%80%93Shanno_algorithm">Wikipedia
 *      page</a>
 * @see pulse.search.linear.WolfeOptimiser
 */

public class ApproximatedHessianOptimiser extends PathOptimiser {

	private static ApproximatedHessianOptimiser instance = new ApproximatedHessianOptimiser();

	private ApproximatedHessianOptimiser() {
		super();
	}

	/**
	 * Uses an approximation of the Hessian matrix, containing the information on
	 * second derivatives, calculated with the BFGS formula in combination with the
	 * local value of the gradient to evaluate the direction of the minimum on
	 * {@code p}. Invokes {@code p.setDirection()}.
	 */

	@Override
	public Vector direction(Path p) {
		Vector dir = (((ComplexPath) p).getHessian().inverse()).multiply(p.getGradient()).inverted();
		p.setDirection(dir);
		return dir;
	}

	/**
	 * <p>
	 * Calculated the gradient at the end of this step. Invokes {@code hessian(...)}
	 * to calculate the Hessian matrix at the {@code <i>k</i>+1} step using the
	 * <math><i>g</i><sub><i>k</i></sub></math> and
	 * <math><i>g</i><sub><i>k</i>+1</sub></math> gradient values, the previously
	 * calculated Hessian matrix on step <i>k</i>, and the result of the linear
	 * search <i>&alpha;<sub>k</i>+1</sub>.
	 * </p>
	 * 
	 * @throws SolverException
	 */

	@Override
	public void endOfStep(SearchTask task) throws SolverException {
		ComplexPath p = (ComplexPath) task.getPath();
		Vector dir = p.getDirection();

		final double minimumPoint = p.getMinimumPoint();
		final Matrix prevHessian = p.getHessian();
		final Vector g0 = p.getGradient(); // g0
		Vector g1 = gradient(task); // g1

		p.setHessian(hessian(g0, g1, dir, prevHessian, minimumPoint)); // g_k, g_k+1, p_k+1, B_k, alpha_k+1

		p.setGradient(g1); // set g1 as the new gradient for next step

	}

	/**
	 * Uses the BFGS formula to calculate the Hessian.
	 * 
	 * @param g1          gradient at step <i>k</i>
	 * @param g2          gradient at step <i>k</i>+1
	 * @param dir         direction pointing to the minimum at step <i>k</i>+1
	 * @param prevHessian the Hessian matrix at step <i>k</i>
	 * @param alpha       the results of the linear search at step <i>k</i>+1
	 * @return a Hessian {@code Matrix}
	 */

	private Matrix hessian(Vector g1, Vector g2, Vector dir, Matrix prevHessian, double alpha) {
		Vector y = g2.subtract(g1); // g[k+1] - g[k]
		return prevHessian.sum((outerProduct(g1, g1)).multiply(1. / g1.dot(dir)))
				.sum((outerProduct(y, y)).multiply(1. / (alpha * y.dot(dir)))); // BFGS for Ge[k+1]
	}

	@Override
	public String toString() {
		return Messages.getString("ApproximatedHessianSolver.Descriptor");
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static ApproximatedHessianOptimiser getInstance() {
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
		return new ComplexPath(t);
	}

}