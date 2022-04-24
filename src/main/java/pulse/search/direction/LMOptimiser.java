package pulse.search.direction;

import java.util.Arrays;
import static pulse.math.linear.SquareMatrix.asSquareMatrix;
import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.DAMPING_RATIO;

import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.linear.Matrices;
import pulse.math.linear.RectangularMatrix;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.search.direction.CompositePathOptimiser.EPS;
import pulse.search.statistics.OptimiserStatistic;
import pulse.search.statistics.ResidualStatistic;
import pulse.search.statistics.SumOfSquares;
import pulse.tasks.SearchTask;
import pulse.tasks.logs.Status;
import pulse.ui.Messages;

/**
 * Given an objective function equal to the sum of squared residuals,
 * iteratively approaches the minimum of this function by applying the
 * Levenberg-Marquardt formulas.
 *
 */
public class LMOptimiser extends GradientBasedOptimiser {

    private static LMOptimiser instance = new LMOptimiser();
    private double dampingRatio;
    
    /**
     * Up to {@value MAX_FAILED_ATTEMPTS} failed attempts are allowed.
     */
    
    public final static int MAX_FAILED_ATTEMPTS = 4;

    private LMOptimiser() {
        super();
        dampingRatio = (double) def(DAMPING_RATIO).getValue();
        this.setSolver(new HessianDirectionSolver() {
            // see default implementation
        });
    }

    @Override
    public boolean iteration(SearchTask task) throws SolverException {
        var p = (LMPath) task.getIterativeState(); // the previous path of the task

        boolean accept = true; //accept the step by default

        /*
		 * Checks whether an iteration limit has been already reached
         */
        if (compare(p.getIteration(), getMaxIterations()) > 0) {

            task.setStatus(Status.TIMEOUT);

        } else {

            double initialCost = task.solveProblemAndCalculateCost();
            var parameters = task.searchVector();

            p.setParameters(parameters); // store current parameters

            prepare(task); // do the preparatory step

            var lmDirection = getSolver().direction(p);

            var candidate = parameters.sum(lmDirection);
            
            if( Arrays.stream( candidate.getData() ).anyMatch(el -> !Double.isFinite(el) ) ) {
                throw new SolverException("Illegal candidate parameters: not finite! " + p.getIteration());
            }
                
            task.assign(new ParameterVector(
                    parameters, candidate)); // assign new parameters
                        
            double newCost = task.solveProblemAndCalculateCost(); // calculate the sum of squared residuals

            /*
			 * Delayed gratification
             */
            if (newCost > initialCost - EPS && p.getFailedAttempts() < MAX_FAILED_ATTEMPTS) {
                p.setLambda(p.getLambda() * 2.0);
                task.assign(parameters); // roll back if cost increased
                p.setComputeJacobian(true);
                p.incrementFailedAttempts();
                accept = false;
            } else {
                task.storeState();
                p.resetFailedAttempts();
                p.setLambda(p.getLambda() / 3.0);
                p.setComputeJacobian(false);
                p.incrementStep(); // increment the counter of successful steps
            }

        }

        return accept; //either accept or reject this step

    }

    /**
     * Calculates the Jacobian, if needed, evaluates the gradient and the
     * Hessian matrix.
     */
    @Override
    public void prepare(SearchTask task) throws SolverException {
        var p = (LMPath) task.getIterativeState();

        //store residual vector at current parameters
        p.setResidualVector(new Vector(residualVector(task.getCurrentCalculation().getOptimiserStatistic())));

        // Calculate the Jacobian -- if needed
        if (p.isComputeJacobian()) {
            p.setJacobian(jacobian(task)); // J
            p.setNonregularisedHessian(halfHessian(p)); // this is just J'J
        }

        // the Jacobian is then used to calculate the 'gradient'
        Vector g1 = halfGradient(p); // g1
        p.setGradient(g1);
        
        if(Arrays.stream(g1.getData()).anyMatch(v -> !Double.isFinite(v))) {
            throw new SolverException("Could not calculate objective function gradient");
        }
            
        // the Hessian is then regularised by adding labmda*I
        var hessian = p.getNonregularisedHessian();
        var damping = (levenbergDamping(hessian).multiply(dampingRatio)
                .sum(marquardtDamping(hessian).multiply(1.0 - dampingRatio)))
                .multiply(p.getLambda());
        var regularisedHessian = asSquareMatrix(hessian.sum(damping)); // J'J + lambda I

        p.setHessian(regularisedHessian); // so this is the new Hessian	

    }

