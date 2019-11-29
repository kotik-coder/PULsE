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

import static java.lang.Math.PI;

/**
 * This class implements the simple explicit finite-difference scheme 
 * (also called the forward-time centered space scheme) for solving 
 * the one-dimensional heat conduction problem. 
 * <p>The explicit scheme uses a standard 4-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>), 
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>), 
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>), &Theta;(x<sub>i+1</sub>,t<sub>m</sub>)</i></math>. 
 * Hence, the calculation of the grid-function at the timestep <math><i>m</i>+1</math> 
 * can be done <i>explicitly</i>. The derivative in the boundary conditions is approximated using a simple forward difference. 
 * </p> 
 * <p>The explicit scheme is stable only if <math><i>&tau; &le; h<sup>2</sup></i></math> and has an order of 
 * approximation of <math><i>O(&tau; + h)</i></math>. Note that this scheme is only used for validating more complex
 * schemes and does not give accurate results due to the lower order of approximation. When calculations using this 
 * scheme are performed, the <code>gridDensity</code> is chosen to be at least 80, which ensures that the error 
 * is not too high (typically a {@code 1.5E-2} relative error).</p>    
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public class ExplicitScheme extends DifferenceScheme {
	
	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.5} 
	 * for this scheme.
	 */
	
	public final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
	
	/**
	 * The default value of {@code gridDensity}, which is set to {@code 80} 
	 * for this scheme.
	 */
	
	public final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 80);
	
	/**
	 * Performs a fully-dimensionless calculation for the {@code LinearisedProblem}.
	 * <p>Calls {@code super.solve(Problem)}. Relies on using the heat equation
	 * to calculate the value of the grid-function at the next timestep. Fills
	 * the {@code grid} completely at each specified spatial point. The heating curve is updated with the rear-side temperature <math><i>&Theta;(x<sub>N</sub>,t<sub>i</sub></i></math>) 
	 * (here <math><i>N</i></math> is the grid density) at the end of 
	 * {@code timeLimit} intervals, which comprise of {@code timeLimit/tau} time steps.
	 * The {@code HeatingCurve} is scaled (re-normalised) by a factor of {@code maxTemp/maxVal},
	 * where {@code maxVal} is the absolute maximum of the calculated solution (with respect to time),
	 * and {@code maxTemp} is the {@code maximumTemperature} {@code NumericProperty} of {@code problem}.</p>    
	 * @see super.solve(Problem)
	 */
	
	public Solver<LinearisedProblem> explicitLinearisedSolver = ( problem -> 
	{
			super.prepare(problem);
			
			final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
			final double Bi2 = (double) problem.getHeatLossRear().getValue();
			final double maxTemp = (double) problem.getMaximumTemperature().getValue(); 
					
			final double EPS = 1e-5;
			
			double[] U 	   = new double[grid.N + 1];
			double[] V     = new double[grid.N + 1];
			
			HeatingCurve curve = problem.getHeatingCurve();
			curve.reinit();
			final int counts = (int) curve.getNumPoints().getValue();
			
			double maxVal = 0;		
			int i, m, w;
			double pls;
			
			/*
			 * Constants used in the calculation loop
			 */
			
			double TAU_HH = grid.tau/pow(grid.hx,2);		
			double a = 1./(1. + Bi1*grid.hx);
			double b = 1./(1. + Bi2*grid.hx);			
			
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
					
					/*
					 * Uses the heat equation explicitly to calculate the 
					 * grid-function everywhere except the boundaries
					 */
					
					for(i = 1; i < grid.N; i++)
						V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
					
					/*
					 * Calculates boundary values
					 */
					
					pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );
					V[0] = (V[1] + grid.hx*pls)*a ;
					V[grid.N] =	V[grid.N-1]*b;
					
					System.arraycopy(V, 0, U, 0, grid.N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[grid.N]); //the temperature of the rear face
				maxVal = Math.max(maxVal, V[grid.N]);
				curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
				
			}			

			curve.scale( maxTemp/maxVal );
					
	});
	
	public Solver<NonlinearProblem> explicitNonlinearSolver = (ref -> {
			super.prepare(ref);
			
			final double T   					= (double) ref.getTestTemperature().getValue();
			final double dT   					= ref.maximumHeating();
			final double fixedPointPrecisionSq  = Math.pow( (double) ref.getNonlinearPrecision().getValue(), 2);
			
			final double Bi1 		= (double) ref.getFrontHeatLoss().getValue();
			final double Bi2 		= (double) ref.getHeatLossRear().getValue(); 
			
			double TAU_HH = grid.tau/pow(grid.hx,2);
			
			double[] U 	   = new double[grid.N + 1];
			double[] V     = new double[grid.N + 1];
			
			final double EPS = 1e-5;
			
			double maxVal = 0;
			
			int i, m, w;
			double pls;
			
			HeatingCurve curve = ref.getHeatingCurve();
			curve.reinit();
			final int counts = (int) curve.getNumPoints().getValue();
			
			double a00 = 2*grid.tau/(grid.hx*grid.hx + 2*grid.tau);
			double a11 = grid.hx*grid.hx/(2.0*grid.tau); 
			double f01 = 0.25*Bi1*T/dT;
			double fN1 = 0.25*Bi2*T/dT;
			double f0, fN;

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					for(i = 1; i < grid.N; i++)
						V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
					
					pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );

					/**
					 * y = 0
					 */
					
					for(double lastIteration = Double.POSITIVE_INFINITY; 
							pow((V[0] - lastIteration), 2) > fixedPointPrecisionSq; 
							) {
						lastIteration = V[0]; 
						f0 	 = f01*( pow(lastIteration*dT/T + 1, 4) - 1);
				    	V[0] = a00*( V[1] + a11*U[0] + grid.hx*( pls - f0) );
				    }										
					
					/**
					 * y = 1
					 */
					
					for(double lastIteration = Double.POSITIVE_INFINITY; 
							pow((V[grid.N] - lastIteration), 2) > fixedPointPrecisionSq; 
							) {
						lastIteration	= V[grid.N];
						fN				= fN1*( pow(lastIteration*dT/T + 1, 4) - 1);
				    	V[grid.N]		= a00*( V[grid.N-1] + a11*U[grid.N] - grid.hx*fN );
				    }					
					
					System.arraycopy(V, 0, U, 0, grid.N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[grid.N]);
				curve.setTimeAt( w,	(w*timeInterval)*grid.tau*ref.timeFactor() );
				
			}		
			
			curve.scale( dT );			
			
	});

	/**
	 * Constructs a default explicit scheme using the default 
	 * values of {@code GRID_DENSITY} and {@code TAU_FACTOR}. 
	 */
	
	public ExplicitScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}	
	
	/**
	 * Constructs an explicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}.
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 */
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);	
		grid = new Grid(N, timeFactor);	
		grid.setParent(this);
	}
	
	/**
	 * <p>Constructs an explicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}. 
	 * Sets the time limit of this scheme to {@code timeLimit}  
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 * @param timeLimit the {@code NumericProperty} with the type {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ExplicitScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public String toString() {
		return Messages.getString("ExplicitScheme.4");
	}
	
	@Override
	public Solver<? extends Problem> solver(Problem problem) {
		if(problem instanceof TwoDimensional)
			return null;
		
		if(problem instanceof LinearisedProblem)
			return explicitLinearisedSolver;
		else if(problem instanceof NonlinearProblem)
			return explicitNonlinearSolver;
		else 
			return null;
	}
	
}