/**
 * 
 */
package pulse.problem.statements;

import static java.lang.Math.pow;

import java.util.ArrayList;
import java.util.List;
import pulse.Baseline;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.util.SaveableDirectory;

import static java.lang.Math.PI;
import static java.lang.Math.exp;

import static pulse.properties.NumericPropertyKeyword.*;

/**
 * @author Artem V. Lunev
 *
 */
public abstract class Problem extends PropertyHolder implements Reflexive, SaveableDirectory {		
	
	protected HeatingCurve curve;
	protected Pulse	pulse;
	protected double a, l;
	protected double Bi1, Bi2;
	protected double signalHeight;
	protected double cV, rho;
	
	private static boolean singleStatement;
	private static boolean hideDetailedAdjustment = true;
	private static NumericPropertyKeyword[] 
			criticalParameters = new NumericPropertyKeyword[]{ THICKNESS, DIAMETER,
			PULSE_WIDTH, SPOT_DIAMETER, PYROMETER_SPOT};
	
	private final double PARKERS_COEFFICIENT = 0.1388; //in mm
		
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(MAXTEMP));
		list.add(NumericProperty.def(DIFFUSIVITY));
		list.add(NumericProperty.def(THICKNESS));
		list.add(NumericProperty.def(HEAT_LOSS_FRONT));
		list.add(NumericProperty.def(HEAT_LOSS_REAR));
		//heating curve				
		return list;
	}
	
	protected Problem() {
		super();
		a = (double)NumericProperty.def(DIFFUSIVITY).getValue();
		l = (double)NumericProperty.def(THICKNESS).getValue();
		Bi1 = (double)NumericProperty.def(HEAT_LOSS_FRONT).getValue();
		Bi2 = (double)NumericProperty.def(HEAT_LOSS_REAR).getValue();
		signalHeight = (double)NumericProperty.def(MAXTEMP).getValue();
		singleStatement = true;
		pulse = new Pulse();
		curve = new HeatingCurve();
		curve.setParent(this);
		pulse.setParent(this);
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

	public void copyMainDetailsFrom(Problem p) {
		this.l 	   = p.l;
		this.pulse = new Pulse(p.pulse);
	}
	
	public final double classicSolutionAt(double time, int precision) {
		
		final double EPS = 1E-8; 
		double Fo = time*a/Math.pow(l, 2);
		
		if(time < EPS) return 0;
		
		double sum = 0;

		for (int i = 1; i <= precision; i++)
			sum += pow(-1, i)*exp(-pow(i*PI, 2)*Fo);
		
		return (1. + 2. * sum)*signalHeight;
		
	}
		
	public abstract DifferenceScheme[] availableSolutions();

	public NumericProperty getDiffusivity() {
		return NumericProperty.derive(DIFFUSIVITY, a);
	}
	
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		NumericPropertyKeyword prop = (NumericPropertyKeyword)type;
		double newVal = ((Number)value.getValue()).doubleValue();
		
		switch(prop) {
			case DIFFUSIVITY		: 	a = newVal; 			return;
			case MAXTEMP 			: 	signalHeight = newVal; 	return;
			case THICKNESS 			: 	l = newVal; 			return;
			case HEAT_LOSS_FRONT 	: 	Bi1 = newVal; 			return;
			case HEAT_LOSS_REAR 	: 	Bi2 = newVal; 			return;
			case SPECIFIC_HEAT 		: 	cV = newVal; 			return;
			case DENSITY 			: 	rho = newVal; 			return;
		}
		
		
		
	}

	public void setDiffusivity(NumericProperty a) {
		this.a = (double)a.getValue();
	}
	
	public void reset(ExperimentalData curve) {
		Bi1 = 0;
		Bi2 = 0;
	  	this.retrieveData(curve);
	}
	
	public NumericProperty getMaximumTemperature() {
		return NumericProperty.derive(MAXTEMP, signalHeight);
	}
	
	public void setMaximumTemperature(NumericProperty maxTemp) {
		this.signalHeight = (double) maxTemp.getValue();
	}

	public NumericProperty getSampleThickness() {
		return NumericProperty.derive(THICKNESS, l);
	}

	public void setSampleThickness(NumericProperty l) {
		this.l = (double)l.getValue();
	}

	public NumericProperty getHeatLoss() {
		final double eps = 1e-5;
		
		if(Math.abs(Bi1 - Bi2) > eps)
			System.err.println("Bi1 = " + Bi1 + " is not equal to " + " Bi2 = " + Bi2);
		
		return NumericProperty.derive(HEAT_LOSS, Bi1);
	}
	
	public NumericProperty getFrontHeatLoss() {
		return NumericProperty.derive(HEAT_LOSS_FRONT, Bi1);
	}

	public void setFrontHeatLoss(NumericProperty bi1) {
		this.Bi1 = (double)bi1.getValue();
	}
	
	public NumericProperty getHeatLossRear() {
		return NumericProperty.derive(HEAT_LOSS_REAR, Bi2);
	}

	public void setHeatLossRear(NumericProperty bi2) {
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
		pulse.setParent(this);
	}
	
	public double timeFactor() {
		return pow(l,2)/a;
	}
	
	public void retrieveData(ExperimentalData c) {		
		curve.getBaseline().fitTo(c);				
		estimateSignalRange(c);
		updateProperties(this, c.getMetadata());
		useParkersSolution(c);		
	}
	
	public void estimateSignalRange(ExperimentalData c) {
		signalHeight = c.maxTemperature() - curve.getBaseline().valueAt(0);
	}
	
	public void useParkersSolution(ExperimentalData c) {
		double t0 = c.halfRiseTime();	
		this.a = PARKERS_COEFFICIENT*l*l/t0;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
	public IndexedVector objectiveFunction(List<Flag> flags) {	
		IndexedVector objectiveFunction = new IndexedVector(Flag.convert(flags));
		int size = objectiveFunction.dimension(); 		
		
		Baseline baseline = curve.getBaseline();
		
		for(int i = 0; i < size; i++) {
			
			switch( objectiveFunction.getIndex(i) ) {
				case DIFFUSIVITY		:	objectiveFunction.set(i, a/(l*l)); break;
				case MAXTEMP			:	objectiveFunction.set(i, signalHeight); break;
				case BASELINE_INTERCEPT	: objectiveFunction.set(i, baseline.parameters()[0]); break; 
				case BASELINE_SLOPE		: objectiveFunction.set(i, baseline.parameters()[1]); break;	
				case HEAT_LOSS			:	objectiveFunction.set(i, Bi1 ); break;
				default 				: 	continue;
			}
		}
		
		return objectiveFunction;
		
	}
	
	public void assign(IndexedVector params) {
		for(int i = 0, size = params.dimension(); i < size; i++) {
			
			switch( params.getIndex(i) ) {
				case DIFFUSIVITY		:	a = params.get(i)*(l*l); break;
				case MAXTEMP			:	signalHeight = params.get(i);  break;
				case BASELINE_INTERCEPT	:	(curve.getBaseline()).
											setParameter(0, params.get(i)); break;
				case BASELINE_SLOPE		:	(curve.getBaseline()).
											setParameter(1, params.get(i)); break;
				case HEAT_LOSS			:	Bi1 = params.get(i); Bi2 = params.get(i); break;
				default 				: 	continue;
			}
		}
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
		return NumericProperty.derive(SPECIFIC_HEAT, cV);
	}

	public void setSpecificHeat(NumericProperty cV) {
		this.cV = (double)cV.getValue();
	}

	public NumericProperty getDensity() {
		return NumericProperty.derive(DENSITY, rho);
	}

	public String shortName() {
		return getClass().getSimpleName();
	}
	
	public NumericPropertyKeyword[] getCriticalParameters() {
		return criticalParameters;
	}
	
	public boolean isEnabled() {
		return true;
	}
	
}
