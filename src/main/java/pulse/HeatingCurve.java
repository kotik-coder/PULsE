package pulse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.util.stream.Collectors.toList;
import static pulse.input.listeners.CurveEventType.RESCALED;
import static pulse.input.listeners.CurveEventType.TIME_ORIGIN_CHANGED;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;

import java.util.ArrayList;
import static java.util.Collections.max;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import pulse.baseline.Baseline;
import pulse.input.ExperimentalData;
import pulse.input.listeners.CurveEvent;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.FunctionSerializer;

/**
 * The {@code HeatingCurve} represents a time-temperature profile (a
 * {@code AbstractData} instance) generated using a calculation algorithm
 * implemented by a {@code Problem}'s {@code Solver}. In addition to the time
 * and signal lists defined in the super-class, it features baseline-corrected
 * signal values stored in a separate list.The {@code HeatingCurve} may have
 * {@code HeatingCurveListener}s to process simple events. To enable comparison
 * with {@code ExperimentalData}, a {@code HeatingCurve} builds a spline
 * interpolation of its time - baseline-adjusted signal values, thus
 * representing a continuous curve, rather than just a collection of discrete
 * data.
 *
 * @see pulse.HeatingCurveListener
 * @see org.apache.commons.math3.analysis.interpolation.UnivariateInterpolation
 *
 */
public class HeatingCurve extends AbstractData {

    /**
     *
     */
    private static final long serialVersionUID = 7071147065094996971L;
    private List<Double> adjustedSignal;
    private List<Double> lastCalculation;
    private double startTime;

    private List<HeatingCurveListener> listeners;

    private transient UnivariateInterpolator interpolator;
    private transient UnivariateFunction interpolation;

    protected HeatingCurve(List<Double> time, List<Double> signal, final double startTime, String name) {
        super(time, name);
        this.adjustedSignal = signal;
        this.startTime = startTime;
        initListeners();
    }

    @Override
    public void initListeners() {
        super.initListeners();
        listeners = new ArrayList<>();
    }

    /**
     * Calls the super-constructor and initialises the baseline-corrected signal
     * list. Creates a {@code SplineInterpolator} object.
     */
    public HeatingCurve() {
        super();
        adjustedSignal = new ArrayList<>((int) this.getNumPoints().getValue());
        interpolator = new SplineInterpolator();
    }

    /**
     * Copy constructor. In addition to copying the data, also re-builds the
     * splines.
     *
     * @param c another instance of this class
     * @see refreshInterpolation()
     */
    public HeatingCurve(HeatingCurve c) {
        super(c);
        this.adjustedSignal = new ArrayList<>(c.adjustedSignal);
        this.startTime = c.startTime;
        interpolator = new SplineInterpolator();
        if (c.interpolation != null) {
            this.refreshInterpolation();
        }
    }

    /**
     * Creates a {@code HeatingCurve}, where the number of elements in the
     * {@code time}, {@code signal}, and {@code adjustedSignal} collections are
     * set to {@code count.getValue()}. The time shift is initialized with a
     * default value.
     *
     * @param count The {@code NumericProperty} that is derived from the
     * {@code NumericPropertyKeyword.NUMPOINTS}.
     */
    public HeatingCurve(NumericProperty count) {
        super(count);
        setPrefix("Solution");

        adjustedSignal = new ArrayList<>((int) count.getValue());
        startTime = (double) def(TIME_SHIFT).getValue();

        interpolator = new SplineInterpolator();
    }

    //TODO
    public void copyToLastCalculation() {
        lastCalculation = new ArrayList<>(0);
        lastCalculation = new ArrayList<>(adjustedSignal);
    }

    @Override
    public void clear() {
        super.clear();
        adjustedSignal.clear();
    }

    /**
     * Retrieves the time from the stored list of values, adding the value of
     * {@code startTime} to the result.
     *
     * @return time at {@code index} + startTime
     *
     */
    @Override
    public double timeAt(int index) {
        return super.timeAt(index) + startTime;
    }

