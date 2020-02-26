package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.statements.AbsorptionModel;
import pulse.problem.statements.AbsorptionModel.SpectralRange;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TranslucentMaterialProblem;
import pulse.properties.NumericProperty;

public class ExplicitTranslucentSolver 
				extends ExplicitScheme 
					implements Solver<TranslucentMaterialProblem> {
				
	public ExplicitTranslucentSolver() {
		super();
	}
				
	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}
				
	public ExplicitTranslucentSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void solve(TranslucentMaterialProblem problem) {
		prepare(problem);
		
		int N		= (int)grid.getGridDensity().getValue();
		double hx	= grid.getXStep();
		double tau	= grid.getTimeStep();
		
		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue(); 
		
		final double EPS = 1e-5;
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();
		
		double maxVal = 0;		
		int i, m, w;
		
		double[] U 	   = new double[N + 1];
		double[] V     = new double[N + 1];
		
		AbsorptionModel absorb = problem.getAbsorptionModel();
		double pls;
		double signal = 0;
		
		/*
		 * Constants used in the calculation loop
		 */
		
		double TAU_HH = tau/pow(hx,2);		
		double a = 1./(1. + Bi1*hx);
		double b = 1./(1. + Bi2*hx);			
		
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
				
				pls = discretePulse.evaluateAt((m - EPS) * tau);
				
				/*
				 * Uses the heat equation explicitly to calculate the 
				 * grid-function everywhere except the boundaries
				 */
				for(i = 1; i < N; i++) 
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) + 
							tau*pls*absorb.absorption(SpectralRange.LASER, (i - EPS)*hx );
				
				/*
				 * Calculates boundary values
				 */
				
				V[0] = V[1]*a ;
				V[N] = V[N-1]*b;
				
				System.arraycopy(V, 0, U, 0, N + 1);
							
			}
			
			signal = 0;
			
			for(i = 0; i < N; i++) 
				signal += V[N - i]*absorb.absorption(SpectralRange.THERMAL, i*hx) + 
						   V[N - 1 - i]*absorb.absorption(SpectralRange.THERMAL,(i + 1)*hx);
			
			signal *= hx/2.0;
			
			maxVal = Math.max(maxVal, signal);			
			
			curve.addPoint(
					(w * timeInterval) * tau * problem.timeFactor(),
					signal );
			
		}			

		curve.scale( maxTemp/maxVal );		
		
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ExplicitTranslucentSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public Class<? extends Problem> domain() {
		return TranslucentMaterialProblem.class;
	}
	
}