package pulse.problem.schemes.solvers;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.problem.schemes.CoupledImplicitScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.TridiagonalMatrixAlgorithm;
import pulse.problem.schemes.rte.Fluxes;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.NumericProperty;

public class ImplicitCoupledSolver extends CoupledImplicitScheme implements Solver<ParticipatingMedium> {

    private RadiativeTransferSolver rte;
    private Fluxes fluxes;

    private double alpha1;
    private int N;

    private double hx;

    private double HX2_2TAU;
    private double HX2TAU0_2NP;
    private double HX_NP;

    private double v1;

    public ImplicitCoupledSolver() {
        super(derive(GRID_DENSITY, 20), derive(TAU_FACTOR, 0.66667));
    }

    public ImplicitCoupledSolver(NumericProperty gridDensity, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(gridDensity, timeFactor, timeLimit);
    }

    private void prepare(ParticipatingMedium problem) throws SolverException {
        super.prepare(problem);

        final var grid = getGrid();

        var coupling = getCoupling();
        coupling.init(problem, grid);
        rte = coupling.getRadiativeTransferEquation();

        N = (int) getGrid().getGridDensity().getValue();
        hx = grid.getXStep();
        final double HH = hx * hx;
        final double tau = grid.getTimeStep();

        var p = (ThermoOpticalProperties) problem.getProperties();
        final double Bi1 = (double) p.getHeatLoss().getValue();
        final double Np = (double) p.getPlanckNumber().getValue();
        final double tau0 = (double) p.getOpticalThickness().getValue();

        final double TAU0_NP = tau0 / Np;
        HX2_2TAU = HH / (2.0 * tau);
        HX_NP = hx / Np;
        HX2TAU0_2NP = 0.5 * HH * tau0 / Np;

        v1 = 1.0 + HX2_2TAU + hx * Bi1;

        fluxes = rte.getFluxes();
        
        var tridiagonal = new TridiagonalMatrixAlgorithm(grid) {

            @Override
            public double phi(int i) {
                return TAU0_NP * fluxes.fluxDerivative(i);
            }

        };

        alpha1 = 1.0 / (1.0 + Bi1 * hx + HX2_2TAU);
        tridiagonal.setAlpha(1, alpha1);

        tridiagonal.setCoefA(1. / HH);
        tridiagonal.setCoefB(1. / tau + 2. / HH);
        tridiagonal.setCoefC(1. / HH);

        tridiagonal.evaluateAlpha();
        setTridiagonalMatrixAlgorithm(tridiagonal);

    }

    @Override
    public void solve(ParticipatingMedium problem) throws SolverException {
        this.prepare(problem);
        setCalculationStatus(rte.compute(getPreviousSolution()));

        runTimeSequence(problem);

        var status = getCalculationStatus();
        if (status != RTECalculationStatus.NORMAL) {
            throw new SolverException(status.toString());
        }

    }

    @Override
    public String toString() {
        return getString("ImplicitScheme.4");
    }

    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ImplicitCoupledSolver(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }

    @Override
    public double evalRightBoundary(final int m, final double alphaN, final double betaN) {
        /*
		 * UNCOMMENT FOR A SIMPLIFIED CALCULATION 
		 * return (betaN + HX2_2TAU * getPreviousSolution()[N] + HX_2NP * (fluxes.getFlux(N - 1) +
		 * fluxes.getFlux(N))) / (v1 - alphaN);
         */
        return (betaN + HX2_2TAU * getPreviousSolution()[N] + HX_NP * fluxes.getFlux(N)
                + HX2TAU0_2NP * fluxes.fluxDerivativeRear()) / (v1 - alphaN);
    }

    @Override
    public double firstBeta(final int m) {
        /*
		 * UNCOMMENT FOR A SIMPLIFIED CALCULATION
		 * return (HX2_2TAU * getPreviousSolution()[0] + hx * getCurrentPulseValue() - HX_2NP *
		 * (fluxes.getFlux(0) + fluxes.getFlux(1))) * alpha1;
         */
        return (HX2_2TAU * getPreviousSolution()[0] + hx * getCurrentPulseValue()
                + HX2TAU0_2NP * fluxes.fluxDerivativeFront() - HX_NP * fluxes.getFlux(0)) * alpha1;
    }

}
