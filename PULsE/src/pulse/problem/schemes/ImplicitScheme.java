package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.statements.AbsorptionModel;
import pulse.problem.statements.AbsorptionModel.SpectralRange;
import pulse.problem.statements.DiathermicMaterialProblem;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TranslucentMaterialProblem;
import pulse.problem.statements.TwoDimensional;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * This class implements the fully implicit finite-difference scheme for solving
 * the one-dimensional heat conduction problem.
 * <p>
 * The fully implicit scheme uses a standard 4-point template on a
 * one-dimensional grid that utilises the following grid-function values on each
 * step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. Because no
 * <i>explicit</i> formula can be used for calculating the grid-function at
 * timestep <math><i>m</i>+1</math>, a sweep method is implemented instead. The
 * boundary conditions are approximated with a Taylor expansion up to the third
 * term, hence the scheme has an increased order of approximation.
 * </p>
 * <p>
 * The fully implicit scheme is unconditionally stable and has an order of
 * approximation of at least <math><i>O(&tau; + h<sup>2</sup>)</i></math> for
 * both the heat equation and the boundary conditions.
 * </p>
 * 
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 */

public class ImplicitScheme extends DifferenceScheme {

	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.25} for this
	 * scheme.
	 */

	public final static NumericProperty TAU_FACTOR = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.25);

	/**
	 * The default value of {@code gridDensity}, which is set to {@code 30} for this
	 * scheme.
	 */

	public final static NumericProperty GRID_DENSITY = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);

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
	 * @see super.solve(Problem)
	 */

	public Solver<LinearisedProblem> implicitLinearisedSolver = (problem -> {

		super.prepare(problem);

		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();

		final double EPS = 1e-7; // a small value ensuring numeric stability

		double[] U = new double[grid.N + 1];
		double[] V = new double[grid.N + 1];
		double[] alpha = new double[grid.N + 1];
		double[] beta = new double[grid.N + 1];

		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		double maxVal = 0;
		int i, j, m, w;
		double pls;

		// coefficients for difference equation

		double a = 1. / pow(grid.hx, 2);
		double b = 1. / grid.tau + 2. / pow(grid.hx, 2);
		double c = 1. / pow(grid.hx, 2);

		// precalculated constants

		double HH = pow(grid.hx, 2);
		double _2HTAU = 2. * grid.hx * grid.tau;

		double F;

		// precalculated constants

		double Bi1HTAU = Bi1 * grid.hx * grid.tau;
		double Bi2HTAU = Bi2 * grid.hx * grid.tau;

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (w = 1; w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * grid.tau); // NOTE: EPS is very important here and ensures
																		// numeric stability!

				alpha[1] = 2. * grid.tau / (2. * Bi1HTAU + 2. * grid.tau + HH);
				beta[1] = (HH * U[0] + _2HTAU * pls) / (2. * Bi1HTAU + 2. * grid.tau + HH);

				for (i = 1; i < grid.N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					F = -U[i] / grid.tau;
					beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
				}

				V[grid.N] = (HH * U[grid.N] + 2. * grid.tau * beta[grid.N])
						/ (2 * Bi2HTAU + HH - 2. * grid.tau * (alpha[grid.N] - 1));

				for (j = grid.N - 1; j >= 0; j--) 
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				System.arraycopy(V, 0, U, 0, grid.N + 1);

			}

			maxVal = Math.max(maxVal, V[grid.N]);
			curve.addPoint(
					(w * timeInterval) * grid.tau * problem.timeFactor(),
					V[grid.N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}

		curve.scale(maxTemp / maxVal);

	});
	
	public Solver<DiathermicMaterialProblem> diathermicSolver = (problem -> {

		super.prepare(problem);

		final double Bi1		= (double) problem.getFrontHeatLoss().getValue();
		final double maxTemp	= (double) problem.getMaximumTemperature().getValue();
		final double eta		= (double)problem.getDiathermicCoefficient().getValue();

		final double EPS = 1e-7; // a small value ensuring numeric stability

		int N		= grid.N;
		double hx	= grid.hx;
		double tau	= grid.tau;
		
		double[] U = new double[N + 1];
		double[] V = new double[N + 1];
		double[] p = new double[N];
		double[] q = new double[N];
		
		double[] alpha	= new double[N + 1];
		double[] beta	= new double[N + 1];
		double[] gamma	= new double[N + 1];

		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		double maxVal = 0;
		int i, m, w;
		double pls;
		
		final double HX2_TAU = pow(hx,2)/tau;

		// coefficients for difference equation

		double a = 1.0;
		double c = 1.0;
		double b = 2.0 + HX2_TAU;

		// precalculated constants

		final double z0		= 1.0 + 0.5*HX2_TAU + hx*Bi1*(1.0 + eta);
		final double zN_1	= -hx*eta*Bi1;
		final double f01	= HX2_TAU/2.0;
		final double fN1	= f01;

		double F;
		
		alpha[1] = 1.0/z0;
		gamma[1] = -zN_1/z0;

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (w = 1; w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */			

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau); // NOTE: EPS is very important here and ensures
																 // numeric stability!
				
				beta[1] = (hx*hx/(2.0*tau)*U[0] + hx*pls)/z0;
				
				for (i = 1; i < N; i++) {
					alpha[i + 1]	= c / (b - a * alpha[i]);
					F 				= -U[i] * HX2_TAU;
					beta[i + 1]		= (a * beta[i] - F) / (b - a * alpha[i]);
					gamma[i + 1]	= a*gamma[i]/(b - a * alpha[i]);
				}

				p[N-1] = beta[N]; 
				q[N-1] = alpha[N] + gamma[N];
				
				for (i = N - 2; i >= 0; i--) {
					p[i] = alpha[i + 1] * p[i + 1] + beta[i + 1];
					q[i] = alpha[i + 1] * q[i + 1] + gamma[i + 1];
				}
				
				V[N] = (fN1*U[N] - zN_1*p[0] + p[N-1])/(z0 + zN_1*q[0] - q[N-1]);
				
				for (i = N - 1; i >= 0; i--) 
					V[i] = p[i] + V[N]*q[i];
				
				System.arraycopy(V, 0, U, 0, N + 1);

			}

			maxVal = Math.max(maxVal, V[N]);
			
			curve.addPoint(
					(w * timeInterval) * grid.tau * problem.timeFactor(),
					V[N] );			

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}

		curve.scale(maxTemp / maxVal);

	});
	
	public Solver<TranslucentMaterialProblem> translucentSolver = (problem -> {

		super.prepare(problem);
		
		AbsorptionModel absorption = problem.getAbsorptionModel();		

		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();

		final double EPS = 1e-7; // a small value ensuring numeric stability

		int N		= grid.N;
		double hx	= grid.hx;
		double tau	= grid.tau;
		
		double signal = 0;
		
		double[] U		= new double[N + 1];
		double[] V		= new double[N + 1];
		double[] alpha	= new double[N + 2];
		double[] beta	= new double[N + 2];

		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		double maxVal = 0;
		int i, j, m, w;
		double pls;

		// coefficients for difference equation

		double a = 1. / pow(hx, 2);
		double b = 1. / tau + 2. / pow(hx, 2);
		double c = 1. / pow(hx, 2);
		
		// precalculated constants

		double HH		= pow(hx, 2);		
		double F;

		// precalculated constants

		double Bi1H = Bi1 * hx;
		double Bi2H = Bi2 * hx;

		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */

		for (w = 1; w < counts; w++) {

			/*
			 * Two adjacent points of the heating curves are separated by timeInterval on
			 * the time grid. Thus, to calculate the next point on the heating curve,
			 * timeInterval/tau time steps have to be made first.
			 */

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * tau); // NOTE: EPS is very important here and ensures
																 // numeric stability!

				alpha[1] = 1.0 / ( 1.0 + HH/(2.0*tau) + Bi1H );
				beta[1]	 = ( U[0] + tau*pls*absorption.absorption(SpectralRange.LASER, 0.0) ) / (1.0 + 2.0*tau/HH * (1 + Bi1H));

				for (i = 1; i < N; i++) {
					alpha[i + 1] = c / (b - a * alpha[i]);
					F			 = -U[i] / tau - pls*absorption.absorption(SpectralRange.LASER, (i - EPS)*hx );
					beta[i + 1]	 = (F - a * beta[i]) / (a * alpha[i] - b);
				}				

				V[N] = (HH * (U[N] + tau * pls * absorption.absorption(SpectralRange.LASER, (N - EPS)*hx)) + 2. * tau * beta[N])
						/ (2 * Bi2H * tau + HH + 2. * tau * (1 - alpha[N]));

				for (j = N - 1; j >= 0; j--)
					V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				System.arraycopy(V, 0, U, 0, N + 1);

			}

			signal = 0;
						
			for(i = 0; i < N; i++) 
				signal += V[N - i]*absorption.absorption(SpectralRange.THERMAL, i*hx) + 
						   V[N - 1 - i]*absorption.absorption(SpectralRange.THERMAL,(i + 1)*hx);
			
			signal *= hx/2.0;
			
			maxVal = Math.max(maxVal, signal);
			
			curve.addPoint(
					(w * timeInterval) * grid.tau * problem.timeFactor(),
					signal );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}

		curve.scale(maxTemp / maxVal);

	});

	/**
	 * Nonlinear solver
	 */

	public Solver<NonlinearProblem> implicitNonlinearSolver = (ref -> {
		super.prepare(ref);

		final double HH = pow(grid.hx, 2);

		HeatingCurve curve = ref.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();

		final double Bi1 = (double) ref.getFrontHeatLoss().getValue();
		final double Bi2 = (double) ref.getHeatLossRear().getValue();
		final double fixedPointPrecisionSq = Math.pow((double) ref.getNonlinearPrecision().getValue(), 2);

		final double T = (double) ref.getTestTemperature().getValue();
		final double dT = ref.maximumHeating();

		double[] U = new double[grid.N + 1];
		double[] V = new double[grid.N + 1];
		double[] alpha = new double[grid.N + 2];
		double[] beta = new double[grid.N + 2];

		final double EPS = 1e-5;

		// constant for bc calc

		double a1 = 2. * grid.tau / (HH + 2. * grid.tau);
		double b1 = HH / (2. * grid.tau + HH);
		double b2 = a1 * grid.hx;
		double b3 = Bi1 * T / (4.0 * dT);
		double c1 = -0.5 * grid.hx * grid.tau * Bi2 * T / dT;
		double c2;

		int i, m, w, j;
		double F, pls;

		double a = 1. / pow(grid.hx, 2);
		double b = 1. / grid.tau + 2. / pow(grid.hx, 2);
		double c = 1. / pow(grid.hx, 2);

		// time cycle

		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls = discretePulse.evaluateAt((m - EPS) * grid.tau);
				alpha[1] = a1;

				for (i = 1; i < grid.N; i++)
					alpha[i + 1] = c / (b - a * alpha[i]);

				c2 = 1. / (HH + 2. * grid.tau - 2 * alpha[grid.N] * grid.tau);

				for (double lastIteration = Double.POSITIVE_INFINITY; pow((0.5 * (V[0] + V[grid.N]) - lastIteration),
						2) > fixedPointPrecisionSq;) {

					lastIteration = 0.5 * (V[0] + V[grid.N]);

					beta[1] = b1 * U[0] + b2 * (pls - b3 * (pow(V[0] * dT / T + 1, 4) - 1));

					for (i = 1; i < grid.N; i++) {
						F = -U[i] / grid.tau;
						beta[i + 1] = (F - a * beta[i]) / (a * alpha[i] - b);
					}

					V[grid.N] = c2 * (2. * beta[grid.N] * grid.tau + HH * U[grid.N]
							+ c1 * (pow(V[grid.N] * dT / T + 1, 4) - 1));

					for (j = grid.N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];

				}

				System.arraycopy(V, 0, U, 0, grid.N + 1);

			}

			curve.addPoint(
					(w * timeInterval) * grid.tau * ref.timeFactor(),
					V[grid.N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(ref, V, w);

		}

		curve.scale(dT);

	});

	/**
	 * Constructs a default fully-implicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public ImplicitScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}

	/*
	private void debug(Problem ref, double[] V, int w) {

		if (w % 2 != 0)
			return;

		double time = (w * timeInterval) * grid.tau;
		System.out.println(time + "\t" + V[0] + "\t" + V[grid.N]);

		/*
		 * final double alpha = 1.14E-5; final double nu = 0.032; final double E =
		 * 2.87E11; final double T = 295; final double rho = 1850; final double Cp =
		 * 1925; final double A0 = alpha*(1-nu)/(1.-2.*nu)*(1. - nu/pow(1.-nu,2.));
		 * final double l = 2E-3; final double a = 71.0186; final double Tmax = 4.39;
		 * 
		 * double Tav = 0;
		 * 
		 * for(int i = 0; i <= grid.N; i++) Tav += V[i];
		 * 
		 * Tav /= (grid.N + 1);
		 * 
		 * 
		 * 
		 * OUTPUTs THE FULL SOLUTION T(x,t)
		 * 
		 * 
		 * if(time > 1) return;
		 * 
		 * if(w % 2 != 0) return;
		 * 
		 * System.out.println(); System.out.println(); //System.out.println("\"t=" +
		 * String.format("%3.2E", time*l*l/71.2E-6) + "\"");
		 * 
		 * System.out.println("\"t=" + String.format("%3.2E", time*ref.timeFactor() ) );
		 * for(int i = 0; i <= grid.N; i++) System.out.println(String.format("%3.2E",
		 * i*grid.hx) + "\t" + String.format("%3.2E", V[i]));
		 * 
		 * 
		 * OUTPUTs THE X-INTEGRAL OF T(x,t)
		 * 
		 * 
		 * double i1 = 0; double i2 = 0;a
		 * 
		 * for(int j = 0; j < grid.N; j++) { i1 += 0.5*((V[j] - Tav) + (V[j+1] -
		 * Tav))/Tav*grid.hx*l*Tmax; i2 += 0.5*((V[j] - Tav)*j*grid.hx + (V[j+1] -
		 * Tav)*(j+1)*grid.hx)/Tav*grid.hx*l*l*Tmax; }
		 * 
		 * double B = A0*(4.0*l*i1 - 6.0*i2); double A = 2.0*A0/l/l*i1 - 2.0*B/l;
		 * 
		 * //System.out.println("\t" + w*tiameInterval*grid.tau + "\t" + i1 + "\t" + i2);
		 * 
		 * double x, epsilon; double sigma = 0;
		 * 
		 * for(int j = 0; j < grid.N; j++) { x = (j + j+1)/2.0*grid.hx*l; epsilon = A*x
		 * + B; sigma = E*epsilon/(1 - nu) + E*alpha*(V[j] - Tav)*Tmax/(1 -
		 * 2.0*nu)*(nu/(1 - nu)/(1 - nu) - 1);
		 * //System.out.println(String.format("%3.2E", x) + "\t" +
		 * String.format("%3.2E", epsilon) + "\t" + String.format("%3.2E", sigma)); }
		 * 
		 * double dT_Wang = (-sigma*alpha*T/Cp/rho);
		 * 
		 * System.out.println(String.format("%3.2E", time*l*l/71.2E-6) + "\t" +
		 * String.format("%3.2E", V[grid.N]*Tmax) + "\t" + String.format("%3.2E",
		 * dT_Wang) + "\t" + String.format("%3.2E", V[grid.N]*Tmax + dT_Wang));
		 */

	//}

	/**
	 * Constructs a fully-implicit scheme on a one-dimensional grid that is
	 * specified by the values {@code N} and {@code timeFactor}.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 */

	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		grid = new Grid(N, timeFactor);
		grid.setParent(this);
	}

	/**
	 * <p>
	 * Constructs a fully-implicit scheme on a one-dimensional grid that is
	 * specified by the values {@code N} and {@code timeFactor}. Sets the time limit
	 * of this scheme to {@code timeLimit}
	 * 
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 * @param timeLimit  the {@code NumericProperty} with the type
	 *                   {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */

	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}

	@Override
	public DifferenceScheme copy() {
		return new ImplicitScheme(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public String toString() {
		return Messages.getString("ImplicitScheme.4");
	}

	@SuppressWarnings("unchecked")
	@Override
	public Solver<? extends Problem> solver(Problem problem) {
		if (problem instanceof TwoDimensional)
			return null;		

		Class<?> problemClass = problem.getClass();
		
		if (problemClass.equals(LinearisedProblem.class))
			return implicitLinearisedSolver;
		else if (problemClass.equals(NonlinearProblem.class))
			return implicitNonlinearSolver;
		else if (problemClass.equals(TranslucentMaterialProblem.class))
			return translucentSolver;
		else if (problemClass.equals(DiathermicMaterialProblem.class))
			return diathermicSolver;
		else
			return null;		
	}

}