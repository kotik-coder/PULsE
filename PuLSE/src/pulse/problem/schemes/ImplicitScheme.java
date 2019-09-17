package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

/**
 * This class implements the fully implicit finite-difference scheme for solving 
 * the one-dimensional heat conduction problem. 
 * <p>The fully implicit scheme uses a standard 4-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>), 
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>), 
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>), &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. 
 * Because no <i>explicit</i> formula can be used for calculating the grid-function at timestep <math><i>m</i>+1</math>,
 * a sweep method is implemented instead.
 * The boundary conditions are approximated with a Taylor expansion up to 
 * the third term, hence the scheme has an increased order of approximation. 
 * </p> 
 * <p>The fully implicit scheme is unconditionally stable and has an order of 
 * approximation of at least <math><i>O(&tau; + h<sup>2</sup>)</i></math> for both the heat equation and the boundary conditions. 
 * </p>
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 */

public class ImplicitScheme extends DifferenceScheme {		
	
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
	
	public Solver<LinearisedProblem> implicitLinearisedSolver = 
			(problem -> {								
				
					super.prepare(problem);		
					
					final double Bi1 		= (double) problem.getFrontHeatLoss().getValue();
					final double Bi2 		= (double) problem.getHeatLossRear().getValue();
					final double maxTemp 	= (double) problem.getMaximumTemperature().getValue();
					
					final double EPS = 1e-7; //a small value ensuring numeric stability
					
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
					
					//coefficients for difference equation

					double a = 1./pow(grid.hx,2);
					double b = 1./grid.tau + 2./pow(grid.hx,2);
					double c = 1./pow(grid.hx,2);

					//precalculated constants

					double HH      = pow(grid.hx,2);
					double _2HTAU  = 2.*grid.hx*grid.tau;
					
					double F;
					
					//precalculated constants

					double Bi1HTAU = Bi1*grid.hx*grid.tau;
					double Bi2HTAU = Bi2*grid.hx*grid.tau;
					
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
							
							pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau ); //NOTE: EPS is very important here and ensures numeric stability!
							
							alpha[1] = 2.*grid.tau/(2.*Bi1HTAU + 2.*grid.tau + HH);
							beta[1]  = (HH*U[0] + _2HTAU*pls)/(2.*Bi1HTAU + 2.*grid.tau + HH);

							for(i = 1; i < grid.N; i++) {
								alpha[i+1] = c/(b - a*alpha[i]);
								F		   = -U[i]/grid.tau;
								beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);						
							}
							
							V[grid.N] = (HH*U[grid.N] + 2.*grid.tau*beta[grid.N])/(2*Bi2HTAU + HH - 2.*grid.tau*(alpha[grid.N] - 1));

							for (j = grid.N-1; j >= 0; j--)
								V[j] = alpha[j+1]*V[j+1] + beta[j+1];
												
