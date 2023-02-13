package pulse.problem.statements;

import java.io.Serializable;
import java.util.Arrays;
import static pulse.input.listeners.CurveEventType.RESCALED;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import pulse.HeatingCurve;
import pulse.baseline.Baseline;
import pulse.baseline.FlatBaseline;
import pulse.baseline.LinearBaseline;
import pulse.input.ExperimentalData;
import pulse.math.Parameter;
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import static pulse.problem.schemes.solvers.SolverException.SolverExceptionType.ILLEGAL_PARAMETERS;
import pulse.problem.statements.model.ThermalProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
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

    /**
     *
     */
    private static final long serialVersionUID = 7275327427201737684L;
    private ThermalProperties properties;
    private HeatingCurve curve;
    private Baseline baseline;
    private Pulse pulse;

    private static boolean hideDetailedAdjustment = true;
    private ProblemComplexity complexity = ProblemComplexity.LOW;

    private InstanceDescriptor<? extends Baseline> instanceDescriptor
            = new InstanceDescriptor<>(
                    "Baseline Selector", Baseline.class);

    /**
     * Creates a {@code Problem} with default parameters (as found in the .XML
     * file).
     * <p>
     * First, invokes the {@code super()} constructor of {@code PropertyHolder}
     * to initialise {@code PropertyHolderListener}s, then initialises the
     * variables and creates default {@code Pulse} and {@code HeatingCurve},
     * setting this object as their parent.
     * </p>
     */
    protected Problem() {
        initProperties();
        setHeatingCurve(new HeatingCurve());
        addListeners();
        instanceDescriptor.attemptUpdate(LinearBaseline.class.getSimpleName());
    }

    /**
     * Copies all essential parameters from {@code p}, excluding the heating
     * curve, which is created anew.
     *
     * @param p the {@code Problem} to replicate
     */
    public Problem(Problem p) {
        initProperties(p.getProperties().copy());
        setHeatingCurve(new HeatingCurve(p.getHeatingCurve()));
        curve.setNumPoints(p.getHeatingCurve().getNumPoints());
        setBaseline(p.getBaseline());
        addListeners();
    }

    public abstract Problem copy();

    public final void setHeatingCurve(HeatingCurve curve) {
        this.curve = curve;
        curve.setParent(this);
    }

    private void addListeners() {
        instanceDescriptor.addListener(() -> {
            initBaseline();
            this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
        });
        curve.addHeatingCurveListener(e -> {
            if (e.getType() == RESCALED) {
                curve.apply(getBaseline());
            }
        });
    }

    /**
     * Lists the available {@code DifferenceScheme}s for this {@code Problem}.
     * <p>
     * This is done utilising the {@code Reflexive} interface implemented by the
     * class {@code DifferenceSheme}. This method dynamically locates any
     * subclasses of the {@code DifferenceScheme} in the associated package
     * (note this can be extended to include plugins) and checks whether any of
     * the instances of those schemes return a non-{@code null} result when
     * calling the {@code solver(Problem)} method.
     * </p>
     *
     * @return a {@code List} of available {@code DifferenceScheme}s for solving
     * this {@code Problem}.
     */
    public final List<DifferenceScheme> availableSolutions() {
        var allSchemes = Reflexive.instancesOf(DifferenceScheme.class);
        return allSchemes.stream().filter(scheme -> scheme instanceof Solver)
                .filter(s -> Arrays.asList(s.domain()).contains(this.getClass()))
                .collect(Collectors.toList());
    }

    /**
     * Used to change the parameter values of this {@code Problem}. It is only
     * allowed to use those types of {@code NumericPropery} that are listed by
     * the {@code listedParameters()}.
     *
     * @see listedTypes()
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty value) {
        properties.set(type, value);
    }

    public final HeatingCurve getHeatingCurve() {
        return curve;
    }

    public final Pulse getPulse() {
        return pulse;
    }

    /**
     * Sets the {@code pulse} of this {@code Problem} and assigns this
     * {@code Problem} as its parent.
     *
     * @param pulse a {@code Pulse} object
     */
    public final void setPulse(Pulse pulse) {
        this.pulse = pulse;
        this.pulse.setParent(this);
    }

    /**
     * This will use the data contained in {@code c} to estimate the detector
     * signal span and the thermal diffusivity for this {@code Problem}. Note
     * these estimates may be very rough.
     *
     * @param c the {@code ExperimentalData} object
     */
    public void retrieveData(ExperimentalData c) {
        baseline.fitTo(c);
        estimateSignalRange(c);
        updateProperties(this, c.getMetadata());
        properties.useTheoreticalEstimates(c);
    }

    /**
     * The signal range is defined as <math>max{ <i>T(t)</i> } - min{
     * <i>T(t)</i>
     * }</math>, where <math>max{...}</math> and <math>min{...}</math> are
     * robust to outliers. This calls the {@code maxTemperature} method of
     * {@code c} and uses the baseline value at {@code 0} as the
     * <math>min{...}</math> value.
     *
     * @param c the {@code ExperimentalData} object
     * @see pulse.input.ExperimentalData.maxTemperature()
     */
    public void estimateSignalRange(ExperimentalData c) {
        var maxPoint = c.getHalfTimeCalculator().getFilteredMaximum();
        var flatBaseline = new FlatBaseline();
        flatBaseline.fitTo(c);
        final double signalSpan = maxPoint.getY() - flatBaseline.valueAt(maxPoint.getX());
        properties.setMaximumTemperature(derive(MAXTEMP, signalSpan));
    }

    /**
     * Calculates the vector argument defined on
     * <math><b>R</b><sup>n</sup></math>
     * to the scalar objective function for this {@code Problem}. To fill the
     * vector with data, only those parameters from this {@code Problem} will be
     * used which are defined by the {@code flags}, e.g. if the flag associated
     * with the {@code HEAT_LOSS} keyword is set to false, its value will be
     * skipped when creating the vector.
     * </p>
     *
     * @see listedTypes()
     */

    /*
	 * TODO put relative bounds in a constant field Consider creating a Bounds
	 * class, or putting them in the XML file
     */
    @Override
    public void optimisationVector(ParameterVector output) {

        baseline.optimisationVector(output);

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            Segment bounds = Segment.boundsFrom(key);
            double value;

            switch (key) {
                case THICKNESS:
                    value = (double) properties.getSampleThickness().getValue();
                    break;
                case DIFFUSIVITY:
                    value = (double) properties.getDiffusivity().getValue();
                    bounds = new Segment(0.01 * value, 20.0 * value);
                    break;
                case MAXTEMP:
                    final double signalHeight = (double) properties.getMaximumTemperature().getValue();
                    bounds = new Segment(0.5 * signalHeight, 1.5 * signalHeight);
                    value = signalHeight;
                    break;
                case HEAT_LOSS:
                    value = (double) properties.getHeatLoss().getValue();
                    p.setTransform(new StickTransform(bounds));
                    break;
                case TIME_SHIFT:
                    double magnitude = 0.25 * properties.characteristicTime();
                    bounds = new Segment(-magnitude, magnitude);
                    value = (double) curve.getTimeShift().getValue();
                    break;
                default:
                    continue;
            }

            p.setTransform(new StickTransform(bounds));
            p.setBounds(bounds);
            p.setValue(value);

        }

    }

    /**
     * Assigns parameter values of this {@code Problem} using the optimisation
     * vector {@code params}. Only those parameters will be updated, the types
     * of which are listed as indices in the {@code params} vector.
     *
     * @see listedTypes()
     */
    @Override
    public void assign(ParameterVector params) throws SolverException {
        baseline.assign(params);

        List<NumericProperty> malformedList = params.findMalformedElements();

        if (!malformedList.isEmpty()) {
            StringBuilder sb = new StringBuilder("Cannot assign values: ");
            malformedList.forEach(p
                    -> sb.append(String.format("%n %-25s", p.toString()))
            );
            throw new SolverException(sb.toString(), ILLEGAL_PARAMETERS);
        }

        for (Parameter p : params.getParameters()) {

            double value = p.inverseTransform();
            var key = p.getIdentifier().getKeyword();

            switch (key) {
                case THICKNESS:
                    properties.setSampleThickness(derive(THICKNESS, value));
                    break;
                case DIFFUSIVITY:
                    properties.setDiffusivity(derive(DIFFUSIVITY, value));
                    break;
                case MAXTEMP:
                    properties.setMaximumTemperature(derive(MAXTEMP, value));
                    break;
                case HEAT_LOSS:
                    properties.setHeatLoss(derive(HEAT_LOSS, value));
                    break;
                case TIME_SHIFT:
                    curve.set(TIME_SHIFT, derive(TIME_SHIFT, value));
                    break;
                default:
            }
        }

    }

    /**
     * Checks whether some 'advanced' details should stay hidden by the GUI when
     * customising the {@code Problem} statement.
     *
     * @return {@code true} if the user does not want to see the details (by
     * default), {@code false} otherwise.
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
     * {@code false} otherwise.
     */
    public final static void setDetailsHidden(boolean b) {
        Problem.hideDetailedAdjustment = b;
    }

    /**
     * Used for debugging. Initially, the nonlinear and two-dimensional problem
     * statements are disabled, since they have not yet been thoroughly tested
     *
     * @return {@code true} if this problem statement has been enabled,
     * {@code false} otherwise
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * Constructs a {@code DiscretePulse} on the specified {@code grid} using
     * the {@code Pulse} corresponding to this {@code Problem}.
     *
     * @param grid the grid
     * @return a {@code DiscretePulse} objects constructed for this
     * {@code Problem} and the {@code grid}
     */
    public DiscretePulse discretePulseOn(Grid grid) {
        return new DiscretePulse(this, grid);
    }

    /**
     * Listed parameters include:
     * <code>MAXTEMP, DIFFUSIVITY, THICKNESS, HEAT_LOSS_FRONT, HEAT_LOSS_REAR</code>.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(MAXTEMP);
        set.add(DIFFUSIVITY);
        set.add(THICKNESS);
        set.add(HEAT_LOSS);
        return set;
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(instanceDescriptor);
        return list;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public final ProblemComplexity getComplexity() {
        return complexity;
    }

    public final void setComplexity(ProblemComplexity complexity) {
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
     * {@code HeatingCurve} when done and sets the {@code parent} of the
     * baseline to this object.
     *
     * @param baseline the new baseline.
     * @see pulse.baseline.Baseline.apply(Baseline)
     */
    public final void setBaseline(Baseline baseline) {
        instanceDescriptor.setSelectedDescriptor(baseline.getClass().getSimpleName());
        this.baseline = baseline.copy();
        this.baseline.setParent(this);
        curve.apply(this.baseline);
    }

    public final InstanceDescriptor<? extends Baseline> getBaselineDescriptor() {
        return instanceDescriptor;
    }

    private void initBaseline() {
        setBaseline(instanceDescriptor.newInstance(Baseline.class));
        var searchTask = (SearchTask) this.specificAncestor(SearchTask.class);
        if (searchTask != null) {
            var experimentalData = (ExperimentalData) searchTask.getInput();
            Runnable baselineFitter = (Runnable & Serializable) () -> baseline.fitTo(experimentalData);
            Executors.newSingleThreadExecutor().submit(baselineFitter);
        }
        parameterListChanged();
    }

    public final ThermalProperties getProperties() {
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
