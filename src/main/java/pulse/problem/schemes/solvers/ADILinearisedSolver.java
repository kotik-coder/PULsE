package pulse.problem.schemes.solvers;

import pulse.problem.laser.DiscretePulse2D;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid2D;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.statements.ClassicalProblem2D;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.ExtendedThermalProperties;
import pulse.properties.NumericProperty;

/**
 * An alternating direction implicit (ADI) solver for a classical
 * two-dimensional linearised problem.
 *
 */
public class ADILinearisedSolver extends ADIScheme implements Solver<ClassicalProblem2D> {

    private TridiagonalMatrixAlgorithm tridiagonal;

    private int N;
    private double hx;
    private double hy;
    private double tau;
    private int firstIndex;
    private int lastIndex;

    private double d;
    private double l;
    private double Bi1;
    private double Bi3;

    private double[][] U1;
    private double[][] U2;
    private double[][] U1_E;
    private double[][] U2_E;

    private double[] a1;
    private double[] b1;
    private double[] c1;

    private double a2;
    private double b2;
    private double c2;

    private double a11;
    private double _a11;
    private double b11;
    private double _b11;
    private double _b12;
    private double _c11;
    private double HX2;
    private double HY2;

    private double C1_U2;
    private double C2_U2;
    private double C3_U2;

    private double C1_U1;

    private double TAU_HY;
    private double OMEGA_SQ_HX2;
    private double E_C_U2;
    private double E_C_U1;

    private final static double EPS = 1e-8;

    public ADILinearisedSolver() {
        super();
    }