    /**
     * <p>
     * Calculates the Jacobian of the model function given as a discrete set of
     * time-signal values. The elements of the Jacobian are calculated using
     * central differences from two residual vectors evaluated by shifting the
     * search vector slightly to the right or left of each search parameter.
     * </p>
     * <p>
     * This is also equivalent to calculating the difference of the model values
     * when performing the shift, when taking the model values at the time
     * points of the reference dataset. Because of a different discretisation of
     * the model, it is easier to substitute these with the residuals, which had
     * already been interpolated at the reference time values.
     * </p>
     *
     * @param task the task being optimised
     * @return the jacobian matrix
     * @throws SolverException
     * @see pulse.search.statistics.ResidualStatistic.calculateResiduals()
     */
    public RectangularMatrix jacobian(SearchTask task) throws SolverException {

        var residualCalculator = task.getCurrentCalculation().getOptimiserStatistic();
        
        var p = ((LMPath) task.getIterativeState());

        final var params = p.getParameters();

        final int numPoints = p.getResidualVector().dimension();
        final int numParams = params.dimension();

        var jacobian = new double[numPoints][numParams];

        for (int i = 0; i < numParams; i++) {

            double dx = dx( NumericProperties.def(params.getIndex(i)), params.get(i));

            final var shift = new Vector(numParams);
            shift.set(i, 0.5 * dx);

            // + shift
            task.assign(new ParameterVector(params, params.sum(shift)));
            task.solveProblemAndCalculateCost();
            var r1 = residualVector(residualCalculator);

            // - shift
            task.assign(new ParameterVector(params, params.subtract(shift)));
            task.solveProblemAndCalculateCost();
            var r2 = residualVector(residualCalculator);

            for (int j = 0, realNumPoints = Math.min(numPoints, r2.length); j < realNumPoints; j++) {

                jacobian[j][i] = (r1[j] - r2[j]) / dx;

            }

        }
        
        // revert to original params
        task.assign(params);

        return Matrices.createMatrix(jacobian);

    }

    private static double[] residualVector(ResidualStatistic rs) {
        return rs.getResiduals().stream().mapToDouble(array -> array[1]).toArray();
    }

    @Override
    public GradientGuidedPath initState(SearchTask t) {
        this.configure(t);
        return new LMPath(t);
    }

    private Vector halfGradient(LMPath path) {
        var jacobian = path.getJacobian();
        var residuals = path.getResidualVector();
        return jacobian.transpose().multiply(new Vector(residuals));
    }

    private SquareMatrix halfHessian(LMPath path) {
        var jacobian = path.getJacobian();
        return asSquareMatrix(jacobian.transpose().multiply(jacobian));
    }

    /*
	 * Additive damping strategy, where the scaling matrix is simply the identity matrix.
     */
    private SquareMatrix levenbergDamping(SquareMatrix hessian) {
        return Matrices.createIdentityMatrix(hessian.getData().length);
    }

    /*
	 * Multiplicative damping strategy, where the scaling matrix is equal to the 'hessian' block-diagonal matrix.
	 * Works best for badly scaled problems. However, this is also scale-invariant, 
	 * which mean it increases the susceptibility to parameter evaporation.
     */
    private SquareMatrix marquardtDamping(SquareMatrix hessian) {
        return hessian.blockDiagonal();
    }
    
   @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(DAMPING_RATIO);
        return set;
    }

    /**
     * This class uses a singleton pattern, meaning there is only instance of
     * this class.
     *
     * @return the single (static) instance of this class
     */
    public static LMOptimiser getInstance() {
        return instance;
    }

    @Override
    public String toString() {
        return Messages.getString("LMOptimiser.Descriptor");
    }

    /**
     * The Levenberg-Marquardt optimiser will only accept ordinary least-squares
     * as its objective function. Therefore, {@code os} should be an instance of
     * {@code SumOfSquares}.
     *
     * @return {@code true} if {@code.getClass()} returns
     * {@code SumOfSquares.class}, {@code false} otherwise
     */
    @Override
    public boolean compatibleWith(OptimiserStatistic os) {
        return os.getClass().equals(SumOfSquares.class);
    }

    public NumericProperty getDampingRatio() {
        return derive(DAMPING_RATIO, dampingRatio);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        if (type == DAMPING_RATIO) {
            setDampingRatio(property);
        }
    }

    public void setDampingRatio(NumericProperty dampingRatio) {
        requireType(dampingRatio, DAMPING_RATIO);
        this.dampingRatio = (double) dampingRatio.getValue();
        firePropertyChanged(this, dampingRatio);
    }

}