							System.arraycopy(V, 0, U, 0, grid.N + 1);
										
						}
						
						curve.setTemperatureAt(w, V[grid.N]); //the temperature of the rear face
						maxVal = Math.max(maxVal, V[grid.N]);
						curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
						
					}
					
					curve.scale( maxTemp/maxVal );
					
				
			});
	
	/**
	 * Nonlinear solver
	 */
	
	public Solver<NonlinearProblem> implicitNonlinearSolver = (ref -> {
			super.prepare(ref);
			
			final double HH      = pow(grid.hx,2);
			final double _2HTAU  = 2.*grid.hx*grid.tau;
			
			HeatingCurve curve	= ref.getHeatingCurve();
			final int counts	= (int) curve.getNumPoints().getValue();
			
			final double l 					= (double) ref.getSampleThickness().getValue();
			final double Bi1 				= (double) ref.getFrontHeatLoss().getValue();
			final double Bi2 				= (double) ref.getHeatLossRear().getValue();
			final double T   				= (double) ref.getTestTemperature().getValue();
			final double rho 				= (double) ref.getDensity().getValue();
			final double cV  				= (double) ref.getSpecificHeat().getValue();
			final double qAbs 				= (double) ref.getAbsorbedEnergy().getValue();
			final double nonlinearPrecision = (double) ref.getNonlinearPrecision().getValue(); 
			final double maxTemp 			= (double) ref.getMaximumTemperature().getValue();
			
			double[] U 	   = new double[grid.N + 1];
			double[] V     = new double[grid.N + 1];
			double[] alpha = new double[grid.N + 2];
			double[] beta  = new double[grid.N + 2];
			
			final double EPS = 1e-5;
			
			//constant for bc calc

			double a1 = 2.*grid.tau/(HH + 2.*grid.tau);
			double b1 = HH/(2.*grid.tau + HH);
			double b2 = -0.5*grid.hx*grid.tau/(2.*grid.tau + HH)*Bi1*T;
			double spotDiameter = (double)ref.getPulse().getSpotDiameter().getValue();
			double b3 = _2HTAU/(2.*grid.tau + HH)*qAbs
					  / (cV*rho*PI*l*pow(spotDiameter, 2));
			double c1 = -0.5*grid.hx*grid.tau*Bi2*T;
			double c2;
			
			int i, m, w, j;
			double F, pls;
			double maxVal = 0;
			
			double a = 1./pow(grid.hx,2);
			double b = 1./grid.tau + 2./pow(grid.hx,2);
			double c = 1./pow(grid.hx,2);

			//time cycle
			
			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );					
					alpha[1] = a1;					
					
					for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
					
						beta[1]  = b1*U[0] + b2*(pow(V[0]/T + 1, 4) - 1) + b3*pls;

						for(i = 1; i < grid.N; i++) {
							alpha[i+1] = c/(b - a*alpha[i]);
							F		   = -U[i]/grid.tau;
							beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);						
						}
						
						diff = -(0.5*V[0] + 0.5*V[grid.N]);

						c2   = 1./(HH + 2.*grid.tau - 2*alpha[grid.N]*grid.tau);
						V[grid.N] = c2*(2.*beta[grid.N]*grid.tau + HH*U[grid.N] + c1*(pow(V[grid.N]/T + 1, 4) - 1) );

						for (j = grid.N-1; j >= 0; j--)
							V[j] = alpha[j+1]*V[j+1] + beta[j+1];
						
						diff += 0.5*V[0] + 0.5*V[grid.N];
					
					}
										
					System.arraycopy(V, 0, U, 0, grid.N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[grid.N]);
				maxVal = Math.max(maxVal, V[grid.N]);
				curve.setTimeAt( w,	(w*timeInterval)*grid.tau*ref.timeFactor() );
				
			}			

			ref.setMaximumTemperature(NumericProperty.derive(NumericPropertyKeyword.MAXTEMP, maxVal));
		
	});
	
	/**
	 * Constructs a default fully-implicit scheme using the default 
	 * values of {@code GRID_DENSITY} and {@code TAU_FACTOR}. 
	 */
	
	public ImplicitScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}	
	
	/**
	 * Constructs a fully-implicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}.
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 */
	
	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		grid = new Grid(N, timeFactor);	
		grid.setParent(this);
	}
	
	/**
	 * <p>Constructs a fully-implicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}. 
	 * Sets the time limit of this scheme to {@code timeLimit}  
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 * @param timeLimit the {@code NumericProperty} with the type {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */
	
	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ImplicitScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public String toString() {
		return Messages.getString("ImplicitScheme.4"); //$NON-NLS-1$
	}

	@Override
	public Solver<? extends Problem> solver(Problem problem) {
		if(problem instanceof LinearisedProblem)
			return implicitLinearisedSolver;
		else if(problem instanceof NonlinearProblem)
			return implicitNonlinearSolver;
		else 
			return null;
	}

}