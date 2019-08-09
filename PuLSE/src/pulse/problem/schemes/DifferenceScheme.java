/**
 * 
 */
package pulse.problem.schemes;

import static java.lang.Math.pow;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import pulse.input.Pulse;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.util.PropertyHolder;

/**
 * @author Artem V. Lunev
 *
 */
public abstract class DifferenceScheme extends PropertyHolder {

	protected double  tau, tauFactor;
	protected int 	  N;
	protected double  hx;
	protected double  timeLimit;
	private   boolean normalized;
	protected int	  timeInterval;	
	
	private static boolean hideDetailedAdjustment = true;
	private final static long STANDARDT_TIMEOUT = 20000; //milliseconds
	
	protected DifferenceScheme(NumericProperty N) {
		this.N  = (int)N.getValue();
		this.hx = 1./this.N;						
	}
	
	public DifferenceScheme(DifferenceScheme df) {
		copyEverythingFrom(df);
	}
	
	public long getTimeoutAfterMillis() {
		return STANDARDT_TIMEOUT;
	}
	
	public void copyEverythingFrom(DifferenceScheme df) {
		this.N			= df.N;
		this.tau		= df.tau;
		this.tauFactor	= df.tauFactor;
		
		this.timeInterval = df.timeInterval;
		this.timeLimit	  = df.timeLimit;
		this.normalized	  = df.normalized;
		
		this.hx			  = df.hx;
	}
	
	public static DifferenceScheme copy(DifferenceScheme df) {
		
		Class<?> schemeClass = df.getClass();
		Constructor<?> c = null;
		
		try {
			c = schemeClass.getConstructor(df.getClass());
		} catch (NoSuchMethodException | SecurityException e) {
			System.err.println(Messages.getString("DifferenceScheme.0") + df.getClass()); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		try {
			return (DifferenceScheme) c.newInstance(df);
		} catch (InstantiationException e) {
			System.err.println(Messages.getString("DifferenceScheme.1") + df.getClass()); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println(Messages.getString("DifferenceScheme.2") + df.getClass()); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println(Messages.getString("DifferenceScheme.3") + c); //$NON-NLS-1$
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println(Messages.getString("DifferenceScheme.4") + df.getClass()); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public abstract void solve(Problem problem) throws IllegalArgumentException;
	
	public NumericProperty getTimeStep() {
		return new NumericProperty(Messages.getString("DifferenceScheme.5"), tau); //$NON-NLS-1$
	}
	
	public NumericProperty getTimeStepFactor() {
		return new NumericProperty(Messages.getString("DifferenceScheme.6"), tauFactor); //$NON-NLS-1$
	}
	
	public NumericProperty getGridDensity() {
		return new NumericProperty(this.N, NumericProperty.DEFAULT_GRID_DENSITY);
	}
	
	public final boolean isNormalized() {
		return this.normalized;
	}
	
	public final void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	public NumericProperty getXStep() {
		return new NumericProperty(Messages.getString("DifferenceScheme.7"), hx); //$NON-NLS-1$
	}

	public void setXStep(NumericProperty hx) {
		this.hx = (double)hx.getValue();
	}

	public NumericProperty getTimeLimit() {
		return new NumericProperty(timeLimit, NumericProperty.DEFAULT_TIME_LIMIT);
	}

	public void setTimeLimit(NumericProperty timeLimit) {
		this.timeLimit = (double)timeLimit.getValue();
	}
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(9);
		map.put(Messages.getString("DifferenceScheme.8"), Messages.getString("DifferenceScheme.9")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Messages.getString("DifferenceScheme.10"), Messages.getString("DifferenceScheme.11")); //$NON-NLS-1$ //$NON-NLS-2$
		map.put(Messages.getString("DifferenceScheme.12"), Messages.getString("DifferenceScheme.13"));		 //$NON-NLS-1$ //$NON-NLS-2$
		return map;
	}
	
	@Override	
	public String toString() {
		return shortName();
	}		
	
	public String shortName() {
		return this.getClass().getSimpleName();
	}
	
	public void adjustScheme(Problem problem) {
		
		/* first, adjust N and hx  */
		
		Pulse pulse = problem.getPulse();
		double pWidth = pulse.pSpotDiameter(problem, this);
				
		for(final double factor = 1.05; factor*hx > pWidth ; hx = 1./N, pWidth = pulse.pSpotDiameter(problem, this)) {
			N += 5;			
		}
		
		/* second, adjust tau */
		
		double pTime = pulse.pWidth(problem, this);
		
		for(final double factor = 1.05; 
				factor*tau > pTime ; 
				pTime = pulse.pWidth(problem, this)) {
			tauFactor	/= 1.5;						
			tau			 = tau();
		}
		
	}
	
	protected double tau() {
		return tauFactor*pow(hx,2);
	}
	
	@Override
	public boolean areDetailsHidden() {
		return DifferenceScheme.hideDetailedAdjustment;
	}
	
	public static void setDetailsHidden(boolean b) {
		DifferenceScheme.hideDetailedAdjustment = b;
	}
	
}
