package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public class ExplicitNonlinearSolver 
				extends ExplicitScheme 
					implements Solver<NonlinearProblem> {
				
	public ExplicitNonlinearSolver() {
		super();
	}
				
	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}
				
	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void solve(NonlinearProblem problem) {
		prepare(problem);
		
		int N		= (int)grid.getGridDensity().getValue();
		double hx	= grid.getXStep();
		double tau	= grid.getTimeStep();
		
		final double T   					= (double) problem.getTestTemperature().getValue();
		final double dT   					= problem.maximumHeating();
		final double fixedPointPrecisionSq  = Math.pow( (double) problem.getNonlinearPrecision().getValue(), 2);
		
		final double Bi1 		= (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 		= (double) problem.getHeatLossRear().getValue(); 
		
		double TAU_HH = tau/pow(hx,2);
		
		double[] U 	   = new double[N + 1];
		double[] V     = new double[N + 1];
		
		final double EPS = 1e-5;
		
		int i, m, w;
		double pls;
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();
		
		double a00 = 2*tau/(hx*hx + 2*tau);
		double a11 = hx*hx/(2.0*tau); 
		double f01 = 0.25*Bi1*T/dT;
		double fN1 = 0.25*Bi2*T/dT;
		double f0, fN;
		
		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				for(i = 1; i < N; i++)
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
				
				pls  = discretePulse.evaluateAt( (m - EPS)*tau );

				/**
				 * y = 0
				 */
				
				for(double lastIteration = Double.POSITIVE_INFINITY; 
						pow((V[0] - lastIteration), 2) > fixedPointPrecisionSq; 
						) {
					lastIteration = V[0]; 
					f0 	 = f01*( pow(lastIteration*dT/T + 1, 4) - 1);
			    	V[0] = a00*( V[1] + a11*U[0] + hx*( pls - f0) );
			    }										
				
				/**
				 * y = 1
				 */
				
				for(double lastIteration = Double.POSITIVE_INFINITY; 
						pow((V[N] - lastIteration), 2) > fixedPointPrecisionSq; 
						) {
					lastIteration	= V[N];
					fN				= fN1*( pow(lastIteration*dT/T + 1, 4) - 1);
			    	V[N]		= a00*( V[N-1] + a11*U[N] - hx*fN );
			    }					
				
				System.arraycopy(V, 0, U, 0, N + 1);
							
			}
			
			curve.addPoint(
					(w * timeInterval) * tau * problem.timeFactor(),
					V[N] );
			
		}		
		
		curve.scale( dT );			
		
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ExplicitNonlinearSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return NonlinearProblem.class;
	}
	
}