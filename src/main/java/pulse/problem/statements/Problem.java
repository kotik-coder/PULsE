package pulse.problem.statements;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;

import java.util.List;
import java.util.stream.Collectors;

import pulse.HeatingCurve;
import pulse.baseline.Baseline;
import pulse.baseline.FlatBaseline;
import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.solvers.Solver;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.Optimisable;
import pulse.tasks.SearchTask;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * A {@code Problem} is an abstract class defining the general rules for
 * handling heat conduction problems, and also providing access to the basic
 * properties used in the calculation with one of the {@code DifferenceScheme}s.
 * Most importantly, this class sets out the procedures for reading and writing
 * the vector argument of the objective function for solving the optimisation
 * problem.
 * 
 * @see pulse.problem.schemes.DifferenceScheme
 */

public abstract class Problem extends PropertyHolder implements Reflexive, Optimisable {

	private ThermalProperties properties;
	private HeatingCurve curve;
	private Baseline baseline;
	private Pulse pulse;

	private static boolean hideDetailedAdjustment = true;
	private ProblemComplexity complexity = ProblemComplexity.LOW;

	private InstanceDescriptor<? extends Baseline> instanceDescriptor = new InstanceDescriptor<Baseline>(
			"Baseline Selector", Baseline.class);

	/**
	 * Creates a {@code Problem} with default parameters (as found in the .XML
	 * file).
	 * <p>
	 * First, invokes the {@code super()} constructor of {@code PropertyHolder} to
	 * initialise {@code PropertyHolderListener}s, then initialises the variables
	 * and creates default {@code Pulse} and {@code HeatingCurve}, setting this
	 * object as their parent.
	 * </p>
	 */

	protected Problem() {
		super();
		initProperties();

		curve = new HeatingCurve();
		curve.setParent(this);

		instanceDescriptor.attemptUpdate(FlatBaseline.class.getSimpleName());
		addListeners();
		initBaseline();
	}

	/**
	 * Copies all essential parameters from {@code p}, excluding the heating curve,
	 * which is created anew.
	 * 
	 * @param p the {@code Problem} to replicate
	 */

	public Problem(Problem p) {
		super();
		initProperties(p.getProperties().copy());

		this.curve = new HeatingCurve();
		this.curve.setParent(this);
		this.curve.setNumPoints(p.getHeatingCurve().getNumPoints());

		instanceDescriptor.attemptUpdate(p.getBaseline().getClass().getSimpleName());
		addListeners();
		initBaseline();
	}
	
	private void addListeners() {
		instanceDescriptor.addListener(() -> {
			initBaseline();
			this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
		});
		curve.addHeatingCurveListener(() -> {
			if (!curve.isIncomplete())
				curve.apply(getBaseline());
		});
	}

	/**
	 * Lists the available {@code DifferenceScheme}s for this {@code Problem}.
	 * <p>
	 * This is done utilising the {@code Reflexive} interface implemented by the
	 * class {@code DifferenceSheme}. This method dynamically locates any subclasses
	 * of the {@code DifferenceScheme} in the associated package (note this can be
	 * extended to include plugins) and checks whether any of the instances of those
	 * schemes return a non-{@code null} result when calling the
	 * {@code solver(Problem)} method.
	 * </p>
	 * 
	 * @return a {@code List} of available {@code DifferenceScheme}s for solving
	 *         this {@code Problem}.
	 */

	public List<DifferenceScheme> availableSolutions() {
		var allSchemes = Reflexive.instancesOf(DifferenceScheme.class);
		return allSchemes.stream().filter(scheme -> scheme instanceof Solver).filter(s -> s.domain() == this.getClass())
				.collect(Collectors.toList());
	}

	/**
	 * Used to change the parameter values of this {@code Problem}. It is only
	 * allowed to use those types of {@code NumericPropery} that are listed by the
	 * {@code listedParameters()}.
	 * 
	 * @see listedTypes()
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		properties.set(type, value);
	}

	public HeatingCurve getHeatingCurve() {
		return curve;
	}

	public Pulse getPulse() {
		return pulse;
	}

	/**
	 * Sets the {@code pulse} of this {@code Problem} and assigns this
	 * {@code Problem} as its parent.
	 * 
	 * @param pulse a {@code Pulse} object
	 */

