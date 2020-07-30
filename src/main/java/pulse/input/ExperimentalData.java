package pulse.input;

import static java.lang.Double.valueOf;
import static java.util.Collections.max;
import static pulse.input.listeners.DataEventType.TRUNCATED;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import pulse.HeatingCurve;
import pulse.baseline.FlatBaseline;
import pulse.input.listeners.DataEvent;
import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.util.PropertyHolderListener;

/**
 * <p>
 * An {@code ExperimentalData} object is essentially a {@code HeatingCurve} with
 * adjustable range and linked {@code Metadata}. It is used to store
 * experimental data points loaded with one of the available
 * {@code CurveReader}s. Any manipulation (e.g. truncation) of the data triggers
 * an event associated with this {@code ExperimentalData}.
 */

public class ExperimentalData extends HeatingCurve {

	private Metadata metadata;
	private IndexRange indexRange;
	private Range range;

	/**
	 * This is the cutoff factor which is used as a criterion for data truncation.
	 * Described in Lunev, A., &amp; Heymer, R. (2020). Review of Scientific Instruments, 91(6), 064902.
	 */
	
	public final static double CUTOFF_FACTOR = 7.2;
	
	/**
	 * The binning factor used to build a crude approximation of the heating curve. 
	 * Described in Lunev, A., &amp; Heymer, R. (2020). Review of Scientific Instruments, 91(6), 064902.
	 */
	
	public final static int	REDUCTION_FACTOR = 32;
	
	/**
	 * A fail-safe factor.
	 */
	
	public final static double FAIL_SAFE_FACTOR = 3.0;

	private static Comparator<Point2D> pointComparator = (p1, p2) -> valueOf(p1.getY())
			.compareTo(valueOf(p2.getY()));

	/**
	 * Constructs an {@code ExperimentalData} object using the superclass constructor
	 * and rejecting the responsibility for the {@code baseline}, making its parent
	 * {@code null}. The number of points is set to zero by default.
	 * 
	 * @see HeatingCurve
	 */

	public ExperimentalData() {
		super();
		setPrefix("RawData");
		setNumPoints(derive(NUMPOINTS, 0));
		indexRange = new IndexRange();
	}

	/**
	 * Calls reset for both the {@code IndexRange} and {@code Range} objects using the current time sequence.
	 * @see pulse.input.Range.reset()
	 * @see pulse.input.IndexRange.reset()
	 */
	
	public void resetRanges() {
		indexRange.reset( getTimeSequence() );
		range.reset(indexRange, getTimeSequence() );
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("Experimental data ");
		if (metadata.getSampleName() != null)
			sb.append("for " + metadata.getSampleName() + " ");
		sb.append("(" + metadata.numericProperty(TEST_TEMPERATURE).formattedValueAndError(false) + ")");
		return sb.toString();
	}

	/**
	 * Adds {@code time} and {@code temperature} to the respective {@code List}s.
	 * <p>
	 * Note that the {@code baselineAdjustedTemperature} will be the same as the
	 * corresponding {@code temperature}, i.e. no baseline subtraction is performed.
	 * Upon completion, the {@code count} variable will be incremented.
	 * </p>
	 * 
	 * @param time the next time value
	 * @param signal the next signal value
	 */

	@Override
	public void addPoint(double time, double signal) {
		super.addPoint(time, signal);
		getAlteredSignalData().add(signal);
		incrementCount();
	}

	/**
	 * Constructs a deliberately crude representation of this heating curve by calculating a running average.
	 * <p>
	 * This is done using a binning algorithm, which will group the time-temperature
	 * data associated with this {@code ExperimentalData} in
	 * {@code count/reductionFactor - 1} bins, calculate the average value for time
	 * and temperature within each bin, and collect those values in a
	 * {@code List<Point2D>}. This is useful to cancel out the effect of signal
	 * outliers, e.g. when calculating the half-rise time.
	 * </p>
	 * 
	 * The algorithm is described in more detail in Lunev, A., &amp; Heymer, R. (2020). 
	 * Review of Scientific Instruments, 91(6), 064902.
	 * 
	 * @param reductionFactor the factor, by which the number of points
	 *                        {@code count} will be reduced for this
	 *                        {@code ExperimentalData}.
	 * @return a {@code List<Point2D>}, representing the degraded
	 *         {@code ExperimentalData}.
	 * @see halfRiseTime()
	 * @see pulse.HeatingCurve.maxTemperature()
	 */

	public List<Point2D> runningAverage(int reductionFactor) {

		int count = (int)getNumPoints().getValue();
		
		List<Point2D> crudeAverage = new ArrayList<>( count / reductionFactor);

		int start = indexRange.getLowerBound();
		int end = indexRange.getUpperBound();

		int step = (end - start) / (count / reductionFactor);
		double av = 0;

		int i1, i2;

		for (int i = 0, max = (count / reductionFactor) - 1; i < max; i++) {
			i1 = start + step * i;
			i2 = i1 + step;
			
			av = 0;

			for (int j = i1; j < i2; j++) 
				av += signalAt(j);
			
			av /= step;

			crudeAverage.add(new Point2D.Double( timeAt((i1 + i2) / 2), av ));

		}

		return crudeAverage;

	}

	/**
	 * Instead of returning the absolute maximum (which can be an outlier!) of the
	 * temperature, this overriden method calculates the (absolute) maximum of the {@code runningAverage} 
	 * using the default reduction factor {@value REDUCTION_FACTOR}.
	 * 
	 * @see pulse.problem.statements.Problem.estimateSignalRange(ExperimentalData)
	 */

	@Override
	public double maxAdjustedSignal() {
		var degraded = runningAverage(REDUCTION_FACTOR);
		return (max(degraded, pointComparator)).getY();
	}

