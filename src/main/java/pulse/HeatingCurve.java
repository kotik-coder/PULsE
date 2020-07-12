package pulse;

import static java.lang.Math.abs;
import static java.util.Collections.max;
import static java.util.Collections.min;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;
import static pulse.input.listeners.DataEventType.CHANGE_OF_ORIGIN;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;
import static pulse.properties.NumericPropertyKeyword.TIME_SHIFT;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import pulse.input.ExperimentalData;
import pulse.input.listeners.DataEvent;
import pulse.input.listeners.DataListener;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/**
 * The {@code HeatingCurve} represents a time-temperature profile either
 * resulting from a finite-difference calculation or measured directly in the
 * experiment (and then it is called {@code ExperimentalData}).
 * <p>
 * The notion of temperature is loosely used here, and this can represent just
 * the pyrometer signal in mV. Unless a nonlinear problem statement is used, the
 * unit of the temperature can be arbitrary, and only the shape of the heating
 * curve matters when calculating the reverse solution of the heat problem.
 * </p>
 *
 */

public class HeatingCurve extends PropertyHolder {

	protected int count;
	
	protected List<Double> time;
	protected List<Double> signal;
	protected List<Double> adjustedSignal;
	
	protected Baseline baseline;
	private double startTime;
	private String name;
	
	private final static int DEFAULT_CLASSIC_PRECISION = 200;

	private List<DataListener> dataListeners;
	
