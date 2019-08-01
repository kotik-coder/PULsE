/**
 * 
 */
package pulse.problem.schemes;

import static java.lang.Math.pow;

import pulse.HeatingCurve;
import pulse.problem.statements.LinearizedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

import static java.lang.Math.abs;
import static java.lang.Math.PI;

/**
 * @author Artem V. Lunev
 *
 */
public class MixedScheme extends DifferenceScheme {
	
	private final static NumericProperty DEFAULT_TAU_FACTOR = new NumericProperty(Messages.getString("MixedScheme.0"), 0.25, 1E-4, 0.5, 0.25, 1.0, true); //$NON-NLS-1$
	private final static NumericProperty DEFAULT_N		    = new NumericProperty(Messages.getString("MixedScheme.1"), 80, 15, 1000, 80, 1, true); //$NON-NLS-1$
	
	/**
	 * 
	 */
	
	public MixedScheme() {
		this(DEFAULT_N);
	}
	
	public MixedScheme(NumericProperty N) {
		this(N, NumericProperty.DEFAULT_TIME_LIMIT);	
	}
	
	public MixedScheme(NumericProperty N, NumericProperty timeLimit) {
		super(N);
		this.tauFactor = (double)DEFAULT_TAU_FACTOR.getValue();
		this.tau	   = tauFactor*pow(hx, 2);
		this.timeLimit = (double) timeLimit.getValue();		
	}
	
	public MixedScheme(MixedScheme df) {
		super(df);
	}

	@Override
	public final NumericProperty getTimeStepFactor() {
		return new NumericProperty(tauFactor, DEFAULT_TAU_FACTOR);
	}
	
	@Override
	public NumericProperty getGridDensity() {
		return new NumericProperty(N, DEFAULT_N);
	}
	
	/* (non-Javadoc)
	 * @see lenin.direct.DifferenceScheme#solve(lenin.direct.Problem)
	 */
	@Override
	public
	void solve(Problem problem) throws IllegalArgumentException {
		
		//quick links
		
		final double l = (double) problem.getSampleThickness().getValue();
		final double Bi1 = (double) problem.getFrontLosses().getValue();
		final double Bi2 = (double) problem.getRearLosses().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue(); 
		
		//end
		
		final double EPS = 1e-5;
		
		double[] U 	   = new double[N + 1];
		double[] V     = new double[N + 1];
		double[] alpha = new double[N + 2];
		double[] beta  = new double[N + 2];
		
		problem.getPulse().transform(problem, this);
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.flattenToBaseline();
		
		final int counts = (int) curve.getNumPoints().getValue();		
		
		double maxVal = 0;
		
		timeInterval = (int) ( timeLimit / 
				 ( tau*problem.timeFactor() * counts ) 
				 + 1 );
		
		if(timeInterval < 1)
			throw new IllegalArgumentException(Messages.getString("MixedScheme.2") + timeInterval); //$NON-NLS-1$
		
		int i, j, m, w;
		double pls;
		
		//coefficients for difference equation

		double a = 1./pow(hx,2);
		double b = 2./tau + 2./pow(hx,2);
		double c = 1./pow(hx,2);

		//precalculated constants

		double HH      = pow(hx,2);
		double F;			
		
		if(problem instanceof LinearizedProblem) {
			
			LinearizedProblem ref = (LinearizedProblem)problem;
			
			//precalculated constants
			
			double Bi1HTAU = Bi1*hx*tau;
			double Bi2HTAU = Bi2*hx*tau;
			
			//constant for bc calc

			double a1 = tau/(Bi1HTAU + HH + tau);
			double b1 = 1./(Bi1HTAU + HH + tau);
			double b2 = -hx*(Bi1*tau - hx);
			double b3 = hx*tau;
			double c1 = b2;
			double c2 = Bi2HTAU + HH;
			
			//time cycle

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					alpha[1] = a1;
					pls 	 = problem.getPulse().evaluateAt( (m - EPS)*tau ) 
							 + problem.getPulse().evaluateAt( (m + 1 + EPS)*tau ); //possibly change to m + 1 - eps
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
				
				curve.setTemperatureAt(w, V[N]);
				maxVal = Math.max(maxVal, V[N]);
				curve.setTimeAt( w,	(w*timeInterval)*tau*problem.timeFactor() );
				
			}			

			if(!((boolean)ref.isDimensionless().getValue()))
				curve.scale( maxTemp/maxVal );
			
			return;
			
		}
		
		if(problem instanceof NonlinearProblem) {
			
			NonlinearProblem ref = (NonlinearProblem)problem;
			
			final double T   = (double) ref.getTestTemperature().getValue();
			final double rho = (double) ref.getDensity().getValue();
			final double cV  = (double) ref.getSpecificHeat().getValue();
			final double qAbs = (double) ref.getAbsorbedEnergy().getValue();
			final double nonlinearPrecision = (double) ref.getNonlinearPrecision().getValue(); 			
			
			//constants for bc calc

			double a1 = tau/(HH + tau);
			double b1 = -0.25*hx*tau/(HH + tau)*Bi1*T;
			double pulseWidth = (double)ref.getPulse().getSpotDiameter().getValue();
			double b2 = hx*tau/(HH + tau)*qAbs
					  /(cV*rho*PI*l*pow(pulseWidth, 2));
			double b3 = tau/(HH + tau);
			double b4 = (HH - tau)/(HH + tau);
			double c1 = 0.25*hx*tau*Bi2*T;
			double c2;
			
			//time cycle

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
								
					alpha[1] = a1;
					pls 	 = problem.getPulse().evaluateAt( (m - EPS)*tau ) 
							 + problem.getPulse().evaluateAt( (m + 1 + EPS)*tau ); //possibly change to m + 1 - eps					
					
					for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
					
						beta[1]  = b1*(pow(V[0]/T + 1, 4) 
								 - pow(U[0]/T + 1, 4)) + b2*pls + b3*U[1] + b4*U[0];

						for (i = 1; i < N; i++) {
							alpha[i+1] = c/(b - a*alpha[i]);
							F          =  - 2.*U[i]/tau - (U[i+1] - 2.*U[i] + U[i-1])/HH;
							beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);	
						}

						diff = -0.5*V[0] - 0.5*V[N];
						
						c2   = 1./(HH + tau - alpha[N]*tau);
						V[N] = c2*( tau*beta[N] - c1*(pow(V[N]/T + 1, 4) 
							 + pow(U[N]/T + 1, 4) - 2) + tau*U[N-1] - tau*U[N] + HH*U[N] );

						for (j = N-1; j >= 0; j--)
							V[j] = alpha[j+1]*V[j+1] + beta[j+1];
						
						diff += 0.5*V[0] + 0.5*V[N];
					
					}
										
					System.arraycopy(V, 0, U, 0, N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[N]);
				maxVal = Math.max(maxVal, V[N]);
				curve.setTimeAt( w,	(w*timeInterval)*tau*problem.timeFactor() );
				
			}
			
			ref.setMaximumTemperature(new NumericProperty(maxVal, NumericProperty.DEFAULT_MAXTEMP));
			
			return;
		}
		
		throw new IllegalArgumentException(Messages.getString("MixedScheme.3") + problem.toString()); //$NON-NLS-1$
		
	}
	
	public String toString() {
		return Messages.getString("MixedScheme.4"); //$NON-NLS-1$
	}

}
