package pulse.problem.schemes;

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

public abstract class ImplicitScheme extends DifferenceScheme {

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

	/*
	 * private void debug(Problem ref, double[] V, int w) {
	 * 
	 * if (w % 2 != 0) return;
	 * 
	 * double time = (w * timeInterval) * grid.tau; System.out.println(time + "\t" +
	 * V[0] + "\t" + V[grid.N]);
	 * 
	 * /* final double alpha = 1.14E-5; final double nu = 0.032; final double E =
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
	 * //System.out.println("\t" + w*tiameInterval*grid.tau + "\t" + i1 + "\t" +
	 * i2);
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

	// }

	/**
	 * Constructs a default fully-implicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public ImplicitScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}

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
		super();
		initGrid(N, timeFactor);
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
		super(timeLimit);
		initGrid(N, timeFactor);
	}

	@Override
	public String toString() {
		return Messages.getString("ImplicitScheme.4");
	}

}