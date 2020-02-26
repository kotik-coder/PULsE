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
				
	private double Bi1;
	private double Bi2;
	
	private int N;
	private int counts;
	private double hx;
	private double tau;
	
	private HeatingCurve curve;
	
	private double[] U;
	private double[] V;
	
	private final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	private double T, dT;
	
	public ExplicitNonlinearSolver() {
		super();
	}
				
	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}
				
	public ExplicitNonlinearSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	private void prepare(NonlinearProblem problem) {
		super.prepare(problem);		
		HeatingCurve curve = problem.getHeatingCurve();
		
		N		= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		
		T   					= (double) problem.getTestTemperature().getValue();
		dT   					= problem.maximumHeating();
		
		Bi1 		= (double) problem.getFrontHeatLoss().getValue();
		Bi2 		= (double) problem.getHeatLossRear().getValue(); 		
				
		counts = (int) curve.getNumPoints().getValue();			
	}
	
	@Override
	public void solve(NonlinearProblem problem) {
		prepare(problem);
		
		int i, m, w;
		double pls;
				
		final double TAU_HH = tau/pow(hx,2);
		final double a00 = 2*tau/(hx*hx + 2*tau);
		final double a11 = hx*hx/(2.0*tau); 
		final double f01 = 0.25*Bi1*T/dT;
		final double fN1 = 0.25*Bi2*T/dT;
		double f0, fN;	
		
		final double fixedPointPrecisionSq  = pow( (double) problem.getNonlinearPrecision().getValue(), 2);
		
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