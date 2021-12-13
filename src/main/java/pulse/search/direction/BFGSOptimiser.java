package pulse.search.direction;

import static pulse.math.linear.SquareMatrix.asSquareMatrix;
import static pulse.math.linear.SquareMatrix.outerProduct;

import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
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
 * page</a>
 * @see pulse.search.linear.WolfeOptimiser
 */
public class BFGSOptimiser extends CompositePathOptimiser {

    private static BFGSOptimiser instance = new BFGSOptimiser();

    private BFGSOptimiser() {
        super();
        this.setSolver(new HessianDirectionSolver() {
            //empty statement
        });
    }

    /**
     * <p>
     * Calculated the gradient at the end of this step. Invokes
     * {@code hessian(...)} to calculate the Hessian matrix at the
     * {@code <i>k</i>+1} step using the
     * <math><i>g</i><sub><i>k</i></sub></math> and
     * <math><i>g</i><sub><i>k</i>+1</sub></math> gradient values, the
     * previously calculated Hessian matrix on step <i>k</i>, and the result of
     * the linear search <i>&alpha;<sub>k</i>+1</sub>.
     * </p>
     *
     * @throws SolverException
     */
    @Override
    public void prepare(SearchTask task) throws SolverException {
        var p = (ComplexPath) task.getIterativeState();
        Vector dir = p.getDirection(); //p[k]

        final double minimumPoint = p.getMinimumPoint(); // alpha[k]
        final SquareMatrix prevHessian = p.getHessian(); // B[k]

        final Vector g0 = p.getGradient();	// g[k]
        final Vector g1 = gradient(task); 	// g[k+1]

        var hessian = hessian(g0, g1, dir, prevHessian, minimumPoint); //B[k+1]

        p.setHessian(hessian); // g_k, g_k+1, p_k+1, B_k, alpha_k+1
        p.setGradient(g1); // set g1 as the new gradient for next step
    }
    
    /**
     * Uses the BFGS formula to calculate the Hessian. 
     *
     * @param g1 gradient at step <i>k</i>
     * @param g2 gradient at step <i>k</i>+1
     * @param dir direction pointing to the minimum at step <i>k</i>+1
     * @param prevHessian the Hessian matrix at step <i>k</i>
     * @param alpha the results of the linear search at step <i>k</i>+1
     * @return a Hessian {@code Matrix}
     */
    private SquareMatrix hessian(Vector g1, Vector g2, Vector dir, SquareMatrix prevHessian, double alpha) {
        Vector y = g2.subtract(g1); // g[k+1] - g[k]

        var m = prevHessian.sum((outerProduct(g1, g1)).multiply(1. / g1.dot(dir)))
                .sum((outerProduct(y, y)).multiply(1. / (alpha * y.dot(dir)))); // BFGS formula

        return asSquareMatrix(m);
    }

    @Override
    public String toString() {
        return Messages.getString("ApproximatedHessianSolver.Descriptor");
    }

    /**
     * This class uses a singleton pattern, meaning there is only instance of
     * this class.
     *
     * @return the single (static) instance of this class
     */
    public static BFGSOptimiser getInstance() {
        return instance;
    }

}
