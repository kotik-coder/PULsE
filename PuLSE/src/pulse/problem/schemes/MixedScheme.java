package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

import static java.lang.Math.abs;
import static java.lang.Math.PI;

public class MixedScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.25);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 80);

	
	public MixedScheme() {
		super(GRID_DENSITY, TAU_FACTOR);
	}	
	
	public MixedScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);	
	}
	
	public MixedScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new MixedScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	public String toString() {
		return Messages.getString("MixedScheme.4"); //$NON-NLS-1$
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
		double b = 2./grid.tau + 2./pow(grid.hx,2);
		double c = 1./pow(grid.hx,2);

		//precalculated constants

		double HH      = pow(grid.hx,2);
		double F;			
		
		//precalculated constants
		
		double Bi1HTAU = Bi1*grid.hx*grid.tau;
		double Bi2HTAU = Bi2*grid.hx*grid.tau;
		
		//constant for bc calc

		double a1 = grid.tau/(Bi1HTAU + HH + grid.tau);
		double b1 = 1./(Bi1HTAU + HH + grid.tau);
		double b2 = -grid.hx*(Bi1*grid.tau - grid.hx);
		double b3 = grid.hx*grid.tau;
		double c1 = b2;
		double c2 = Bi2HTAU + HH;
		
		//time cycle

		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				alpha[1] = a1;
				pls 	 = discretePulse.evaluateAt( (m - EPS)*grid.tau ) 
						 + discretePulse.evaluateAt( (m + 1 + EPS)*grid.tau ); //possibly change to m + 1 - eps
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
			
			curve.setTemperatureAt(w, V[grid.N]);
			maxVal = Math.max(maxVal, V[grid.N]);
			curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
			
		}			

		curve.scale( maxTemp/maxVal );

	}
	
	public void solve(NonlinearProblem ref) {
		super.solve(ref);
		
		//quick links
		
		final double l			= (double) ref.getSampleThickness().getValue();
		final double Bi1		= (double) ref.getFrontHeatLoss().getValue();
		final double Bi2 		= (double) ref.getHeatLossRear().getValue();
		final double maxTemp	= (double) ref.getMaximumTemperature().getValue(); 
		
		//end
		
		final double EPS = 1e-5;
		
		double[] U 	   = new double[grid.N + 1];
		double[] V     = new double[grid.N + 1];
		double[] alpha = new double[grid.N + 2];
		double[] beta  = new double[grid.N + 2];
		
		//coefficients for difference equation

		double a = 1./pow(grid.hx,2);
		double b = 2./grid.tau + 2./pow(grid.hx,2);
		double c = 1./pow(grid.hx,2);
			
		HeatingCurve curve = ref.getHeatingCurve();
		curve.reinit();
		
		final int counts = (int) curve.getNumPoints().getValue();		
		
		double maxVal = 0;
		int i, j, m, w;
		double pls;
		
		//precalculated constants

		double HH      = pow(grid.hx,2);
		double F;			
		
		//precalculated constants
		
		final double T   = (double) ref.getTestTemperature().getValue();
		final double rho = (double) ref.getDensity().getValue();
		final double cV  = (double) ref.getSpecificHeat().getValue();
		final double qAbs = (double) ref.getAbsorbedEnergy().getValue();
		final double nonlinearPrecision = (double) ref.getNonlinearPrecision().getValue(); 			
		
		//constants for bc calc

		double a1 = grid.tau/(HH + grid.tau);
		double b1 = -0.25*grid.hx*grid.tau/(HH + grid.tau)*Bi1*T;
		double pulseWidth = (double)ref.getPulse().getSpotDiameter().getValue();
		double b2 = grid.hx*grid.tau/(HH + grid.tau)*qAbs
				  /(cV*rho*PI*l*pow(pulseWidth, 2));
		double b3 = grid.tau/(HH + grid.tau);
		double b4 = (HH - grid.tau)/(HH + grid.tau);
		double c1 = 0.25*grid.hx*grid.tau*Bi2*T;
		double c2;
		
		//time cycle

		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
							
				alpha[1] = a1;
				pls 	 = discretePulse.evaluateAt( (m - EPS)*grid.tau ) 
						 + discretePulse.evaluateAt( (m + 1 + EPS)*grid.tau ); //possibly change to m + 1 - eps					
				
				for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
				
					beta[1]  = b1*(pow(V[0]/T + 1, 4) 
							 - pow(U[0]/T + 1, 4)) + b2*pls + b3*U[1] + b4*U[0];

					for (i = 1; i < grid.N; i++) {
						alpha[i+1] = c/(b - a*alpha[i]);
						F          =  - 2.*U[i]/grid.tau - (U[i+1] - 2.*U[i] + U[i-1])/HH;
						beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);	
					}

					diff = -0.5*V[0] - 0.5*V[grid.N];
					
					c2   = 1./(HH + grid.tau - alpha[grid.N]*grid.tau);
					V[grid.N] = c2*( grid.tau*beta[grid.N] - c1*(pow(V[grid.N]/T + 1, 4) 
						 + pow(U[grid.N]/T + 1, 4) - 2) + grid.tau*U[grid.N-1] - grid.tau*U[grid.N] + HH*U[grid.N] );

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
		
		ref.setMaximumTemperature(new NumericProperty(maxVal, 
				NumericProperty.def(NumericPropertyKeyword.MAXTEMP)));
	}

}