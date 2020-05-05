package pulse.problem.statements;

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_FRONT;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_REAR;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;

import java.util.List;
import java.util.stream.Collectors;

import pulse.Baseline;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.StandartType;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.DiscretePulse;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.solvers.Solver;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * A {@code Problem} is an abstract class defining the general rules for handling
 * heat conduction problems, and also providing access to the basic properties 
 * used in the calculation with one of the {@code DifferenceScheme}s. Most importantly,
 * this class sets out the procedures for reading and writing the vector argument of the 
 * objective function for solving the optimisation problem.
 * @see pulse.problem.schemes.DifferenceScheme
 */

public abstract class Problem extends PropertyHolder implements Reflexive {		
	
	protected HeatingCurve curve;
	protected Pulse	pulse;
	protected double a, l;
	protected double Bi1, Bi2;
	protected double signalHeight;
	protected double cP, rho;
	
	private static boolean singleStatement = true;
	private static boolean hideDetailedAdjustment = true;
	
	/**
	 * The <b>corrected</b> proportionality factor setting out the relation between
	 * the thermal diffusivity and the half-rise time of an {@code ExperimentalData}
	 * curve. 
	 * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i> Journal of Applied Physics <b>32</b> (1961) 1679</a>
	 * @see <a href="https://doi.org/10.1016/j.ces.2019.01.014">Parker <i>et al.</i> Chem. Eng. Sci. <b>199</b> (2019) 546-551</a>
	 */
	
	public final double PARKERS_COEFFICIENT = 0.1370; //in mm
	
	/**
	 * Creates a {@code Problem} with default parameters (as found in the .XML file).
	 * <p>First, invokes the {@code super()} constructor of {@code PropertyHolder} to
	 * initialise {@code PropertyHolderListener}s, then initialises the variables 
	 * and creates default {@code Pulse} and {@code HeatingCurve}, setting this object
	 * as their parent.</p>
	 */
	
	protected Problem() {
		super();
		a = (double)NumericProperty.def(DIFFUSIVITY).getValue();
		l = (double)NumericProperty.def(THICKNESS).getValue();
		Bi1 = (double)NumericProperty.def(HEAT_LOSS_FRONT).getValue();
		Bi2 = (double)NumericProperty.def(HEAT_LOSS_REAR).getValue();
		signalHeight = (double)NumericProperty.def(MAXTEMP).getValue();
		pulse = new Pulse();
		curve = new HeatingCurve();
		curve.setParent(this);
		pulse.setParent(this);
	}
	
	/**
	 * Copies all essential parameters from {@code p}, excluding the heating curve,
	 * which is created anew.  
	 * @param p the {@code Problem} to replicate
	 */
	
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
	
	/**
	 * <p>Calculates the classic analytical solution <math><i>T(x=l</i>,<code>time</code>)</math> of Parker et al. at the specified {@code time}
	 * using the first {@code n = precision} terms of the solution series. The results is then scaled
	 * by a factor of {@code signalHeight} and returned.</p>   
	 * @param time The calculation time
	 * @param precision The number of terms in the approximated solution
	 * @return a double, representing <math><i>T(x=l</i>,<code>time</code>)</math> 
	 * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i> Journal of Applied Physics <b>32</b> (1961) 1679</a> 
	 */
	
	public final double classicSolutionAt(double time, int precision) {
		
		final double EPS = 1E-8; 
		double Fo = time*a/pow(l, 2);
		
		if(time < EPS) return 0;
		
		double sum = 0;

		for (int i = 1; i <= precision; i++)
			sum += pow(-1, i)*exp(-pow(i*PI, 2)*Fo);
		
		return (1. + 2. * sum)*signalHeight;
		
	}
	
	/**
	 * Lists the available {@code DifferenceScheme}s for this {@code Problem}.
	 * <p>This is done utilising the {@code Reflexive} interface implemented by the
	 * class {@code DifferenceSheme}. This method dynamically locates any subclasses
	 * of the {@code DifferenceScheme} in the associated package (note this can be 
	 * extended to include plugins) and checks whether any of the instances of those schemes
	 * return a non-{@code null} result when calling the {@code solver(Problem)} method.</p>
	 * @return a {@code List} of available {@code DifferenceScheme}s for solving this {@code Problem}.
	 */
	
	public List<DifferenceScheme> availableSolutions() {
		List<DifferenceScheme> allSchemes = Reflexive.instancesOf(DifferenceScheme.class);
		return allSchemes.stream().
				filter(scheme -> scheme instanceof Solver).
				filter(s -> s.domain() == this.getClass()).collect(Collectors.toList());
	}
	
	/**
	 * Used to change the parameter values of this {@code Problem}. It is only allowed
	 * to use those types of {@code NumericPropery} that are listed by the {@code listedParameters()}.
	 * @see listedTypes()  
	 */
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		NumericPropertyKeyword prop = type;
		
