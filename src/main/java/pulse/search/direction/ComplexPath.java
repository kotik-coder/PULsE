package pulse.search.direction;

import static pulse.math.linear.Matrices.createIdentityMatrix;

import pulse.math.linear.SquareMatrix;
import pulse.search.GeneralTask;

/**
 * <p>
 * A more complex version of {@code Path}, which in addition to other variables
 * stores the Hessian matrix at the current step. Note the {@code reset} method
 * is overriden.
 * </p>
 *
 */
public class ComplexPath extends GradientGuidedPath {

    private SquareMatrix hessian;
    private SquareMatrix inverseHessian;

    protected ComplexPath(GeneralTask task) {
        super(task);
    }

    /**
     * In addition to the superclass method, resets the Hessian to an Identity
     * matrix.
     *
     * @param task
     */
    @Override
    public void configure(GeneralTask task) {
        hessian = createIdentityMatrix(this.getParameters().dimension());
        inverseHessian = createIdentityMatrix(hessian.getData().length);
        super.configure(task);
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
