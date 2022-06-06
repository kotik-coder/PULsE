package pulse.problem.schemes.solvers;

import pulse.problem.schemes.BlockMatrixAlgorithm;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.statements.ClassicalProblem;
import pulse.problem.statements.DiathermicMedium;
import pulse.problem.statements.Problem;
import pulse.problem.statements.model.DiathermicProperties;
import pulse.properties.NumericProperty;

public class ImplicitDiathermicSolver extends ImplicitScheme implements Solver<DiathermicMedium> {

    private double hx;
    private int N;

    private double HX2_2TAU;
    private double z0;
    private double zN_1;
    private double zeta;
   
    public ImplicitDiathermicSolver() {
        super();
    }

    public ImplicitDiathermicSolver(NumericProperty N, NumericProperty timeFactor) {
        super(N, timeFactor);
    }

    public ImplicitDiathermicSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
    }

    @Override
    public void prepare(Problem problem) throws SolverException {
        super.prepare(problem);

        var grid = getGrid();

        N = (int) grid.getGridDensity().getValue();
        hx = grid.getXStep();

        final double HX2 = hx * hx;
        final double tau = grid.getTimeStep();
        HX2_2TAU = HX2 / (2.0 * grid.getTimeStep());

        /* Constants */
        var properties = (DiathermicProperties) problem.getProperties();

        final double Bi1 = (double) properties.getHeatLoss().getValue();
        final double eta = (double) properties.getDiathermicCoefficient().getValue();

        z0 = 1.0 + HX2_2TAU + hx * Bi1 * (1.0 + eta);
        zN_1 = -hx * eta * Bi1;

        /* End of constants */
        var tridiagonal = new BlockMatrixAlgorithm(grid);

        tridiagonal.setCoefA(1.0);
        tridiagonal.setCoefB(2.0 + HX2 / tau);
        tridiagonal.setCoefC(1.0);

        tridiagonal.setAlpha(1, 1.0 / z0);
        tridiagonal.evaluateAlpha();
        setTridiagonalMatrixAlgorithm(tridiagonal);

        zeta = (double) ((ClassicalProblem)problem).getGeometricFactor().getValue();
    }

    @Override
    public void leftBoundary() {
        var tridiagonal = (BlockMatrixAlgorithm) getTridiagonalMatrixAlgorithm();
        tridiagonal.setGamma(1, -zN_1 / z0);
        super.leftBoundary();
    }

    @Override
    public double firstBeta() {
        return (HX2_2TAU * getPreviousSolution()[0] + hx * zeta * getCurrentPulseValue()) / z0;
    }

    @Override
    public double evalRightBoundary(double alphaN, double betaN) {
        var tri = (BlockMatrixAlgorithm) getTridiagonalMatrixAlgorithm();
        var p = tri.getP();
        var q = tri.getQ();
        return (HX2_2TAU * getPreviousSolution()[N] + (1.0 - zeta) *hx * getCurrentPulseValue() 
                - zN_1 * p[0] + p[N - 1])
                / (z0 + zN_1 * q[0] - q[N - 1]);
    }

    @Override
    public void solve(DiathermicMedium problem) throws SolverException {
        prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ImplicitDiathermicSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }

    @Override
    public Class<? extends Problem>[] domain() {
        return new Class[]{DiathermicMedium.class};
    }

}
