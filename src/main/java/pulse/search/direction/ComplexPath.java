package pulse.search.direction;

import static pulse.math.linear.Matrices.createIdentityMatrix;

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
	private SquareMatrix inverseHessian;

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
	public void configure(SearchTask task) {
		super.configure(task);
		hessian = createIdentityMatrix(ActiveFlags.activeParameters(task).size());
		inverseHessian = createIdentityMatrix(hessian.getData().length);
	}

	public SquareMatrix getHessian() {
		return hessian;
	}

	public void setHessian(SquareMatrix hes) {
		this.hessian = hes;
	}

	public SquareMatrix getInverseHessian() {
		return inverseHessian;
	}

	public void setInverseHessian(SquareMatrix inverseHessian) {
		this.inverseHessian = inverseHessian;
	}

}