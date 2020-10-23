package pulse.search.direction;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;

public interface HessianDirectionSolver extends DirectionSolver {

	/**
	 * Uses an approximation of the Hessian matrix, containing the information on
	 * second derivatives, calculated with the BFGS formula in combination with the
	 * local value of the gradient to evaluate the direction of the minimum on
	 * {@code p}. Invokes {@code p.setDirection()}.
	 * @throws SolverException 
	 */

	public default Vector direction(Path p) throws SolverException {
		var cp = (ComplexPath) p;
		final int dimg = p.getGradient().dimension();

		Vector invGrad = p.getGradient().inverted();
		Vector result;

		// use linear solver for big matrices
		if (dimg > 4) {

			var hess = new DMatrixRMaj(cp.getHessian().getData());
			var antigrad = new DMatrixRMaj(invGrad.getData());
			var dirv = new DMatrixRMaj(dimg, 1);

			if (!CommonOps_DDRM.solve(hess, antigrad, dirv)) {
				throw new SolverException("Singular matrix!");
			}

			result = new Vector(dirv.getData());

		} else // use fast inverse
			result = cp.getHessian().inverse().multiply(invGrad);

		p.setDirection(result);
		return result;
	}

}