		switch(prop) {
			case DIFFUSIVITY		: 	setDiffusivity(value); 			break;
			case MAXTEMP 			: 	setMaximumTemperature(value); 	break;
			case THICKNESS 			: 	setSampleThickness(value); 		break;
			case HEAT_LOSS_FRONT 	: 	setFrontHeatLoss(value); 		break;
			case HEAT_LOSS_REAR 	: 	setRearHeatLoss(value); 		break;
			case SPECIFIC_HEAT 		: 	setSpecificHeat(value); 		break;
			case DENSITY 			: 	setDensity(value); 				break;
			default:
				break;
		}
		
	}

	public NumericProperty getDiffusivity() {
		return NumericProperty.derive(DIFFUSIVITY, a);
	}
	
	public void setDiffusivity(NumericProperty a) {
		this.a = (double)a.getValue();
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
	
	/**
	 * <p>Assuming that <code>Bi<sub>1</sub> = Bi<sub>2</sub></code>, returns the value of <code>Bi<sub>1</sub></code>.
	 * If <code>Bi<sub>1</sub> = Bi<sub>2</sub></code>, this will print a warning message (but will not throw an exception)</p> 
	 * @return Bi<sub>1</sub> as a {@code NumericProperty}
	 */

	public NumericProperty getHeatLoss() {
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

	public void setRearHeatLoss(NumericProperty bi2) {
		this.Bi2 = (double)bi2.getValue();
	}

	public HeatingCurve getHeatingCurve() {
		return curve;
	}

	public Pulse getPulse() {
		return pulse;
	}
	
	/**
	 * Sets the {@code pulse} of this {@code Problem} and assigns this {@code Problem} as its parent. 
	 * @param pulse a {@code Pulse} object 
	 */

	public void setPulse(Pulse pulse) {
		this.pulse = pulse;
		pulse.setParent(this);
	}
	
	/**
	 * Performs simple calculation of the <math><i>l<sup>2</sup>/a</i></math> factor 
	 * that is commonly used to evaluate the dimensionless time {@code t/timeFactor}.
	 * @return the time factor
	 */
	
	public double timeFactor() {
		return pow(l,2)/a;
	}
	
	/**
	 * This will use the data contained in {@code c} to estimate the detector signal span
	 * and the thermal diffusivity for this {@code Problem}. Note these estimates may be very rough. 
	 * @param c the {@code ExperimentalData} object
	 */
	
	public void retrieveData(ExperimentalData c) {		
		curve.getBaseline().fitTo(c); //used to estimate the floor of the signal range 				
		estimateSignalRange(c); 		
		updateProperties(this, c.getMetadata());
		useTheoreticalEstimates(c);		
	}
	
	/**
	 * The signal range is defined as <math>max{ <i>T(t)</i> } - min{ <i>T(t)</i> }</math>,
	 * where <math>max{...}</math> and <math>min{...}</math> are robust to outliers. This calls
	 * the {@code maxTemperature} method of {@code c} and uses the baseline value at {@code 0} 
	 * as the <math>min{...}</math> value.   	 
	 * @param c the {@code ExperimentalData} object
	 * @see pulse.input.ExperimentalData.maxTemperature()
	 */
	
	public void estimateSignalRange(ExperimentalData c) {
		signalHeight = c.maxTemperature() - curve.getBaseline().valueAt(0);
	}
	
	/**
	 * Calculates the half-rise time <i>t</i><sub>1/2</sub> of {@code c} and uses it to estimate 
	 * the thermal diffusivity of this problem: <code><i>a</i>={@value PARKERS_COEFFICIENT}*<i>l</i><sup>2</sup>/<i>t</i><sub>1/2</sub></code>.
	 * @param c the {@code ExperimentalData} used to estimate the thermal diffusivity value
	 * @see pulse.input.ExperimentalData.halfRiseTime()
	 */
	
	public void useTheoreticalEstimates(ExperimentalData c) {
		double t0	= c.halfRiseTime();	
		this.a		= PARKERS_COEFFICIENT*l*l/t0;
	}
	
	/**
	 * Calculates the vector argument defined on <math><b>R</b><sup>n</sup></math> to the 
	 * scalar objective function for this {@code Problem}.
	 * <p>This arguments is represented by an {@code IndexedVector}.The indices in this {@code IndexedVector} are 
	 * determined based on the respective {@code NumericPropertyKeyword} of each 
	 * {@code Flag} contained in the {@code flags}. To fill the vector with data, only
	 * those parameters from this {@code Problem} will be used which are defined by the {@code flags}, 
	 * e.g. if the flag associated with the {@code HEAT_LOSS} keyword
	 * is set to false, its value will be skipped when creating the vector.</p>
	 * @param flags a list of {@code Flag} objects, which determine the basis of the search
	 * @return an {@code IndexedVector} object, representing the objective function.
	 * @see listedTypes()
	 */
	
	/*
	 * TODO put relative bounds in a constant field
	 * Consider creating a Bounds class, or putting them in the XML file
	 */
	
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {	

		int size = output[0].dimension(); 		
		
		Baseline baseline = curve.getBaseline();
		
		for(int i = 0; i < size; i++) {
			
			switch( output[0].getIndex(i) ) {
				case DIFFUSIVITY		:
					double prefactor = pow(l, -2);
					output[0].set(i, a*prefactor, prefactor);
					output[1].set(i, 0.75*a*prefactor, prefactor);
					break;
				case MAXTEMP			:	
					output[0].set(i, signalHeight);
					output[1].set(i, 0.5*signalHeight);
					break;
				case BASELINE_INTERCEPT	:   
					output[0].set(i, baseline.parameters()[0]);
					output[1].set(i, 5);
					break; 
				case BASELINE_SLOPE		:   
					output[0].set(i, baseline.parameters()[1]);
					output[1].set(i, 1000);
					break;	
				case HEAT_LOSS			:	
					output[0].set(i, Bi1 );
					output[1].set(i, 2.0);
					break;
				case TIME_SHIFT			:	
					output[0].set(i, (double)curve.getTimeShift().getValue() );
					double timeLimit = curve.timeLimit(); 
					output[1].set(i, timeLimit*0.25);
					break;
				default 				: 	continue;
			}
		}
			
	}
	
	/**
	 * Assigns parameter values of this {@code Problem} using the optimisation vector {@code params}.
	 * Only those parameters will be updated, the types of which are listed as indices in the {@code params} vector.   
	 * @param params the optimisation vector, containing a similar set of parameters to this {@code Problem}
	 * @see listedTypes()
	 */
	
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
				case TIME_SHIFT			:	curve.set(NumericPropertyKeyword.TIME_SHIFT,
													  NumericProperty.derive(
													  NumericPropertyKeyword.TIME_SHIFT, 
													  params.get(i)));
											break;
				default 				: 	continue;
			}
		}				
		
	}
	
	/**
	 * Checks if all the details necessary to calculate the solution of this {@code Problem} are specified.
	 * May only return {@code false} in instances of {@code NonlinearProblem} or {@code NonlinearProblem2D} 
	 * if the specific heat or density data have not been loaded.
	 * @return {@code true} if the calculation can proceed, {@code false} if something is missing
	 */
	
	public boolean allDetailsPresent() {
		return true;
	}
	
	/**
	 * Checks whether changes in this {@code Problem} should automatically be 
	 * accounted for by other instances of this class. 
	 * @return {@code true} if the user has specified so (set by default), {@code false} otherwise
	 */

	public static boolean isSingleStatement() {
		return singleStatement;
	}
	
	/**
	 * Sets the flag to isolate or inter-connects changes in all instances of {@code Problem} 
	 * @param singleStatement {@code false} if other {@code Problem}s should disregard changes, which happened to this instances.
	 * {@code true} otherwise.
	 */

	public static void setSingleStatement(boolean singleStatement) {
		Problem.singleStatement = singleStatement;
	}
	
	/**
	 * Checks whether some 'advanced' details should stay hidden by the GUI when customising the {@code Problem} statement. 
	 * @return {@code true} if the user does not want to see the details (by default), {@code false} otherwise. 
	 */
	
	@Override
	public boolean areDetailsHidden() {
		return Problem.hideDetailedAdjustment;
	}
	
	/**
	 * Allows to either hide or display all 'advanced' settings for this {@code Problem}. 
	 * @param b {@code true} if the user does not want to see the details, {@code false} otherwise. 
	 */

	public static void setDetailsHidden(boolean b) {
		Problem.hideDetailedAdjustment = b;
	}
	
	public NumericProperty getSpecificHeat() {
		return NumericProperty.derive(SPECIFIC_HEAT, cP);
	}

	public void setSpecificHeat(NumericProperty cV) {
		this.cP = (double)cV.getValue();
		this.notifyListeners(InterpolationDataset.getDataset(StandartType.SPECIFIC_HEAT), cV);
	}

	public NumericProperty getDensity() {
		return NumericProperty.derive(DENSITY, rho);
	}
	
	public void setDensity(NumericProperty p) { 
		this.rho = (double)(p.getValue());
		this.notifyListeners(InterpolationDataset.getDataset(StandartType.DENSITY), p);
	}

	public String shortName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * Used for debugging. Initially, the nonlinear and two-dimensional
	 * problem statements are disabled, since they have not yet been
	 * thoroughly tested
	 * @return {@code true} if this problem statement has been enabled, {@code false} otherwise
	 */
	
	public boolean isEnabled() {
		return true;
	}
	
	/**
	 * Constructs a {@code DiscretePulse} on the specified {@code grid} using
	 * the {@code Pulse} corresponding to this {@code Problem}.
	 * @param grid the grid
	 * @return a {@code DiscretePulse} objects constructed for this {@code Problem} and the {@code grid}
	 */
	
	public DiscretePulse discretePulseOn(Grid grid) {
		return new DiscretePulse(this, getPulse(), grid);	
	}
	
	/**
	 * Listed parameters include: <code>MAXTEMP, DIFFUSIVITY, THICKNESS, HEAT_LOSS_FRONT, HEAT_LOSS_REAR</code>.
	 */
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(MAXTEMP));
		list.add(NumericProperty.def(DIFFUSIVITY));
		list.add(NumericProperty.def(THICKNESS));
		list.add(NumericProperty.def(HEAT_LOSS_FRONT));
		list.add(NumericProperty.def(HEAT_LOSS_REAR));	
		return list;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}
	
}