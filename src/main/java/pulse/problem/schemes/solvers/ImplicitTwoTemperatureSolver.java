package pulse.problem.schemes.solvers;

import java.util.Set;
import static pulse.problem.schemes.DistributedDetection.evaluateSignal;
import static pulse.problem.statements.model.SpectralRange.LASER;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.FixedPointIterations;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TwoTemperatureModel;
import pulse.problem.statements.model.AbsorptionModel;
import pulse.problem.statements.model.TwoTemperatureProperties;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

public class ImplicitTwoTemperatureSolver extends ImplicitScheme
        implements Solver<TwoTemperatureModel>, FixedPointIterations {

    private static final long serialVersionUID = 7955478815933535623L;
    private AbsorptionModel absorption;
    private TridiagonalMatrixAlgorithm gasSolver;

    private int N;
    private double hBi;
    private double hBiPrime;
    private double HH;
    private double tau;
    private double _05HH_TAU;

    private double[] gasTemp;

    private double diffRatio;
    private double g;
    private double gPrime;

    private double nonlinearPrecision;

    public ImplicitTwoTemperatureSolver() {
        super();
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
    }

    public ImplicitTwoTemperatureSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
    }

    private void initSolidPart() {
        var grid = getGrid();
        final double hx = grid.getXStep();

        var solid = new TridiagonalMatrixAlgorithm(grid) {

            @Override
            public double phi(final int i) {
                return getCurrentPulseValue() * absorption.absorption(LASER, i * hx)
                        + g * gasTemp[i];
            }

        };

        solid.setCoefA(1.0 / HH);
        solid.setCoefB(1.0 / tau + 2.0 / HH + g);
        solid.setCoefC(1.0 / HH);

        solid.setAlpha(1, 1.0 / (1.0 + hBi + _05HH_TAU + 0.5 * HH * g));
        solid.evaluateAlpha();
        setTridiagonalMatrixAlgorithm(solid);
    }

    private void initGasPart() {
        var grid = getGrid();

        gasTemp = new double[N + 1];
        var solidTemp = this.getCurrentSolution();

        gasSolver = new TridiagonalMatrixAlgorithm(grid) {

            @Override
            public double phi(final int i) {
                return gPrime * solidTemp[i];
            }

            @Override
            public void evaluateAlpha() {
                setAlpha(1, 1.0 / (1.0 + hBiPrime + diffRatio * (_05HH_TAU + 0.5 * HH * gPrime)));
                super.evaluateAlpha();
            }

            @Override
            public void evaluateBeta(final double[] U, final int start, final int endExclusive) {
                setBeta(1, diffRatio * (0.5 * HH * phi(0) + _05HH_TAU * U[0]) * getAlpha()[1]);
                super.evaluateBeta(U, start, endExclusive);
            }

        };

        double invDiffRatio = 1.0 / diffRatio;
        gasSolver.setCoefA(invDiffRatio / HH);
        gasSolver.setCoefB(1.0 / tau + gPrime + 2.0 / HH * invDiffRatio);
        gasSolver.setCoefC(invDiffRatio / HH);

        gasSolver.evaluateAlpha();
    }

    @Override
    public void prepare(Problem problem) throws SolverException {
        if (!(problem instanceof TwoTemperatureModel)) {
            throw new IllegalArgumentException("Illegal model type");
        }

        super.prepare(problem);
        var model = (TwoTemperatureModel) problem;
        var ttp = (TwoTemperatureProperties) model.getProperties();

        double hx = getGrid().getXStep();
        tau = getGrid().getTimeStep();
        N = (int) getGrid().getGridDensity().getValue();

        HH = hx * hx;
        _05HH_TAU = 0.5 * HH / tau;
        hBi = (double) ttp.getHeatLoss().getValue() * hx;
        hBiPrime = (double) ttp.getGasHeatLoss().getValue() * hx;

        g = (double) ttp.getSolidExchangeCoefficient().getValue();
        absorption = model.getAbsorptionModel();

        diffRatio = model.diffusivityRatio();
        gPrime = (double) ttp.getGasExchangeCoefficient().getValue();

        initGasPart();
        initSolidPart();
    }

    @Override
    public void solve(TwoTemperatureModel problem) throws SolverException {
        prepare(problem);
        runTimeSequence(problem);
    }

    @Override
    public void timeStep(final int m) throws SolverException {
        doIterations(gasTemp, nonlinearPrecision, m);
    }

    @Override
    public void iteration(int m) throws SolverException {
        //first solve for the solid
        super.timeStep(m);
        //then for the gas
        gasSolver.evaluateBeta(gasTemp);
        gasTemp[N] = (diffRatio * (0.5 * HH * gasSolver.phi(N) + _05HH_TAU * gasTemp[N])
                + gasSolver.getBeta()[N]) / (1.0 + diffRatio * (_05HH_TAU + 0.5 * HH * gPrime)
                + hBiPrime - gasSolver.getAlpha()[N]);
        gasSolver.sweep(gasTemp);
    }

    @Override
    public double signal() {
        return evaluateSignal(absorption, getGrid(), getCurrentSolution());
    }

    @Override
    public double evalRightBoundary(final double alphaN, final double betaN) {
        var tridiagonal = this.getTridiagonalMatrixAlgorithm();
        return (_05HH_TAU * getPreviousSolution()[N] + 0.5 * HH * tridiagonal.phi(N) + betaN)
                / (1.0 + _05HH_TAU + 0.5 * HH * g + hBi - alphaN);
    }

    @Override
    public double firstBeta() {
        var tridiagonal = this.getTridiagonalMatrixAlgorithm();
        return (_05HH_TAU * getPreviousSolution()[0] + 0.5 * HH * tridiagonal.phi(0))
                * tridiagonal.getAlpha()[1];
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ImplicitTwoTemperatureSolver(grid.getGridDensity(),
                grid.getTimeFactor(), getTimeLimit());
    }

    /**
     * Prints out the description of this problem type.
     *
     * @return a verbose description of the problem.
     */
    @Override
    public String toString() {
        return getString("ImplicitScheme.4");
    }

    @Override
    public Class<? extends Problem>[] domain() {
        return new Class[]{TwoTemperatureModel.class};
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
        if (type == NONLINEAR_PRECISION) {
            setNonlinearPrecision(property);
        }
    }

}