	public void setPulse(Pulse pulse) {
		this.pulse = pulse;
		pulse.setParent(this);
	}

	/**
	 * This will use the data contained in {@code c} to estimate the detector signal
	 * span and the thermal diffusivity for this {@code Problem}. Note these
	 * estimates may be very rough.
	 * 
	 * @param c the {@code ExperimentalData} object
	 */

	public void retrieveData(ExperimentalData c) {
		baseline.fitTo(c); // used to estimate the floor of the signal range
		estimateSignalRange(c);
		updateProperties(this, c.getMetadata());
		properties.useTheoreticalEstimates(c);
	}

	/**
	 * The signal range is defined as <math>max{ <i>T(t)</i> } - min{ <i>T(t)</i>
	 * }</math>, where <math>max{...}</math> and <math>min{...}</math> are robust to
	 * outliers. This calls the {@code maxTemperature} method of {@code c} and uses
	 * the baseline value at {@code 0} as the <math>min{...}</math> value.
	 * 
	 * @param c the {@code ExperimentalData} object
	 * @see pulse.input.ExperimentalData.maxTemperature()
	 */

	public void estimateSignalRange(ExperimentalData c) {
		final double signalHeight = c.maxAdjustedSignal() - baseline.valueAt(0);
		properties.setMaximumTemperature(derive(MAXTEMP, signalHeight));
	}

	/**
	 * Calculates the vector argument defined on <math><b>R</b><sup>n</sup></math>
	 * to the scalar objective function for this {@code Problem}. To fill the vector
	 * with data, only those parameters from this {@code Problem} will be used which
	 * are defined by the {@code flags}, e.g. if the flag associated with the
	 * {@code HEAT_LOSS} keyword is set to false, its value will be skipped when
	 * creating the vector.
	 * </p>
	 * 
	 * @see listedTypes()
	 */

	/*
	 * TODO put relative bounds in a constant field Consider creating a Bounds
	 * class, or putting them in the XML file
	 */

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {

		baseline.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {

			switch (output[0].getIndex(i)) {
			case DIFFUSIVITY:
				final double l = (double)properties.getSampleThickness().getValue();
				final double a = (double)properties.getDiffusivity().getValue();
				final double prefactor = 1.0/(l*l);
				output[0].set(i, a * prefactor);
				output[1].set(i, 0.45 * a * prefactor);
				break;
			case MAXTEMP:
				final double signalHeight = (double)properties.getMaximumTemperature().getValue();
				output[0].set(i, signalHeight);
				output[1].set(i, 0.5 * signalHeight);
				break;
			case HEAT_LOSS:
				final double Bi = (double)properties.getHeatLoss().getValue();
				output[0].set(i,
						properties.areThermalPropertiesLoaded() ? atanh(2.0 * Bi / properties.maxBiot() - 1.0) : log(Bi));
				output[1].set(i, 2.0);
				break;
			case TIME_SHIFT:
				output[0].set(i, (double) curve.getTimeShift().getValue());
				output[1].set(i, curve.timeLimit() * 0.25);
				break;
			default:
				continue;
			}
		}

	}

	/**
	 * Assigns parameter values of this {@code Problem} using the optimisation
	 * vector {@code params}. Only those parameters will be updated, the types of
	 * which are listed as indices in the {@code params} vector.
	 * 
	 * @see listedTypes()
	 */

	@Override
	public void assign(IndexedVector params) {
		baseline.assign(params);
		for (int i = 0, size = params.dimension(); i < size; i++) {

			switch (params.getIndex(i)) {
			case DIFFUSIVITY:
				final double l = (double)properties.getSampleThickness().getValue();
				properties.setDiffusivity(derive(DIFFUSIVITY, params.get(i) * (l * l)));
				break;
			case MAXTEMP:
				properties.setMaximumTemperature(derive(MAXTEMP, params.get(i)));
				break;
			case HEAT_LOSS:
				final double bi = properties.areThermalPropertiesLoaded() ? 0.5 * properties.maxBiot() * (tanh(params.get(i)) + 1.0)
						: exp(params.get(i));
				properties.setHeatLoss(derive(HEAT_LOSS, bi));
				break;
			case TIME_SHIFT:
				curve.set(TIME_SHIFT, derive(TIME_SHIFT, params.get(i)));
				break;
			default:
				continue;
			}
		}

	}

