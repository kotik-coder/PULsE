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

public class ImplicitScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.25);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);
	
	public ImplicitScheme() {
		super(GRID_DENSITY, TAU_FACTOR);
	}	
	
	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);	
	}
	
	public ImplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
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
	public void solve(Problem p) {
		if(p instanceof LinearisedProblem)
			solve((LinearisedProblem) p);
		else if(p instanceof NonlinearProblem)
			solve((NonlinearProblem) p);
	}
	
	public void solve(LinearisedProblem problem) {
		super.solve(problem);		
		
		//quick links
		
		final double Bi1 				= (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 				= (double) problem.getHeatLossRear().getValue();
		final double maxTemp 			= (double) problem.getMaximumTemperature().getValue();
		
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
		double b = 1./grid.tau + 2./pow(grid.hx,2);
		double c = 1./pow(grid.hx,2);

		//precalculated constants

		double HH      = pow(grid.hx,2);
		double _2HTAU  = 2.*grid.hx*grid.tau;
		
		double F;
		
		//precalculated constants

		double Bi1HTAU = Bi1*grid.hx*grid.tau;
		double Bi2HTAU = Bi2*grid.hx*grid.tau;
		
		//time cycle

		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				pls  = discretePulse.evaluateAt( (m - EPS)*grid.tau );
				
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
			
			curve.setTemperatureAt(w, V[grid.N]);
			maxVal = Math.max(maxVal, V[grid.N]);

			curve.setTimeAt( w,	(w*timeInterval)*grid.tau*problem.timeFactor() );
			
		}
		
		curve.scale( maxTemp/maxVal );
		
	}
	
	public void solve(NonlinearProblem ref) {
		super.solve(ref);
		
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
	}

}