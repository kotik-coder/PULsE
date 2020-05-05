package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.radiation.RadiativeTransfer;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class ImplicitCoupledSolver 
					extends ImplicitScheme 
							implements Solver<AbsorbingEmittingProblem> {

	/**
	 * The default value of {@code tauFactor}, which is set to {@code 1.0} for this
	 * scheme.
	 */

	public final static NumericProperty TAU_FACTOR = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.66667);

	/**
	 * The default value of {@code gridDensity}, which is set to {@code 30} for this
	 * scheme.
	 */

	public final static NumericProperty GRID_DENSITY = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 20);
	
	private int N;
	private int counts;
	private double hx;
	private double tau;
	private double maxTemp;
	
	private HeatingCurve curve;
	
	private double[] U, V;
	private double[] alpha, beta;
	
	private RadiativeTransfer rte;
	private double Np;
	
	private final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	private double b11;
	private double a;
	private double b;
	private double c;
	
	private double HX2_2TAU;
	private double HX_2NP;
	
	private double v1;
	
	public ImplicitCoupledSolver() {
		this(GRID_DENSITY, TAU_FACTOR);
	}
	
	public ImplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(GRID_DENSITY, TAU_FACTOR);
		rte = new RadiativeTransfer(grid);
		rte.setParent(this);
	}
	
	public ImplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	private void prepare(AbsorbingEmittingProblem problem) {
		super.prepare(problem);
		curve = problem.getHeatingCurve();
		
		N	= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		maxTemp = (double) problem.getMaximumTemperature().getValue(); 

		counts = (int) curve.getNumPoints().getValue();

		double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		double Bi2 = (double) problem.getHeatLossRear().getValue();
		
		rte.init(problem, grid);
		
		Np = (double)problem.getPlanckNumber().getValue();
		
		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 2];
		beta = new double[N + 2];

		a = 1. / pow(hx, 2);
		b = 1. / tau + 2. / pow(hx, 2);
		c = 1. / pow(hx, 2);	
		
		b11 = 1.0/(2.0*Np*hx);
		
		HX2_2TAU = hx*hx/(2.0*tau);
		HX_2NP = hx/(2.0*Np);
		
		alpha[1] = 1.0/(1.0 + Bi1*hx + HX2_2TAU);
		
		v1 = 1.0 + HX2_2TAU + hx*Bi2;
		
	}
	
	@Override
	public void solve(AbsorbingEmittingProblem problem) {

		prepare(problem);
		
		final double errorSq = pow((double) problem.getNonlinearPrecision().getValue(), 2);
		
		int i, m, w, j;
		double F, pls;
		
		double V_0 = 0;
		double V_N = 0;
		
		double wFactor = timeInterval * tau * problem.timeFactor();

		rte.radiosities(U);
		rte.fluxes(U);
		
		for (i = 1; i < N; i++)
			alpha[i + 1] = c / (b - a * alpha[i]);
		
		// time cycle

		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau);

				for( V_0 = errorSq + 1, V_N = errorSq + 1; 
						   (pow((V[0] - V_0), 2) > errorSq) ||
						   (pow((V[N] - V_N), 2) > errorSq)
						 ; rte.radiosities(V), rte.fluxes(V)) {
					
					beta[1] = (HX2_2TAU * U[0] + hx*pls - HX_2NP*(rte.getFlux(0) + rte.getFlux(1)) )*alpha[1];

					for (i = 1; i < N; i++) {
						F = U[i] / tau + b11*(rte.getFlux(i-1) - rte.getFlux(i+1));
						beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
					}

					V_N = V[N];
					V[N] = (beta[N] + HX2_2TAU*U[N] + HX_2NP*(rte.getFlux(N-1) + rte.getFlux(N)) )/(v1 - alpha[N]);

					V_0 = V[0]; 
					for (j = N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
					
				}
				
				System.arraycopy(V, 0, U, 0, N + 1);

			}

			curve.addPoint( w * wFactor, V[N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}
	
		curve.scale( maxTemp/curve.apparentMaximum() );

	}
	
	@Override
	public DifferenceScheme copy() {
		return new ImplicitCoupledSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return AbsorbingEmittingProblem.class;
	}
	
	public RadiativeTransfer getRadiativeTransferEquation() {
		return rte;
	}

}