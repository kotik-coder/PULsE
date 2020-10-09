package pulse;

import static java.util.Collections.max;
import static java.util.stream.Collectors.toList;
import static pulse.input.listeners.CurveEventType.RESCALED;
import static pulse.input.listeners.CurveEventType.TIME_ORIGIN_CHANGED;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import pulse.baseline.Baseline;
import pulse.input.ExperimentalData;
import pulse.input.listeners.CurveEvent;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * The {@code HeatingCurve} represents a time-temperature profile (a {@code AbstractData} instance) 
 * generated using a finite-difference calculation algorithm.
 *
 */

public class HeatingCurve extends AbstractData {

	private List<Double> adjustedSignal;
	private double startTime;

	private List<HeatingCurveListener> listeners = new ArrayList<HeatingCurveListener>();

	private UnivariateInterpolator splineInterpolator;
	private UnivariateFunction splineInterpolation;

	protected HeatingCurve(List<Double> time, List<Double> signal, final double startTime, String name) {
		super(time, name);
		this.adjustedSignal = signal;
		this.startTime = startTime;
	}
	
	public HeatingCurve() {
		super();
		adjustedSignal = new ArrayList<Double>((int)this.getNumPoints().getValue());
		splineInterpolator = new SplineInterpolator();
	}
	
	public HeatingCurve(HeatingCurve c) {
		super(c);
		this.adjustedSignal = new ArrayList<>(c.adjustedSignal);
		this.startTime = c.startTime;
		splineInterpolator = new SplineInterpolator();
	}

	/**
	 * Creates a {@code HeatingCurve}, where the number of elements in the
	 * {@code time} and {@code temperature} collections are set to
	 * {@code count.getValue()}.
	 * 
	 * @param count The {@code NumericProperty} that is derived from the
	 *              {@code NumericPropertyKeyword.NUMPOINTS}.
	 */

	public HeatingCurve(NumericProperty count) {
		super(count);
		setPrefix("Solution");

		adjustedSignal = new ArrayList<>((int)count.getValue());
		startTime = (double) def(TIME_SHIFT).getValue();

		splineInterpolator = new SplineInterpolator();
	}

	@Override
	public void clear() {
		super.clear();
		this.adjustedSignal.clear();
	}

	/**
	 * Retrieves the time from the stored list of values, adding the value of {@code startTime} to the result
	 * 
	 */

	@Override
	public double timeAt(int index) {
		return super.timeAt(index) + startTime;
	}

	/**
	 * Retrieves the <b>baseline-subtracted</b> temperature corresponding to
	 * {@code index} in the respective {@code List}.
	 * 
	 * @param index the index of the element
	 * @return a double, respresenting the baseline-subtracted temperature at
	 *         {@code index}
	 */

	public double signalAt(int index) {
		return adjustedSignal.get(index);
	}

	/**
	 * Scales the temperature values by a factor of {@code scale}.
	 * <p>
	 * This is done by manually setting each temperature value to {@code T*scale},
	 * where T is the current temperature value at this index. Finally. applies the
	 * baseline to the scaled temperature values.
	 * </p>
	 * This method is used in the DifferenceScheme classes when a dimensionless
	 * solution needs to be re-scaled to the given maximum temperature (usually
	 * matching the {@code ExperimentalData}, but also used as a search variable by
	 * the {@code SearchTask}.
	 * 
	 * @param scale the scale
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @see pulse.problem.statements.Problem
	 * @see pulse.tasks.SearchTask
	 */

	public void scale(double scale) {
		var signal = getSignalData();
		final int count = this.actualNumPoints();
		for (int i = 0; i < count; i++)
			signal.set(i, signal.get(i) * scale);
		var dataEvent = new CurveEvent(RESCALED, this);
		fireCurveEvent(dataEvent);
	}

	private void refreshInterpolation() {

		/*
		 * Prepare extended time array
		 */

		var time = this.getTimeSequence();
		var timeExtended = new double[time.size() + 1];

		for (int i = 1; i < timeExtended.length; i++)
			timeExtended[i] = timeAt(i - 1);

		final double dt = timeExtended[2] - timeExtended[1];
		timeExtended[0] = timeExtended[1] - dt; // extrapolate linearly

		/*
		 * Prepare extended signal array
		 */

		var adjustedSignalExtended = new double[adjustedSignal.size() + 1];

		for (int i = 1; i < timeExtended.length; i++)
			adjustedSignalExtended[i] = signalAt(i - 1);

		final double alpha = -1.0;
		adjustedSignalExtended[0] = alpha * adjustedSignalExtended[2] - (1.0 - alpha) * adjustedSignalExtended[1]; // extrapolate
																													// linearly

		/*
		 * Submit to spline interpolation
		 */

		splineInterpolation = splineInterpolator.interpolate(timeExtended, adjustedSignalExtended);
	}

