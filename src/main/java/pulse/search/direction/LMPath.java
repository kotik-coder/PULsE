package pulse.search.direction;

import pulse.math.linear.RectangularMatrix;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.search.GeneralTask;

class LMPath extends ComplexPath {

    private static final long serialVersionUID = -7154616034580697035L;
    private Vector residualVector;
    private RectangularMatrix jacobian;
    private SquareMatrix nonregularisedHessian;
    private double lambda;
    private boolean computeJacobian;

    public LMPath(GeneralTask t) {
        super(t);
    }

    @Override
    public void configure(GeneralTask t) {
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

    public boolean isComputeJacobian() {
        return computeJacobian;
    }

    public void setComputeJacobian(boolean computeJacobian) {
        this.computeJacobian = computeJacobian;
    }

}