    public ADILinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
    }

    public ADILinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
    }

    private void prepare(ClassicalProblem2D problem) {
        super.prepare(problem);

        var grid = getGrid();
        tridiagonal = new TridiagonalMatrixAlgorithm(grid);

        N = (int) grid.getGridDensity().getValue();

        hx = grid.getXStep();
        hy = ((Grid2D) getGrid()).getYStep();
        HX2 = hx * hx;
        HY2 = hy * hy;

        tau = grid.getTimeStep();

        var properties = (ExtendedThermalProperties) problem.getProperties();

        Bi1 = (double) properties.getHeatLoss().getValue();
        Bi3 = (double) properties.getSideLosses().getValue();

        d = (double) properties.getSampleDiameter().getValue();
        final double fovOuter = (double) properties.getFOVOuter().getValue();
        final double fovInner = (double) properties.getFOVInner().getValue();
        l = (double) properties.getSampleThickness().getValue();

        // end
        U1 = new double[N + 1][N + 1];
        U2 = new double[N + 1][N + 1];

        U1_E = new double[N + 3][N + 3];
        U2_E = new double[N + 3][N + 3];

        a1 = new double[N + 1];
        b1 = new double[N + 1];
        c1 = new double[N + 1];

        // a[i]*u[i-1] - b[i]*u[i] + c[i]*u[i+1] = F[i]
        lastIndex = (int) (fovOuter / d / hx);
        lastIndex = lastIndex > N ? N : lastIndex;

        firstIndex = (int) (fovInner / d / hx);
        firstIndex = firstIndex < 0 ? 0 : firstIndex;

        initConst();
    }

    // precalculated FD constants
    private void initConst() {
        final double OMEGA = 2.0 * l / d;
        final double OMEGA_SQ = OMEGA * OMEGA;

        for (int i = 1; i < N + 1; i++) {
            a1[i] = OMEGA_SQ * (i - 0.5) / (HX2 * i);
            b1[i] = 2. / tau + 2. * OMEGA_SQ / HX2;
            c1[i] = OMEGA_SQ * (i + 0.5) / (HX2 * i);
        }

        a2 = 1. / HY2;
        b2 = 2. / HY2 + 2. / tau;
        c2 = 1. / HY2;

        // precalc coefs
        a11 = 1.0 / (1.0 + HX2 / (OMEGA_SQ * tau));
        b11 = 0.5 * tau / (1.0 + OMEGA_SQ * tau / HX2);

        _a11 = 1.0 / (1.0 + Bi1 * hy + HY2 / tau);
        _b11 = 1.0 / ((1 + hy * Bi1) * tau + HY2);
        _c11 = 0.5 * HY2 * tau * OMEGA_SQ / HX2;
        _b12 = _c11 * _b11;

        C1_U2 = 1.0 + hx * OMEGA * Bi3;
        C2_U2 = OMEGA_SQ * tau;
        C3_U2 = HX2 * tau / (2.0 * HY2);

        C1_U1 = 1.0 + hy * Bi1;

        TAU_HY = tau * hy;
        OMEGA_SQ_HX2 = OMEGA_SQ / HX2;

        E_C_U2 = 2.0 * hx * OMEGA * Bi3;
        E_C_U1 = 2.0 * hy * Bi1;
    }

    @Override
    public void solve(ClassicalProblem2D problem) {
        prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ADILinearisedSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }

    @Override
    public Class<? extends Problem> domain() {
        return ClassicalProblem2D.class;
    }

    @Override
    public double signal() {
        double sum = 0;

        for (int i = firstIndex; i <= lastIndex; i++) {
            sum += U1[i][N];
        }

        return sum / (lastIndex - firstIndex + 1);
    }

    public double pulse(final int m, final int i) {
        return ((DiscretePulse2D) getDiscretePulse()).evaluateAt((m - EPS) * tau, i * hx);
    }

    private void extendedU1(final int m) {
        for (int i = 0; i <= N; i++) {

            System.arraycopy(U1[i], 0, U1_E[i + 1], 1, N + 1);

            U1_E[i + 1][0] = U1[i][1] + 2.0 * hy * pulse(m, i) - E_C_U1 * U1[i][0];
            U1_E[i + 1][N + 2] = U1[i][N - 1] - E_C_U1 * U1[i][N];
        }

    }

    private void extendedU2() {
        for (int j = 0; j <= N; j++) {

            for (int i = 0; i <= N; i++) {
                U2_E[i + 1][j + 1] = U2[i][j];
            }

            U2_E[N + 2][j + 1] = U2[N - 1][j] - E_C_U2 * U2[N][j];
        }
    }

    private double diff2(double[][] U, final int i, final int j) {
        return (U[i][j + 1] - 2. * U1_E[i][j] + U[i][j - 1]);
    }

    private double diff2r(double[][] U, final int i, final int j) {
        final double C = 1.0 / (2.0 * (i - 1.0));
        return U[i + 1][j] * (1.0 + C) - 2. * U[i][j] + (1.0 - C) * U[i - 1][j];
    }

    @Override
    public void timeStep(int m) {
        var alpha = tridiagonal.getAlpha();
        var beta = tridiagonal.getBeta();

        /* create extended U1 array to accommodate edge values */
        extendedU1(m);

        // first equation, i -> x (radius), j -> y (thickness)
        tridiagonal.setAlpha(1, a11);

        for (int j = 0; j <= N; j++) {

            tridiagonal.setBeta(1, b11 * (2. * U1_E[1][j + 1] / tau + diff2(U1_E, 1, j + 1) / HY2));

            for (int i = 1; i < N; i++) {
                final double F = -2. * U1_E[i + 1][j + 1] / tau - diff2(U1_E, i + 1, j + 1) / HY2;
                final double denominator = b1[i] - a1[i] * alpha[i];
                tridiagonal.setAlpha(i + 1, c1[i] / denominator);
                tridiagonal.setBeta(i + 1, (a1[i] * beta[i] - F) / denominator);
            }

            U2[N][j] = (C2_U2 * beta[N] + HX2 * U1_E[N + 1][j + 1] + C3_U2 * diff2(U1_E, N + 1, j + 1))
                    / ((C1_U2 - alpha[N]) * C2_U2 + HX2);

            for (int i = N - 1; i >= 0; i--) {
                U2[i][j] = alpha[i + 1] * U2[i + 1][j] + beta[i + 1];
            }

        }

        // second equation

        /* create extended U2 array to accommodate edge values */
        extendedU2();
        tridiagonal.setAlpha(1, _a11);

        for (int i = 1; i <= N; i++) {

            tridiagonal.setBeta(1,
                    (TAU_HY * pulse(m + 1, i) + HY2 * U2_E[i + 1][1]) * _b11 + _b12 * diff2r(U2_E, i + 1, 1));

            for (int j = 1; j < N; j++) {
                final double F = -2. / tau * U2_E[i + 1][j + 1] - OMEGA_SQ_HX2 * diff2r(U2_E, i + 1, j + 1);
                final double denominator = b2 - a2 * alpha[j];
                tridiagonal.setAlpha(j + 1, c2 / denominator);
                tridiagonal.setBeta(j + 1, (a2 * beta[j] - F) / denominator);
            }

            U1[i][N] = (tau * beta[N] + HY2 * U2_E[i + 1][N + 1] + _c11 * diff2r(U2_E, i + 1, N + 1))
                    / ((C1_U1 - alpha[N]) * tau + HY2);

            tridiagonal.sweep(U1[i]);

        }

        // i = 0 boundary
        tridiagonal.setBeta(1,
                (TAU_HY * pulse(m + 1) + HY2 * U2_E[1][1]) * _b11 + 2.0 * _b12 * (U2_E[2][1] - U2_E[1][1]));

        for (int j = 1; j < N; j++) {
            final double F = -2. / tau * U2_E[1][j + 1] - 2.0 * OMEGA_SQ_HX2 * (U2_E[2][j + 1] - U2_E[1][j + 1]);
            tridiagonal.setBeta(j + 1, (F - a2 * beta[j]) / (a2 * alpha[j] - b2));
        }

        U1[0][N] = (tau * beta[N] + HY2 * U2_E[1][N + 1] + 2.0 * _c11 * (U2_E[2][N + 1] - U2_E[1][N + 1]))
                / ((C1_U1 - alpha[N]) * tau + HY2);

        tridiagonal.sweep(U1[0]);
    }

    @Override
    public void finaliseStep() {
        // do nothing
    }

}
