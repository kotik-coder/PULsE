package pulse.search.direction;

import pulse.math.ParameterVector;
import pulse.math.linear.RectangularMatrix;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.tasks.SearchTask;

class LMPath extends ComplexPath {

	private ParameterVector parameters;
	private Vector residualVector;
	private RectangularMatrix jacobian;
	private SquareMatrix nonregularisedHessian;
	private double lambda;
	private boolean computeJacobian;

	public LMPath(SearchTask t) {
		super(t);
	}
	
	@Override
	public void configure(SearchTask t) {
		super.configure(t);
		this.lambda = 1.0;
		computeJacobian = true;
	}

	public RectangularMatrix getJacobian() {
		return jacobian;
	}

	public void setJacobian(RectangularMatrix jacobian) {
		this.jacobian = jacobian;
	}

	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	public SquareMatrix getNonregularisedHessian() {
		return nonregularisedHessian;
	}

	public void setNonregularisedHessian(SquareMatrix nonregularisedHessian) {
		this.nonregularisedHessian = nonregularisedHessian;
	}

	public Vector getResidualVector() {
		return residualVector;
	}

	public void setResidualVector(Vector residualVector) {
		this.residualVector = residualVector;
	}

	public ParameterVector getParameters() {
		return parameters;
	}

	public void setParameters(ParameterVector parameters) {
		this.parameters = parameters;
	}

	public boolean isComputeJacobian() {
		return computeJacobian;
	}

	public void setComputeJacobian(boolean computeJacobian) {
		this.computeJacobian = computeJacobian;
	}

}