package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

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

public class MixedLinearisedSolver 
				extends MixedScheme 
					implements Solver<LinearisedProblem> {
	
	private double maxTemp;
	
	private HeatingCurve curve;
	
	private int N;
	private int counts;
	private double hx;
	private double tau;
	
	private double a,b,c,a1,b1,c1,b2,b3,c2;
	
	private double[] U;
	private double[] V;
	private double[] alpha;
	private double[] beta;
	private double maxVal;	
	
	private final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	public MixedLinearisedSolver() {
		super();
	}
	
	public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}
	
	public MixedLinearisedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void prepare(Problem problem) {
		super.prepare(problem);		
		curve = problem.getHeatingCurve();
		
		N		= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		
		double Bi1		= (double) problem.getFrontHeatLoss().getValue();
		double Bi2		= (double) problem.getHeatLossRear().getValue();
		maxTemp = (double) problem.getMaximumTemperature().getValue(); 
				
		U		= new double[N + 1];
		V		= new double[N + 1];
		alpha	= new double[N + 2];
		beta	= new double[N + 2];		
		
		counts = (int) curve.getNumPoints().getValue();		
		
		//coefficients for the finite-difference heat equation

		a = 1./pow(hx,2);
		b = 2./tau + 2./pow(hx,2);
		c = 1./pow(hx,2);

		//precalculated constants

		double HH      = pow(hx,2);		
				
		double Bi1HTAU = Bi1*hx*tau;
		double Bi2HTAU = Bi2*hx*tau;
		
		//constant for boundary-conditions calculation

		a1 = tau/(Bi1HTAU + HH + tau);
		b1 = 1./(Bi1HTAU + HH + tau);
		b2 = -hx*(Bi1*tau - hx);
		b3 = hx*tau;
		c1 = b2;
		c2 = Bi2HTAU + HH;
		
		maxVal = 0;		
	}
	
	@Override
	public void solve(LinearisedProblem problem) {
		prepare(problem);
		
		//precalculated constants

		double HH      = pow(hx,2);
		double F;			
		
		int i, j, m, w;
		double pls;
		
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
				pls 	 = discretePulse.evaluateAt( (m - EPS)*tau ) 
						 + discretePulse.evaluateAt( (m + 1 - EPS)*tau ); //changed to m + 1 - eps
				beta[1]  = b1*(b2*U[0] + b3*pls - tau*(U[0] - U[1]));

				for (i = 1; i < N; i++) {
					alpha[i+1] = c/(b - a*alpha[i]);
					F          =  - 2.*U[i]/tau - (U[i+1] - 2.*U[i] + U[i-1])/HH;
					beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);	
				}

		   	    V[N] = (c1*U[N] + tau*beta[N] - tau*(U[N] - U[N-1]))/(c2 - tau*(alpha[N] - 1));

				for (j = N-1; j >= 0; j--)
					V[j] = alpha[j+1]*V[j+1] + beta[j+1];
									
				System.arraycopy(V, 0, U, 0, N + 1);
							
			}
			
			maxVal = Math.max(maxVal, V[N]);
			
			curve.addPoint(
					(w * timeInterval) * tau * problem.timeFactor(),
					V[N] );
			
		}			

		curve.scale( maxTemp/maxVal );
		
	}
	
	@Override
	public DifferenceScheme copy() {
		return new MixedLinearisedSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return LinearisedProblem.class;
	}

}