    /**
     * Retrieves the <b>baseline-corrected</b> temperature corresponding to
     * {@code index} in the respective {@code List}.
     *
     * @param index the index of the element
     * @return a double, representing the baseline-corrected temperature at
     * {@code index}
     */
    @Override
    public double signalAt(int index) {
        return adjustedSignal.get(index);
    }

    /**
     * Scales the temperature values by a factor of {@code scale}.
     * <p>
     * This is done by manually setting each temperature value to
     * {@code T*scale}, where T is the current temperature value at this index.
     * Finally. applies the baseline to the scaled temperature values.
     * </p>
     * This method is used in the DifferenceScheme subclasses when a
     * dimensionless solution needs to be re-scaled to the given maximum
     * temperature (usually matching the {@code ExperimentalData}, but also used
     * as a search variable by the {@code SearchTask}.
     * <p>
     * Triggers a {@code RESCALED} {@code CurveEvent}.
     * </p>
     *
     * @param scale the scale
     * @see pulse.problem.schemes.DifferenceScheme
     * @see pulse.problem.statements.Problem
     * @see pulse.tasks.SearchTask
     * @see pulse.input.listeners.CurveEvent
     */
    public void scale(double scale) {
        final int count = this.actualNumPoints();
        for (int i = 0, max = Math.min(count, signal.size()); i < max; i++) {
            signal.set(i, signal.get(i) * scale);
        }
        var dataEvent = new CurveEvent(RESCALED);
        fireCurveEvent(dataEvent);
    }

    private void refreshInterpolation() {

        /*
	 * Prepare extended time array
         */
        var timeExtended = new double[time.size() + 1];

        for (int i = 1; i < timeExtended.length; i++) {
            timeExtended[i] = timeAt(i - 1);
        }

        final double dt = timeExtended[2] - timeExtended[1];
        timeExtended[0] = timeExtended[1] - dt; // extrapolate linearly

        /*
		 * Prepare extended signal array
         */
        var adjustedSignalExtended = new double[adjustedSignal.size() + 1];

        for (int i = 1; i < timeExtended.length; i++) {
            adjustedSignalExtended[i] = signalAt(i - 1);
        }

        final double alpha = -1.0;
        adjustedSignalExtended[0] = alpha * adjustedSignalExtended[2]
                - (1.0 - alpha) * adjustedSignalExtended[1]; // extrapolate
        // linearly

        /*
	 * Submit to spline interpolation
         */
        interpolation = interpolator.interpolate(timeExtended, adjustedSignalExtended);
    }

    /**
     * Retrieves the simple maximum (in arbitrary units) of the
     * <b>baseline-corrected</b> temperature list.
     *
     * @return the simple maximum of the baseline-adjusted temperature.
     */
    public double maxAdjustedSignal() {
        return max(adjustedSignal);
    }

    /**
     * Adds the baseline value to each element of the {@code signal} list.
     * <p>
     * The {@code baseline.valueAt} method is explicitly invoked for all
     * {@code time} values, and the result of adding the baseline value to the
     * corresponding {@code signal} is assigned to a position in the
     * {@code adjustedSignal} list.
     * </p>
     *
     * @param baseline the baseline. Note it may not specifically belong to this
     * heating curve.
     */
    public void apply(Baseline baseline) {
        adjustedSignal.clear();
        int size = time.size();

        if (size > 0) {

            for (int i = 0; i < size; i++) {
                adjustedSignal.add(signal.get(i) + baseline.valueAt(timeAt(i)));
            }

            if (time.get(0) > -startTime) {
                time.add(0, -startTime);
                adjustedSignal.add(0, baseline.valueAt(-startTime));
            }

            refreshInterpolation();

        }

    }

