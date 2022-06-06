package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.statements.ClassicalProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

/**
 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
 * <p>
 * Calls {@code super.solve(Problem)}, then initiates constants for calculations
 * and uses a sweep method to evaluate the solution for each subsequent
 * timestep, filling the {@code grid} completely at each specified spatial
 * point. The heating curve is updated with the rear-side temperature
 * <math><i>&Theta;(x<sub>N</sub>,t<sub>i</sub></i></math>) (here
 * <math><i>N</i></math> is the grid density) at the end of {@code timeLimit}
 * intervals, which comprise of {@code timeLimit/tau} time steps. The
 * {@code HeatingCurve} is scaled (re-normalised) by a factor of
 * {@code maxTemp/maxVal}, where {@code maxVal} is the absolute maximum of the
 * calculated solution (with respect to time), and {@code maxTemp} is the
 * {@code maximumTemperature} {@code NumericProperty} of {@code problem}.
 * </p>
 *
 * <p>
 * The semi-implicit scheme uses a 6-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step:
 * <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. The boundary conditions
 * are approximated with a Taylor expansion up to the third term, hence the
 * scheme has an increased order of approximation.
 * </p>
 * <p>
 * The semi-implicit scheme is unconditionally stable and has an order of
 * approximation of <math><i>O(&tau;<sup>2</sup> + h<sup>2</sup>)</i></math>.
 * Note this scheme is prone to spurious oscillations when either a high spatial
 * resolution or a large timestep are used. It has been noticed that due to the
 * pulse term in the boundary condition, a higher error is introduced into the
 * calculation than for the implicit scheme.
 * </p>
 *
 * @see super.solve(Problem)
 */
public class MixedLinearisedSolver extends MixedScheme implements Solver<ClassicalProblem> {

    private double b1;
    private double b2;
    private double b3;
    private double c1;
    private double c2;
    
    private double zeta;

    private final static double EPS = 1e-7; // a small value ensuring numeric stability

    public MixedLinearisedSolver() {
        super();
    }

    public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
    }

    public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
    }

    @Override
    public void prepare(Problem problem) throws SolverException {
        super.prepare(problem);

        var grid = getGrid();

        final double hx = grid.getXStep();
        final double tau = grid.getTimeStep();

        final double Bi1 = (double) problem.getProperties().getHeatLoss().getValue();

        // precalculated constants
        final double HH = pow(hx, 2);
        final double Bi1HTAU = Bi1 * hx * tau;

        // constant for boundary-conditions calculation
        b1 = 1. / (Bi1HTAU + HH + tau);
        b2 = -hx * (Bi1 * tau - hx);
        b3 = hx * tau;
        c1 = b2;
        c2 = Bi1HTAU + HH;
        
        zeta = (double) ((ClassicalProblem)problem).getGeometricFactor().getValue();

        var tridiagonal = new TridiagonalMatrixAlgorithm(grid) {

            @Override
            public double phi(int i) {
                final var U = getPreviousSolution();
                return U[i] / tau + (U[i + 1] - 2. * U[i] + U[i - 1]) / HH;
            }

        };

        setTridiagonalMatrixAlgorithm(tridiagonal);

        final double a1 = tau / (Bi1HTAU + HH + tau);
        tridiagonal.setAlpha(1, a1);

        // coefficients for the finite-difference heat equation
        tridiagonal.setCoefA(1. / pow(hx, 2));
        tridiagonal.setCoefB(2. / tau + 2. / pow(hx, 2));
        tridiagonal.setCoefC(1. / pow(hx, 2));

        tridiagonal.evaluateAlpha();

    }

    @Override
    public double evalRightBoundary(final double alphaN, final double betaN) {
        final var U = getPreviousSolution();

        final var grid = getGrid();
        final double tau = grid.getTimeStep();
        final int N = (int) grid.getGridDensity().getValue();

        return ( c1 * U[N] + tau * betaN + b3 * (1.0 - zeta) * getCurrentPulseValue() 
                - tau * (U[N] - U[N - 1]) ) / (c2 - tau * (alphaN - 1));
    }
    
    @Override
    public double pulse(int m) {
        final double tau = getGrid().getTimeStep();
        var pulse = getDiscretePulse();
        return pulse.laserPowerAt((m - 1 + EPS) * tau) + pulse.laserPowerAt((m - EPS) * tau); 
    }

    @Override
    public double firstBeta() {
        final double tau = getGrid().getTimeStep();
        final var U = getPreviousSolution();
        return b1 * (b2 * U[0] + b3 * zeta * getCurrentPulseValue() - tau * (U[0] - U[1]));
    }

    @Override
    public void solve(ClassicalProblem problem) throws SolverException {
        this.prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new MixedLinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }

    @Override
    public Class<? extends Problem>[] domain() {
        return new Class[]{ClassicalProblem.class};
    }

}
