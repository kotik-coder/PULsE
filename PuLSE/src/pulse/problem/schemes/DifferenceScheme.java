/**
 * 
 */
package pulse.problem.schemes;

import static java.lang.Math.pow;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.input.Pulse;
import pulse.input.Pulse.PulseShape;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;
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
	
	public double getTimeStep() {
		return tau;
	}
	
	public NumericProperty getTimeStepFactor() {
		return new NumericProperty(
				NumericPropertyKeyword.TAU_FACTOR, 
				Messages.getString("Tau.Descriptor"), 
				Messages.getString("Tau.Abbreviation"), tauFactor); //$NON-NLS-1$
	}
	
	public void setTimeStepFactor(NumericProperty property) {
		this.tauFactor = (double)property.getValue();
	}
	
	public NumericProperty getGridDensity() {
		return new NumericProperty(this.N, NumericProperty.GRID_DENSITY);
	}
	
	public void setGridDensity(NumericProperty density) {
		this.N = (int)density.getValue();
		hx = 1./N;
	}
	
	public final boolean isNormalized() {
		return this.normalized;
	}
	
	public final void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	public double getXStep() {
		return hx;
	}

	public void setXStep(double hx) {
		this.hx = hx;
	}

	public NumericProperty getTimeLimit() {
		return new NumericProperty(timeLimit, NumericProperty.TIME_LIMIT);
	}

	public void setTimeLimit(NumericProperty timeLimit) {
		this.timeLimit = (double)timeLimit.getValue();
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>(9);
		list.add(NumericProperty.TIME_LIMIT);
		return list;
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
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case TIME_LIMIT : setTimeLimit(property); break;
		case TAU_FACTOR : setTimeStepFactor(property); break;
		case GRID_DENSITY : setGridDensity(property); break;
		}
	}
	
}
