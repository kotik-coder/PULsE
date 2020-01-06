package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TwoDimensional;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

import static java.lang.Math.abs;
import static java.lang.Math.PI;

/**
 * This class implements a symmetric (weight <math> = 0.5</math>) second-order in time Crank-Nicolson semi-implicit finite-difference scheme 
 * for solving the one-dimensional heat conduction problem. 
 * <p>The semi-implicit scheme uses a 6-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>), 
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>), 
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>), &Theta;(x<sub>i+1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>), &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. 
 * The boundary conditions are approximated with a Taylor expansion up to 
 * the third term, hence the scheme has an increased order of approximation. 
 * </p> 
 * <p>The semi-implicit scheme is unconditionally stable and has an order of 
 * approximation of <math><i>O(&tau;<sup>2</sup> + h<sup>2</sup>)</i></math>. Note this scheme
 * is prone to spurious oscillations when either a high spatial resolution or 
 * a large timestep are used. It has been noticed that due to the pulse term in the 
 * boundary condition, a higher error is introduced into the calculation than 
 * for the implicit scheme.</p>    
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public class MixedScheme extends DifferenceScheme {
	
	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.25} 
	 * for this scheme.
	 */
	
	public final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.25);
	
	/**
	 * The default value of {@code gridDensity}, which is set to {@code 30} 
	 * for this scheme.
	 */
	
	public final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);
	
	/**
	 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
	 * <p>Calls {@code super.solve(Problem)}, then initiates constants for calculations
	 * and uses a sweep method to evaluate the solution for each subsequent timestep, filling
	 * the {@code grid} completely at each specified spatial point. The heating curve is updated with the rear-side temperature <math><i>&Theta;(x<sub>N</sub>,t<sub>i</sub></i></math>) 
	 * (here <math><i>N</i></math> is the grid density) at the end of 
	 * {@code timeLimit} intervals, which comprise of {@code timeLimit/tau} time steps.
	 * The {@code HeatingCurve} is scaled (re-normalised) by a factor of {@code maxTemp/maxVal},
	 * where {@code maxVal} is the absolute maximum of the calculated solution (with respect to time),
	 * and {@code maxTemp} is the {@code maximumTemperature} {@code NumericProperty} of {@code problem}.</p>    
	 * @see super.solve(Problem)
	 */
	
	public Solver<LinearisedProblem> mixedLinearisedSolver = (problem -> {
			super.prepare(problem);
			
			final double Bi1		= (double) problem.getFrontHeatLoss().getValue();
			final double Bi2		= (double) problem.getHeatLossRear().getValue();
			final double maxTemp 	= (double) problem.getMaximumTemperature().getValue(); 
			
			final double EPS = 1e-7;
			
			double[] U 	   = new double[grid.N + 1];
			double[] V     = new double[grid.N + 1];
			double[] alpha = new double[grid.N + 2];
			double[] beta  = new double[grid.N + 2];
				
			HeatingCurve curve = problem.getHeatingCurve();
			curve.reinit();
			
			final int counts = (int) curve.getNumPoints().getValue();		
			
			double maxVal = 0;
			int i, j, m, w;
			double pls;
			
			//coefficients for the finite-difference heat equation

			double a = 1./pow(grid.hx,2);
			double b = 2./grid.tau + 2./pow(grid.hx,2);
			double c = 1./pow(grid.hx,2);

			//precalculated constants

			double HH      = pow(grid.hx,2);
			double F;			
					
			double Bi1HTAU = Bi1*grid.hx*grid.tau;
			double Bi2HTAU = Bi2*grid.hx*grid.tau;
			
			//constant for boundary-conditions calculation

			double a1 = grid.tau/(Bi1HTAU + HH + grid.tau);
			double b1 = 1./(Bi1HTAU + HH + grid.tau);
			double b2 = -grid.hx*(Bi1*grid.tau - grid.hx);
			double b3 = grid.hx*grid.tau;
			double c1 = b2;
			double c2 = Bi2HTAU + HH;
			
			/*
			 * The outer cycle iterates over the number of points of the HeatingCurve
			 */

			for (w = 1; w < counts; w++) {
				
				/*
				 * Two adjacent points of the heating curves are 
				 * separated by timeInterval on the time grid. Thus, to calculate
				 * the next point on the heating curve, timeInterval/tau time steps
				 * have to be made first.
				 */
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					alpha[1] = a1;
					pls 	 = discretePulse.evaluateAt( (m - EPS)*grid.tau ) 
							 + discretePulse.evaluateAt( (m + 1 - EPS)*grid.tau ); //changed to m + 1 - eps
					beta[1]  = b1*(b2*U[0] + b3*pls - grid.tau*(U[0] - U[1]));

					for (i = 1; i < grid.N; i++) {
						alpha[i+1] = c/(b - a*alpha[i]);
						F          =  - 2.*U[i]/grid.tau - (U[i+1] - 2.*U[i] + U[i-1])/HH;
						beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);	
					}

			   	    V[grid.N] = (c1*U[grid.N] + grid.tau*beta[grid.N] - grid.tau*(U[grid.N] - U[grid.N-1]))/(c2 - grid.tau*(alpha[grid.N] - 1));

					for (j = grid.N-1; j >= 0; j--)
						V[j] = alpha[j+1]*V[j+1] + beta[j+1];
										
					System.arraycopy(V, 0, U, 0, grid.N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[grid.N]); //the temperature of the rear face
				maxVal = Math.max(maxVal, V[grid.N]);
				
				curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
				
			}			

			curve.scale( maxTemp/maxVal );
			curve.scale(1.0);
			
	}
	);
	
	/**
	 * Constructs a default semi-implicit scheme using the default 
	 * values of {@code GRID_DENSITY} and {@code TAU_FACTOR}. 
	 */
	
	public MixedScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}
	
	/**
	 * Constructs a semi-implicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}.
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 */
	
	public MixedScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		grid = new Grid(N, timeFactor);	
		grid.setParent(this);
	}
	
	/**
	 * <p>Constructs a semi-implicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}. 
	 * Sets the time limit of this scheme to {@code timeLimit}  
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 * @param timeLimit the {@code NumericProperty} with the type {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */
	
	public MixedScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new MixedScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	public String toString() {
		return Messages.getString("MixedScheme.4");
	}
	
	@Override
	public Solver<? extends Problem> solver(Problem problem) {
		if(problem instanceof TwoDimensional)
			return null;
		
		if(problem.getClass().equals(LinearisedProblem.class))
			return mixedLinearisedSolver;
		else 
			return null;
	}
	
}