package pulse.problem.schemes.solvers;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;
import java.util.Set;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.FixedPointIterations;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse2D;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
import pulse.properties.Property;

public class ImplicitNonlinearSolver extends ImplicitScheme implements Solver<NonlinearProblem>, FixedPointIterations {

    private int N;
    private double HH;
    private double tau;
    private double pls;

    private double dT_T;

    private double b1;
    private double c1;
    private double c2;
    private double b2;
    private double b3;

    private double nonlinearPrecision;

    public ImplicitNonlinearSolver() {
        super();
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
    }

    public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
    }

    public ImplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
    }

    private void prepare(NonlinearProblem problem) {
        super.prepare(problem);

        var grid = getGrid();

        N = (int) grid.getGridDensity().getValue();
        final double hx = grid.getXStep();
        tau = grid.getTimeStep();

        HH = hx * hx;

        var p = problem.getProperties();

        final double Bi1 = (double) p.getHeatLoss().getValue();

        final double T = (double) p.getTestTemperature().getValue();
        final double dT = p.maximumHeating((Pulse2D) problem.getPulse());
        dT_T = dT / T;

        // constant for bc calc
        final double a1 = 2. * tau / (HH + 2. * tau);
        b1 = HH / (2. * tau + HH);
        b2 = a1 * hx;
        b3 = Bi1 * T / (4.0 * dT);
        c1 = -0.5 * hx * tau * Bi1 * T / dT;

        var tridiagonal = getTridiagonalMatrixAlgorithm();

        tridiagonal.setCoefA(1.0 / HH);
        tridiagonal.setCoefB(1.0 / tau + 2.0 / HH);
        tridiagonal.setCoefC(1.0 / HH);

        tridiagonal.setAlpha(1, a1);
        tridiagonal.evaluateAlpha();
        c2 = 1. / (HH + 2. * tau - 2 * tridiagonal.getAlpha()[N] * tau);
    }

    @Override
    public void solve(NonlinearProblem problem) throws SolverException {
        prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ImplicitNonlinearSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }

    @Override
    public Class<? extends Problem> domain() {
        return NonlinearProblem.class;
    }

    public NumericProperty getNonlinearPrecision() {
        return derive(NONLINEAR_PRECISION, nonlinearPrecision);
    }

    public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
        this.nonlinearPrecision = (double) nonlinearPrecision.getValue();
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(NONLINEAR_PRECISION);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        switch (type) {
            case NONLINEAR_PRECISION:
                setNonlinearPrecision(property);
                break;
            default:
                throw new IllegalArgumentException("Property not recognised: " + property);
        }
    }

    @Override
    public void timeStep(final int m) throws SolverException {
        pls = pulse(m);
        doIterations(getCurrentSolution(), nonlinearPrecision, m);
    }

    @Override
    public void iteration(int m) throws SolverException {
        super.timeStep(m);
    }

    @Override
    public double evalRightBoundary(int m, double alphaN, double betaN) {
        return c2 * (2. * betaN * tau + HH * getPreviousSolution()[N] 
                + c1 * (fastPowLoop(getCurrentSolution()[N] * dT_T + 1, 4) - 1));
    }

    @Override
    public double firstBeta(int m) {
        return b1 * getPreviousSolution()[0] + b2 * (pls - b3 * (fastPowLoop(getCurrentSolution()[0] * dT_T + 1, 4) - 1));
    }

}