	/**
	 * Retrieves the absolute maximum (in arbitrary untis) of the
	 * <b>baseline-subtracted</b> temperature list.
	 * 
	 * @return the absolute maximum of the baseline-adjusted temperature.
	 */

	public double maxAdjustedSignal() {
		return max(adjustedSignal);
	}

	/**
	 * Subtracts the baseline values from each element of the {@code temperature}
	 * list.
	 * <p>
	 * The baseline.valueAt(...) is explicitly invoked for all {@code time} values,
	 * and the result of subtracting the baseline value from the corresponding
	 * {@code temperature} is assigned to a position in the
	 * {@code baselineAdjustedTemperature} list.
	 * </p>
	 * 
	 * @param baseline the baseline. Note it may not specifically belong to this
	 *                 heating curve.
	 */

	public void apply(Baseline baseline) {
		var time = this.getTimeSequence();
		var signal = this.getSignalData();
		adjustedSignal.clear();
		for (int i = 0, size = time.size(); i < size; i++) 
			adjustedSignal.add(signal.get(i) + baseline.valueAt(timeAt(i)));

		if (time.get(0) > -startTime) {
			time.add(0, -startTime);
			adjustedSignal.add(0, baseline.valueAt(-startTime));
		}

		refreshInterpolation();
	}

	/**
	 * This creates a new {@code HeatingCurve} to match the time boundaries of the
	 * {@code data}.
	 * <p>
	 * Curves derived in this way are called <i>extended</i> and are used primarily
	 * to visually inspect how the calculated baseline correlates with the
	 * {@code data} at times {@code t < 0}. This method is not used in any
	 * calculation and is introduced primarily because the search for the reverse
	 * solution of the heat problems only regards time value at
	 * <math><mi>t</mi><mo>&#x2265;</mo><mn>0</mn></math>, whereas in reality it may
	 * not be consistent with the experimental baseline value at {@code t < 0}.
	 * </p>
	 * 
	 * @param data the experimental data, with a time range broader than the time
	 *             range of this {@code HeatingCurve}.
	 * @return a new {@code HeatingCurve}, extended to match the time limits of
	 *         {@code data}
	 */

	public final HeatingCurve extendedTo(ExperimentalData data, Baseline baseline) {

		int dataStartIndex = data.getIndexRange().getLowerBound();

		if (dataStartIndex < 1) // no extension required
			return this;

		var baselineTime = data.getTimeSequence().stream().filter(t -> t < 0).collect(toList());
		var baselineSignal = baselineTime.stream().map(bTime -> baseline.valueAt(bTime)).collect(toList());

		var time = this.getTimeSequence();
		
		baselineTime.addAll(time);
		baselineSignal.addAll(adjustedSignal);

		return new HeatingCurve(baselineTime, baselineSignal, startTime, getName());
	}

	/**
	 * Provides general setter accessibility for the number of points of this
	 * {@code HeatingCurve}.
	 * 
	 * @param type     must be equal to {@code NumericPropertyKeyword.NUMPOINTS}
	 * @param property the property of the type
	 *                 {@code NumericPropertyKeyword.NUMPOINTS}
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		if (type == TIME_SHIFT)
			setTimeShift(property);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(TIME_SHIFT));
		return list;
	}

	/**
	 * Removes an element with the index {@code i} from all three {@code List}s
	 * (time, temperature, and baseline-subtracted temperature).
	 * 
	 * @param i the element to be removed
	 */

	public void remove(int i) {
		super.remove(i);
		this.adjustedSignal.remove(i);
	}

	public NumericProperty getTimeShift() {
		return derive(TIME_SHIFT, startTime);
	}

	public void setTimeShift(NumericProperty startTime) {
		requireType(startTime, TIME_SHIFT);
		this.startTime = (double) startTime.getValue();
		var dataEvent = new CurveEvent(TIME_ORIGIN_CHANGED, this);
		fireCurveEvent(dataEvent);
		firePropertyChanged(this, startTime);
	}

	public UnivariateFunction getSplineInterpolation() {
		return splineInterpolation;
	}

	public List<Double> getAlteredSignalData() {
		return adjustedSignal;
	}

	public void addHeatingCurveListener(HeatingCurveListener l) {
		this.listeners.add(l);
	}

	public void removeHeatingCurveListeners() {
		listeners.clear();
	}

	private void fireCurveEvent(CurveEvent event) {
		for (HeatingCurveListener l : listeners)
			l.onCurveEvent(event);
	}

	@Override
	public boolean equals(Object o) {
		if(! (o instanceof HeatingCurve ))
			return false;
		
		return super.equals(o) && adjustedSignal.containsAll( ((HeatingCurve)o).adjustedSignal);
	}

}