	/**
	 * Checks whether some 'advanced' details should stay hidden by the GUI when
	 * customising the {@code Problem} statement.
	 * 
	 * @return {@code true} if the user does not want to see the details (by
	 *         default), {@code false} otherwise.
	 */

	@Override
	public boolean areDetailsHidden() {
		return Problem.hideDetailedAdjustment;
	}

	/**
	 * Allows to either hide or display all 'advanced' settings for this
	 * {@code Problem}.
	 * 
	 * @param b {@code true} if the user does not want to see the details,
	 *          {@code false} otherwise.
	 */

	public static void setDetailsHidden(boolean b) {
		Problem.hideDetailedAdjustment = b;
	}

	public String shortName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * Used for debugging. Initially, the nonlinear and two-dimensional problem
	 * statements are disabled, since they have not yet been thoroughly tested
	 * 
	 * @return {@code true} if this problem statement has been enabled,
	 *         {@code false} otherwise
	 */

	public boolean isEnabled() {
		return true;
	}

	/**
	 * Constructs a {@code DiscretePulse} on the specified {@code grid} using the
	 * {@code Pulse} corresponding to this {@code Problem}.
	 * 
	 * @param grid the grid
	 * @return a {@code DiscretePulse} objects constructed for this {@code Problem}
	 *         and the {@code grid}
	 */

	public DiscretePulse discretePulseOn(Grid grid) {
		return new DiscretePulse(this, grid);
	}

	/**
	 * Listed parameters include:
	 * <code>MAXTEMP, DIFFUSIVITY, THICKNESS, HEAT_LOSS_FRONT, HEAT_LOSS_REAR</code>.
	 */

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(MAXTEMP));
		list.add(def(DIFFUSIVITY));
		list.add(def(THICKNESS));
		list.add(def(HEAT_LOSS));
		list.add(instanceDescriptor);
		return list;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public ProblemComplexity getComplexity() {
		return complexity;
	}

	public void setComplexity(ProblemComplexity complexity) {
		this.complexity = complexity;
	}

	/**
	 * Return the {@code Baseline} of this {@code Problem}.
	 * 
	 * @return the baseline
	 */

	public Baseline getBaseline() {
		return baseline;
	}

	/**
	 * Sets a new baseline. Calls {@code apply(baseline)} on the
	 * {@code HeatingCurve} when done and sets the {@code parent} of the baseline to
	 * this object.
	 * 
	 * @param baseline the new baseline.
	 * @see pulse.baseline.Baseline.apply(Baseline)
	 */

	public void setBaseline(Baseline baseline) {
		this.baseline = baseline;
		if (!curve.isIncomplete())
			curve.apply(baseline);
		baseline.setParent(this);
		
		var searchTask = (SearchTask) this.specificAncestor(SearchTask.class);
		if (searchTask != null) {
			var experimentalData = searchTask.getExperimentalCurve();
			baseline.fitTo(experimentalData);
		}
	}

	public InstanceDescriptor<? extends Baseline> getBaselineDescriptor() {
		return instanceDescriptor;
	}

	private void initBaseline() {
		//TODO
		var baseline = instanceDescriptor.newInstance(Baseline.class);
		setBaseline(baseline);
		parameterListChanged();
	}

	public ThermalProperties getProperties() {
		return properties;
	}
	
	public final void setProperties(ThermalProperties properties) {
		this.properties = properties;
		this.properties.setParent(this);
	}

	public abstract void initProperties();
	public abstract void initProperties(ThermalProperties properties);
	public abstract Class<? extends DifferenceScheme> defaultScheme();
	public abstract boolean isReady();
	
}