	/**
	 * Calculates the approximate half-rise time used for crude estimation of
	 * thermal diffusivity.
	 * <p>
	 * This uses the {@code runningAverage} method by applying the default reduction
	 * factor of {@value REDUCTION_FACTOR}. The calculation is based on finding the
	 * approximate value corresponding to the half-maximum of the temperature. The
	 * latter is calculated using the running average curve. The index
	 * corresponding to the closest temperature value available for that curve
	 *  is used to retrieve the half-rise time (which also has
	 * the same index). If this fails, i.e. the associated index is less than 1,
	 * this will print out a warning message and still return a value equal to 
	 * the acquistion time divided by a fail-safe factor {@value FAIL_SAFE_FACTOR}.
	 * </p>
	 * 
	 * @return A double, representing the half-rise time (in seconds).
	 */

	public double halfRiseTime() {
		var degraded = runningAverage(REDUCTION_FACTOR);
		double max = (max(degraded, pointComparator)).getY();
		var baseline = new FlatBaseline();
		baseline.fitTo(this);

		double halfMax = (max + baseline.valueAt(0)) / 2.0;

		int index = IndexRange.closestLeft(halfMax, 
				degraded.stream().map(point -> point.getY()).collect(Collectors.toList()) 
				); 

		if (index < 1) {
			System.err.println(Messages.getString("ExperimentalData.HalfRiseError"));
			return max( getTimeSequence() ) / FAIL_SAFE_FACTOR;
		}

		return degraded.get(index).getX();

	}

	/**
	 * Retrieves the {@code Metadata} object for this {@code ExperimentalData}.
	 * 
	 * @return the linked {@code Metadata}
	 */

	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExperimentalData))
			return false;

		var other = (ExperimentalData) o;

		if (!this.metadata.equals(other.getMetadata()))
			return false;

		return super.equals(o);

	}

	/**
	 * Checks if the acquisition time used to collect this {@code ExperimentalData}
	 * is sensible.
	 * <p>
	 * The acquisition time is essentially the last element in the
	 * {@code time List}. By default, it is deemed sensible if that last element is
	 * less than {@value CUTOFF_FACTOR}*{@code halfRiseTime}.
	 * </p>
	 * 
	 * @return {@code true} if the acquisition time is below the truncation
	 *         threshold, {@code false} otherwise.
	 */

	public boolean isAcquisitionTimeSensible() {
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR * halfMaximum;
		int count = (int)getNumPoints().getValue();
		return getTimeSequence().get(count - 1) < cutoff;
	}

	/**
	 * Truncates the {@code range} and {@code indexRange} of this {@code ExperimentalData} above a certain threshold,
	 * NOT removing any data elements.
	 * <p>
	 * The threshold is calculated based on the {@code halfRiseTime} value and is
	 * set by default to {@value CUTOFF_FACTOR}*{@code halfRiseTime}. A
	 * {@code DataEvent} will be created and passed to the {@code dataListeners} (if
	 * any) with the {@code DataEventType.TRUNCATED} as argument.
	 * </p>
	 * 
	 * @see halfRiseTime
	 * @see DataEvent
	 * @see fireDataChanged
	 */

	public void truncate() {
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR * halfMaximum;

		this.range.setUpperBound(derive(UPPER_BOUND, cutoff));
		this.indexRange.set(getTimeSequence(), range);

		fireDataChanged( new DataEvent(TRUNCATED, this) );
	}

	/**
	 * Sets a new {@code Metadata} object for this {@code ExperimentalData}.
	 * <p>
	 * The {@code pulseWidth} property recorded in {@code Metadata} will be used to
	 * set the time range for the reverse problem solution. Whenever this property
	 * is changed in the {@code metadata}, a listener will ensure an updated range is used.
	 * </p>
	 * 
	 * @param metadata the new Metadata object
	 * @see PropertyHolderListener
	 */

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
		metadata.setParent(this);
		doSetMetadata();
	}
	
	private void doSetMetadata() {

		if (range != null)
			range.updateMinimum(metadata.numericProperty(PULSE_WIDTH));

		metadata.addListener(event -> {

			if (event.getProperty() instanceof NumericProperty) {
				var p = (NumericProperty) event.getProperty();

				if (p.getType() == PULSE_WIDTH)
					range.updateMinimum(metadata.numericProperty(PULSE_WIDTH));

			}

		});
		
	}
	
	/**
	 * Gets the time sequence element corresponding to the lower bound of the index range
	 * @return the time (in seconds) associated with {@code indexRange.getLowerBound()}
	 */

	public double getEffectiveStartTime() {
		return getTimeSequence().get(indexRange.getLowerBound());
	}
	
	/**
	 * Gets the time sequence element corresponding to the upper bound of the index range
	 * @return the time (in seconds) associated with {@code indexRange.getUpperBound()}
	 */

	public double getEffectiveEndTime() {
		return getTimeSequence().get(indexRange.getUpperBound());
	}
	
	/**
	 * Gets the dimensional time {@code Range} of this data.
	 * @return the range
	 */

	public Range getRange() {
		return range;
	}
	
	/**
	 * Gets the index range of this data.
	 * @return the index range
	 */

	public IndexRange getIndexRange() {
		return indexRange;
	}

	/**
	 * Sets the range, assigning {@code this} to its parent, and forcing changes to the {@code indexRange}.
	 * @param range the range
	 */
	
	public void setRange(Range range) {
		this.range = range;
		range.setParent(this);
		doSetRange();
	}
	
	private void doSetRange() {
		var time = getTimeSequence();
		indexRange.set(time, range);

		addHierarchyListener(l -> {
			if (l.getSource() == range)
				indexRange.set(time, range);
		});
		
		if(metadata != null)
			range.updateMinimum(metadata.numericProperty(PULSE_WIDTH));
	}

}