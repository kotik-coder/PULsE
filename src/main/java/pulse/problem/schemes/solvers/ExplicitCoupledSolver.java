package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.radiation.RTESolverNonlinear;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public class ExplicitCoupledSolver 
					extends ExplicitScheme 
						implements Solver<AbsorbingEmittingProblem> {

	
	private double[] U;
	private double[] V;
	
	private RTESolverNonlinear rteSolver;
	private double opticalThickness;
	private double Np;
	
	private HeatingCurve curve;
	private int N;
	private int counts;
	private double hx;
	private double tau;
	private double maxTemp;
	private double maxVal;	
	private double a,b;
	
	private final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	public ExplicitCoupledSolver() {
		super();
	}
	
	public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}
	
	public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}
	
	@Override
	public void prepare(Problem problem) {
		super.prepare(problem);		
		curve = problem.getHeatingCurve();
		
		N	= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		
		U		= new double[N + 1];
		V		= new double[N + 1];
		
		double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		double Bi2 = (double) problem.getHeatLossRear().getValue();
		maxTemp = (double) problem.getMaximumTemperature().getValue(); 
				
		counts = (int) curve.getNumPoints().getValue();
		
		maxVal = 0;				
				
		a = 1./(1. + Bi1*hx);
		b = 1./(1. + Bi2*hx);
		
		if(problem instanceof AbsorbingEmittingProblem) {
		
			var p = (AbsorbingEmittingProblem)problem;
			rteSolver = new RTESolverNonlinear(p, hx);
			
			opticalThickness = (double)p.getOpticalThickness().getValue();
			Np = (double)p.getPlanckNumber().getValue();

		}
			
	}
	
	@Override
	public void solve(AbsorbingEmittingProblem problem) {
		prepare(problem);
				
		int i, m, w;
		double pls;
		final double TAU_HH = tau/pow(hx,2);	
		final double HX_NP = hx/Np; 
		
		final double prefactor = tau*opticalThickness/Np;
		
		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */
		
		for (w = 1; w < counts; w++) {
			
			//System.out.println(w + " out of " + counts );
			
			/*
			 * Two adjacent points of the heating curves are 
			 * separated by timeInterval on the time grid. Thus, to calculate
			 * the next point on the heating curve, timeInterval/tau time steps
			 * have to be made first.
			 */
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				rteSolver.radiosities(U);
				
				/*
				 * Uses the heat equation explicitly to calculate the 
				 * grid-function everywhere except the boundaries
				 */
				
				for(i = 1; i < N; i++) 
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] )
							+ prefactor*rteSolver.fluxDerivative(U, i); //+ tau_0/Np * (-dF*/d\tau) --> CHECK SIGN!!!
				
				/*
				 * Calculates boundary values
				 */
				
				pls  = discretePulse.evaluateAt( (m - EPS)*tau );
				
				V[0] = ( V[1] + hx*pls 
						 - HX_NP*rteSolver.fluxFront(U) )*a;
				V[N] = ( V[N-1] 
						 + HX_NP*rteSolver.fluxRear(U) )*b;
				
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
		return new ExplicitCoupledSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return AbsorbingEmittingProblem.class;
	}

}