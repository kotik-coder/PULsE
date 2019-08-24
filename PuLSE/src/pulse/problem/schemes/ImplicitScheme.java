/**
 * 
 */
package pulse.problem.schemes;

import static java.lang.Math.pow;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem;
import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

import static java.lang.Math.PI;
import static java.lang.Math.abs;

/**
 * @author Artem V. Lunev
 *
 */
public class ImplicitScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = new NumericProperty(NumericPropertyKeyword.TAU_FACTOR, 
			Messages.getString("Tau.Descriptor"), Messages.getString("Tau.Abbreviation"), 0.25, 1E-4, 0.5, 0.25, 1.0, true); //$NON-NLS-1$
	private final static NumericProperty GRID_DENSITY = new NumericProperty(NumericPropertyKeyword.GRID_DENSITY, 
			Messages.getString("N.Descriptor"), Messages.getString("N.Abbreviation"), 30, 15, 1000, 80, 1, true); //$NON-NLS-1$
	
	/**
	 * 
	 */
	public ImplicitScheme() {
		this(GRID_DENSITY);
	}	
	
	public ImplicitScheme(NumericProperty N) {
		this(N, NumericProperty.TIME_LIMIT);	
	}
	
	public ImplicitScheme(NumericProperty N, NumericProperty timeLimit) {
		super(N);
		this.tauFactor = (double)TAU_FACTOR.getValue();
		this.tau	   = tauFactor*pow(hx, 2);
		this.timeLimit = (double) timeLimit.getValue();		
	}
	
	public ImplicitScheme(ImplicitScheme df) {
		super(df);
	}
	
	@Override
	public final NumericProperty getTimeStepFactor() {
		return new NumericProperty(tauFactor, TAU_FACTOR);
	}
	
	@Override
	public NumericProperty getGridDensity() {
		return new NumericProperty(N, GRID_DENSITY);
	}

	/* (non-Javadoc)
	 * @see lenin.direct.DifferenceScheme#solve(lenin.direct.Problem)
	 */
	@Override
	public
	void solve(Problem problem) throws IllegalArgumentException {
		
		//quick links
		
		final double l = (double) problem.getSampleThickness().getValue();
		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue(); 
		
		//end
		
		final double EPS = 1e-5;
		
		double[] U 	   = new double[N + 1];
		double[] V     = new double[N + 1];
		double[] alpha = new double[N + 2];
		double[] beta  = new double[N + 2];
		
		problem.getPulse().transform(problem, this);
		
		HeatingCurve curve = problem.getHeatingCurve();
		
		curve.reinit();
		
		final int counts = (int) curve.getNumPoints().getValue();
		
		double maxVal = 0;
		
		timeInterval = (int) ( timeLimit / 
				 ( tau*problem.timeFactor() * counts ) 
				 + 1 );
		
		if(timeInterval < 1) {
			System.exit(1);
			throw new IllegalArgumentException(Messages.getString("ImplicitScheme.2") + timeInterval); //$NON-NLS-1$
		}
			
		int i, j, m, w;
		double pls;
		
		//coefficients for difference equation

		double a = 1./pow(hx,2);
		double b = 1./tau + 2./pow(hx,2);
		double c = 1./pow(hx,2);

		//precalculated constants

		double HH      = pow(hx,2);
		double _2HTAU  = 2.*hx*tau;
		
		double F;		
		
		if(problem instanceof LinearisedProblem) {
			
			//precalculated constants

			double Bi1HTAU = Bi1*hx*tau;
			double Bi2HTAU = Bi2*hx*tau;

			//time cycle

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					pls  = problem.getPulse().evaluateAt( (m - EPS)*tau );
					
					alpha[1] = 2.*tau/(2.*Bi1HTAU + 2.*tau + HH);
					beta[1]  = (HH*U[0] + _2HTAU*pls)/(2.*Bi1HTAU + 2.*tau + HH);

					for(i = 1; i < N; i++) {
						alpha[i+1] = c/(b - a*alpha[i]);
						F		   = -U[i]/tau;
						beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);						
					}
					
					V[N] = (HH*U[N] + 2.*tau*beta[N])/(2*Bi2HTAU + HH - 2.*tau*(alpha[N] - 1));

					for (j = N-1; j >= 0; j--)
						V[j] = alpha[j+1]*V[j+1] + beta[j+1];
										
					System.arraycopy(V, 0, U, 0, N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[N]);
				maxVal = Math.max(maxVal, V[N]);

				curve.setTimeAt( w,	(w*timeInterval)*tau*problem.timeFactor() );
				
			}
			
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
			
			//constant for bc calc

			double a1 = 2.*tau/(HH + 2.*tau);
			double b1 = HH/(2.*tau + HH);
			double b2 = -0.5*hx*tau/(2.*tau + HH)*Bi1*T;
			double pulseWidth = (double)ref.getPulse().getSpotDiameter().getValue();
			double b3 = _2HTAU/(2.*tau + HH)*qAbs
					  / (cV*rho*PI*l*pow(pulseWidth, 2));
			double c1 = -0.5*hx*tau*Bi2*T;
			double c2;

			//time cycle
			
			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					pls  = problem.getPulse().evaluateAt( (m - EPS)*tau );					
					alpha[1] = a1;					
					
					for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
					
						beta[1]  = b1*U[0] + b2*(pow(V[0]/T + 1, 4) - 1) + b3*pls;

						for(i = 1; i < N; i++) {
							alpha[i+1] = c/(b - a*alpha[i]);
							F		   = -U[i]/tau;
							beta[i+1]  = (F - a*beta[i])/(a*alpha[i] - b);						
						}
						
						diff = -(0.5*V[0] + 0.5*V[N]);

						c2   = 1./(HH + 2.*tau - 2*alpha[N]*tau);
						V[N] = c2*(2.*beta[N]*tau + HH*U[N] + c1*(pow(V[N]/T + 1, 4) - 1) );

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

			ref.setMaximumTemperature(new NumericProperty(maxVal, NumericProperty.MAXTEMP));
			
			return;
		}
		
		throw new IllegalArgumentException(Messages.getString("ImplicitScheme.3") + problem.toString()); //$NON-NLS-1$
		

	}
	
	@Override
	public String toString() {
		return Messages.getString("ImplicitScheme.4"); //$NON-NLS-1$
	}	
	
	@Override
	public List<Property> listedParameters() {		
		List<Property> list = super.listedParameters();
		list.add(GRID_DENSITY);
		list.add(TAU_FACTOR);
		return list;
	}

}
