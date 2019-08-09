/**
 * 
 */
package pulse.problem.statements;

import static java.lang.Math.pow;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.Pulse;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Vector;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolder;
import pulse.util.PropertyHolderListener;
import pulse.util.Reflexive;
import pulse.util.ReflexiveFinder;
import pulse.util.SaveableDirectory;

import static java.lang.Math.PI;
import static java.lang.Math.exp;

/**
 * @author Artem V. Lunev
 *
 */
public abstract class Problem extends PropertyHolder implements Reflexive, SaveableDirectory {		
	
	protected HeatingCurve curve;
	protected Pulse	pulse;
	protected double a, l;
	protected double Bi1, Bi2;
	protected double maximumTemperature;
	protected double cV, rho;
	
	private static boolean singleStatement;
	private static boolean hideDetailedAdjustment = true;
	private static NumericProperty[] mainInput = new NumericProperty[]{ NumericProperty.DEFAULT_THICKNESS, NumericProperty.DEFAULT_DIAMETER,
			NumericProperty.DEFAULT_PULSE_WIDTH, NumericProperty.DEFAULT_SPOT_DIAMETER, NumericProperty.DEFAULT_PYROMETER_SPOT
	};
		
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(9);
		map.put(getMaximumTemperature().getSimpleName(), Messages.getString("Problem.0")); //$NON-NLS-1$
		map.put(getDiffusivity().getSimpleName(), Messages.getString("Problem.1")); //$NON-NLS-1$
		map.put(getSampleThickness().getSimpleName(), Messages.getString("Problem.3")); //$NON-NLS-1$
		map.put(getFrontLosses().getSimpleName(), Messages.getString("Problem.4")); //$NON-NLS-1$
		map.put(getRearLosses().getSimpleName(), Messages.getString("Problem.5")); //$NON-NLS-1$
		map.put(Pulse.class.getSimpleName(), Messages.getString("Problem.6")); //$NON-NLS-1$
		map.put(HeatingCurve.class.getSimpleName(), Messages.getString("Problem.7")); //$NON-NLS-1$
		return map;
	}
	
	protected Problem() {
		super();
		a = (double)NumericProperty.DEFAULT_DIFFUSIVITY.getValue();
		l = (double)NumericProperty.DEFAULT_THICKNESS.getValue();
		pulse = new Pulse();
		Bi1 = (double)NumericProperty.DEFAULT_BIOT.getValue();
		Bi2 = (double)NumericProperty.DEFAULT_BIOT.getValue();
		maximumTemperature = (double)NumericProperty.DEFAULT_MAXTEMP.getValue();
		curve = new HeatingCurve();	
		singleStatement = true;
	}
	
	public Problem(Problem p) {	
		super();
		this.l			= p.l;
		
		this.pulse 		= new Pulse(p.getPulse());
		this.curve 		= new HeatingCurve();
		this.curve.setNumPoints(p.getHeatingCurve().getNumPoints());
		
		this.a 			= p.a;
		this.Bi1 		= p.Bi1;
		this.Bi2 		= p.Bi2;
	}
	
	public HeatingCurve classicSolution() {
		 final int N		= 30;
		 int size = curve.realCount();
		 HeatingCurve classicCurve = new HeatingCurve(new NumericProperty(size, NumericProperty.DEFAULT_COUNT));
		 
		 double time;
		 
	     for(int i = 0; i < size; i++) {
	    	 	time = curve.timeAt(i);
	    	 	classicCurve.set(i, time, classicSolutionAt(time, N)*maximumTemperature);
	     }
	     
	     classicCurve.setName("Classic solution");
	     return classicCurve;
	     
	}

	
	private void superUpdateProperty(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super.updateProperty(property);
	}
	
	@Override
	public void updateProperty(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		
		super.updateProperty(property);
		if(!Problem.isSingleStatement())
			return;
		
		Problem p;
		
		for(SearchTask task : TaskManager.getTaskList()) {
			p = task.getProblem();
			
			if( p .equals( this ) )
				continue;

			p.superUpdateProperty(property);
			
		}
		
	}
	
	public void copyMainDetailsFrom(Problem p) {
		this.l 	   = p.l;
		this.pulse = new Pulse(p.pulse);
	}
	
	private final double classicSolutionAt(double time, int precision) {
		
		final double EPS = 1E-8; 
		double Fo = time*a/Math.pow(l, 2);
		
		if(time < EPS) return 0;
		
		double sum = 0;

		for (int i = 1; i < precision; i++)
			sum += pow(-1, i)*exp(-pow(i*PI, 2)*Fo);
		
		return (1 + 2. * sum);
		
	}
		
	public abstract DifferenceScheme[] availableSolutions();

	public NumericProperty getDiffusivity() {
		return new NumericProperty(a, NumericProperty.DEFAULT_DIFFUSIVITY);
	}

	public void setDiffusivity(NumericProperty a) {
		this.a = (double)a.getValue();
	}
	
	public void reset(ExperimentalData curve) {
		Bi1 = 0;
		Bi2 = 0;
	  	this.removeListeners();
	  	this.couple(curve);
	}
	
	public NumericProperty getMaximumTemperature() {
		return new NumericProperty(maximumTemperature, NumericProperty.DEFAULT_MAXTEMP);
	}
	
	public void setMaximumTemperature(NumericProperty maxTemp) {
		this.maximumTemperature = (double) maxTemp.getValue();
	}

	public NumericProperty getSampleThickness() {
		return new NumericProperty(l, NumericProperty.DEFAULT_THICKNESS);
	}

	public void setSampleThickness(NumericProperty l) {
		this.l = (double)l.getValue();
	}

	public NumericProperty getHeatLoss() {
		final double eps = 1e-5;
		
		if(Math.abs(Bi1 - Bi2) > eps)
			throw new IllegalStateException("Bi1 = " + Bi1 + " is not equal to " + " Bi2 = " + Bi2); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		return new NumericProperty(Messages.getString("Problem.12"), Bi1, NumericProperty.DEFAULT_BIOT); //$NON-NLS-1$
	}
	
	public NumericProperty getFrontLosses() {
		return new NumericProperty(Messages.getString("Problem.13"), Bi1, NumericProperty.DEFAULT_BIOT); //$NON-NLS-1$
	}

	public void setFrontLosses(NumericProperty bi1) {
		this.Bi1 = (double)bi1.getValue();
	}
	
	public NumericProperty getRearLosses() {
		return new NumericProperty(Messages.getString("Problem.14"), Bi2, NumericProperty.DEFAULT_BIOT); //$NON-NLS-1$
	}

	public void setRearLosses(NumericProperty bi2) {
		this.Bi2 = (double)bi2.getValue();
	}

	public HeatingCurve getHeatingCurve() {
		return curve;
	}

	public Pulse getPulse() {
		return pulse;
	}

	public void setPulse(Pulse pulse) {
		this.pulse = pulse;
	}
	
	public double timeFactor() {
		return pow(l,2)/a;
	}
	
	public void couple(ExperimentalData c) {

		Object[][] data = c.getMetadata().data();
		
		for(int i = 0; i < data.length; i++) {
			try {
				super.updateProperty( (Property) data[i][1] );
				
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("Error when trying to update property " + data[i][1] + " in " + this);
				e.printStackTrace();
			} 
		}
		
		assignMaximumTemperature(c);
		assignThermalDiffusivity(c);
		
		this.addListener(new PropertyHolderListener() {

			@Override
			public void onPropertyChanged(PropertyEvent event) {
				Property property = event.getProperty();
				
				if(! (property instanceof NumericProperty) )
					return;
				
				NumericProperty p = (NumericProperty) property;
			
				for(NumericProperty critical : mainInput)
					if(p.getSimpleName().equals(critical.getSimpleName())) {
						assignMaximumTemperature(c);
						assignThermalDiffusivity(c);
					}
			
			}

		});
		
	}
	
	public void assignMaximumTemperature(ExperimentalData c) {
		maximumTemperature = c.findAveragedMaximum();
	}
	
	public void assignThermalDiffusivity(ExperimentalData c) {
		final double COEF = 0.1388; //in mm
		double t0 = c.findHalfMaximumTime();	
		this.a = COEF*l*l/t0;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	public Vector objectiveFunction(BooleanProperty[] flags) {	
		List<Double> data = new LinkedList<Double>();
		
		for(BooleanProperty flag : flags) {
			if(! (boolean) flag.getValue() )
				continue; 
			
			switch( ObjectiveFunctionIndex.valueOf(flag.getSimpleName()) ) {
				case DIFFUSIVITY	:	data.add( a/(l*l) ); break;
				case MAX_TEMP		:	data.add( maximumTemperature ); break;
				case BASELINE		:	data.add( curve.getBaselineValue() ); break;
				case HEAT_LOSSES	:	data.add( Bi1 ); break;
				default 			: 	continue;
			}
		}
		
		double[] dataArray = new double[data.size()];
		
		for(int i = 0; i < dataArray.length; i++)
			dataArray[i] = data.get(i);
		
		return new Vector(dataArray);
		
	}

	public static double parameterValue(Vector params, BooleanProperty[] flags, ObjectiveFunctionIndex param) {
		for(int i = 0, realIndex = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue;
			
			realIndex = convert(i, flags);
			
			if( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()) != param )
				continue;
			
			switch( param ) {
				case DIFFUSIVITY	: 
				case MAX_TEMP		:
				case BASELINE		:	
				case HEAT_LOSSES	:	return params.get(realIndex);
				default 			: 	continue;
			}
		}
		
		return 0;
		
	}
	
	public void assign(Vector params, BooleanProperty[] flags) {
		for(int i = 0, realIndex = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue;
			
			realIndex = convert(i, flags);
			
			switch( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()) ) {
				case DIFFUSIVITY	:	a = params.get(realIndex)*(l*l); break;
				case MAX_TEMP		:	maximumTemperature = params.get(realIndex);  break;
				case BASELINE		:	curve.setBaselineValue(params.get(realIndex)); break;
				case HEAT_LOSSES	:	Bi1 = params.get(realIndex); Bi2 = params.get(realIndex); break;
				default 			: 	continue;
			}
		}
	}
	
	protected static int convert(int index, BooleanProperty[] searchFlags) {
		int convertedIndex = index;
		
		for(int i = 0; i < searchFlags.length; i++) {
			if( (boolean) searchFlags[i].getValue() ) 
				continue;

			if(index > i) 
				convertedIndex--;
			else
				break;
			
		}
		
		return convertedIndex;
		
	}
	
	public static Problem[] findKnownProblems(String pckgname) {		
		List<Reflexive> ref = new LinkedList<Reflexive>();
		List<Problem> p = new LinkedList<Problem>();
		
		ref.addAll(ReflexiveFinder.findAllInstances(pckgname));
		
		for(Reflexive r : ref) 
			if(r instanceof Problem)
				p.add((Problem) r);
		
		return (Problem[])p.toArray(new Problem[p.size()]);
		
	}
	
	public static Problem[] findKnownProblems() {		
		return findKnownProblems(Problem.class.getPackage().getName());
	}
	
	public boolean isReady() {
		return true;
	}

	public static boolean isSingleStatement() {
		return singleStatement;
	}

	public static void setSingleStatement(boolean singleStatement) {
		Problem.singleStatement = singleStatement;
	}
	
	@Override
	public boolean areDetailsHidden() {
		return Problem.hideDetailedAdjustment;
	}
	
	public static void setDetailsHidden(boolean b) {
		Problem.hideDetailedAdjustment = b;
	}
	
	public NumericProperty getSpecificHeat() {
		return new NumericProperty(cV, NumericProperty.DEFAULT_CV);
	}

	public void setSpecificHeat(NumericProperty cV) {
		this.cV = (double)cV.getValue();
	}

	public NumericProperty getDensity() {
		return new NumericProperty(rho, NumericProperty.DEFAULT_RHO);
	}

	public void setDensity(NumericProperty rho) {
		this.rho = (double)rho.getValue();
	}

	public String shortName() {
		return getClass().getSimpleName();
	}
	
}