	private UnivariateInterpolator	splineInterpolator;
	private UnivariateFunction		splineInterpolation;

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof HeatingCurve))
			return false;

		var other = (HeatingCurve) o;

		final double EPS = 1e-8;

		if (abs(count - (Integer) other.getNumPoints().getValue()) > EPS)
			return false;

		if (!getTimeShift().equals(other.getTimeShift()))
			return false;

		if (signal.hashCode() != other.signal.hashCode())
			return false;

		if (time.hashCode() != other.time.hashCode())
			return false;

		if (!time.containsAll(other.time))
			return false;

		if (!signal.containsAll(other.signal))
			return false;

		return adjustedSignal.containsAll(other.adjustedSignal);

	}

	/**
	 * Creates a {@code HeatingCurve} with the default number of points (set in the
	 * corresponding XML file).
	 */

	public HeatingCurve() {
		this(def(NUMPOINTS));
	}

	/**
	 * Creates a {@code HeatingCurve}, where the number of elements in the
	 * {@code time} and {@code temperature} collections are set to
	 * {@code count.getValue()}, and then calls {@code reinit()}.
	 * <p>
	 * Creates a new default {@code Baseline} and sets its parent to {@code this}.
	 * 
	 * @param count The {@code NumericProperty} that is derived from the
	 *              {@code NumericPropertyKeyword.NUMPOINTS}.
	 * @see reinit
	 */

	public HeatingCurve(NumericProperty count) {
		setPrefix("Solution");
		setNumPoints(count);
		
		time = new ArrayList<>(this.count);
		signal = new ArrayList<>(this.count);
		adjustedSignal = new ArrayList<>(this.count);
		
		baseline = new Baseline();
		baseline.setParent(this);
		
		startTime = (double) def(TIME_SHIFT).getValue();
		
		dataListeners = new ArrayList<>();
		
		reinit();
		splineInterpolator = new SplineInterpolator();
	}

	public int actualDataPoints() {
		return signal.size();
	}

	public void addDataListener(DataListener listener) {
		dataListeners.add(listener);
	}

	public void clearDataListener() {
		dataListeners.clear();
	}

	public void fireDataChanged(DataEvent dataEvent) {
		dataListeners.stream().forEach(l -> l.onDataChanged(dataEvent));
	}

	/**
	 * Clears all elements from the three {@code List} objects, thus releasing
	 * memory.
	 */

	public void clear() {
		this.time.clear();
		this.signal.clear();
		this.adjustedSignal.clear();
	}

	/**
	 * Calls {@code clear()}, and add the first element (0.0, 0.0).
	 * 
	 * @see getNumPoints()
	 * @see clear()
	 */

	public void reinit() {
		clear();
		addPoint(0.0, 0.0);
	}

	/**
	 * Returns the size of the {@code List} object containing baseline-adjusted
	 * temperature values, used later in calculations and optimisation procedures.
	 * 
	 * @return the size of the {@code baselineAdjustTemperature}
	 */

	public int adjustedSize() {
		return adjustedSignal.size();
	}

	/**
	 * Getter method providing accessibility to the {@code count NumericProperty}.
	 * 
	 * @return a {@code NumericProperty} derived from
	 *         {@code NumericPropertyKeyword.NUMPOINTS} with the value of
	 *         {@code count}
	 */

	public NumericProperty getNumPoints() {
		return derive(NUMPOINTS, count);
	}

	/**
	 * Return the {@code Baseline} of this {@code HeatingCurve}.
	 * 
	 * @return the baseline
	 */

	public Baseline getBaseline() {
		return baseline;
	}

	/**
	 * Sets a new baseline. Calls {@code apply(baseline)} when done and sets the
	 * {@code parent} of the baseline to this object.
	 * 
	 * @param baseline the new baseline.
	 */

	public void setBaseline(Baseline baseline) {
		this.baseline = baseline;
		apply(baseline);
		baseline.setParent(this);
	}

	/**
	 * Sets the number of points for this baseline.
	 * <p>
	 * The {@code List} data objects, containing time, temperature, and
	 * baseline-subtracted temperature are filled with zeroes.
	 * 
	 * @param c
	 */

	public void setNumPoints(NumericProperty c) {
		requireType(c, NUMPOINTS);
		this.count = (int) c.getValue();
		
		signal = new ArrayList<>(nCopies(this.count, 0.0));
		adjustedSignal = new ArrayList<>(nCopies(this.count, 0.0));
		time = new ArrayList<>(nCopies(this.count, 0.0));
	}

	/**
	 * Retrieves an element from the {@code time List} specified by {@code index}
	 * 
	 * @param index the index of the element to be returned
	 * @return a time value corresponding to {@code index}
	 */

	public double timeAt(int index) {
		return time.get(index) + startTime;
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
	 * Attempts to set the time {@code t} and temperature {@code T} values
	 * corresponding to index {@code i}.
	 * <p>
	 * A baseline-subtracted version of the temperature at the same index will be
	 * calculated using the current baseline.
	 * 
	 * @param i the index to be used for setting both {@code t} and {@code T}
	 * @param t the time double value
	 * @param T the temperature double value
	 */

	public void set(int i, double t, double T) {
		time.set(i, t);
		signal.set(i, T);
		adjustedSignal.set(i, T + baseline.valueAt(i));
	}

	public void addPoint(double time, double temperature) {
		this.time.add(time);
		this.signal.add(temperature);
	}

	/**
	 * Sets the time {@code t} at the position {@code index} of the
	 * {@code time List}.
	 * 
	 * @param index the index
	 * @param t     the new time value at this index
	 */

	public void setTimeAt(int index, double t) {
		time.set(index, t);
	}

	/**
	 * Sets the temperature {@code t} at the position {@code index} of the
	 * {@code temperature List}.
	 * 
	 * @param index the index
	 * @param t     the new temperature value at this index
	 */

	public void setSignalAt(int index, double t) {
		signal.set(index, t);
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
		for (int i = 0; i < count; i++) 
			signal.set(i, signal.get(i) * scale);
		
		apply(baseline);
		refreshInterpolation();
	}
	
	private void refreshInterpolation() {
		
		/*
		 * Prepare extended time array
		 */
		
		var timeExtended = new double[time.size() + 1];
		
		for(int i = 1; i < timeExtended.length; i++)
			timeExtended[i] = timeAt(i - 1);
		
		final double dt = timeExtended[2] - timeExtended[1];
		timeExtended[0] = timeExtended[1] - dt;	//extrapolate linearly
		
		/*
		 * Prepare extended signal array
		 */
		
		var adjustedSignalExtended = new double[adjustedSignal.size() + 1];
		
		for(int i = 1; i < timeExtended.length; i++)
			adjustedSignalExtended[i] = signalAt(i - 1);
	
		final double alpha = -1.0;
		adjustedSignalExtended[0] = alpha*adjustedSignalExtended[2] - (1.0 - alpha)*adjustedSignalExtended[1]; //extrapolate linearly
		
		/*
		 * Submit to spline interpolation
		 */
		
		splineInterpolation = splineInterpolator.interpolate( timeExtended, 
															  adjustedSignalExtended );
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

	public double apparentMaximum() {
		return max(signal);
	}

	/**
	 * Retrieves the last element of the {@code time List}. This is used e.g. by the
	 * {@code DifferenceScheme} to set the calculation limit for the
	 * finite-difference scheme.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @return a double, equal to the last element of the {@code time List}.
	 */

	public double timeLimit() {
		return time.get(time.size() - 1);
	}
	
	@Override
	public String toString() {
		return name != null ? name : getClass().getSimpleName() + " (" + getNumPoints() + ")";
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

		for (int i = 0, size = time.size(); i < size; i++) 
			adjustedSignal.add(
					signal.get(i) + baseline.valueAt( time.get(i) + startTime ) );

		if( min(time) > 0) {
			time.add(0, 0.0);
			adjustedSignal.add(0, baseline.valueAt(0.0) );
		}
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public final HeatingCurve extendedTo(ExperimentalData data) {

		int dataStartIndex = data.getIndexRange().getLowerBound();

		if (dataStartIndex < 1) //no extension required
			return this;

		var baselineTime	= data.time.stream().filter(t -> t < 0).collect(toList());
		var baselineSignal	= baselineTime.stream().map(bTime -> baseline.valueAt(bTime) ).collect(toList());

		baselineTime.addAll(time);
		baselineSignal.addAll(adjustedSignal);
		
		var newCurve = new HeatingCurve();

		newCurve.time = baselineTime;
		newCurve.adjustedSignal = baselineSignal;
		newCurve.count = newCurve.adjustedSignal.size();
		newCurve.baseline = baseline;
		newCurve.name = name;
		newCurve.startTime = startTime;

		return newCurve;

	}

	/**
	 * A static factory method for calculating a heating curve based on the
	 * analytical solution of Parker et al.
	 * <p>
	 * The math itself is done separately in the {@code Problem} class. This method
	 * creates a {@code HeatingCurve} with the number of points equal to that of the
	 * {@code p.getHeatingCurve()}, and with the same baseline. The solution is
	 * calculated for the time range {@code 0 <= t <= timeLimit}.
	 * </p>
	 * 
	 * @param p         The problem statement, providing access to the
	 *                  {@code classicSolutionAt} method and to the
	 *                  {@code HeatingCurve} object it owns.
	 * @param timeLimit The upper time limit (in seconds)
	 * @param precision The second argument passed to the {@code classicSolutionAt}
	 * @return a {@code HeatingCurve} representing the analytical solution.
	 * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i> Journal
	 *      of Applied Physics <b>32</b> (1961) 1679</a>
	 * @see Problem.classicSolutionAt(double,int)
	 */

	public static HeatingCurve classicSolution(Problem p, double timeLimit, int precision) {
		var hc = p.getHeatingCurve();
		var classicCurve = new HeatingCurve(derive(NUMPOINTS, hc.count));

		double time, step;

		if (hc.time.size() < hc.count)
			step = timeLimit / (hc.count - 1.0);
		else
			step = hc.timeAt(1) - hc.timeAt(0);

		for (int i = 0; i < hc.count; i++) {
			time = i * step;
			classicCurve.addPoint(time, p.classicSolutionAt(time, precision));
			classicCurve.adjustedSignal.add(classicCurve.signal.get(i) + hc.baseline.valueAt(time));
		}

		classicCurve.setName("Classic solution");

		return classicCurve;

	}

	/**
	 * Calculates the classic solution, using the default value of the
	 * {@code precision} and the time limit specified by the {@code HeatingCurve} of
	 * {@code p}.
	 * 
	 * @param p the problem statement
	 * @return a {@code HeatinCurve}, representing the classic solution.
	 * @see classicSolution
	 */

	public static HeatingCurve classicSolution(Problem p) {
		return classicSolution(p, p.getHeatingCurve().timeLimit(), DEFAULT_CLASSIC_PRECISION);
	}

	public static HeatingCurve classicSolution(Problem p, double timeLimit) {
		return classicSolution(p, timeLimit, DEFAULT_CLASSIC_PRECISION);
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
		switch (type) {
		case NUMPOINTS:
			setNumPoints(property);
			break;
		case TIME_SHIFT:
			setTimeShift(property);
			break;
		default:
			break;
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(getNumPoints());
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
		this.time.remove(i);
		this.signal.remove(i);
		this.adjustedSignal.remove(i);
	}

	public NumericProperty getTimeShift() {
		return derive(TIME_SHIFT, startTime);
	}

	public void setTimeShift(NumericProperty startTime) {
		if (startTime.getType() != TIME_SHIFT)
			throw new IllegalArgumentException("Illegal type: " + startTime.getType());
		this.startTime = (double) startTime.getValue();
		var dataEvent = new DataEvent(CHANGE_OF_ORIGIN, this);
		fireDataChanged(dataEvent);
	}

	@Override
	public boolean ignoreSiblings() {
		return true;
	}

	public UnivariateFunction getSplineInterpolation() {
		return splineInterpolation;
	}
	
	public List<Double> getTimeSequence() {
		return time;
	}

}