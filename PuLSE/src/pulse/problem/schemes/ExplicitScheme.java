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

public class ExplicitScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 80);

	public ExplicitScheme() {
		super(GRID_DENSITY, TAU_FACTOR);
	}	
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);	
	}
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ExplicitScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public String toString() {
		return Messages.getString("ExplicitScheme.4");		 //$NON-NLS-1$
	}
	
	@Override
	public void solve(Problem p) {
		if(p instanceof LinearisedProblem)
			solve((LinearisedProblem) p);
		else if(p instanceof NonlinearProblem)
			solve((NonlinearProblem) p);
	}
	
	public void solve(LinearisedProblem problem) {
		super.solve(problem);
		
		//quick links
		
		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue(); 
		
		//end
				
		final double EPS = 1e-5;
		
		double[] U 	   = new double[grid.N + 1];
		double[] V     = new double[grid.N + 1];
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();
		
		double maxVal = 0;
		
		double TAU_HH = grid.tau/pow(grid.hx,2);
		
		int i, m, w;
		double pls;
		
		//solution of linearized problem with explicit scheme
		
		double a = 1./(1. + Bi1*grid.hx);
		double b = 1./(1. + Bi2*grid.hx);			
		
		//time cycle

		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				for(i = 1; i < grid.N; i++)
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
				
				pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );
				V[0] = (V[1] + grid.hx*pls)*a ;
				V[grid.N] =	V[grid.N-1]*b;
				
				System.arraycopy(V, 0, U, 0, grid.N + 1);
							
			}
			
			curve.setTemperatureAt(w, V[grid.N]);
			maxVal = Math.max(maxVal, V[grid.N]);
			curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
			
		}			

		curve.scale( maxTemp/maxVal );
		
	}
	
	public void solve(NonlinearProblem ref) {
		super.solve(ref);
		
		final double T   				= (double) ref.getTestTemperature().getValue();
		final double rho 				= (double) ref.getDensity().getValue();
		final double cV  				= (double) ref.getSpecificHeat().getValue();
		final double qAbs 				= (double) ref.getAbsorbedEnergy().getValue();
		final double nonlinearPrecision = (double) ref.getNonlinearPrecision().getValue(); 	
		
		final double l 			= (double) ref.getSampleThickness().getValue();
		final double Bi1 		= (double) ref.getFrontHeatLoss().getValue();
		final double Bi2 		= (double) ref.getHeatLossRear().getValue();
		final double maxTemp 	= (double) ref.getMaximumTemperature().getValue(); 
		
		double pulseWidth = (double)ref.getPulse().getSpotDiameter().getValue();
		double c = qAbs/(cV*rho*PI*l*pow(pulseWidth, 2));
		
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

		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				for(i = 1; i < grid.N; i++)
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
				
				pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );
				
			    for(double diff = 100, tmp = 0; diff/maxTemp > nonlinearPrecision; ) {
			    	tmp = V[1] + c*grid.hx*pls - 0.25*grid.hx*Bi1*T*( pow(V[0]/T + 1, 4) - 1); //bc1
			    	diff = tmp - V[0];
			    	V[0] = tmp;
			    }
			    
			    for(double diff = 100, tmp = 0; diff/maxTemp > nonlinearPrecision; ) {
			    	tmp = V[grid.N-1] - 0.25*grid.hx*Bi2*T*( pow(V[grid.N]/T + 1, 4) - 1); //bc1
			    	diff = tmp - V[grid.N];
			    	V[grid.N] = tmp;
			    }					
				
				System.arraycopy(V, 0, U, 0, grid.N + 1);
							
			}
			
			curve.setTemperatureAt(w, V[grid.N]);
			maxVal = Math.max(maxVal, V[grid.N]);
			curve.setTimeAt( w,	(w*timeInterval)*grid.tau*ref.timeFactor() );
			ref.setMaximumTemperature(NumericProperty.derive(NumericPropertyKeyword.MAXTEMP, maxVal));
			
		}		
	}
	
}