    /**
     * This creates a new {@code HeatingCurve} to match the time boundaries of
     * the {@code data}.
     * <p>
     * Curves derived in this way are called <i>extended</i> and are used
     * primarily to visually inspect how the calculated baseline correlates with
     * the {@code data} at times {@code t < 0}. This method is not used in any
     * calculation and is introduced primarily because the search for the
     * reverse solution of the heat problems only regards time value at
     * <math><mi>t</mi><mo>&#x2265;</mo><mn>0</mn></math>, whereas in reality it
     * may not be consistent with the experimental baseline value at
     * {@code t < 0}.
     * </p>
     *
     * @param data the experimental data, with a time range broader than the
     * time range of this {@code HeatingCurve}.
     * @param baseline
     * @return a new {@code HeatingCurve}, extended to match the time limits of
     * {@code data}
     */
    public final HeatingCurve extendedTo(ExperimentalData data, Baseline baseline) {

        int dataStartIndex = data.getIndexRange().getLowerBound();

        if (dataStartIndex < 1) // no extension required
        {
            return this;
        }

        var baselineTime = data.getTimeSequence().stream().filter(t -> t < 0).collect(toList());
        var baselineSignal = baselineTime.stream().map(bTime -> baseline.valueAt(bTime)).collect(toList());

        baselineTime.addAll(time);
        this.copyToLastCalculation();
        baselineSignal.addAll(lastCalculation);

        return new HeatingCurve(baselineTime, baselineSignal, startTime, getName());
    }

    /**
     * Calls {@code super.set} and provides write access to the
     * {@code TIME_SHIFT} property.
     *
     * @param property the property of the type
     * {@code NumericPropertyKeyword.NUMPOINTS}
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        if (type == TIME_SHIFT) {
            setTimeShift(property);
        }
    }

    /**
     * @return {@code TIME_SHIFT} and {@code NUM_POINTS}.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(TIME_SHIFT);
        return set;
    }

    /**
     * Removes an element with the index {@code i} from all three {@code List}s
     * (time, signal, and baseline-corrected signal).
     *
     * @param i the element to be removed
     */
    @Override
    public void remove(int i) {
        super.remove(i);
        this.adjustedSignal.remove(i);
    }

    /**
     * The time shift is the position of the 'zero-time'.
     *
     * @return a {@code TIME_SHIFT} property
     */
    public NumericProperty getTimeShift() {
        return derive(TIME_SHIFT, startTime);
    }

    /**
     * Sets the time shift and triggers {@code TIME_ORIGIN_CHANGED} in
     * {@code CurveEvent}. Triggers the {@code firePropertyChanged}.
     *
     * @param startTime the new start time value
     */
    public void setTimeShift(NumericProperty startTime) {
        requireType(startTime, TIME_SHIFT);
        this.startTime = (double) startTime.getValue();
        var dataEvent = new CurveEvent(TIME_ORIGIN_CHANGED);
        fireCurveEvent(dataEvent);
        firePropertyChanged(this, startTime);
    }

    public UnivariateFunction getInterpolation() {
        return interpolation;
    }

    public List<Double> getBaselineCorrectedData() {
        return adjustedSignal;
    }

    public void addHeatingCurveListener(HeatingCurveListener l) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        this.listeners.add(l);
    }

    @Override
    public void removeListeners() {
        listeners.clear();
    }

    private void fireCurveEvent(CurveEvent event) {
        for (HeatingCurveListener l : listeners) {
            l.onCurveEvent(event);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HeatingCurve)) {
            return false;
        }

        return super.equals(o) && adjustedSignal.containsAll(((HeatingCurve) o).adjustedSignal);
    }

    public double interpolateSignalAt(double x) {
        double min = this.timeAt(0);
        double max = timeLimit();
        return min < x && max > x ? interpolation.value(x)
                : (x < min ? signalAt(0) : signalAt(actualNumPoints() - 1));
    }

    /*
    * Serialization
     */
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        // default serialization 
        oos.defaultWriteObject();
        // write the object
        FunctionSerializer.writeSplineFunction((PolynomialSplineFunction) interpolation, oos);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();
        this.interpolation = FunctionSerializer.readSplineFunction(ois);
        this.interpolator = new SplineInterpolator();
    }

}
