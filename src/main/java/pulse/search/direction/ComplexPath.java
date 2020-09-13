package pulse.search.direction;

import static pulse.math.linear.Matrices.createIdentityMatrix;
import static pulse.search.direction.PathOptimiser.activeParameters;
import static pulse.search.direction.PathOptimiser.getSelectedPathOptimiser;
import static pulse.search.direction.PathOptimiser.gradient;

import pulse.math.linear.SquareMatrix;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;

/**
 * <p>
 * A more complex version of {@code Path}, which in addition to other variables
 * stores the Hessian matrix at the current step. Note the {@code reset} method
 * is overriden.
 * </p>
 *
 */

public class ComplexPath extends Path {

	private SquareMatrix hessian;

	protected ComplexPath(SearchTask task) {
		super(task);
	}

	/**
	 * In addition to the superclass method, resets the Hessian to an Identity
	 * matrix.
	 * 
	 * @throws SolverException
	 */

	@Override
	public void reset(SearchTask task) {
		setGradient(gradient(task));
		hessian = createIdentityMatrix(activeParameters(task).size());
		setDirection(getSelectedPathOptimiser().direction(this));
	}

	public SquareMatrix getHessian() {
		return hessian;
	}

	public void setHessian(SquareMatrix hes) {
		this.hessian = hes;
	}

}