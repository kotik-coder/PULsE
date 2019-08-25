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

/**
 * @author Artem V. Lunev
 *
 */
public class ExplicitScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 80);
	
	/**
	 * 
	 */
	public ExplicitScheme() {
		this(GRID_DENSITY);
	}	
	
	public ExplicitScheme(NumericProperty N) {
		this(N, NumericProperty.def(NumericPropertyKeyword.TIME_LIMIT));		
	}
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeLimit) {
		super(N);
		this.tauFactor = (double)TAU_FACTOR.getValue();
		this.tau	   = tauFactor*pow(hx, 2);
		this.timeLimit = (double) timeLimit.getValue();			
	}
	
	public ExplicitScheme(ExplicitScheme df) {
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
		
		double[] U 	   = new double[N+ 1];
		double[] V     = new double[N + 1];
		
		problem.getPulse().transform(problem, this);
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		final int counts = (int) curve.getNumPoints().getValue();
		
		double maxVal = 0;
		
		double TAU_HH = tau/pow(hx,2);
		
		int i, m, w;
		double pls;
		
		timeInterval = (int) ( timeLimit / 
				 ( tau*problem.timeFactor() * counts ) 
				 + 1 );
		
		if(timeInterval < 1)
			throw new IllegalArgumentException(Messages.getString("ExplicitScheme.2") + timeInterval);		 //$NON-NLS-1$
		
		//solution of linearized problem with explicit scheme
		
		if(problem instanceof LinearisedProblem) {			
			
			LinearisedProblem ref = (LinearisedProblem)problem;			
			
			double a = 1./(1. + Bi1*hx);
			double b = 1./(1. + Bi2*hx);			
			
			//time cycle

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					for(i = 1; i < N; i++)
						V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
					
					pls  = problem.getPulse().evaluateAt( (m - EPS)*tau );
					V[0] = (V[1] + hx*pls)*a ;
					V[N] =	V[N-1]*b;
					
					System.arraycopy(V, 0, U, 0, N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[N]);
				maxVal = Math.max(maxVal, V[N]);
				curve.setTimeAt( w,	(w*timeInterval)*tau*problem.timeFactor() );
				
			}			

			curve.scale( maxTemp/maxVal );
			
			return; 			
			
		}
		
		//solution of linearized problem with explicit scheme (nonlinear heat sink)
		
		if(problem instanceof NonlinearProblem) {
			
			NonlinearProblem ref = ((NonlinearProblem)problem);
			final double T   = (double) ref.getTestTemperature().getValue();
			final double rho = (double) ref.getDensity().getValue();
			final double cV  = (double) ref.getSpecificHeat().getValue();
			final double qAbs = (double) ref.getAbsorbedEnergy().getValue();
			final double nonlinearPrecision = (double) ref.getNonlinearPrecision().getValue(); 			
			
			double pulseWidth = (double)ref.getPulse().getSpotDiameter().getValue();
			double c = qAbs/(cV*rho*PI*l*pow(pulseWidth, 2));

			for (w = 1; w < counts; w++) {
				
				for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
					
					for(i = 1; i < N; i++)
						V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] ) ;
					
					pls  = problem.getPulse().evaluateAt( (m - EPS)*tau );
					
				    for(double diff = 100, tmp = 0; diff/maxTemp > nonlinearPrecision; ) {
				    	tmp = V[1] + c*hx*pls - 0.25*hx*Bi1*T*( pow(V[0]/T + 1, 4) - 1); //bc1
				    	diff = tmp - V[0];
				    	V[0] = tmp;
				    }
				    
				    for(double diff = 100, tmp = 0; diff/maxTemp > nonlinearPrecision; ) {
				    	tmp = V[N-1] - 0.25*hx*Bi2*T*( pow(V[N]/T + 1, 4) - 1); //bc1
				    	diff = tmp - V[N];
				    	V[N] = tmp;
				    }					
					
					System.arraycopy(V, 0, U, 0, N + 1);
								
				}
				
				curve.setTemperatureAt(w, V[N]);
				maxVal = Math.max(maxVal, V[N]);
				curve.setTimeAt( w,	(w*timeInterval)*tau*problem.timeFactor() );
				ref.setMaximumTemperature(NumericProperty.derive(NumericPropertyKeyword.MAXTEMP, maxVal));
				
			}			
			
			return;
		}
		
		throw new IllegalArgumentException(Messages.getString("ExplicitScheme.3") + problem.toString()); //$NON-NLS-1$
		
	}
	
	@Override
	public String toString() {
		return Messages.getString("ExplicitScheme.4");		 //$NON-NLS-1$
	}
	
	@Override
	public List<Property> listedParameters() {		
		List<Property> list = super.listedParameters();
		list.add(GRID_DENSITY);
		list.add(TAU_FACTOR);
		return list;
	}

}
